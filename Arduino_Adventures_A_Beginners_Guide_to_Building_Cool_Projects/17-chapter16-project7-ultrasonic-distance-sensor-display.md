# Chapter 16: Project 7: Ultrasonic Distance Sensor Display

(Content to be ~1500-2000 words, image integration for HC-SR04, circuit, and final project with LCD/Serial display)

In our final "cool project" for this section, we'll explore measuring distance using an **HC-SR04 ultrasonic distance sensor**. This sensor uses sound waves to determine the distance to an object, much like sonar used by bats or submarines. We'll measure the distance and display it on the Serial Monitor, and optionally, on an I2C LCD.

## Project Overview

The HC-SR04 ultrasonic sensor emits a short burst of ultrasonic sound and listens for the echo. By measuring the time it takes for the echo to return, it can calculate the distance to the object that reflected the sound.

We will:
1.  Understand the working principle of the HC-SR04 sensor.
2.  Interface the sensor with Arduino (it requires two digital pins).
3.  Write code to trigger the sensor and read the echo pulse.
4.  Convert the echo pulse duration into distance (in centimeters and inches).
5.  Display the measured distance.

**What you'll learn:**

*   How ultrasonic distance sensing works.
*   Using digital pins for both output (trigger) and input (echo).
*   Using the `pulseIn()` function to measure pulse durations.
*   Performing calculations to convert time to distance.
*   Practical application in robotics, presence detection, or simple range finding.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   1 x HC-SR04 Ultrasonic Distance Sensor
*   Jumper Wires (approx. 4-6)
*   **(Optional, for LCD Display Extension):**
    *   1 x I2C LCD Module (e.g., 16x2)
    *   4 x Jumper Wires (for I2C LCD)

## Understanding the HC-SR04 Ultrasonic Sensor

The HC-SR04 module has four pins:

*   **VCC:** Power (+5V from Arduino).
*   **Trig (Trigger):** An input pin to the sensor. To start a measurement, you send a short HIGH pulse (10 microseconds) to this pin.
*   **Echo:** An output pin from the sensor. After you trigger it, this pin goes HIGH for a period of time equal to how long the ultrasonic sound wave traveled to the object and back.
*   **GND:** Ground.

```
[Placeholder for an image of an HC-SR04 ultrasonic sensor module, clearly showing its VCC, Trig, Echo, and GND pins.
 Alt Text: "HC-SR04 ultrasonic distance sensor module with its two 'eyes' (transmitter and receiver) and four pins: VCC, Trig, Echo, GND labeled."]
```
*(Image: `images/hc-sr04_sensor.png`)*

**Working Principle:**

1.  **Trigger:** Your Arduino sends a 10µs (microsecond) HIGH pulse to the HC-SR04's `Trig` pin.
2.  **Transmission:** The HC-SR04 automatically transmits a burst of 8 cycles of 40kHz ultrasonic sound from its transmitter.
3.  **Listening for Echo:** The module then sets its `Echo` pin HIGH and waits for the sound to reflect off an object and return to its receiver.
4.  **Echo Received:** When the echo is detected, the HC-SR04 sets its `Echo` pin LOW.
5.  **Pulse Duration:** The duration for which the `Echo` pin was HIGH is proportional to the round-trip time of the sound wave.
6.  **Distance Calculation:**
    *   Speed of sound in air is approximately 343 meters/second (or 29.1 microseconds/centimeter, or 74 microseconds/inch).
    *   Distance = (Duration of Echo Pulse * Speed of Sound) / 2
        *   We divide by 2 because the pulse duration represents the time for the sound to go to the object *and* come back. We want only the one-way distance.

**Range:** Typically 2cm to 400cm (about 1 inch to 13 feet), but accuracy can decrease at extremes and with certain surfaces.

## Wiring the HC-SR04 Sensor

1.  **Disconnect Power.**
2.  **Wire the HC-SR04:**
    *   Connect **VCC** pin of HC-SR04 to **5V** on Arduino.
    *   Connect **GND** pin of HC-SR04 to **GND** on Arduino.
    *   Connect **Trig** pin of HC-SR04 to Arduino digital pin **9**.
    *   Connect **Echo** pin of HC-SR04 to Arduino digital pin **10**.

