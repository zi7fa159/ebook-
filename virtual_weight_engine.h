/**
 * @file virtual_weight_engine.h
 * @brief Header for the Virtual Weight Resolver in FN-CLE.
 * Designed for ESP32-S3 and STM32H7.
 */

#ifndef VIRTUAL_WEIGHT_ENGINE_H
#define VIRTUAL_WEIGHT_ENGINE_H

#include <stdint.h>
#include <stdbool.h>

#define MAX_TRAINABLE_WEIGHTS 512

/**
 * @brief Structure representing a log-structured delta update entry in flash memory.
 * Size: 8 Bytes.
 */
typedef struct {
    uint16_t weight_id;      /**< Logical index of the parameter weight. */
    uint16_t padding;        /**< 16-bit padding for perfect alignment. */
    uint32_t sequence_num;   /**< 32-bit Sequence counter to prevent counter overflow. */
    float delta_value;       /**< Floating point parameter update offset (delta). */
} __attribute__((packed)) fncle_log_entry_t;

/**
 * @brief Structure representing the SRAM weight index map.
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
 * @brief Resolve a weight on-the-fly.
 *
 * @param weight_id The logical parameter index.
 * @param base_weight The initial read-only factory parameter.
 * @return Resolved floating point weight (Base + Delta).
 */
float fncle_vwe_resolve(uint16_t weight_id, float base_weight);

#endif // VIRTUAL_WEIGHT_ENGINE_H
