/**
 * @file flash_driver.c
 * @brief Hardware-agnostic low-level Flash HAL operations implementation.
 */

#include "fncle/flash_driver.h"
#include <string.h>

static const fncle_hal_ops_t *s_hal_ops = NULL;

// Statically allocated fallback flash memory buffer is wrapped under an emulation flag
// to prevent wasting 64KB of SRAM on memory-constrained production MCU targets!
#if defined(FNCLE_EMULATION) || !defined(__arm__)
#define FALLBACK_FLASH_SIZE 65536
static uint8_t s_fallback_flash[FALLBACK_FLASH_SIZE];
static bool s_fallback_initialized = false;

static void init_fallback(void) {
    if (!s_fallback_initialized) {
        memset(s_fallback_flash, 0xFF, sizeof(s_fallback_flash));
        s_fallback_initialized = true;
    }
}
#endif

void fncle_flash_register_hal(const fncle_hal_ops_t *ops) {
    s_hal_ops = ops;
}

bool fncle_flash_erase_sector(uint32_t sector_addr) {
    if (s_hal_ops && s_hal_ops->erase_sector) {
        return s_hal_ops->erase_sector(sector_addr);
    }

#if defined(FNCLE_EMULATION) || !defined(__arm__)
    init_fallback();
    // Defensive bounds check preventing integer overflows
    if (sector_addr <= FALLBACK_FLASH_SIZE && FN_CLE_FLASH_SECTOR_SIZE <= FALLBACK_FLASH_SIZE - sector_addr) {
        memset(&s_fallback_flash[sector_addr], 0xFF, FN_CLE_FLASH_SECTOR_SIZE);
        return true;
    }
#endif
    return false;
}

bool fncle_flash_write_data(uint32_t addr, const uint8_t *data, uint32_t size) {
    if (s_hal_ops && s_hal_ops->write_data) {
        return s_hal_ops->write_data(addr, data, size);
    }

#if defined(FNCLE_EMULATION) || !defined(__arm__)
    init_fallback();
    // Defensive bounds check preventing integer overflows
    if (addr <= FALLBACK_FLASH_SIZE && size <= FALLBACK_FLASH_SIZE - addr) {
        memcpy(&s_fallback_flash[addr], data, size);
        return true;
    }
#endif
    return false;
}

bool fncle_flash_read_data(uint32_t addr, uint8_t *data, uint32_t size) {
    if (s_hal_ops && s_hal_ops->read_data) {
        return s_hal_ops->read_data(addr, data, size);
    }

#if defined(FNCLE_EMULATION) || !defined(__arm__)
    init_fallback();
    // Defensive bounds check preventing integer overflows
    if (addr <= FALLBACK_FLASH_SIZE && size <= FALLBACK_FLASH_SIZE - addr) {
        memcpy(data, &s_fallback_flash[addr], size);
        return true;
    }
#endif
    return false;
}
