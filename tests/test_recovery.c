#include "fncle/virtual_weight_engine.h"
#include "fncle/flash_driver.h"
#include <stdio.h>
#include <assert.h>
#include <string.h>

void reset_flash(void) {
    for (uint32_t addr = 0; addr < FN_CLE_LOG_PARTITION_SIZE; addr += FN_CLE_FLASH_SECTOR_SIZE) {
        fncle_flash_erase_sector(addr);
    }
}

void test_reboot_recovery(void) {
    printf("[TEST] Running Reboot Recovery Test...\n");
    reset_flash();

    // Initialize the engine (it will find empty flash and start at 0)
    fncle_vwe_init();

    // Register deltas for weight 10, 20, 30
    assert(fncle_vwe_register_delta(10, 0.123f));
    assert(fncle_vwe_register_delta(20, -0.456f));
    assert(fncle_vwe_register_delta(30, 0.789f));

    // Verify weights resolved correctly before reboot
    assert(fncle_vwe_resolve(10, 1.0f) == 1.123f);
    assert(fncle_vwe_resolve(20, 1.0f) == 0.544f);
    assert(fncle_vwe_resolve(30, 1.0f) == 1.789f);

    // Simulate a "reboot" by calling fncle_vwe_init() again on the same flash
    fncle_vwe_init();

    // Verify the deltas were successfully recovered from flash log partition!
    assert(fncle_vwe_resolve(10, 1.0f) == 1.123f);
    assert(fncle_vwe_resolve(20, 1.0f) == 0.544f);
    assert(fncle_vwe_resolve(30, 1.0f) == 1.789f);

    printf("[PASSED] Reboot Recovery Test.\n\n");
}

void test_sector_wraparound_consolidation(void) {
    printf("[TEST] Running Sector Wraparound and Consolidation Test...\n");
    reset_flash();

    // Clean start
    fncle_vwe_init();

    // Let's register some weights.
    // Each log entry is 12 bytes. One sector is 4096 bytes.
    // 4096 / 12 = 341 entries.
    // Let's register 340 unique weights in Sector 0.
    for (uint16_t i = 0; i < 340; i++) {
        assert(fncle_vwe_register_delta(i, (float)i * 0.001f));
    }

    // Now Sector 0 is almost full.
    // Let's verify weight 100 resolves correctly.
    float expected_val = 100.0f * 0.001f;
    assert(fncle_vwe_resolve(100, 0.0f) == expected_val);

    // Adding one more entry should cause Sector 0 offset to exceed boundary.
    assert(fncle_vwe_register_delta(340, 0.340f)); // offset becomes 4092

    // This 342nd write will overflow and force wrap/transition to Sector 1!
    // Since Sector 1 is empty, it will erase Sector 1 and start writing at Sector 1 offset 0.
    assert(fncle_vwe_register_delta(341, 0.341f));

    // Let's verify our wraparound consolidation policy works when we fill the circular partition!
    // Total partition size is 65536 bytes (16 sectors).
    // Let's write until we fill up all 16 sectors and wrap around back to Sector 0.
    reset_flash();
    fncle_vwe_init();

    assert(fncle_vwe_register_delta(5, 1.234f)); // Lives in Sector 0
    assert(fncle_vwe_register_delta(9, 5.678f)); // Lives in Sector 0

    // Fill the rest of the partition
    uint32_t total_writes = 5500;
    for (uint32_t i = 0; i < total_writes; i++) {
        uint16_t w_id = 10 + (i % 400);
        assert(fncle_vwe_register_delta(w_id, (float)i * 0.0001f));
    }

    // At this point, we must have wrapped around.
    // Let's verify that weight 5 and weight 9 did NOT get lost or corrupted!
    assert(fncle_vwe_resolve(5, 0.0f) == 1.234f);
    assert(fncle_vwe_resolve(9, 0.0f) == 5.678f);

    printf("[PASSED] Sector Wraparound and Consolidation Test.\n\n");
}

void test_torn_write_recovery(void) {
    printf("[TEST] Running Torn Write Recovery Test...\n");
    reset_flash();

    // Clean start
    fncle_vwe_init();

    // Register some valid deltas
    assert(fncle_vwe_register_delta(1, 0.1f));
    assert(fncle_vwe_register_delta(2, 0.2f));

    // Write a corrupt / torn entry manually at offset 24 (third entry position)
    uint8_t corrupt_bytes[12] = {0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0xAA, 0xBB, 0xCC}; // Padding != 0 and weight_id invalid!
    assert(fncle_flash_write_data(24, corrupt_bytes, 12));

    // Perform "reboot" recovery scan
    fncle_vwe_init();

    // Recovery should stop exactly at offset 24 because of the corrupt entry.
    // It should successfully recover weight 1 and 2, but NOT crash on the torn write.
    assert(fncle_vwe_resolve(1, 0.0f) == 0.1f);
    assert(fncle_vwe_resolve(2, 0.0f) == 0.2f);

    // And s_current_sector_offset should be reverted to 24, so the next write overwrites the corrupt entry!
    assert(fncle_vwe_register_delta(3, 0.3f));

    // Verify weight 3 resolves correctly
    assert(fncle_vwe_resolve(3, 0.0f) == 0.3f);

    printf("[PASSED] Torn Write Recovery Test.\n\n");
}

int main(void) {
    printf("==================================================\n");
    printf("   FN-CLE ROBUSTNESS AND RECOVERY VERIFICATION    \n");
    printf("==================================================\n\n");

    test_reboot_recovery();
    test_sector_wraparound_consolidation();
    test_torn_write_recovery();

    printf("==================================================\n");
    printf("     ALL UNIT TESTS COMPLETED & PASSED SUCCESSFULLY! \n");
    printf("==================================================\n");
    return 0;
}
