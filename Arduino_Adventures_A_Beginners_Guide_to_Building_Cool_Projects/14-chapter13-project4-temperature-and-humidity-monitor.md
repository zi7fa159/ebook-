# Chapter 13: Project 4: Temperature and Humidity Monitor

(Content to be ~1500-2000 words, image integration for DHT sensor, LCD, circuit, and final project)

In this project, we'll build a practical device: a digital thermometer and hygrometer (humidity sensor). We'll use a common sensor, the **DHT11** (or DHT22/AM2302, which is more accurate but similar to interface), to read ambient temperature and humidity. We'll then display these readings on the Serial Monitor and, as an optional extension, on an I2C LCD screen.

## Project Overview

Monitoring environmental conditions like temperature and humidity is useful in many applications, from home comfort to agriculture and scientific experiments. The DHT series of sensors are popular for hobbyists because they are inexpensive and relatively easy to use, providing digital output for both temperature and humidity with a single data wire.

We will:
1.  Learn about DHT sensors (DHT11/DHT22).
2.  Install and use the DHT sensor library.
3.  Read temperature and humidity data from the sensor.
4.  Display the readings on the Serial Monitor.
5.  (Optional Extension) Display readings on an I2C LCD.

**What you'll learn:**

*   Working with a digital sensor that requires a specific communication protocol.
*   Using an Arduino library to simplify sensor interfacing.
*   Reading and interpreting multiple data points (temperature and humidity) from a single sensor.
*   Basic error checking when reading sensor data.
*   (Optional) Interfacing with an I2C LCD display.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   1 x DHT11 (blue) or DHT22/AM2302 (white) Temperature and Humidity Sensor
    *   *(Note: DHT11 is less accurate and has a smaller range than DHT22. Code is mostly compatible, but you need to specify the type in the library.)*
    *   Some DHT sensors come as a 3-pin module with a built-in pull-up resistor. Others are 4-pin bare sensors; if you have a 4-pin one, one pin is not used, and you'll likely need an external pull-up resistor (4.7kΩ to 10kΩ) for the data line. We'll assume the common 3-pin module version for simplicity in wiring, or a 4-pin sensor where you add the resistor.
