/**
 * @file virtual_weight_engine.c
 * @brief Implementation of the Virtual Weight Resolver with real pointer-mapping and flash byte emulation.
 */

#include "virtual_weight_engine.h"
#include <string.h>

static fncle_sram_entry_t s_sram_map[MAX_TRAINABLE_WEIGHTS];

// Emulate a real physical 64KB external flash partition (log-structured memory)
#define EMULATED_FLASH_SIZE 65536
static uint8_t s_physical_flash_log[EMULATED_FLASH_SIZE];

static uint32_t s_current_write_sector_base = 0; // Starts at sector 0 of our partition
static uint16_t s_current_sector_offset = 0;
static uint32_t s_global_sequence = 0;

// Thread safety state simulation (Critical Sections / FreeRTOS Locks)
static volatile bool s_critical_section_active = false;

static void mock_enter_critical(void) {
    s_critical_section_active = true;
}

static void mock_exit_critical(void) {
    s_critical_section_active = false;
}

void fncle_vwe_init(void) {
    mock_enter_critical();
    memset(s_sram_map, 0, sizeof(s_sram_map));
    memset(s_physical_flash_log, 0xFF, sizeof(s_physical_flash_log)); // Erased flash is filled with 0xFF
    s_current_write_sector_base = 0;
    s_current_sector_offset = 0;
    s_global_sequence = 0;
    mock_exit_critical();
}

bool fncle_vwe_register_delta(uint16_t weight_id, float delta_val) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS) {
        return false;
    }

    fncle_log_entry_t entry;
    entry.weight_id = weight_id;
    entry.padding = 0;
    entry.sequence_num = s_global_sequence++;
    entry.delta_value = delta_val;

    // Simulate formatting log entry and checking page limits (sector size = 4KB)
    if (s_current_sector_offset + sizeof(fncle_log_entry_t) > 4096) {
        // Increment base sector offset (move to next physical 4KB sector)
        s_current_write_sector_base += 4096;
        s_current_sector_offset = 0;

        // Wrap-around circular log safety checks
        if (s_current_write_sector_base + 4096 > EMULATED_FLASH_SIZE) {
            s_current_write_sector_base = 0;
        }
    }

    // Calculate physical memory address inside our flash log
    uint32_t physical_address = s_current_write_sector_base + s_current_sector_offset;

    // Perform actual write into the emulated flash storage
    memcpy(&s_physical_flash_log[physical_address], &entry, sizeof(fncle_log_entry_t));

    // Update SRAM active map with proper thread-locking validation
    mock_enter_critical();
    s_sram_map[weight_id].flash_sector_addr = s_current_write_sector_base;
    s_sram_map[weight_id].sector_offset = s_current_sector_offset;
    s_sram_map[weight_id].is_active = true;
    mock_exit_critical();

    s_current_sector_offset += sizeof(fncle_log_entry_t);
    return true;
}

float fncle_vwe_resolve(uint16_t weight_id, float base_weight) {
    // Zero-overhead lookup
    if (weight_id >= MAX_TRAINABLE_WEIGHTS || !s_sram_map[weight_id].is_active) {
        return base_weight;
    }

    // Thread safe check
    mock_enter_critical();
    uint32_t base_addr = s_sram_map[weight_id].flash_sector_addr;
    uint16_t offset = s_sram_map[weight_id].sector_offset;
    mock_exit_critical();

    uint32_t physical_address = base_addr + offset;

    // Read and resolve directly from our log-structured flash storage
    fncle_log_entry_t *entry_ptr = (fncle_log_entry_t *)(&s_physical_flash_log[physical_address]);

    // Reconstruct Weight on the fly: Base + Delta
    return base_weight + entry_ptr->delta_value;
}