```
[Placeholder for a Fritzing diagram of the HC-SR04 sensor connected to Arduino.
 Alt Text: "Circuit: HC-SR04 VCC to Arduino 5V, GND to Arduino GND, Trig to Arduino D9, Echo to Arduino D10."]
```
*(Image: `images/hc-sr04_circuit.png`)*

## The Code: Measuring and Displaying Distance (Serial Monitor)

```cpp
// Define Trig and Echo pins
const int trigPin = 9;
const int echoPin = 10;

// Variables for duration and distance
long duration_us; // Duration of echo pulse in microseconds
float distance_cm;
float distance_in;

void setup() {
  Serial.begin(9600); // Initialize serial communication

  pinMode(trigPin, OUTPUT); // Set Trig pin as an Output
  pinMode(echoPin, INPUT);  // Set Echo pin as an Input

  Serial.println("Ultrasonic Distance Sensor Test");
  Serial.println("-----------------------------");
}

void loop() {
  // --- Step 1: Send the Trigger Pulse ---
  // Ensure Trig pin is LOW for a short period before sending pulse, for a clean pulse.
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2); // Wait for 2 microseconds

  // Send a 10 microsecond HIGH pulse to Trig pin
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10); // Pulse width of 10 microseconds
  digitalWrite(trigPin, LOW);

  // --- Step 2: Read the Echo Pulse ---
  // The pulseIn() function reads a pulse (either HIGH or LOW) on a pin.
  // pulseIn(pin, value, timeout)
  //   pin: the pin to read the pulse on.
  //   value: type of pulse to read: HIGH or LOW.
  //   timeout (optional): the number of microseconds to wait for the pulse to complete.
  //                     Defaults to one second if not specified.
  // Returns the length of the pulse in microseconds, or 0 if no pulse is completed before timeout.
  duration_us = pulseIn(echoPin, HIGH);

  // --- Step 3: Calculate Distance ---
  // Speed of sound in air:
  //   Approximately 343 meters/second
  //   = 0.0343 centimeters/microsecond
  //   = 0.0135 inches/microsecond

  // Distance = (Time * SpeedOfSound) / 2  (because time is for round trip)

  // Calculate distance in centimeters
  // distance_cm = (duration_us * 0.0343) / 2.0;
  // Or, more simply: distance_cm = duration_us / 29.1 / 2.0; (since 1/0.0343 = 29.1 us/cm)
  // Or, even simpler: distance_cm = duration_us / 58.2; (common simplification)
  distance_cm = duration_us * 0.01715; // (0.0343 / 2)

  // Calculate distance in inches
  // distance_in = (duration_us * 0.0135) / 2.0;
  // Or, more simply: distance_in = duration_us / 74.0 / 2.0; (since 1/0.0135 = 74 us/inch)
  // Or, even simpler: distance_in = duration_us / 148.0;
  distance_in = duration_us * 0.00675; // (0.0135 / 2)


  // --- Step 4: Display the Results ---
  Serial.print("Distance: ");
  if (distance_cm <= 0 || duration_us == 0) { // Check for timeout or out of range
    Serial.println("Out of range / No echo");
  } else {
    Serial.print(distance_cm);
    Serial.print(" cm  (");
    Serial.print(distance_in);
    Serial.println(" inches)");
  }

  delay(500); // Wait 500ms before next measurement
               // Sensor needs some time between measurements (e.g., >60ms recommended by some datasheets)
}
```

**Explanation:**

*   **`trigPin`, `echoPin`:** Define the pins connected to the sensor.
*   **`duration_us`, `distance_cm`, `distance_in`:** Variables to store results. `duration_us` is `long` because pulse durations can be large.
*   **`setup()`:**
    *   Initializes Serial communication.
    *   Sets `trigPin` to `OUTPUT` and `echoPin` to `INPUT`.
