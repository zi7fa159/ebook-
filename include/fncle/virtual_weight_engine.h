/**
 * @file virtual_weight_engine.h
 * @brief Header for the Virtual Weight Resolver in FN-CLE.
 * This file is completely hardware-agnostic and alignment safe.
 */

#ifndef VIRTUAL_WEIGHT_ENGINE_H
#define VIRTUAL_WEIGHT_ENGINE_H

#include <stdint.h>
#include <stdbool.h>

#define MAX_TRAINABLE_WEIGHTS 512

/* Configurable partition bounds */
#ifndef FN_CLE_LOG_PARTITION_SIZE
#define FN_CLE_LOG_PARTITION_SIZE 65536 /**< Default log partition size: 64KB */
#endif

/**
 * @brief Log entry packed format.
 * Size: exactly 12 Bytes.
 * Struct member layout is ordered by descending size to prevent compiler padding
 * across standard 16/32/64-bit platforms.
 */
typedef struct {
    uint32_t sequence_num;   /**< 32-bit Monotonic sequence counter. */
    float delta_value;       /**< 32-bit float parameter update delta. */
    uint16_t weight_id;      /**< 16-bit Logical weight index. */
    uint16_t padding;        /**< 16-bit padding for strict 32-bit alignment. */
} __attribute__((packed)) fncle_log_entry_t;

/**
 * @brief SRAM-resident pointer map structure.
 */
typedef struct {
    uint32_t flash_sector_addr; /**< Physical flash sector address offset. */
    uint16_t sector_offset;     /**< Physical offset within the sector. */
    bool is_active;             /**< Flag indicating if a delta update exists. */
} fncle_sram_entry_t;

/**
 * @brief Initialize the Virtual Weight Engine.
 */
void fncle_vwe_init(void);

/**
 * @brief Register a weight delta update in the log-structured cache.
 */
bool fncle_vwe_register_delta(uint16_t weight_id, float delta_val);

/**
 * @brief Resolve a weight on-the-fly inside Multiply-Accumulate loops.
 */
float fncle_vwe_resolve(uint16_t weight_id, float base_weight);

#endif // VIRTUAL_WEIGHT_ENGINE_H
