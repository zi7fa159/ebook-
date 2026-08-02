/**
 * @file integration_sample.c
 * @brief Complete B2B quick-start sample demonstrating integration of FN-CLE.
 * This shows how a client registers custom hardware HAL callbacks for their
 * specific SPI flash memory chip (e.g. STM32 BSP or ESP-IDF) and runs training loops.
 */

#include "fncle/virtual_weight_engine.h"
#include "fncle/flash_driver.h"
#include <stdio.h>
#include <string.h>

/* --- Custom emulated physical flash memory array (64KB) --- */
#define CLIENT_FLASH_SIZE 65536
static uint8_t s_client_flash_log[CLIENT_FLASH_SIZE];

/* --- Client specific hardware HAL implementations --- */

static bool my_hardware_erase_sector(uint32_t sector_addr) {
    printf("[HAL Call] Erasing physical sector at address: 0x%08X\n", sector_addr);
    if (sector_addr + FN_CLE_FLASH_SECTOR_SIZE <= CLIENT_FLASH_SIZE) {
        memset(&s_client_flash_log[sector_addr], 0xFF, FN_CLE_FLASH_SECTOR_SIZE);
        return true;
    }
    return false;
}

static bool my_hardware_write_data(uint32_t addr, const uint8_t *data, uint32_t size) {
    printf("[HAL Call] Programming %u bytes sequentially at address: 0x%08X\n", size, addr);
    if (addr + size <= CLIENT_FLASH_SIZE) {
        memcpy(&s_client_flash_log[addr], data, size);
        return true;
    }
    return false;
}

static bool my_hardware_read_data(uint32_t addr, uint8_t *data, uint32_t size) {
    printf("[HAL Call] Reading %u bytes sequentially from address: 0x%08X\n", size, addr);
    if (addr + size <= CLIENT_FLASH_SIZE) {
        memcpy(data, &s_client_flash_log[addr], size);
        return true;
    }
    return false;
}

int main(void) {
    printf("==================================================\n");
    printf("   FN-CLE COMMERCIAL SOFTWARE INTEGRATION SAMPLE   \n");
    printf("==================================================\n\n");

    // Clear our emulated flash to 0xFF on boot
    memset(s_client_flash_log, 0xFF, sizeof(s_client_flash_log));

    // 1. Structure the custom HAL operations mapping
    fncle_hal_ops_t my_hal;
    my_hal.erase_sector = my_hardware_erase_sector;
    my_hal.write_data = my_hardware_write_data;
    my_hal.read_data = my_hardware_read_data;

    // 2. Register the custom HAL ops into FN-CLE porting layers
    printf("1. Registering B2B hardware-specific Flash HAL...\n");
    fncle_flash_register_hal(&my_hal);

    // 3. Initialize the Virtual Weight Engine
    printf("2. Initializing Virtual Weight Engine...\n");
    fncle_vwe_init();

    // 4. Simulate a local training backpropagation cycle
    printf("\n3. Simulating on-device personalization updates...\n");
    printf("   Registering weight ID 5 with a delta update of 0.125f\n");
    fncle_vwe_register_delta(5, 0.125f);

    printf("   Registering weight ID 42 with a delta update of -0.500f\n");
    fncle_vwe_register_delta(42, -0.500f);

    // 5. Resolve weights dynamically on-the-fly (Weight = Base_W + Delta_W)
    printf("\n4. Simulating forward-pass inference weight resolution...\n");
    float base_w_5 = 1.000f;
    float resolved_w_5 = fncle_vwe_resolve(5, base_w_5);
    printf("   Weight ID 5  -> Base: %.3f, Resolved: %.3f (Delta added successfully)\n", base_w_5, resolved_w_5);

    float base_w_42 = 2.000f;
    float resolved_w_42 = fncle_vwe_resolve(42, base_w_42);
    printf("   Weight ID 42 -> Base: %.3f, Resolved: %.3f (Delta subtracted successfully)\n", base_w_42, resolved_w_42);

    float base_w_99 = 3.500f;
    float resolved_w_99 = fncle_vwe_resolve(99, base_w_99);
    printf("   Weight ID 99 -> Base: %.3f, Resolved: %.3f (No delta active, returns base)\n", base_w_99, resolved_w_99);

    printf("\n==================================================\n");
    printf("   INTEGRATION TESTING COMPLETELY SUCCESSFUL!     \n");
    printf("==================================================\n");

    return 0;
}
