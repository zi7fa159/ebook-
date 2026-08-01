/**
 * @file flash_driver.c
 * @brief Hardware-agnostic low-level Flash HAL operations implementation.
 */

#include "flash_driver.h"
#include <string.h>

static const fncle_hal_ops_t *s_hal_ops = NULL;

// Fallback internal memory buffer to emulate flash when no hardware HAL is registered
#define FALLBACK_FLASH_SIZE 65536
static uint8_t s_fallback_flash[FALLBACK_FLASH_SIZE];
static bool s_fallback_initialized = false;

static void init_fallback(void) {
    if (!s_fallback_initialized) {
        memset(s_fallback_flash, 0xFF, sizeof(s_fallback_flash));
        s_fallback_initialized = true;
    }
}

void fncle_flash_register_hal(const fncle_hal_ops_t *ops) {
    s_hal_ops = ops;
}

bool fncle_flash_erase_sector(uint32_t sector_addr) {
    if (s_hal_ops && s_hal_ops->erase_sector) {
        return s_hal_ops->erase_sector(sector_addr);
    }

    // Fallback Mock Erase (revert to 0xFF)
    init_fallback();
    if (sector_addr + FN_CLE_FLASH_SECTOR_SIZE <= FALLBACK_FLASH_SIZE) {
        memset(&s_fallback_flash[sector_addr], 0xFF, FN_CLE_FLASH_SECTOR_SIZE);
        return true;
    }
    return false;
}

bool fncle_flash_write_data(uint32_t addr, const uint8_t *data, uint32_t size) {
    if (s_hal_ops && s_hal_ops->write_data) {
        return s_hal_ops->write_data(addr, data, size);
    }

    // Fallback Mock Program
    init_fallback();
    if (addr + size <= FALLBACK_FLASH_SIZE) {
        memcpy(&s_fallback_flash[addr], data, size);
        return true;
    }
    return false;
}

bool fncle_flash_read_data(uint32_t addr, uint8_t *data, uint32_t size) {
    if (s_hal_ops && s_hal_ops->read_data) {
        return s_hal_ops->read_data(addr, data, size);
    }

    // Fallback Mock Read
    init_fallback();
    if (addr + size <= FALLBACK_FLASH_SIZE) {
        memcpy(data, &s_fallback_flash[addr], size);
        return true;
    }
    return false;
}
