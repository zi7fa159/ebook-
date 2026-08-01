/**
 * @file virtual_weight_engine.c
 * @brief Portable implementation of the Virtual Weight Resolver with standard alignment and endianness handling.
 */

#include "virtual_weight_engine.h"
#include "flash_driver.h"
#include <string.h>

static fncle_sram_entry_t s_sram_map[MAX_TRAINABLE_WEIGHTS];

static uint32_t s_current_write_sector_base = 0;
static uint16_t s_current_sector_offset = 0;
static uint32_t s_global_sequence = 0;

static volatile bool s_critical_section_active = false;

static void portable_enter_critical(void) {
    s_critical_section_active = true;
}

static void portable_exit_critical(void) {
    s_critical_section_active = false;
}

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
    portable_enter_critical();
    memset(s_sram_map, 0, sizeof(s_sram_map));
    s_current_write_sector_base = 0;
    s_current_sector_offset = 0;
    s_global_sequence = 0;
    portable_exit_critical();
}

bool fncle_vwe_register_delta(uint16_t weight_id, float delta_val) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS) {
        return false;
    }

    fncle_log_entry_t entry;
    entry.sequence_num = s_global_sequence++;
    entry.delta_value = delta_val;
    entry.weight_id = weight_id;
    entry.padding = 0;

    uint8_t packed_buf[sizeof(fncle_log_entry_t)];
    serialize_log_entry(&entry, packed_buf);

    // Check sector boundaries (NOR sector typically is 4KB)
    if (s_current_sector_offset + sizeof(fncle_log_entry_t) > FN_CLE_FLASH_SECTOR_SIZE) {
        s_current_write_sector_base += FN_CLE_FLASH_SECTOR_SIZE;
        s_current_sector_offset = 0;

        // Circular buffer safety wrapping (e.g. 64KB log size)
        if (s_current_write_sector_base + FN_CLE_FLASH_SECTOR_SIZE > 65536) {
            s_current_write_sector_base = 0;
        }
    }

    uint32_t target_addr = s_current_write_sector_base + s_current_sector_offset;

    // Write via hardware-agnostic HAL call
    if (!fncle_flash_write_data(target_addr, packed_buf, sizeof(fncle_log_entry_t))) {
        return false;
    }

    // Update active map cleanly
    portable_enter_critical();
    s_sram_map[weight_id].flash_sector_addr = s_current_write_sector_base;
    s_sram_map[weight_id].sector_offset = s_current_sector_offset;
    s_sram_map[weight_id].is_active = true;
    portable_exit_critical();

    s_current_sector_offset += sizeof(fncle_log_entry_t);
    return true;
}

float fncle_vwe_resolve(uint16_t weight_id, float base_weight) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS || !s_sram_map[weight_id].is_active) {
        return base_weight;
    }

    portable_enter_critical();
    uint32_t sector_addr = s_sram_map[weight_id].flash_sector_addr;
    uint16_t offset = s_sram_map[weight_id].sector_offset;
    portable_exit_critical();

    uint8_t packed_buf[sizeof(fncle_log_entry_t)];

    // Read via hardware-agnostic HAL call
    if (!fncle_flash_read_data(sector_addr + offset, packed_buf, sizeof(fncle_log_entry_t))) {
        return base_weight;
    }

    fncle_log_entry_t entry;
    deserialize_log_entry(packed_buf, &entry);

    return base_weight + entry.delta_value;
}