*   (If using a 4-pin bare DHT sensor and it doesn't have an onboard pull-up) 1 x 4.7kΩ or 10kΩ Resistor.
*   Jumper Wires (approx. 3-4)
*   **(Optional, for LCD Display Extension):**
    *   1 x I2C LCD Module (e.g., 16x2 or 20x4 character LCD with an I2C backpack)
    *   4 x Jumper Wires (for I2C LCD)

## Understanding DHT Sensors (DHT11 / DHT22)

DHT sensors are pre-calibrated digital sensors that measure relative humidity and temperature.

*   **DHT11 (Blue):**
    *   Temperature Range: 0-50 °C (±2 °C accuracy)
    *   Humidity Range: 20-80% RH (±5% accuracy)
    *   Sampling Rate: 1 Hz (can only get a reading every 1 second)
*   **DHT22 / AM2302 (White):**
    *   Temperature Range: -40 to +80 °C (±0.5 °C accuracy)
    *   Humidity Range: 0-100% RH (±2-5% accuracy)
    *   Sampling Rate: 0.5 Hz (can only get a reading every 2 seconds)

**Pins (Common 3-pin module or 4-pin sensor):**
*   **VCC / +:** Power (3V to 5.5V DC for DHT11; 3.3V to 6V for DHT22)
*   **DATA / OUT:** Digital data signal (this is where the sensor communicates with the Arduino)
*   **GND / -:** Ground
*   **NC (Not Connected):** On 4-pin sensors, one pin is usually not connected. Check the datasheet or markings.

```
[Placeholder for images of a DHT11 sensor module and a DHT22 sensor.
 Alt Text DHT11: "A blue DHT11 temperature and humidity sensor module with three pins: VCC, DATA, GND."
 Alt Text DHT22: "A white DHT22 (AM2302) temperature and humidity sensor with pins labeled."]
```
*(Image DHT11: `images/dht11_module.png`)*
*(Image DHT22: `images/dht22_sensor.png`)*

**Communication Protocol:**
DHT sensors use a custom single-wire digital communication protocol. The Arduino sends a start signal, and the DHT sensor responds with a 40-bit data packet containing humidity, temperature, and checksum information. Thankfully, we don't need to implement this protocol ourselves; there are excellent Arduino libraries that handle it.

**Pull-up Resistor:**
The DATA line needs a pull-up resistor (typically 4.7kΩ to 10kΩ) connected between the DATA pin and VCC. Many DHT modules have this resistor already built-in. If you have a bare 4-pin sensor, you'll usually need to add this resistor externally.

## Installing the DHT Sensor Library

To easily communicate with DHT sensors, we'll use a library. A very popular one is the "DHT sensor library" by Adafruit.

1.  **Open Arduino IDE.**
2.  Go to **Sketch > Include Library > Manage Libraries...**
3.  In the Library Manager search box, type "**DHT sensor library**".
4.  Look for "DHT sensor library by Adafruit". Click on it.
5.  Click the **Install** button.
    ```
    [Placeholder for a screenshot of the Arduino IDE Library Manager showing the "DHT sensor library by Adafruit" selected and ready for installation.
     Alt Text: "Arduino IDE Library Manager with 'DHT sensor library by Adafruit' highlighted, showing an Install button."]
    ```
    *(Image: `images/dht_library_install.png`)*
6.  **Install Dependency:** This library often depends on another library called "Adafruit Unified Sensor". If the IDE prompts you to install this dependency as well ("Install all"), click "Install all".
7.  Close the Library Manager when done.

## Wiring the DHT Sensor

We'll assume a common 3-pin DHT module (which includes the pull-up resistor) or a 4-pin sensor where you add the pull-up if needed.

1.  **Disconnect Power.**
2.  **Wire the DHT Sensor:**
    *   Connect the **VCC** (or `+`) pin of the DHT sensor to the **5V** pin on the Arduino.
    *   Connect the **GND** (or `-`) pin of the DHT sensor to a **GND** pin on the Arduino.
    *   Connect the **DATA** (or `OUT`) pin of the DHT sensor to Arduino digital pin **2**.
3.  **(If using a 4-pin bare sensor WITHOUT an internal pull-up):**
    *   Connect a 4.7kΩ or 10kΩ resistor between the **DATA** pin of the DHT sensor and the **VCC** (5V) pin. This resistor "pulls up" the data line to HIGH when it's not being actively driven LOW by the sensor or Arduino.

```
[Placeholder for a Fritzing diagram of the DHT11/DHT22 sensor connected to Arduino.
 Alt Text: "Circuit: DHT sensor VCC to Arduino 5V, GND to Arduino GND, DATA pin to Arduino D2. If external pull-up needed, a 10k resistor is shown between DATA and 5V."]
```
*(Image: `images/dht_sensor_circuit.png`)*

## The Code: Reading Temperature and Humidity (Serial Monitor)

```cpp
#include "DHT.h" // Include the DHT library

// Define the type of DHT sensor you are using:
#define DHTTYPE DHT11     // If using DHT11
// #define DHTTYPE DHT22  // If using DHT22 (AM2302)
// #define DHTTYPE DHT21  // If using DHT21 (AM2301)

// Define the digital pin the DHT sensor's DATA pin is connected to
const int DHTPIN = 2;

// Initialize the DHT sensor object
DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(9600);
  Serial.println("DHT Sensor Test - Temperature & Humidity Monitor");

  dht.begin(); // Initialize the sensor

  Serial.println("------------------------------------");
}

void loop() {
  // Wait a few seconds between measurements.
  // DHT11 can be read once per second.
  // DHT22 can be read once per two seconds.
  delay(2000); // Delay 2 seconds - safe for both DHT11 and DHT22

  // Reading temperature or humidity takes about 250 milliseconds.
  // Sensor readings may also be up to 2 seconds old (its a very slow sensor)
  float humidity = dht.readHumidity();          // Read humidity as a percentage
  float temperature_c = dht.readTemperature();  // Read temperature as Celsius (the default)
  float temperature_f = dht.readTemperature(true); // Read temperature as Fahrenheit

  // Check if any reads failed and exit early (to try again).
  if (isnan(humidity) || isnan(temperature_c) || isnan(temperature_f)) {
    Serial.println("Failed to read from DHT sensor!");
    return; // Exit the loop function to try again on the next iteration
  }

  // Compute heat index in Fahrenheit (the default)
  // float heatIndex_f = dht.computeHeatIndex(temperature_f, humidity);
  // Compute heat index in Celsius
  // float heatIndex_c = dht.computeHeatIndex(temperature_c, humidity, false);


  // Print the readings to the Serial Monitor
  Serial.print("Humidity: ");
  Serial.print(humidity);
  Serial.print("%  |  ");

  Serial.print("Temperature: ");
  Serial.print(temperature_c);
  Serial.print(" °C  ~  ");
  Serial.print(temperature_f);
  Serial.println(" °F");

  // Optional: Print Heat Index
  // Serial.print("Heat Index: ");
  // Serial.print(heatIndex_c);
  // Serial.print(" °C  ~  ");
  // Serial.print(heatIndex_f);
  // Serial.println(" °F");

  Serial.println("------------------------------------");
}
```

**Explanation:**

*   **`#include "DHT.h"`:** This line includes the DHT library code, making its functions available to your sketch.
*   **`#define DHTTYPE DHT11` (or `DHT22`, `DHT21`):**
    *   This is very important! You **must** uncomment the line corresponding to the type of DHT sensor you are using. The library needs to know which sensor it's talking to, as their internal timings and data formats differ slightly.
*   **`const int DHTPIN = 2;`:** Defines the Arduino pin connected to the DHT's DATA line.
*   **`DHT dht(DHTPIN, DHTTYPE);`:** This line creates a `DHT` object named `dht`. You'll use this object to interact with the sensor. It's initialized with the pin number and sensor type.
*   **`setup()`:**
    *   Starts Serial communication.
    *   `dht.begin();` initializes the DHT sensor. It's important to call this once.
*   **`loop()`:**
    *   **`delay(2000);`:** DHT sensors are slow. You shouldn't try to read them too frequently. Once every 2 seconds is a safe interval for both DHT11 and DHT22.
    *   **`float humidity = dht.readHumidity();`:** Calls the `readHumidity()` function from the `dht` object to get the humidity reading. It returns a `float` value (percentage).
    *   **`float temperature_c = dht.readTemperature();`:** Reads temperature in Celsius (default).
    *   **`float temperature_f = dht.readTemperature(true);`:** Reads temperature in Fahrenheit (by passing `true` as an argument).
    *   **Error Checking `isnan(...)`:**
        *   `isnan()` is a standard C function that checks if a floating-point number is "Not a Number."
        *   If the DHT sensor fails to provide a valid reading (due to timing issues, bad connection, or sensor fault), the library functions will return `NaN`.
        *   It's crucial to check for this. If a read fails, the code prints an error message and uses `return;` to exit the `loop()` function early, effectively skipping the rest of the current iteration and trying again after the next `delay()`.
    *   **`dht.computeHeatIndex(...)` (Optional):** The library can also calculate the heat index (apparent temperature) if you provide it with temperature and humidity. This is commented out but available.
    *   **Printing Readings:** The rest of the `loop()` formats and prints the humidity and temperature values to the Serial Monitor.

**Upload and Test:**
1.  **Make sure you've selected the correct `DHTTYPE` in the code!**
2.  Upload the sketch.
3.  Open the Serial Monitor (Tools > Serial Monitor, set baud rate to 9600).
4.  After a brief moment (the first reading might take a couple of seconds or fail as the sensor initializes), you should start seeing temperature and humidity readings printed every 2 seconds.
    *   Example output:
        ```
        DHT Sensor Test - Temperature & Humidity Monitor
        ------------------------------------
        Humidity: 52.00%  |  Temperature: 23.50 °C  ~  74.30 °F
        ------------------------------------
        Humidity: 52.30%  |  Temperature: 23.60 °C  ~  74.48 °F
        ------------------------------------
        ```
5.  Try breathing on the sensor. You should see the humidity reading increase and possibly the temperature change slightly.

**Troubleshooting:**
*   **"Failed to read from DHT sensor!"**:
    *   Double-check your wiring: VCC to 5V, GND to GND, DATA to the correct Arduino pin (D2 in our example).
    *   Ensure `DHTPIN` in the code matches your wiring.
    *   Ensure `DHTTYPE` is correct for your sensor.
    *   If using a 4-pin bare sensor, make sure you have the pull-up resistor between DATA and VCC (5V). If your module has it, you don't need an extra one.
    *   Try a longer delay in `loop()` (e.g., `delay(3000);`).
    *   The sensor might be faulty.
*   **Unrealistic Readings (e.g., 0% humidity, -40°C):** Often also a sign of wiring problems, incorrect `DHTTYPE`, or a missing pull-up resistor.

## Optional Extension: Displaying on an I2C LCD

Displaying the data on an LCD makes the project standalone. We'll use a common I2C LCD module (usually a 16x2 or 20x4 character LCD with an I2C "backpack" board soldered to it).

### Installing the I2C LCD Library:

A common library is "LiquidCrystal I2C" by Frank de Brabander.
1.  In Arduino IDE: **Sketch > Include Library > Manage Libraries...**
2.  Search for "**LiquidCrystal I2C**".
3.  Install the one by Frank de Brabander (or a similar popular one).
    ```
    [Placeholder for screenshot of Library Manager showing "LiquidCrystal I2C" by Frank de Brabander.
     Alt Text: "Arduino IDE Library Manager with 'LiquidCrystal I2C by Frank de Brabander' highlighted for installation."]
    ```
    *(Image: `images/i2c_lcd_library_install.png`)*

### Wiring the I2C LCD:

I2C LCD modules typically have 4 pins:
*   **GND:** Connect to Arduino GND.
*   **VCC:** Connect to Arduino 5V.
*   **SDA (Serial Data):** Connect to Arduino **A4** (this is the dedicated SDA pin on Uno).
*   **SCL (Serial Clock):** Connect to Arduino **A5** (this is the dedicated SCL pin on Uno).

```
[Placeholder for a Fritzing diagram showing the DHT sensor AND an I2C LCD connected to the Arduino.
 Alt Text: "Circuit: DHT sensor as before. I2C LCD: GND to Arduino GND, VCC to 5V, SDA to Arduino A4, SCL to Arduino A5."]
```
*(Image: `images/dht_i2c_lcd_circuit.png`)*

### Finding Your LCD's I2C Address:

Each I2C device has an address. Most common LCD backpacks use address `0x27` or `0x3F`. If your LCD doesn't work, you might need to run an "I2C Scanner" sketch to find its address. (Search "Arduino I2C Scanner sketch" online – many examples are available. Upload it, open Serial Monitor, and it will tell you the address of connected I2C devices).

### Code with I2C LCD Display:

```cpp
#include "DHT.h"
#include <Wire.h> // Required for I2C communication
#include <LiquidCrystal_I2C.h> // Include the I2C LCD library

// --- DHT Sensor Configuration ---
#define DHTTYPE DHT11
// #define DHTTYPE DHT22
const int DHTPIN = 2;
DHT dht(DHTPIN, DHTTYPE);

// --- I2C LCD Configuration ---
// Set the LCD address (common ones are 0x27 or 0x3F - find yours with an I2C scanner if needed)
// Set LCD dimensions (16 columns, 2 rows for 16x2 LCD)
LiquidCrystal_I2C lcd(0x27, 16, 2); // lcd(I2C_ADDRESS, COLUMNS, ROWS)

void setup() {
  Serial.begin(9600); // Still useful for debugging
  Serial.println("DHT Sensor with I2C LCD");

  dht.begin(); // Initialize DHT sensor

  lcd.init();      // Initialize the LCD
  lcd.backlight(); // Turn on the LCD backlight
  lcd.setCursor(0, 0); // Set cursor to first column, first row
  lcd.print("Temp & Humidity");
  delay(1500);
  lcd.clear();
}

void loop() {
  delay(2000); // Wait between measurements

  float h = dht.readHumidity();
  float t = dht.readTemperature(); // Celsius by default

  if (isnan(h) || isnan(t)) {
    Serial.println("Failed to read from DHT sensor!");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("DHT Read Error");
    return;
  }

  // Display on LCD
  lcd.clear(); // Clear previous readings

  // Display Temperature on first line
  lcd.setCursor(0, 0); // Column 0, Row 0
  lcd.print("Temp: ");
  lcd.print(t, 1); // Print float 't' with 1 decimal place
  lcd.print((char)223); // Degree symbol (might vary with LCD character sets)
  lcd.print("C");

  // Display Humidity on second line
  lcd.setCursor(0, 1); // Column 0, Row 1
  lcd.print("Humidity: ");
  lcd.print(h, 0); // Print float 'h' with 0 decimal places
  lcd.print("%");

  // Also print to Serial Monitor for debugging
  Serial.print("Temp: "); Serial.print(t); Serial.print(" *C, ");
  Serial.print("Humidity: "); Serial.print(h); Serial.println(" %");
}
```

**Explanation of LCD Code Additions:**

*   **`#include <Wire.h>`:** Necessary for I2C communication.
*   **`#include <LiquidCrystal_I2C.h>`:** The I2C LCD library.
*   **`LiquidCrystal_I2C lcd(0x27, 16, 2);`:** Creates an `lcd` object.
    *   `0x27` is a common I2C address. **Change this if your LCD uses a different address.**
    *   `16, 2` specifies a 16-column, 2-row LCD. Change if you have a 20x4, etc.
*   **`setup()`:**
    *   `lcd.init();` (or sometimes `lcd.begin();` depending on library version) initializes the LCD.
    *   `lcd.backlight();` turns the backlight on.
    *   `lcd.setCursor(col, row);` moves the cursor to a specific position (0-indexed).
    *   `lcd.print("Text");` prints text to the LCD.
    *   `lcd.clear();` clears the LCD screen.
*   **`loop()`:**
    *   After reading DHT values and checking for errors, it clears the LCD.
    *   Uses `lcd.setCursor()` and `lcd.print()` to display formatted temperature and humidity.
    *   `lcd.print(t, 1);` prints the float `t` with 1 decimal place.
    *   `lcd.print((char)223);` attempts to print the degree symbol °. The character code might vary for your specific LCD; some common ones are 223 or 0xDF. Sometimes `lcd.write(B11011111);` or `lcd.write((byte)0);` with custom characters is needed if the default font doesn't have it.

**Upload and Test with LCD:**
If wired correctly and the I2C address is right, your LCD should now display the live temperature and humidity readings.

## Further Extensions & Challenges:

1.  **DHT22/AM2302:** If you have a DHT22, use it instead of DHT11 (remember to change `DHTTYPE`). Compare its accuracy and stability.
2.  **Data Logging:** Store readings in an array or send them to a computer over serial to log data over time (e.g., for plotting in a spreadsheet).
3.  **Min/Max Display:** Keep track of the minimum and maximum temperature/humidity recorded since the Arduino started and display these as well.
4.  **Comfort Zone Indicator:** Define a "comfort zone" (e.g., 20-25°C and 40-60% humidity). Display a message like "Comfortable," "Too Cold," "Too Humid," etc., based on the readings.
5.  **Alerts:** If temperature or humidity goes outside a certain range, trigger an LED or buzzer.

## Summary

This temperature and humidity monitor project has shown you:
*   How to work with DHT11/DHT22 sensors.
*   The importance of using libraries to simplify complex sensor communication.
*   Reading multiple values (temperature and humidity) and performing error checks.
*   Displaying data on the Serial Monitor and optionally on an I2C LCD.

You've built a useful environmental monitor! This project is a great example of how Arduino can gather data from the real world and present it in a human-readable format.
