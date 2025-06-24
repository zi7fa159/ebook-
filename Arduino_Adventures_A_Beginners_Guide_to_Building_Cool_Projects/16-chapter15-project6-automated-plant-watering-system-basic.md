# Chapter 15: Project 6: Automated Plant Watering System (Basic)

(Content to be ~1500-2000 words, image integration for soil moisture sensor, relay, (optional pump/valve), circuit, and final project)

Forgetting to water your plants? This project comes to the rescue! We'll build a basic automated plant watering system. It will use a soil moisture sensor to detect when the soil is dry and then activate a small water pump (or a solenoid valve connected to a larger water source) for a short period to water the plant.

**Disclaimer:** Working with water and electronics requires caution. Ensure your water components (pump, reservoir) are kept separate from your Arduino and other electronics to prevent shorts or damage. This basic project uses a low-voltage pump; if you plan to control mains voltage pumps or valves, extreme caution and proper high-voltage relay/switching knowledge are essential, which is beyond the scope of this beginner project. **Always prioritize safety.**

## Project Overview

The system will:
1.  Read soil moisture levels using a soil moisture sensor.
2.  If the soil moisture drops below a certain threshold (meaning it's too dry), the Arduino will activate a relay.
3.  The relay will, in turn, switch on a small submersible water pump (or open a solenoid valve) for a predefined duration, delivering water to the plant.
4.  After watering, the system will wait for a period before checking again to prevent overwatering.

**What you'll learn:**

*   Using an analog soil moisture sensor.
*   Calibrating a sensor for your specific soil and environment.
*   Controlling a higher current device (like a small pump) using a relay module with Arduino.
*   Implementing timed actions and delays for a practical application.
*   Combining sensor input with actuator output to automate a task.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   1 x Soil Moisture Sensor (common type has two prongs and a small amplifier module with AO and DO pins)
*   1 x 5V Relay Module (single channel is fine)
*   1 x Small Submersible Water Pump (3-6V DC is common for hobby projects)
    *   *Alternatively, a 5V or 12V Solenoid Valve if you plan to connect to a larger water source (ensure your relay and power supply can handle it).*
*   Flexible Tubing for the pump.
*   A Small Container for water (reservoir).
*   A Plant that needs watering!
*   Jumper Wires (approx. 7-10)
*   **(Optional but Recommended for pump):**
    *   A separate power supply for the water pump if it draws more current than the Arduino can safely provide via its 5V pin (e.g., a 4xAA battery pack for a 5-6V pump, or a dedicated 5V/12V power adapter).
    *   A diode (e.g., 1N4001 or 1N4007) for flyback protection if driving the pump directly with a transistor (though most relay modules include this). For a relay module, this is usually handled.

## Understanding the Components

1.  **Soil Moisture Sensor:**
    *   Typically consists of two prongs (electrodes) that are inserted into the soil and a small electronic module.
    *   **How it works:** The resistance between the two prongs changes based on the amount of water in the soil. More water = lower resistance; less water = higher resistance.
    *   **Module Output:**
        *   **AO (Analog Output):** Provides an analog voltage that varies with soil moisture. This is what we'll primarily use. Higher voltage usually means drier soil, lower voltage means wetter soil (but this can be inverted depending on the module design – calibration is key!).
        *   **DO (Digital Output):** Provides a HIGH/LOW signal. There's usually a potentiometer on the module to set a threshold. When moisture exceeds/drops below this threshold, DO changes state. We'll focus on AO for more granular control.
        *   **VCC:** Power (typically 3.3V or 5V).
        *   **GND:** Ground.
    ```
    [Placeholder for an image of a typical soil moisture sensor with its prongs and amplifier module.
     Alt Text: "Soil moisture sensor showing the two prongs for soil insertion and the connected electronic module with VCC, GND, AO, and DO pins."]
    ```
    *(Image: `images/soil_moisture_sensor.png`)*

2.  **5V Relay Module:**
    *   A relay is an electrically operated switch. It allows a low-power signal from the Arduino (e.g., 5V) to control a higher power circuit (like our water pump).
    *   **Module Pins (Typical):**
        *   **Control Side (to Arduino):**
            *   **VCC:** 5V from Arduino.
            *   **GND:** GND from Arduino.
            *   **IN / SIG:** Signal pin from an Arduino digital pin. Sending a LOW signal to this pin usually activates the relay (closes the switch), and a HIGH signal deactivates it (opens the switch). *This can be inverted on some modules.*
        *   **Switched Side (for the load, e.g., pump):**
            *   **COM (Common):** The common terminal.
            *   **NO (Normally Open):** This terminal is disconnected from COM when the relay is off. It connects to COM when the relay is activated. This is usually what you use to switch a device ON.
            *   **NC (Normally Closed):** This terminal is connected to COM when the relay is off. It disconnects from COM when the relay is activated.
    ```
    [Placeholder for an image of a single-channel 5V relay module, showing control pins (VCC, GND, IN) and switched terminals (COM, NO, NC).
     Alt Text: "A single-channel 5V relay module. Control side shows VCC, GND, IN pins. Switched side shows COM, NO, and NC screw terminals."]
    ```
    *(Image: `images/relay_module_5v.png`)*

3.  **Small Submersible Water Pump:**
    *   These are designed to be placed directly in water. They typically have two wires for power (+ and -).
    *   Ensure its voltage rating (e.g., 3-6V) is appropriate.
    *   **Important:** Do NOT run these pumps dry for extended periods, as it can damage them.

## Wiring the System

**Safety First!**
*   Keep the Arduino, breadboard, and relay module DRY and away from the water reservoir and plant.
*   If using an external power supply for the pump, ensure polarities are correct.

1.  **Disconnect Arduino Power.**
2.  **Wire the Soil Moisture Sensor:**
    *   Connect **VCC** of the sensor module to **5V** on the Arduino.
    *   Connect **GND** of the sensor module to **GND** on the Arduino.
    *   Connect **AO** (Analog Output) of the sensor module to Arduino analog pin **A0**.
3.  **Wire the Relay Module (Control Side):**
    *   Connect **VCC** of the relay module to **5V** on the Arduino.
    *   Connect **GND** of the relay module to **GND** on the Arduino.
    *   Connect **IN** (Signal) of the relay module to Arduino digital pin **7**.
4.  **Wire the Water Pump through the Relay (Switched Side):**
    This part requires careful attention to how you're powering the pump.

    **Scenario A: Pump powered by Arduino 5V (ONLY for VERY low current pumps - <40-50mA - NOT generally recommended for motors/pumps as they can draw more and cause Arduino to reset or damage it. Most small pumps need more.)**
    *If your pump is tiny and you're sure it's very low current, you *could* try this, but an external supply is safer.*
        *   Connect one wire of the pump to the **NO (Normally Open)** terminal of the relay.
        *   Connect the other wire of the pump to **GND** on the Arduino.
        *   Connect the **COM (Common)** terminal of the relay to **5V** on the Arduino.
        *(When relay activates, 5V -> COM -> NO -> Pump -> GND)*

    **Scenario B: Pump powered by an External Power Supply (RECOMMENDED & SAFER)**
    Let's say you have a 4xAA battery pack (providing ~5-6V) or a dedicated 5V power adapter for the pump.
        *   Connect the **positive (+)** wire of your external power supply (e.g., battery pack + terminal) to the **COM** terminal of the relay.
        *   Connect one wire of the pump (usually its positive/red wire) to the **NO** terminal of the relay.
        *   Connect the other wire of the pump (usually its negative/black wire) to the **negative (-)** terminal of your external power supply (e.g., battery pack - terminal).
        *   **Crucially, connect the GND of the Arduino to the GND of the external power supply.** This is essential for the relay control signal to work correctly. If the grounds are not common, the relay might not switch reliably.

```
[Placeholder for a Fritzing diagram showing the complete circuit: Soil Moisture Sensor to A0, Relay IN to D7, Relay VCC/GND to Arduino 5V/GND. Pump connected through relay's NO and COM terminals, with pump powered by an external battery pack whose GND is common with Arduino GND.
 Alt Text: "Circuit: Soil moisture sensor (VCC,GND,A0 to Arduino). Relay module (VCC,GND,IN to Arduino 5V,GND,D7). Small water pump positive to Relay NO, Relay COM to external battery pack positive. Pump negative to external battery pack negative. Arduino GND also connected to external battery pack negative."]
```
*(Image: `images/plant_watering_circuit_external_pump_power.png`)*

## Step 1: Calibrating the Soil Moisture Sensor

Different soils and sensor placements will give different analog readings. We need to find out what values correspond to "dry" and "wet" for *your specific setup*.

**Calibration Sketch:**

```cpp
// Pin for Soil Moisture Sensor Analog Output
const int soilMoisturePin = A0;

void setup() {
  Serial.begin(9600);
  pinMode(soilMoisturePin, INPUT); // Good practice, though analog pins default to input
  Serial.println("Soil Moisture Sensor Calibration");
  Serial.println("Reading analog values from A0...");
  Serial.println("------------------------------------");
  Serial.println("Test 1: Sensor in DRY AIR (note this value)");
  Serial.println("Test 2: Sensor in completely DRY SOIL (note this value)");
  Serial.println("Test 3: Sensor in perfectly WET (saturated) SOIL (note this value)");
  Serial.println("------------------------------------");
}

void loop() {
  int sensorValue = analogRead(soilMoisturePin);
  Serial.print("Analog Reading: ");
  Serial.println(sensorValue);
  delay(1000); // Read every second
}
```

**Calibration Process:**

1.  Upload the "Calibration Sketch."
2.  Open the Serial Monitor.
3.  **Test 1: Dry Air:** Hold the sensor prongs in the air (not touching anything conductive). Note down the analog reading from the Serial Monitor. This is your "very dry" or "air" reading. (e.g., might be 700-900+ depending on the sensor module).
4.  **Test 2: Dry Soil:** Insert the sensor prongs into a sample of completely dry soil that you intend to use. Note the reading. This is your "dry soil" threshold. (e.g., might be 500-700).
5.  **Test 3: Wet Soil:** Thoroughly water the soil sample until it's saturated (but not a puddle). Insert the sensor. Note the reading. This is your "wet soil" threshold. (e.g., might be 200-400).

**Interpreting Your Readings:**
*   Most common soil moisture sensors output a **higher analog value for drier soil** and a **lower analog value for wetter soil**.
*   From your tests, you'll have a range. For example:
    *   Air: ~850
    *   Dry Soil: ~650
    *   Wet Soil: ~300
*   You'll use these values to set a **watering threshold**. For instance, if readings go above your "dry soil" value (e.g., > 650), it's time to water.

## The Code: Automated Watering System

Now, let's write the main sketch. **Replace `DRY_SOIL_THRESHOLD` with the value you determined from your calibration (e.g., the reading from your "Dry Soil" test or slightly above it).**

```cpp
// Pins
const int soilMoisturePin = A0; // Soil moisture sensor Analog Output
const int relayPin = 7;         // Digital pin to control the relay (IN pin of relay module)

// --- Calibration & Settings ---
// IMPORTANT: Calibrate your sensor and set this threshold!
// This is the analog reading ABOVE which we consider the soil DRY enough to water.
// Higher analog value usually means drier soil.
const int DRY_SOIL_THRESHOLD = 650; // EXAMPLE VALUE - REPLACE WITH YOURS!

const unsigned long wateringDuration = 3000; // Water for 3 seconds (3000 ms) - ADJUST THIS!
const unsigned long checkInterval = 300000; // Check soil moisture every 5 minutes (300,000 ms)
                                           // Or for testing: 10000; // Check every 10 seconds

// Variables
unsigned long lastWateringTime = 0;
unsigned long lastCheckTime = 0;
boolean isWatering = false;

void setup() {
  Serial.begin(9600);
  pinMode(soilMoisturePin, INPUT);
  pinMode(relayPin, OUTPUT);

  digitalWrite(relayPin, HIGH); // IMPORTANT: Most relay modules activate on LOW.
                                // So, set HIGH initially to keep relay OFF.
                                // If your relay activates on HIGH, set this to LOW.

  Serial.println("Automated Plant Watering System Initialized");
  Serial.print("Dry Soil Threshold (Analog): ");
  Serial.println(DRY_SOIL_THRESHOLD);
  Serial.print("Watering Duration (ms): ");
  Serial.println(wateringDuration);
  Serial.print("Check Interval (ms): ");
  Serial.println(checkInterval);
  Serial.println("------------------------------------");
  lastCheckTime = millis(); // Initialize for the first check
}

void loop() {
  unsigned long currentTime = millis();

  // --- Watering Logic ---
  if (isWatering) {
    // If currently watering, check if watering duration has passed
    if (currentTime - lastWateringTime >= wateringDuration) {
      stopWatering();
    }
  }
  // --- Moisture Check Logic (only if not currently watering) ---
  else if (currentTime - lastCheckTime >= checkInterval) {
    Serial.print(currentTime / 1000);
    Serial.print("s: Checking soil moisture... ");

    int moistureValue = analogRead(soilMoisturePin);
    Serial.print("Analog Reading: ");
    Serial.println(moistureValue);

    if (moistureValue > DRY_SOIL_THRESHOLD) {
      Serial.println("Soil is DRY. Starting to water.");
      startWatering();
    } else {
      Serial.println("Soil is moist enough.");
    }
    lastCheckTime = currentTime; // Reset the check timer
  }
}

void startWatering() {
  if (!isWatering) {
    Serial.println("Relay ON - Pump/Valve Activated.");
    digitalWrite(relayPin, LOW); // Activate relay (most activate on LOW)
                                 // If yours activates on HIGH, change this.
    isWatering = true;
    lastWateringTime = millis(); // Record when watering started
  }
}

void stopWatering() {
  if (isWatering) {
    Serial.println("Relay OFF - Pump/Valve Deactivated.");
    digitalWrite(relayPin, HIGH); // Deactivate relay
    isWatering = false;
    // lastCheckTime will naturally be updated in loop, ensuring a full checkInterval
    // before potentially watering again.
    Serial.println("------------------------------------");
  }
}
```

**Explanation:**

*   **`DRY_SOIL_THRESHOLD`:** **Crucial! Set this based on your calibration.**
*   **`wateringDuration`:** How long the pump runs when activated. Adjust this based on your pump's flow rate and plant's needs. Start small!
*   **`checkInterval`:** How often the system checks the soil moisture. Don't make this too short, as soil moisture doesn't change instantly. 5-30 minutes is reasonable for many plants. For testing, you can make it shorter (e.g., 10-30 seconds).
*   **`lastWateringTime`, `lastCheckTime`, `isWatering`:** Variables for state management using non-blocking `millis()` timing.
*   **`setup()`:**
    *   Sets `relayPin` to `OUTPUT`.
    *   `digitalWrite(relayPin, HIGH);`: **Important!** Most common relay modules are "active LOW," meaning sending a `LOW` signal to the `IN` pin activates the relay (closes the switch), and `HIGH` deactivates it. So, we initialize it to `HIGH` to keep the pump OFF. If your relay module is active HIGH, you'd initialize this to `LOW`.
*   **`loop()`:**
    *   **Watering Logic:** If `isWatering` is true, it checks if `wateringDuration` has passed. If so, it calls `stopWatering()`.
    *   **Moisture Check Logic:** If not currently watering (`else if`), it checks if `checkInterval` has passed since `lastCheckTime`.
        *   Reads `moistureValue` from `soilMoisturePin`.
        *   If `moistureValue > DRY_SOIL_THRESHOLD`, it calls `startWatering()`.
        *   Updates `lastCheckTime`.
*   **`startWatering()` function:**
    *   Sets `isWatering = true`.
    *   Activates the relay (`digitalWrite(relayPin, LOW);` for active-LOW relays).
    *   Records `lastWateringTime`.
*   **`stopWatering()` function:**
    *   Sets `isWatering = false`.
    *   Deactivates the relay (`digitalWrite(relayPin, HIGH);` for active-LOW relays).

**Upload and Test:**
1.  **Double-check your `DRY_SOIL_THRESHOLD`!**
2.  **Start with a very short `wateringDuration` (e.g., 500ms or 1000ms) and a short `checkInterval` (e.g., 10000ms) for initial testing.**
3.  Place the soil moisture sensor prongs into your plant's soil.
4.  Ensure the pump's intake is in your water reservoir and its outlet is positioned to water the plant.
5.  Upload the sketch. Open the Serial Monitor.
6.  Observe:
    *   The system should print its initial status.
    *   After the first `checkInterval`, it will read the soil moisture.
    *   If the reading is above your `DRY_SOIL_THRESHOLD`, the relay should click, and the pump should run for `wateringDuration`.
    *   The pump should then stop.
    *   The system will wait for the next `checkInterval` before checking again.
7.  **Fine-tune:**
    *   Adjust `DRY_SOIL_THRESHOLD` if it waters too soon or too late.
    *   Adjust `wateringDuration` to deliver the right amount of water.
    *   Adjust `checkInterval` for how frequently you want to monitor.

## Safety and Considerations:

*   **Water & Electronics Don't Mix:** Keep your Arduino and relay well away from any potential water spills. Consider enclosing them.
*   **Pump Power:** If your pump seems weak or the Arduino resets when the pump turns on, the pump is drawing too much current from the Arduino. Use a separate, appropriate power supply for the pump (with common grounds, as shown in wiring Scenario B).
*   **Relay Type:** Confirm if your relay is active LOW (common) or active HIGH and adjust `digitalWrite(relayPin, ...)` logic if needed.
*   **Overwatering:** Be careful with `wateringDuration` and `checkInterval`. It's easy to overwater if these are not set appropriately for your plant and soil type. The current system doesn't have a "maximum waterings per day" limit.
*   **Sensor Corrosion:** Soil moisture sensor prongs can corrode over time, especially if continuously powered. Some advanced techniques involve only powering the sensor briefly before taking a reading to extend its life (requires more complex wiring/coding).

## Further Extensions & Challenges:

1.  **Button for Manual Watering:** Add a push button that, when pressed, triggers a watering cycle regardless of moisture (but perhaps still respects the `isWatering` flag to prevent overlap).
2.  **Sleep Mode (Advanced):** To save power (if battery operated), use Arduino's sleep modes and an external interrupt (e.g., from a timer or a button) to wake up and check moisture periodically.
3.  **Low Water Reservoir Alert:** Add a water level sensor (e.g., a float switch or ultrasonic sensor) to your reservoir. If water is low, prevent watering and perhaps blink an alert LED.
4.  **Display on LCD:** Show current moisture level, watering status, and time until next check on an I2C LCD.
5.  **Data Logging:** Send moisture data and watering events to the Serial Monitor or an SD card to track your plant's conditions over time.
6.  **Multiple Plants:** Expand with more sensors and relays/valves to water multiple plants independently, each with its own threshold.

## Summary

You've built a basic automated plant watering system! This project combines:
*   Analog sensor input (soil moisture).
*   Calibration of sensors.
*   Conditional logic to make decisions.
*   Output control via a relay to handle a device like a pump.
*   `millis()` based timing for non-blocking operation.

This is a very practical application of Arduino and can be expanded into a much more sophisticated system. Remember to observe your plant and adjust the settings to keep it happy and healthy!
