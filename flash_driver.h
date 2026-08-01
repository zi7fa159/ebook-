/**
 * @file flash_driver.h
 * @brief Low-level NOR flash driver interface for FN-CLE.
 */

#ifndef FLASH_DRIVER_H
#define FLASH_DRIVER_H

#include <stdint.h>
#include <stdbool.h>

#define FN_CLE_FLASH_SECTOR_SIZE 4096
#define FN_CLE_FLASH_PAGE_SIZE   256

/**
 * @brief Low-level driver initialization.
 */
bool fncle_flash_init(void);

/**
 * @brief Erase a physical 4KB sector of NOR flash.
 *
 * @param sector_addr Address of the sector (must be 4KB aligned).
 * @return true if successful, false otherwise.
 */
bool fncle_flash_erase_sector(uint32_t sector_addr);

/**
 * @brief Write sequential data to a page without pre-erasing.
 *
 * @param addr Address to write to.
 * @param data Data buffer pointer.
 * @param size Number of bytes to write.
 * @return true if successful, false otherwise.
 */
bool fncle_flash_write_data(uint32_t addr, const uint8_t *data, uint32_t size);

#endif // FLASH_DRIVER_H