*   **`loop()`:**
    *   **Trigger Pulse Generation:**
        *   `digitalWrite(trigPin, LOW); delayMicroseconds(2);` Ensures `trigPin` is low before the pulse.
        *   `digitalWrite(trigPin, HIGH); delayMicroseconds(10);` Sends the 10µs HIGH pulse. `delayMicroseconds()` is used for very short delays.
        *   `digitalWrite(trigPin, LOW);` Ends the trigger pulse.
    *   **Reading Echo Pulse with `pulseIn()`:**
        *   `duration_us = pulseIn(echoPin, HIGH);` This is the key function. It waits for `echoPin` to go `HIGH`, starts timing, then waits for `echoPin` to go `LOW`, and stops timing. It returns the duration of the HIGH pulse in microseconds. If the pulse doesn't complete within a default timeout (1 second), `pulseIn()` returns 0.
    *   **Distance Calculation:**
        *   The comments show different ways to derive the formula. The core idea is `Distance = (Time * Speed) / 2`.
        *   `distance_cm = duration_us * 0.01715;` uses the speed of sound (0.0343 cm/µs) and divides by 2.
        *   `distance_in = duration_us * 0.00675;` does the same for inches.
    *   **Displaying Results:**
        *   Prints the calculated distances.
        *   Includes a check `if (distance_cm <= 0 || duration_us == 0)` to handle cases where `pulseIn()` timed out (returned 0) or the calculation resulted in an invalid distance.
    *   **`delay(500);`:** It's good practice to have a delay between measurements to allow echoes to clear and the sensor to settle. The HC-SR04 datasheet usually recommends a measurement cycle period of >60ms. 500ms is a safe and reasonable delay for display purposes.

**Upload and Test:**
1.  Upload the sketch.
2.  Open the Serial Monitor (9600 baud).
3.  Point the HC-SR04 sensor at objects at various distances. You should see the distance in cm and inches printed to the Serial Monitor.
4.  Try pointing it at a wall, then your hand closer and further away.
5.  If you point it at something too far away or an object that doesn't reflect sound well (like soft fabric), you might see "Out of range."

**Accuracy Considerations:**
*   The speed of sound changes with air temperature, humidity, and altitude. The formulas used assume standard conditions. For very precise measurements, you might need to compensate for these factors.
*   The sensor has a minimum detection distance (around 2cm) and a maximum effective range (around 400cm).
*   The "cone" of the ultrasonic beam isn't perfectly narrow, so it might detect objects slightly off-axis.
*   Soft, irregular, or angled surfaces might not reflect sound well back to the sensor, leading to inaccurate or no readings.

## Optional Extension: Displaying Distance on I2C LCD

