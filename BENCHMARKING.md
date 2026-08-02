# BENCHMARKING GUIDE: MEASURING CYCLES ON PHYSICAL STM32 HARDWARE

To obtain real, physical clock cycle counts for `fncle_vwe_register_delta` and `fncle_vwe_resolve` on ARM Cortex-M hardware (such as the STM32H7 or STM32F4 series), you can utilize the microcontroller's on-chip **Data Watchpoint and Trace (DWT)** unit.

Below is the complete, self-contained C code required to initialize and read the hardware clock cycle counter (`CYCCNT`).

## 1. STM32 Hardware Cycle Counter Code

```c
#include "fncle/virtual_weight_engine.h"
#include <stdio.h>

/* Register offsets for the ARM Cortex-M DWT Unit */
#define DWT_CONTROL             (*((volatile uint32_t*)0xE0001000))
#define DWT_CYCCNT              (*((volatile uint32_t*)0xE0001004))
#define DEMCR                   (*((volatile uint32_t*)0xE000EDFC))

/* Bit definitions */
#define TRCENA_BIT              (1 << 24)
#define CYCCNTENA_BIT           (1 << 0)

/**
 * @brief Enable the hardware cycle counter (DWT->CYCCNT) on Cortex-M processors.
 */
void dwt_init(void) {
    /* 1. Enable TRC (Trace) block inside DEMCR register */
    DEMCR |= TRCENA_BIT;

    /* 2. Reset the cycle counter register */
    DWT_CYCCNT = 0;

    /* 3. Enable CYCCNT clock cycle counter inside DWT control register */
    DWT_CONTROL |= CYCCNTENA_BIT;
}

/**
 * @brief Get the current physical clock cycles.
 */
inline uint32_t dwt_get_cycles(void) {
    return DWT_CYCCNT;
}

int main(void) {
    /* Initialize hardware cycle counter */
    dwt_init();

    /* Initialize FN-CLE engine */
    fncle_vwe_init();

    uint32_t start_cycles;
    uint32_t end_cycles;
    uint32_t delta_cycles;

    //---------------------------------------------------------
    // Measure: fncle_vwe_register_delta
    //---------------------------------------------------------
    start_cycles = dwt_get_cycles();
    fncle_vwe_register_delta(15, 0.045f);
    end_cycles = dwt_get_cycles();
    delta_cycles = end_cycles - start_cycles;
    printf("[MEASUREMENT] fncle_vwe_register_delta took: %u cycles\n", delta_cycles);

    //---------------------------------------------------------
    // Measure: fncle_vwe_resolve
    //---------------------------------------------------------
    start_cycles = dwt_get_cycles();
    float resolved = fncle_vwe_resolve(15, 1.2f);
    end_cycles = dwt_get_cycles();
    delta_cycles = end_cycles - start_cycles;
    printf("[MEASUREMENT] fncle_vwe_resolve took: %u cycles (resolved: %f)\n", delta_cycles, resolved);

    return 0;
}
```

## 2. Compiling and Uploading to STM32

If compiling using the standard GNU Arm Embedded Toolchain (`arm-none-eabi-gcc`), ensure optimization flags are set to `-O3` or `-Ofast` to represent production-level compiler optimizations:

```bash
arm-none-eabi-gcc -O3 -mcpu=cortex-m7 -mthumb -Iinclude \
    src/virtual_weight_engine.c src/flash_driver.c main.c \
    -o fncle_benchmark.elf
```

Flash the generated ELF image using standard utilities (`st-flash` or OpenOCD):

```bash
st-flash write fncle_benchmark.bin 0x8000000
```
