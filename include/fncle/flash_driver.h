/**
 * @file flash_driver.h
 * @brief Hardware-agnostic HAL definition for FN-CLE.
 * This abstracts low-level flash operations so that the core FN-CLE logic
 * works seamlessly across ARM Cortex-M, RISC-V, Xtensa, and AVR.
 */

#ifndef FLASH_DRIVER_H
#define FLASH_DRIVER_H

#include <stdint.h>
#include <stdbool.h>

#define FN_CLE_FLASH_SECTOR_SIZE 4096
#define FN_CLE_FLASH_PAGE_SIZE   256

/**
 * @brief Low-level HAL operations structure representing physical hardware calls.
 */
typedef struct {
    bool (*erase_sector)(uint32_t sector_addr);
    bool (*write_data)(uint32_t addr, const uint8_t *data, uint32_t size);
    bool (*read_data)(uint32_t addr, uint8_t *data, uint32_t size);
} fncle_hal_ops_t;

/**
 * @brief Register the hardware-specific Flash HAL callbacks.
 */
void fncle_flash_register_hal(const fncle_hal_ops_t *ops);

/**
 * @brief Hardware-agnostic sector erase.
 */
bool fncle_flash_erase_sector(uint32_t sector_addr);

/**
 * @brief Hardware-agnostic sequential word/data write.
 */
bool fncle_flash_write_data(uint32_t addr, const uint8_t *data, uint32_t size);

/**
 * @brief Hardware-agnostic sequential data read.
 */
bool fncle_flash_read_data(uint32_t addr, uint8_t *data, uint32_t size);

#endif // FLASH_DRIVER_H
