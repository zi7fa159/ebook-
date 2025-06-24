# Chapter 12: Project 3: Intruder Alarm with PIR Sensor

(Content to be ~1500-2000 words, image integration for PIR sensor, circuit, and final project)

Time to add some sensing capabilities to our Arduino projects! In this chapter, we'll build a simple intruder alarm system using a **PIR (Passive Infrared) motion sensor**. When motion is detected, the alarm will trigger an LED and a buzzer (or just an LED if you don't have a buzzer). This project is a great introduction to using digital sensors as inputs.

## Project Overview

A PIR motion sensor detects changes in infrared radiation in its field of view, which usually corresponds to a warm body (like a person or animal) moving. When it detects motion, its output pin typically goes HIGH.

We will:
1.  Interface a PIR motion sensor with the Arduino.
2.  Read the digital output from the PIR sensor.
3.  When motion is detected, activate an LED and (optionally) a piezo buzzer.
4.  Implement a simple "arming" and "triggered" state for the alarm.

**What you'll learn:**

*   How PIR motion sensors work at a basic level.
*   Connecting and reading from a digital sensor.
*   Using conditional logic to react to sensor input.
*   Controlling multiple outputs (LED and buzzer) based on sensor state.
*   Basic state management for a simple alarm system.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   1 x PIR Motion Sensor (HC-SR501 is a common and inexpensive type)
*   1 x LED (e.g., Red, for alarm indicator)
*   1 x 220Ω or 330Ω Resistor (for the LED)
*   (Optional) 1 x Piezo Buzzer (active or passive - active is simpler for direct drive)
*   Jumper Wires (approx. 6-8)

## Understanding PIR Motion Sensors (e.g., HC-SR501)

PIR sensors don't emit any energy; they are passive. They have two pyroelectric sensors inside that are sensitive to infrared radiation. When everything is still, both sensors detect the same amount of ambient IR. When a warm body moves into or out of the sensor's view, it causes a *difference* in the IR levels detected by the two internal sensors. This difference is what triggers the sensor's output.

**Key Features of HC-SR501 (Common PIR Sensor):**

