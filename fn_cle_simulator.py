# fn_cle_simulator.py
import random
import time
import json

class NOREmulator:
    def __init__(self, capacity_kb=8192, sector_size_kb=4, page_size_bytes=256, endurance_limit=10000):
        self.capacity_bytes = capacity_kb * 1024
        self.sector_size = sector_size_kb * 1024
        self.page_size = page_size_bytes
        self.endurance_limit = endurance_limit
        self.num_sectors = self.capacity_bytes // self.sector_size

        # Performance/Wear stats
        self.sectors_erase_count = [0] * self.num_sectors
        self.bytes_written = 0
        self.write_operations = 0
        self.erase_operations = 0
        self.pages_programmed = 0

        # Emulated Timings (realistic high-performance NOR flash)
        self.t_page_prog_ms = 0.8  # 800 microseconds to program a 256-byte page
        self.t_sector_erase_ms = 100.0  # 100 milliseconds to erase a 4KB sector
        self.t_word_write_ms = 0.05  # 50 microseconds for single 32-bit word program (if supported)
        self.t_read_word_us = 0.12  # 120 nanoseconds to read a 32-bit word (typical random SPI flash read)

class SystemABaselineSimulator:
    """System A: Traditional on-device learning by updating weight parameters in-place inside flash sectors."""
    def __init__(self, flash: NOREmulator, num_weights=500):
        self.flash = flash
        self.num_weights = num_weights
        self.weight_sector_index = 0  # Weights stored starting in sector 0

    def perform_epochs_training(self, num_updates=1000):
        # In a traditional system, updating weights requires writing to flash in-place.
        # Since NOR Flash cannot overwrite bits from 0 to 1, we must:
        # 1. Read existing sector to SRAM
        # 2. Erase the sector (which takes ~100ms and resets all bits to 1)
        # 3. Modify the weights in SRAM
        # 4. Write back the updated page/sector (takes ~page_prog_ms)

        for _ in range(num_updates):
            # Read sector
            _ = self.flash.sector_size // 4  # words read
            # Erase sector
            self.flash.sectors_erase_count[self.weight_sector_index] += 1
            self.flash.erase_operations += 1
            # Write back (assuming weights fit in 1 sector, which is 4KB / 4 = 1000 float32 weights)
            pages_needed = (self.num_weights * 4 + self.flash.page_size - 1) // self.flash.page_size
            self.flash.bytes_written += self.num_weights * 4
            self.flash.pages_programmed += pages_needed
            self.flash.write_operations += 1

class SystemBFNCLESimulator:
    """System B: FN-CLE with Virtual Weight Resolution and Log-Structured Storage."""
    def __init__(self, flash: NOREmulator, num_weights=500, log_start_sector=10, log_end_sector=20):
        self.flash = flash
        self.num_weights = num_weights
        self.log_start_sector = log_start_sector
        self.log_end_sector = log_end_sector
        self.log_size_sectors = log_end_sector - log_start_sector + 1

        # State
        self.current_sector = log_start_sector
        self.current_offset_in_sector = 0
        self.sram_lookup_table = {}  # logical weight index -> (physical_flash_sector, offset_in_sector)
        self.power_failure_simulated = False

    def perform_weight_update(self, weight_index, delta_val):
        # In FN-CLE, we write a 64-bit log entry: (weight_index: 16-bit, delta: 32-bit float, sequence: 16-bit)
        # Size = 8 bytes per update. No sector erase is required!
        entry_size = 8

        # Check if current sector is full
        if self.current_offset_in_sector + entry_size > self.flash.sector_size:
            # Move to next sector in our log partition
            self.current_sector += 1
            self.current_offset_in_sector = 0
            if self.current_sector > self.log_end_sector:
                # Trigger background garbage collection/consolidation
                self.trigger_garbage_collection()
                self.current_sector = self.log_start_sector
                self.current_offset_in_sector = 0

        # Write 8-byte entry sequentially to active log sector
        self.flash.bytes_written += entry_size
        self.flash.write_operations += 1

        # Program pages stat: each write occupies a fraction of a page
        # If we assume 256-byte page buffering in the driver:
        # We only program a page when full, or on transaction commits.
        # Let's count page programs:
        self.current_offset_in_sector += entry_size
        if self.current_offset_in_sector % self.flash.page_size == 0:
            self.flash.pages_programmed += 1

        # Update volatile SRAM lookup index
        self.sram_lookup_table[weight_index] = (self.current_sector, self.current_offset_in_sector - entry_size)

    def trigger_garbage_collection(self):
        # GC reads all active deltas from the circular log, consolidates them with static weights
        # to a new base sector, and erases all log sectors.
        # This incurs a predictable erase penalty on the log sectors, but is done infrequently.
        for sector in range(self.log_start_sector, self.log_end_sector + 1):
            self.flash.sectors_erase_count[sector] += 1
            self.flash.erase_operations += 1
        self.sram_lookup_table.clear()

    def simulate_power_loss_recovery(self):
        # Recovery scans the circular log sequentially to rebuild the SRAM lookup table
        # SCAN time: proportional to active log size
        replayed_records = 0
        scan_bytes = 0
        for sector in range(self.log_start_sector, self.current_sector + 1):
            limit = self.current_offset_in_sector if sector == self.current_sector else self.flash.sector_size
            scan_bytes += limit
            replayed_records += limit // 8

        # Estimate scan time (120ns per read word typical SPI)
        scan_time_us = (scan_bytes / 4) * self.flash.t_read_word_us
        return scan_time_us, replayed_records

