/**
 * @file virtual_weight_engine.c
 * @brief Portable implementation of the Virtual Weight Resolver with standard alignment and endianness handling.
 */

#include "fncle/virtual_weight_engine.h"
#include "fncle/flash_driver.h"
#include <string.h>

static fncle_sram_entry_t s_sram_map[MAX_TRAINABLE_WEIGHTS];

static uint32_t s_current_write_sector_base = 0;
static uint16_t s_current_sector_offset = 0;
static uint32_t s_global_sequence = 0;

/* Robust target-specific critical section blocks for interrupt disablement */
#if defined(__arm__) || defined(__thumb__)
#define PORTABLE_ENTER_CRITICAL() __asm volatile("cpsid i" : : : "memory")
#define PORTABLE_EXIT_CRITICAL()  __asm volatile("cpsie i" : : : "memory")
#elif defined(ESP_PLATFORM)
// ESP-IDF specific porting lock calls
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#define PORTABLE_ENTER_CRITICAL() taskENTER_CRITICAL()
#define PORTABLE_EXIT_CRITICAL()  taskEXIT_CRITICAL()
#else
// Fallback compiler optimization fence
#define PORTABLE_ENTER_CRITICAL() __asm volatile("" : : : "memory")
#define PORTABLE_EXIT_CRITICAL()  __asm volatile("" : : : "memory")
#endif

/* Portable Endianness utilities */
static inline uint16_t swap_uint16(uint16_t val) {
    return (val << 8) | (val >> 8);
}

static inline uint32_t swap_uint32(uint32_t val) {
    val = ((val << 8) & 0xFF00FF00) | ((val >> 8) & 0xFF00FF);
    return (val << 16) | (val >> 16);
}

static inline bool is_little_endian(void) {
    volatile uint16_t test = 0x0100;
    return *((volatile uint8_t*)&test) == 0;
}

/* Serialize log entry into endian-independent byte buffer */
static void serialize_log_entry(const fncle_log_entry_t *entry, uint8_t *buf) {
    uint32_t seq = entry->sequence_num;
    uint16_t w_id = entry->weight_id;
    float delta = entry->delta_value;
    uint32_t delta_raw;

    // Copy float bit pattern to uint32
    memcpy(&delta_raw, &delta, 4);

    if (!is_little_endian()) {
        seq = swap_uint32(seq);
        w_id = swap_uint16(w_id);
        delta_raw = swap_uint32(delta_raw);
    }

    // Pack into buffer
    memcpy(buf, &seq, 4);
    memcpy(buf + 4, &delta_raw, 4);
    memcpy(buf + 8, &w_id, 2);
    buf[10] = 0; // padding byte 1
    buf[11] = 0; // padding byte 2
}

/* Deserialize log entry from endian-independent byte buffer */
static void deserialize_log_entry(const uint8_t *buf, fncle_log_entry_t *entry) {
    uint32_t seq;
    uint32_t delta_raw;
    uint16_t w_id;

    memcpy(&seq, buf, 4);
    memcpy(&delta_raw, buf + 4, 4);
    memcpy(&w_id, buf + 8, 2);

    if (!is_little_endian()) {
        seq = swap_uint32(seq);
        w_id = swap_uint16(w_id);
        delta_raw = swap_uint32(delta_raw);
    }

    entry->sequence_num = seq;
    entry->weight_id = w_id;
    entry->padding = 0;
    memcpy(&(entry->delta_value), &delta_raw, 4);
}