*   **Power Supply:** Typically 4.5V to 20V (can often run from Arduino's 5V).
*   **Output Signal:** Digital HIGH (usually 3.3V, but often tolerated by Arduino's 5V digital inputs as HIGH) when motion is detected, LOW otherwise.
*   **Detection Range:** Up to 7 meters (adjustable on some models).
*   **Detection Angle:** Around 100-120 degrees.
*   **Adjustments (often present):**
    *   **Sensitivity Potentiometer (Sx):** Adjusts the detection range/sensitivity.
    *   **Time Delay Potentiometer (Tx):** Adjusts how long the output signal remains HIGH after motion is last detected (e.g., from a few seconds to a few minutes).
    *   **Trigger Mode Jumper (H/L):**
        *   **L (Non-repeatable/Single Trigger):** Output goes HIGH for the set delay time after detection, then LOW. It won't re-trigger during this HIGH period even if motion continues.
        *   **H (Repeatable Trigger):** Output goes HIGH. If motion continues during the delay time, the HIGH period is extended. The output only goes LOW after motion stops for the duration of the delay time. This is often the preferred mode for continuous detection. *(Default is usually H)*.
*   **Pins:**
    *   **VCC:** Power supply (+5V from Arduino).
    *   **OUT:** Digital output signal to Arduino.
    *   **GND:** Ground.

```
[Placeholder for an image of an HC-SR501 PIR motion sensor, highlighting its pins (VCC, OUT, GND) and adjustment potentiometers/jumper if visible.
 Alt Text: "HC-SR501 PIR motion sensor showing VCC, OUT, and GND pins. Adjustment potentiometers for sensitivity and time delay, and trigger mode jumper are also indicated."]
```
*(Image: `images/pir_sensor_hc-sr501.png`)*

**Important Warm-up Time:** PIR sensors often require a "warm-up" or "stabilization" period when first powered on (e.g., 30 seconds to a minute). During this time, they might give false triggers. Your code should account for this.

## Wiring the Intruder Alarm

1.  **Disconnect Power.**
2.  **Wire the PIR Sensor:**
    *   Connect the **VCC** pin of the PIR sensor to the **5V** pin on the Arduino.
    *   Connect the **GND** pin of the PIR sensor to a **GND** pin on the Arduino.
    *   Connect the **OUT** pin of the PIR sensor to Arduino digital pin **2**.
3.  **Wire the Alarm LED:**
    *   Connect the **anode** (longer leg) of the Red LED to Arduino digital pin **12**.
    *   Connect the **cathode** (shorter leg) of the Red LED to one leg of a 220Ω (or 330Ω) resistor.
    *   Connect the other leg of the resistor to a **GND** pin on the Arduino (or the breadboard's ground rail if you're using one).
4.  **(Optional) Wire the Piezo Buzzer:**
    *   If you have an **active piezo buzzer** (usually has two pins, often marked + and - or just different length legs; it makes a tone when DC power is applied):
        *   Connect the positive (+) pin/leg of the buzzer to Arduino digital pin **8**.
        *   Connect the negative (-) pin/leg of the buzzer to **GND**.
    *   If you have a **passive piezo buzzer** (needs an oscillating signal to make sound):
        *   You can connect one pin to digital pin 8 and the other to GND. Then use the `tone()` function in Arduino to make sound. For simplicity in this basic alarm, an active buzzer is easier if you just want a set alarm sound. We'll primarily code for direct digital HIGH/LOW for the buzzer pin, suitable for an active buzzer.

```
[Placeholder for a Fritzing diagram of the PIR sensor, LED, and (optional) buzzer circuit.
 Alt Text: "Circuit: PIR sensor VCC to Arduino 5V, GND to Arduino GND, OUT to Arduino D2. Red LED anode to Arduino D12, cathode via resistor to GND. Optional Active Buzzer: + to Arduino D8, - to GND."]
```
*(Image: `images/pir_alarm_circuit.png`)*

## The Code: Simple Motion Detection Alarm

Let's start with a basic sketch that turns on the LED and buzzer when motion is detected.

```cpp
// Define sensor and output pins
const int pirPin = 2;     // Input pin for the PIR sensor
const int ledPin = 12;    // Output pin for the alarm LED
const int buzzerPin = 8;  // Output pin for the buzzer (optional)

boolean motionDetectedState = false; // Current state of motion detection

// Variables for PIR stabilization time
unsigned long pirStabilizationTime = 30000; // 30 seconds in milliseconds
boolean pirCalibrated = false;

void setup() {
  Serial.begin(9600); // For debugging messages

  pinMode(pirPin, INPUT);
  pinMode(ledPin, OUTPUT);
  pinMode(buzzerPin, OUTPUT); // Even if not used, good to define

  digitalWrite(ledPin, LOW);    // Ensure LED is off initially
  digitalWrite(buzzerPin, LOW); // Ensure buzzer is off initially

  Serial.println("PIR Sensor Alarm System");
  Serial.println("Calibrating PIR sensor...");
  Serial.print("Please wait for approx. ");
  Serial.print(pirStabilizationTime / 1000);
  Serial.println(" seconds.");

  // Blink LED during calibration
  for (int i = 0; i < (pirStabilizationTime / 1000); i++) {
    digitalWrite(ledPin, HIGH);
    delay(500);
    digitalWrite(ledPin, LOW);
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nCalibration complete. Alarm system armed.");
  pirCalibrated = true;
}

void loop() {
  if (!pirCalibrated) {
    // Still in the initial (forced) calibration phase from setup, do nothing in loop
    return;
  }

  // Read the PIR sensor state
  int pirState = digitalRead(pirPin);

  if (pirState == HIGH) { // Motion detected!
    if (!motionDetectedState) { // If it's a new motion event
      Serial.println("MOTION DETECTED!");
      digitalWrite(ledPin, HIGH);    // Turn on LED
      digitalWrite(buzzerPin, HIGH); // Turn on Buzzer (if active buzzer)
      // For passive buzzer, you might use: tone(buzzerPin, 1000, 500); // frequency, duration
      motionDetectedState = true;
    }
  } else { // No motion
    if (motionDetectedState) { // If motion was previously detected, now it's clear
      Serial.println("Motion ended. System clear.");
      digitalWrite(ledPin, LOW);     // Turn off LED
      digitalWrite(buzzerPin, LOW);  // Turn off Buzzer
      // For passive buzzer, use: noTone(buzzerPin);
      motionDetectedState = false;
    }
    // If no motion and no previous motion, do nothing.
  }

  delay(100); // Small delay to reduce rapid checking, adjust as needed
}
```

**Explanation:**

*   **Constants & Variables:**
    *   `pirPin`, `ledPin`, `buzzerPin`: Define the connected pins.
    *   `motionDetectedState`: A boolean flag to keep track of whether we are currently in a "motion detected" state. This helps to only print "MOTION DETECTED!" once when motion starts.
    *   `pirStabilizationTime`: How long to wait in `setup()` for the PIR to stabilize (30 seconds here).
    *   `pirCalibrated`: Flag to indicate if stabilization is done.
*   **`setup()`:**
    *   Initializes Serial communication.
    *   Sets pin modes.
    *   Prints calibration messages.
    *   Includes a `for` loop that blinks the LED during the `pirStabilizationTime`. This gives visual feedback that the system is starting up. **This is a blocking delay for calibration.**
    *   After the delay, sets `pirCalibrated = true`.
*   **`loop()`:**
    *   First checks if `pirCalibrated` is false. If so (which shouldn't happen if `setup` completes fully), it returns, effectively pausing the loop until `setup`'s calibration is done.
    *   `pirState = digitalRead(pirPin);` reads the current output of the PIR sensor.
    *   **If `pirState == HIGH` (Motion Detected):**
        *   It checks `if (!motionDetectedState)`. This means "if motion was NOT already detected in the previous loop iteration." This ensures the "MOTION DETECTED!" message and initial turning ON of alarms only happens once when motion *starts*.
        *   Turns on the LED and Buzzer.
        *   Sets `motionDetectedState = true;`.
    *   **Else (No Motion, `pirState == LOW`):**
        *   It checks `if (motionDetectedState)`. This means "if motion WAS detected in the previous loop iteration, but now it's clear."
        *   Turns off the LED and Buzzer.
        *   Sets `motionDetectedState = false;`.
*   **`delay(100);`:** A small delay in the loop to prevent the Arduino from checking the PIR sensor too rapidly, which is usually not necessary and can sometimes be problematic with sensor settling times.

**Upload and Test:**

1.  Upload the sketch.
2.  Open the Serial Monitor (Tools > Serial Monitor, set baud rate to 9600).
3.  You'll see the calibration message. Wait for the calibration period (e.g., 30 seconds, during which the LED will blink). Avoid moving in front of the sensor during this time.
4.  Once "Calibration complete. Alarm system armed." appears, the system is active.
5.  Move your hand in front of the PIR sensor.
    *   The Red LED should turn on.
    *   The buzzer (if connected and active) should sound.
    *   You should see "MOTION DETECTED!" in the Serial Monitor.
6.  Stop moving or move out of the sensor's range.
    *   After the PIR sensor's internal time delay (Tx setting on the sensor itself) expires and its output goes LOW, the LED and buzzer should turn off, and "Motion ended. System clear." should appear.

**Adjusting the PIR Sensor:**
*   If the alarm triggers too easily or not easily enough, try adjusting the **Sensitivity (Sx)** potentiometer on the PIR sensor.
*   The **Time Delay (Tx)** potentiometer on the PIR sensor controls how long its output stays HIGH after detecting motion. This will affect how long your LED/buzzer stay on *after motion stops*. The Arduino code currently just follows the PIR's output.

## Part 2: Alarm with Auto-Reset/Timeout

The previous code keeps the alarm on as long as the PIR output is HIGH. Let's modify it so the alarm sounds for a fixed duration (e.g., 10 seconds) once triggered, then resets itself (stops the alarm) even if the PIR is still detecting motion, and then waits for a "quiet period" before it can be re-triggered. This prevents the alarm from sounding continuously if something stays in front of the sensor.

```cpp
// Define sensor and output pins
const int pirPin = 2;
const int ledPin = 12;
const int buzzerPin = 8;

// Alarm state variables
enum AlarmStateType { IDLE, TRIGGERED, COOLDOWN };
AlarmStateType currentAlarmState = IDLE;

boolean pirMotionSignal = false; // True if PIR is currently outputting HIGH

// Timing
unsigned long pirStabilizationTime = 15000; // 15 seconds for calibration
boolean pirCalibrated = false;

unsigned long alarmActiveDuration = 10000; // 10 seconds for alarm to sound
unsigned long alarmCooldownDuration = 20000; // 20 seconds before alarm can re-trigger
unsigned long lastStateChangeTimestamp = 0;
unsigned long motionDetectedTimestamp = 0;

void setup() {
  Serial.begin(9600);
  pinMode(pirPin, INPUT);
  pinMode(ledPin, OUTPUT);
  pinMode(buzzerPin, OUTPUT);

  digitalWrite(ledPin, LOW);
  digitalWrite(buzzerPin, LOW);

  Serial.println("PIR Alarm - Auto Reset Version");
  Serial.println("Calibrating PIR sensor...");
  // Non-blocking calibration indication (simple version)
  unsigned long calibrationStartTime = millis();
  while (millis() - calibrationStartTime < pirStabilizationTime) {
    digitalWrite(ledPin, HIGH); delay(100);
    digitalWrite(ledPin, LOW); delay(100);
    Serial.print(".");
    // IMPORTANT: Read PIR during calibration to help it settle (some sensors benefit)
    digitalRead(pirPin);
  }
  pirCalibrated = true;
  digitalWrite(ledPin, LOW); // Ensure LED is off after calibration
  Serial.println("\nCalibration complete. System ARMED (State: IDLE).");
  lastStateChangeTimestamp = millis();
}

void loop() {
  if (!pirCalibrated) return; // Should not happen if setup completes

  unsigned long currentTime = millis();
  pirMotionSignal = (digitalRead(pirPin) == HIGH);

  switch (currentAlarmState) {
    case IDLE:
      // System is armed and waiting for motion
      if (pirMotionSignal) {
        Serial.println("IDLE: Motion detected! -> TRIGGERED");
        currentAlarmState = TRIGGERED;
        motionDetectedTimestamp = currentTime; // Record when motion first seen in this trigger cycle
        lastStateChangeTimestamp = currentTime;
        // Activate alarm outputs
        digitalWrite(ledPin, HIGH);
        digitalWrite(buzzerPin, HIGH); // Or tone() for passive
      }
      break;

    case TRIGGERED:
      // Alarm is active
      // Check if alarm duration has passed
      if (currentTime - motionDetectedTimestamp >= alarmActiveDuration) {
        Serial.println("TRIGGERED: Alarm duration ended. -> COOLDOWN");
        currentAlarmState = COOLDOWN;
        lastStateChangeTimestamp = currentTime;
        // Deactivate alarm outputs
        digitalWrite(ledPin, LOW);
        digitalWrite(buzzerPin, LOW); // Or noTone()
      }
      // Note: Even if pirMotionSignal goes LOW during TRIGGERED state, alarm continues for its duration.
      break;

    case COOLDOWN:
      // Alarm has sounded, now in cooldown before it can be re-armed/re-triggered
      // LED and Buzzer are OFF during cooldown
      if (currentTime - lastStateChangeTimestamp >= alarmCooldownDuration) {
        Serial.println("COOLDOWN: Cooldown ended. -> IDLE (System Re-ARMED)");
        currentAlarmState = IDLE;
        lastStateChangeTimestamp = currentTime;
      }
      break;
  }
  delay(50); // Short delay for loop stability
}
```

**Explanation of Auto-Reset Code:**

*   **`AlarmStateType` enum:** Defines three states: `IDLE`, `TRIGGERED`, `COOLDOWN`.
*   **`currentAlarmState`:** Tracks the current state.
*   **Timing Variables:**
    *   `alarmActiveDuration`: How long the alarm sounds.
    *   `alarmCooldownDuration`: How long the system waits after an alarm before it can be triggered again.
    *   `lastStateChangeTimestamp`: Used to time states.
    *   `motionDetectedTimestamp`: Specifically records when motion *started* the current trigger, so the alarm sounds for its full duration from that point.
*   **`setup()` Calibration:** The calibration loop is now non-blocking for the main `millis()` timer but still effectively pauses `setup`. It also reads the PIR during calibration, which some sensors appreciate.
*   **`loop()` with `switch` statement:**
    *   **`IDLE` State:**
        *   If `pirMotionSignal` is true (PIR detects motion), it transitions to `TRIGGERED`.
        *   Records `motionDetectedTimestamp`.
        *   Activates LED and buzzer.
    *   **`TRIGGERED` State:**
        *   The alarm (LED/buzzer) remains active.
        *   It checks if `currentTime - motionDetectedTimestamp >= alarmActiveDuration`. If the alarm has sounded for the full duration, it transitions to `COOLDOWN`.
        *   Deactivates LED and buzzer.
    *   **`COOLDOWN` State:**
        *   LED and buzzer are off.
        *   If `currentTime - lastStateChangeTimestamp >= alarmCooldownDuration`, the cooldown period is over, and the system transitions back to `IDLE`, re-arming itself.

**Upload and Test Auto-Reset Version:**
1.  Upload. Wait for calibration.
2.  Trigger the sensor. The alarm should sound for `alarmActiveDuration` (10 seconds).
3.  Even if you keep moving in front of the sensor, the alarm should stop after 10 seconds and enter the `COOLDOWN` state (LED/buzzer off).
4.  During `COOLDOWN` (20 seconds), triggering the PIR sensor should *not* restart the alarm.
5.  After the cooldown, the system returns to `IDLE` and can be triggered again.

This state-based approach with non-blocking timers (`millis()`) is much more flexible and allows for more complex behaviors than simple `delay()`-based logic.

## Further Extensions & Challenges:

1.  **Arming/Disarming Switch:** Add another button or a switch. The alarm should only be active if the system is "armed" by this switch.
2.  **Entry/Exit Delay:** Simulate a real alarm by adding an entry delay (e.g., 10 seconds to disarm after motion is detected when armed) and an exit delay (e.g., 30 seconds to leave after arming).
3.  **Silent Alarm Mode:** Add a switch to toggle between audible alarm and silent alarm (only LED, or perhaps send a message via Serial or to an ESP8266 for a notification).
4.  **Multiple Sensors:** Expand the system to monitor multiple PIR sensors (or other sensors like magnetic door switches). If any sensor is triggered, the alarm activates.
5.  **Password Disarm (Advanced):** Use a keypad or a sequence of button presses to disarm the alarm.

## Summary

The PIR intruder alarm project has taught you:
*   The basics of PIR motion sensors and how to interface them.
*   The importance of sensor calibration/stabilization.
*   Reading digital sensor inputs.
*   Creating a system that reacts to environmental changes.
*   Implementing simple and then more advanced state management using `enum` and `millis()` for non-blocking behavior.
*   Controlling alarm outputs like LEDs and buzzers.

This project opens the door to many home automation and security-related ideas. By understanding how to detect presence and manage states, you can build much more intelligent and interactive Arduino systems.