(This assumes you've already installed the "LiquidCrystal I2C" library as in Project 4, Chapter 13).

### Wiring (Add LCD to previous HC-SR04 circuit):

*   Keep HC-SR04 wired as before.
*   **I2C LCD:**
    *   GND to Arduino GND
    *   VCC to Arduino 5V
    *   SDA to Arduino A4
    *   SCL to Arduino A5

```
[Placeholder for a Fritzing diagram showing HC-SR04 AND I2C LCD connected.
 Alt Text: "Circuit: HC-SR04 as before (Trig D9, Echo D10). I2C LCD: GND to Arduino GND, VCC to 5V, SDA to Arduino A4, SCL to Arduino A5."]
```
*(Image: `images/hc-sr04_lcd_circuit.png`)*

### Code with I2C LCD Display:

```cpp
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// Define Trig and Echo pins
const int trigPin = 9;
const int echoPin = 10;

// Variables for duration and distance
long duration_us;
float distance_cm;
// float distance_in; // Can calculate if needed for LCD

// I2C LCD Configuration (change address 0x27 if yours is different, e.g., 0x3F)
LiquidCrystal_I2C lcd(0x27, 16, 2); // Address, Columns, Rows

void setup() {
  Serial.begin(9600); // Keep for debugging

  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  lcd.init();      // Initialize LCD
  lcd.backlight(); // Turn on backlight
  lcd.setCursor(0, 0);
  lcd.print("Distance Meter");
  delay(1000);
  lcd.clear();
  Serial.println("Ultrasonic Distance Sensor with LCD");
}

void loop() {
  // Trigger pulse
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // Read echo
  duration_us = pulseIn(echoPin, HIGH);

  // Calculate distance in cm
  distance_cm = duration_us * 0.01715;

  // Display on LCD
  lcd.clear();
  lcd.setCursor(0, 0); // First line
  lcd.print("Dist: ");

  if (duration_us == 0 || distance_cm > 400 || distance_cm < 2) { // Add range checks
    lcd.print("Out of Range");
    Serial.println("Out of Range");
  } else {
    lcd.print(distance_cm, 1); // Print float with 1 decimal place
    lcd.print(" cm");

    // Print to Serial Monitor as well
    Serial.print("Distance: ");
    Serial.print(distance_cm);
    Serial.println(" cm");

    // Optional: Display a simple bar graph on the second line
    // Max distance for bar graph, e.g., 50 cm
    int maxBarDist = 50;
    int barLength = map(constrain(distance_cm, 0, maxBarDist), 0, maxBarDist, 0, 16); // Map to 16 chars
    lcd.setCursor(0, 1); // Second line
    for (int i = 0; i < barLength; i++) {
      lcd.write((byte)255); // Print a solid block character (custom char often)
                           // Or just lcd.print("#");
    }
  }

  delay(500); // Wait before next measurement
}
```

**Explanation of LCD Code Additions:**
*   Includes `Wire.h` and `LiquidCrystal_I2C.h`.
*   Initializes the `lcd` object.
*   In `setup()`, `lcd.init()` and `lcd.backlight()` are called.
*   In `loop()`:
    *   `lcd.clear()` clears the display before writing new data.
    *   `lcd.setCursor()` positions the text.
    *   `lcd.print()` displays the distance.
    *   Added more robust range checking (`distance_cm > 400 || distance_cm < 2`) for the LCD display.
    *   **Optional Bar Graph:** A simple bar graph is added to the second line of the LCD to visually represent the distance up to `maxBarDist` (e.g., 50cm). `lcd.write((byte)255)` often prints a solid block character, or you can use `"#"` or define a custom character.

**Upload and Test with LCD:**
Your LCD should now display the distance to the nearest object in centimeters.

## Further Extensions & Challenges:

1.  **Units Switch:** Add a button to switch the display between centimeters and inches.
2.  **Parking Assistant:** Mount the sensor on the back of a small robot or a box. As it gets closer to an object (like a wall), make an LED change color (Green for far, Yellow for medium, Red for close) or a buzzer change its beeping rate (faster beeps when closer).
3.  **Intruder Alert Refined:** Use the distance sensor to detect if someone crosses a certain threshold distance in a doorway or hallway, then trigger an alarm. This can be more precise than a PIR for specific zones.
4.  **Water Level Monitor:** Point the sensor downwards at a water surface in a tank to measure the water level (sound reflects off water). You'll need to calibrate for the distance from the sensor to "full" and "empty."
5.  **Averaging Readings:** To get more stable readings, take multiple measurements (e.g., 5) in quick succession and average them before displaying. This can help smooth out occasional erroneous readings.
6.  **NewPing Library:** For more advanced control, error handling (like timeout for `pulseIn`), and potentially easier multi-sensor setups, explore the "NewPing" library by Tim Eckel. It's very popular for HC-SR04 sensors.

## Summary

The ultrasonic distance sensor project has taught you:
*   The principles of ultrasonic ranging with the HC-SR04.
*   How to send a precise trigger pulse and measure the echo pulse duration using `delayMicroseconds()` and `pulseIn()`.
*   Converting time-of-flight to distance.
*   Displaying real-time sensor data.

This sensor is a staple in many robotics projects for navigation and obstacle avoidance, as well as in various measurement and detection applications. You now have another powerful sensing tool in your Arduino toolkit! This concludes the "Building Cool Projects" section. You're now ready to tackle even more advanced topics and dream up your own unique Arduino inventions!