void fncle_vwe_init(void) {
    PORTABLE_ENTER_CRITICAL();
    memset(s_sram_map, 0, sizeof(s_sram_map));
    s_current_write_sector_base = 0;
    s_current_sector_offset = 0;
    s_global_sequence = 0;

    // Walk the log partition to recover existing s_sram_map from flash
    uint32_t addr = 0;
    uint32_t max_seq = 0;
    bool found_any = false;
    uint32_t next_write_addr = 0;
    uint8_t entry_buf[sizeof(fncle_log_entry_t)];

    while (addr + sizeof(fncle_log_entry_t) <= FN_CLE_LOG_PARTITION_SIZE) {
        if (!fncle_flash_read_data(addr, entry_buf, sizeof(fncle_log_entry_t))) {
            break;
        }

        // Stop scanning at the first unwritten/empty record (all 0xFF)
        bool is_empty = true;
        for (size_t i = 0; i < sizeof(fncle_log_entry_t); ++i) {
            if (entry_buf[i] != 0xFF) {
                is_empty = false;
                break;
            }
        }
        if (is_empty) {
            break;
        }

        fncle_log_entry_t entry;
        deserialize_log_entry(entry_buf, &entry);

        // Stop scanning on invalid records (e.g. corrupt or torn writes)
        if (entry.weight_id >= MAX_TRAINABLE_WEIGHTS || entry.padding != 0) {
            break;
        }

        // Recover entry: keep the mapped entry with the highest sequence_num
        bool should_update = false;
        if (!s_sram_map[entry.weight_id].is_active) {
            should_update = true;
        } else {
            uint8_t temp_buf[sizeof(fncle_log_entry_t)];
            uint32_t active_addr = s_sram_map[entry.weight_id].flash_sector_addr + s_sram_map[entry.weight_id].sector_offset;
            if (fncle_flash_read_data(active_addr, temp_buf, sizeof(fncle_log_entry_t))) {
                fncle_log_entry_t existing_entry;
                deserialize_log_entry(temp_buf, &existing_entry);
                if (entry.sequence_num > existing_entry.sequence_num) {
                    should_update = true;
                }
            } else {
                should_update = true;
            }
        }

        if (should_update) {
            s_sram_map[entry.weight_id].flash_sector_addr = (addr / FN_CLE_FLASH_SECTOR_SIZE) * FN_CLE_FLASH_SECTOR_SIZE;
            s_sram_map[entry.weight_id].sector_offset = addr % FN_CLE_FLASH_SECTOR_SIZE;
            s_sram_map[entry.weight_id].is_active = true;
        }

        if (!found_any || entry.sequence_num > max_seq) {
            max_seq = entry.sequence_num;
            next_write_addr = addr + sizeof(fncle_log_entry_t);
            found_any = true;
        }

        addr += sizeof(fncle_log_entry_t);
    }

    if (found_any) {
        s_global_sequence = max_seq + 1;
        if (next_write_addr >= FN_CLE_LOG_PARTITION_SIZE) {
            next_write_addr = 0;
        }
        s_current_write_sector_base = (next_write_addr / FN_CLE_FLASH_SECTOR_SIZE) * FN_CLE_FLASH_SECTOR_SIZE;
        s_current_sector_offset = next_write_addr % FN_CLE_FLASH_SECTOR_SIZE;
    } else {
        s_global_sequence = 0;
        s_current_write_sector_base = 0;
        s_current_sector_offset = 0;
        // Erase sector 0 to prepare for sequential writes on first boot
        fncle_flash_erase_sector(0);
    }
    PORTABLE_EXIT_CRITICAL();
}