def run_simulation():
    print("--- Running High-Fidelity NOR Flash Simulation ---")

    # 1. System A Baseline
    flash_a = NOREmulator()
    sim_a = SystemABaselineSimulator(flash_a, num_weights=500)
    sim_a.perform_epochs_training(num_updates=1500)

    # 2. System B FN-CLE
    flash_b = NOREmulator()
    sim_b = SystemBFNCLESimulator(flash_b, num_weights=500, log_start_sector=10, log_end_sector=20)

    # Perform 1500 updates randomly distributed across 50 trainable weights
    for _ in range(1500):
        w_idx = random.randint(0, 49)
        sim_b.perform_weight_update(w_idx, random.uniform(-0.1, 0.1))

    # Calculate performance metrics
    erase_a = flash_a.erase_operations
    erase_b = flash_b.erase_operations
    bytes_a = flash_a.bytes_written
    bytes_b = flash_b.bytes_written

    waf_a = bytes_a / (1500 * 500 * 4) if bytes_a > 0 else 1.0 # ratio of flash write to logical write
    waf_b = bytes_b / (1500 * 8) if bytes_b > 0 else 1.0

    # Energy estimate (STM32H7 typical active power: erase draw ~150mW, page program draw ~80mW)
    energy_a_joules = (erase_a * 0.1 * 0.15) + (flash_a.pages_programmed * 0.0008 * 0.08)
    energy_b_joules = (erase_b * 0.1 * 0.15) + (flash_b.pages_programmed * 0.0008 * 0.08)

    recovery_us, recovered_records = sim_b.simulate_power_loss_recovery()

    stats = {
        "Baseline_Erase_Ops": erase_a,
        "Baseline_Bytes_Written": bytes_a,
        "Baseline_Energy_Joules": round(energy_a_joules, 4),
        "FNCLE_Erase_Ops": erase_b,
        "FNCLE_Bytes_Written": bytes_b,
        "FNCLE_Energy_Joules": round(energy_b_joules, 4),
        "Erase_Reduction_Factor": round(erase_a / erase_b, 2) if erase_b > 0 else "Infinite",
        "WAF_Baseline": round(waf_a, 4),
        "WAF_FNCLE": round(waf_b, 4),
        "SRAM_Index_Overhead_Bytes": len(sim_b.sram_lookup_table) * 4, # 4 bytes per entry hash map
        "Power_Loss_Recovery_Time_us": round(recovery_us, 2),
        "Power_Loss_Recovered_Records": recovered_records
    }

    print(json.dumps(stats, indent=4))

    with open("simulation_results.json", "w") as sf:
        json.dump(stats, sf, indent=4)

    print("Simulation execution complete. Results saved.")

if __name__ == "__main__":
    run_simulation()
