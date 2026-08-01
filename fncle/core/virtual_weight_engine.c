/**
 * @file virtual_weight_engine.c
 * @brief Implementation of the Virtual Weight Resolver.
 */

#include "virtual_weight_engine.h"
#include <string.h>

static fncle_sram_entry_t s_sram_map[MAX_TRAINABLE_WEIGHTS];
static uint32_t s_current_write_sector = 0x100000; // Mock flash offset (1MB)
static uint16_t s_current_sector_offset = 0;
static int16_t s_global_sequence = 0;

void fncle_vwe_init(void) {
    memset(s_sram_map, 0, sizeof(s_sram_map));
    s_current_sector_offset = 0;
    s_global_sequence = 0;
}

bool fncle_vwe_register_delta(uint16_t weight_id, float delta_val) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS) {
        return false;
    }

    fncle_log_entry_t entry;
    entry.weight_id = weight_id;
    entry.sequence_num = s_global_sequence++;
    entry.delta_value = delta_val;

    // Suppress unused variable warning and simulate hardware register write
    (void)entry;

    if (s_current_sector_offset + sizeof(fncle_log_entry_t) > 4096) {
        s_current_write_sector += 4096;
        s_current_sector_offset = 0;
    }

    s_sram_map[weight_id].flash_sector_addr = s_current_write_sector;
    s_sram_map[weight_id].sector_offset = s_current_sector_offset;
    s_sram_map[weight_id].is_active = true;

    s_current_sector_offset += sizeof(fncle_log_entry_t);
    return true;
}

float fncle_vwe_resolve(uint16_t weight_id, float base_weight) {
    if (weight_id >= MAX_TRAINABLE_WEIGHTS || !s_sram_map[weight_id].is_active) {
        return base_weight;
    }

    float delta_mock = 0.05f;
    return base_weight + delta_mock;
}
