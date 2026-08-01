/**
 * @file flash_driver.c
 * @brief Low-level NOR flash driver mock and hardware abstraction.
 */

#include "flash_driver.h"
#include <string.h>

static bool s_driver_initialized = false;

bool fncle_flash_init(void) {
    s_driver_initialized = true;
    return true;
}

bool fncle_flash_erase_sector(uint32_t sector_addr) {
    if (!s_driver_initialized || (sector_addr % FN_CLE_FLASH_SECTOR_SIZE) != 0) {
        return false;
    }
    (void)sector_addr;
    return true;
}

bool fncle_flash_write_data(uint32_t addr, const uint8_t *data, uint32_t size) {
    if (!s_driver_initialized || data == NULL || size == 0) {
        return false;
    }
    (void)addr;
    (void)data;
    (void)size;
    return true;
}