bool fncle_vwe_register_delta(uint16_t weight_id, float delta_val) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS) {
        return false;
    }

    uint32_t local_seq;
    PORTABLE_ENTER_CRITICAL();
    local_seq = s_global_sequence++;
    PORTABLE_EXIT_CRITICAL();

    fncle_log_entry_t entry;
    entry.sequence_num = local_seq;
    entry.delta_value = delta_val;
    entry.weight_id = weight_id;
    entry.padding = 0;

    uint8_t packed_buf[sizeof(fncle_log_entry_t)];
    serialize_log_entry(&entry, packed_buf);

    PORTABLE_ENTER_CRITICAL();
    // Check sector boundaries (NOR sector typically is 4KB)
    if (s_current_sector_offset + sizeof(fncle_log_entry_t) > FN_CLE_FLASH_SECTOR_SIZE) {
        uint32_t next_sector_base = s_current_write_sector_base + FN_CLE_FLASH_SECTOR_SIZE;

        // Circular buffer safety wrapping
        if (next_sector_base + FN_CLE_FLASH_SECTOR_SIZE > FN_CLE_LOG_PARTITION_SIZE) {
            next_sector_base = 0;
        }

        fncle_log_entry_t live_entries[MAX_TRAINABLE_WEIGHTS];
        uint16_t live_count = 0;

        for (uint16_t i = 0; i < MAX_TRAINABLE_WEIGHTS; i++) {
            if (s_sram_map[i].is_active && s_sram_map[i].flash_sector_addr == next_sector_base) {
                uint8_t temp_buf[sizeof(fncle_log_entry_t)];
                uint32_t active_addr = s_sram_map[i].flash_sector_addr + s_sram_map[i].sector_offset;
                if (fncle_flash_read_data(active_addr, temp_buf, sizeof(fncle_log_entry_t))) {
                    deserialize_log_entry(temp_buf, &live_entries[live_count]);
                    live_count++;
                }
            }
        }

        // Now safely erase the target physical sector before sequential programming starts
        if (!fncle_flash_erase_sector(next_sector_base)) {
            PORTABLE_EXIT_CRITICAL();
            return false;
        }

        s_current_write_sector_base = next_sector_base;
        s_current_sector_offset = 0;

        // Re-write consolidated live deltas to the fresh sector
        for (uint16_t i = 0; i < live_count; i++) {
            uint8_t cons_packed[sizeof(fncle_log_entry_t)];
            live_entries[i].sequence_num = s_global_sequence++;
            serialize_log_entry(&live_entries[i], cons_packed);

            uint32_t target_addr = s_current_write_sector_base + s_current_sector_offset;
            fncle_flash_write_data(target_addr, cons_packed, sizeof(fncle_log_entry_t));

            s_sram_map[live_entries[i].weight_id].flash_sector_addr = s_current_write_sector_base;
            s_sram_map[live_entries[i].weight_id].sector_offset = s_current_sector_offset;
            s_sram_map[live_entries[i].weight_id].is_active = true;

            s_current_sector_offset += sizeof(fncle_log_entry_t);
        }
    }

    uint32_t target_addr = s_current_write_sector_base + s_current_sector_offset;
    PORTABLE_EXIT_CRITICAL();

    // Write via hardware-agnostic HAL call
    if (!fncle_flash_write_data(target_addr, packed_buf, sizeof(fncle_log_entry_t))) {
        return false;
    }

    // Update active map cleanly under critical section
    PORTABLE_ENTER_CRITICAL();
    s_sram_map[weight_id].flash_sector_addr = s_current_write_sector_base;
    s_sram_map[weight_id].sector_offset = s_current_sector_offset;
    s_sram_map[weight_id].is_active = true;
    s_current_sector_offset += sizeof(fncle_log_entry_t);
    PORTABLE_EXIT_CRITICAL();

    return true;
}

float fncle_vwe_resolve(uint16_t weight_id, float base_weight) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS || !s_sram_map[weight_id].is_active) {
        return base_weight;
    }

    PORTABLE_ENTER_CRITICAL();
    uint32_t sector_addr = s_sram_map[weight_id].flash_sector_addr;
    uint16_t offset = s_sram_map[weight_id].sector_offset;
    PORTABLE_EXIT_CRITICAL();

    uint8_t packed_buf[sizeof(fncle_log_entry_t)];

    // Read via hardware-agnostic HAL call
    if (!fncle_flash_read_data(sector_addr + offset, packed_buf, sizeof(fncle_log_entry_t))) {
        return base_weight;
    }

    fncle_log_entry_t entry;
    deserialize_log_entry(packed_buf, &entry);

    return base_weight + entry.delta_value;
}
