# Chapter 11: Project 2: Simple Mood Lamp with RGB LED

(Content to be ~1500-2000 words, image integration for RGB LED types, circuit, and final project)

In this project, we'll move beyond single-color LEDs and explore the fascinating world of color mixing with an **RGB (Red, Green, Blue) LED**. We'll create a simple mood lamp that can display various colors and even cycle through them automatically. This project will give you more practice with `analogWrite()` (PWM) and introduce you to how RGB LEDs work.

## Project Overview

An RGB LED combines three separate LEDs (one red, one green, and one blue) into a single package. By controlling the brightness of each of these individual colors using PWM, you can mix them to create a vast spectrum of other colors, including white (if all three are on brightly).

We will:
1.  Learn to control the individual red, green, and blue components of an RGB LED.
2.  Create functions to display specific mixed colors.
3.  Make the RGB LED slowly fade through different colors automatically.

**What you'll learn:**

*   Understanding common anode vs. common cathode RGB LEDs.
*   Controlling multiple PWM outputs simultaneously.
*   The basics of additive color mixing (RGB model).
*   Creating functions to encapsulate color-setting logic.
*   Using `for` loops and `analogWrite()` to create smooth color transitions.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   1 x RGB LED (either Common Anode or Common Cathode - this is important!)
*   3 x Resistors (typically 220Ω or 330Ω, but values might differ based on your RGB LED's specifications – check its datasheet if available. 220Ω is usually a safe start for common 5V RGB LEDs).
*   Jumper Wires (approx. 4-6)

## Understanding RGB LEDs

RGB LEDs typically have 4 pins:
*   One pin for Red.
*   One pin for Green.
*   One pin for Blue.
*   One common pin.

There are two main types of RGB LEDs based on this common pin:

1.  **Common Cathode (CC):**
    *   The common pin is the cathode (-) for all three internal LEDs.
    *   This common pin should be connected to **GND**.
    *   To turn on an individual color (Red, Green, or Blue), you apply a HIGH signal (through a current-limiting resistor) to its respective R, G, or B pin.
    *   Brightness is controlled by applying a PWM signal (0-255) to the R, G, or B pins. `analogWrite(redPin, 255)` means full red brightness.

2.  **Common Anode (CA):**
    *   The common pin is the anode (+) for all three internal LEDs.
    *   This common pin should be connected to **5V** (or your VCC).
    *   To turn on an individual color, you apply a LOW signal (through a current-limiting resistor, though the resistor is often placed on the R, G, B lines) to its respective R, G, or B pin. This creates a voltage difference across the internal LED.
    *   Brightness control is "inverted" compared to common cathode:
        *   `analogWrite(redPin, 0)` means full red brightness (0V on redPin, 5V on common anode = max voltage difference).
        *   `analogWrite(redPin, 255)` means red is OFF (5V on redPin, 5V on common anode = no voltage difference).
        *   To get a specific brightness `b` (where 0 is off, 255 is full on), you'd use `analogWrite(colorPin, 255 - b)`.

**Identifying your RGB LED Type:**
*   **Datasheet:** The best way is to check the datasheet if you have one.
*   **Longest Pin:** Often, the common pin is the longest of the four.
*   **Testing (Carefully!):**
    1.  Assume it's Common Cathode. Connect the longest pin to GND.
    2.  Connect a 220Ω resistor in series with another pin (e.g., the one next to the longest).
    3.  Connect the other end of the resistor to 5V. If that segment of the LED lights up, it's likely common cathode, and the pin you connected the resistor to is one of the R, G, or B pins.
    4.  If it doesn't light up, try assuming Common Anode. Connect the longest pin to 5V. Connect the resistor in series with another pin, and connect the other end of the resistor to GND. If it lights up, it's common anode.
    *Make sure to identify which physical pin corresponds to R, G, and B. This can vary.*

```
[Placeholder for images showing pinouts of a common cathode and a common anode RGB LED.
 Alt Text (CC): "Diagram of a Common Cathode RGB LED, showing Red, Green, Blue pins and the longest Common Cathode (GND) pin."
 Alt Text (CA): "Diagram of a Common Anode RGB LED, showing Red, Green, Blue pins and the longest Common Anode (VCC/5V) pin."]
```
*(Image CC: `images/rgb_led_cc_pinout.png`)*
*(Image CA: `images/rgb_led_ca_pinout.png`)*

For this project, **we will assume a Common Cathode (CC) RGB LED** for the primary code examples. If you have a Common Anode (CA) type, you'll need to adjust the wiring and the `analogWrite()` logic accordingly (as explained later).

## Wiring the RGB LED (Common Cathode Example)

1.  **Disconnect Power.**
2.  **Identify Pins:** Determine which pin on your RGB LED is Red, Green, Blue, and the Common Cathode (longest pin, usually).
3.  **Place RGB LED on Breadboard:** Insert it, ensuring each pin is in a separate row.
4.  **Connect Common Cathode to GND:**
    *   Connect the common cathode pin (longest one) of the RGB LED to the ground rail on your breadboard. Then connect the breadboard ground rail to an Arduino `GND` pin.
5.  **Connect Resistors and Color Pins to Arduino PWM Pins:**
    *   **Red Pin:** Connect a 220Ω resistor from the Red pin of the RGB LED to a row on the breadboard. Then, use a jumper wire to connect that row to Arduino digital PWM pin **~9**.
    *   **Green Pin:** Connect a 220Ω resistor from the Green pin of the RGB LED to another row. Connect that row to Arduino digital PWM pin **~10**.
    *   **Blue Pin:** Connect a 220Ω resistor from the Blue pin of the RGB LED to a third row. Connect that row to Arduino digital PWM pin **~11**.
    *(We use PWM pins ~9, ~10, ~11 because `analogWrite()` requires them).*

```
[Placeholder for a Fritzing diagram of the Common Cathode RGB LED circuit.
 Alt Text: "Circuit: Common Cathode RGB LED on breadboard. Longest pin (cathode) to GND. Red pin via 220 Ohm resistor to Arduino D9. Green pin via 220 Ohm resistor to Arduino D10. Blue pin via 220 Ohm resistor to Arduino D11."]
```
*(Image: `images/rgb_led_cc_circuit.png`)*

## The Code: Controlling Individual Colors

Let's start by writing a sketch to turn each color on and off individually.

```cpp
// Define RGB LED pins (Common Cathode)
const int redPin = 9;   // PWM pin
const int greenPin = 10; // PWM pin
const int bluePin = 11;  // PWM pin

void setup() {
  // Initialize RGB pins as outputs
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(bluePin, OUTPUT);

  Serial.begin(9600); // For messages
}

void loop() {
  Serial.println("Turning RED ON");
  setColor(255, 0, 0); // Full Red, Green off, Blue off
  delay(1000);

  Serial.println("Turning GREEN ON");
  setColor(0, 255, 0); // Red off, Full Green, Blue off
  delay(1000);

  Serial.println("Turning BLUE ON");
  setColor(0, 0, 255); // Red off, Green off, Full Blue
  delay(1000);

  Serial.println("Turning YELLOW ON (Red + Green)");
  setColor(255, 255, 0); // Full Red, Full Green, Blue off
  delay(1000);

  Serial.println("Turning CYAN ON (Green + Blue)");
  setColor(0, 255, 255);
  delay(1000);

  Serial.println("Turning MAGENTA ON (Red + Blue)");
  setColor(255, 0, 255);
  delay(1000);

  Serial.println("Turning WHITE ON (Red + Green + Blue)");
  setColor(255, 255, 255);
  delay(1000);

  Serial.println("Turning ALL OFF");
  setColor(0, 0, 0); // All off
  delay(1000);
}

// Function to set the color of the RGB LED (for Common Cathode)
// Takes Red, Green, and Blue brightness values (0-255)
void setColor(int redValue, int greenValue, int blueValue) {
  analogWrite(redPin, redValue);
  analogWrite(greenPin, greenValue);
  analogWrite(bluePin, blueValue);
}
```

**Explanation:**

*   **`redPin`, `greenPin`, `bluePin`:** Constants for the Arduino PWM pins connected to the RGB LED.
*   **`setup()`:** Sets these pins as `OUTPUT`.
*   **`setColor(int redValue, int greenValue, int blueValue)` function:**
    *   This is a helper function we created to make it easy to set the color.
    *   It takes three integer arguments: the desired brightness for red, green, and blue (0-255).
    *   It uses `analogWrite()` to set the PWM duty cycle for each color pin.
*   **`loop()`:**
    *   Calls `setColor()` with different combinations of red, green, and blue values to display various colors.
    *   `setColor(255, 0, 0);` means Red=255 (full), Green=0 (off), Blue=0 (off) -> results in Red.
    *   `setColor(255, 255, 0);` means Red=full, Green=full, Blue=off -> results in Yellow (additive mixing).
    *   `setColor(255, 255, 255);` means all colors full -> results in White (or close to it).

**Upload and Test:**
Upload the sketch. Your RGB LED should cycle through Red, Green, Blue, Yellow, Cyan, Magenta, White, and then turn Off, repeating the sequence.

**If you have a Common Anode (CA) RGB LED:**

1.  **Wiring Change:** Connect the common anode pin (longest) to **5V** on the Arduino. The resistors still go on the R, G, B lines, and these lines still connect to Arduino PWM pins 9, 10, 11. The other end of the resistors (that were previously going to the R,G,B pins of the LED) now connect to the R,G,B pins of the LED, and the LED's color pins are what the Arduino PWM pins will try to pull LOW.

2.  **Code Change for `setColor` function:**
    You need to invert the logic for `analogWrite()`.
    ```cpp
    // Function to set the color of the RGB LED (for Common ANODE)
    void setColor(int redValue, int greenValue, int blueValue) {
      // For common anode, 0 is full brightness, 255 is off.
      // So we subtract the desired brightness from 255.
      analogWrite(redPin, 255 - redValue);
      analogWrite(greenPin, 255 - greenValue);
      analogWrite(bluePin, 255 - blueValue);
    }
    ```
    The rest of the `loop()` can remain the same because you're still thinking in terms of "I want this much red (0-255)," and the `setColor` function handles the inversion.

## Part 2: Automatic Color Fading (Mood Lamp)

Now, let's make the RGB LED smoothly transition through a spectrum of colors. We'll do this by independently fading each of the R, G, B components up and down using `for` loops.

```cpp
// Define RGB LED pins (Common Cathode)
const int redPin = 9;
const int greenPin = 10;
const int bluePin = 11;

int redValue = 255;   // Initial red value
int greenValue = 0; // Initial green value
int blueValue = 0;  // Initial blue value

// Fade speed (smaller is faster, larger is slower)
int fadeDelay = 20;

void setup() {
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(bluePin, OUTPUT);
  Serial.begin(9600); // Optional for debugging
}

void loop() {
  // Fade from Red to Yellow (Green up, Red stays, Blue stays off)
  Serial.println("Fading Red to Yellow (Green UP)");
  for (greenValue = 0; greenValue <= 255; greenValue++) {
    setColor(redValue, greenValue, blueValue); // red=255, green increasing, blue=0
    delay(fadeDelay);
  }
  // Now color is Yellow (R=255, G=255, B=0)

  // Fade from Yellow to Green (Red down, Green stays, Blue stays off)
  Serial.println("Fading Yellow to Green (Red DOWN)");
  for (redValue = 255; redValue >= 0; redValue--) {
    setColor(redValue, greenValue, blueValue); // red decreasing, green=255, blue=0
    delay(fadeDelay);
  }
  // Now color is Green (R=0, G=255, B=0)

  // Fade from Green to Cyan (Blue up, Green stays, Red stays off)
  Serial.println("Fading Green to Cyan (Blue UP)");
  for (blueValue = 0; blueValue <= 255; blueValue++) {
    setColor(redValue, greenValue, blueValue); // red=0, green=255, blue increasing
    delay(fadeDelay);
  }
  // Now color is Cyan (R=0, G=255, B=255)

  // Fade from Cyan to Blue (Green down, Blue stays, Red stays off)
  Serial.println("Fading Cyan to Blue (Green DOWN)");
  for (greenValue = 255; greenValue >= 0; greenValue--) {
    setColor(redValue, greenValue, blueValue); // red=0, green decreasing, blue=255
    delay(fadeDelay);
  }
  // Now color is Blue (R=0, G=0, B=255)

  // Fade from Blue to Magenta (Red up, Blue stays, Green stays off)
  Serial.println("Fading Blue to Magenta (Red UP)");
  for (redValue = 0; redValue <= 255; redValue++) {
    setColor(redValue, greenValue, blueValue); // red increasing, green=0, blue=255
    delay(fadeDelay);
  }
  // Now color is Magenta (R=255, G=0, B=255)

  // Fade from Magenta to Red (Blue down, Red stays, Green stays off)
  Serial.println("Fading Magenta to Red (Blue DOWN)");
  for (blueValue = 255; blueValue >= 0; blueValue--) {
    setColor(redValue, greenValue, blueValue); // red=255, green=0, blue decreasing
    delay(fadeDelay);
  }
  // Now color is Red (R=255, G=0, B=0) - cycle restarts
}

// Function to set the color of the RGB LED (for Common Cathode)
void setColor(int r, int g, int b) {
  // Constrain values to be between 0 and 255
  r = constrain(r, 0, 255);
  g = constrain(g, 0, 255);
  b = constrain(b, 0, 255);

  analogWrite(redPin, r);
  analogWrite(greenPin, g);
  analogWrite(bluePin, b);

  // Optional: print values for debugging
  // Serial.print("R: "); Serial.print(r);
  // Serial.print(" G: "); Serial.print(g);
  // Serial.print(" B: "); Serial.println(b);
}

// (If using Common Anode, your setColor function would be different:
// void setColor(int r, int g, int b) {
//   r = constrain(r, 0, 255);
//   g = constrain(g, 0, 255);
//   b = constrain(b, 0, 255);
//   analogWrite(redPin, 255 - r);
//   analogWrite(greenPin, 255 - g);
//   analogWrite(bluePin, 255 - b);
// })
```

**Explanation of Fading Code:**

*   **Global Color Variables:** `redValue`, `greenValue`, `blueValue` are now global so their state persists between `for` loops and across calls within `loop()`.
*   **`fadeDelay`:** Controls the speed of the color transitions. A smaller value means faster fades.
*   **Fading Logic:** The `loop()` function now contains a series of `for` loops. Each pair of loops typically holds two color components constant while fading the third one up or down.
    *   Example: "Fade from Red to Yellow"
        *   Red is already at 255, Blue is at 0.
        *   The `for` loop increments `greenValue` from 0 to 255.
        *   `setColor(255, greenValue, 0)` is called repeatedly, making the color shift from pure Red (255,0,0) to Yellow (255,255,0).
*   **`constrain(value, min, max)` function:**
    *   I added `constrain()` inside `setColor()` as good practice, although with the current `for` loop logic, values shouldn't go out of bounds. `constrain()` ensures that a value stays within a defined minimum and maximum range. If `value` is less than `min`, it returns `min`. If `value` is greater than `max`, it returns `max`. Otherwise, it returns `value`. This prevents `analogWrite` from receiving values outside 0-255.

**Upload and Test:**
Your RGB LED should now smoothly cycle through a rainbow of colors: Red -> Yellow -> Green -> Cyan -> Blue -> Magenta -> and back to Red. Adjust `fadeDelay` to change the speed.

## Further Extensions & Challenges:

1.  **Potentiometer Control:**
    *   Add three potentiometers.
    *   Read the values from each potentiometer (0-1023).
    *   Map these values to the 0-255 range.
    *   Use these mapped values to control the Red, Green, and Blue components of the RGB LED directly, allowing you to manually "dial in" any color.
2.  **Button-Selected Colors:**
    *   Add one or more push buttons.
    *   Make each button press cycle the LED to a different predefined color (e.g., button 1: Red, Green, Blue; button 2: Yellow, Cyan, Magenta).
3.  **Random Colors:**
    *   Use the `random(min, max)` function to generate random values (0-255) for Red, Green, and Blue.
    *   Make the LED switch to a new random color every few seconds. Or, make it fade smoothly to a new random color.
    *   `random(256)` will give a number from 0 to 255.
4.  **More Complex Fading Pattern:**
    *   Try to create a fade where all three R, G, B values change simultaneously but at slightly different rates or with offsets to create more complex and subtle color shifts. This might involve more intricate math or lookup tables.
5.  **Sound Reactivity (Advanced):**
    *   If you have a sound sensor or microphone module, try to make the RGB LED change color or brightness based on the ambient sound level.

## Summary

The RGB LED mood lamp project has introduced you to:
*   The difference between Common Anode and Common Cathode RGB LEDs.
*   Wiring and controlling RGB LEDs using three PWM pins.
*   The principle of additive color mixing (R+G=Yellow, R+B=Magenta, G+B=Cyan, R+G+B=White).
*   Creating helper functions (`setColor`) to simplify your code.
*   Using `for` loops and `analogWrite()` to create smooth fading effects between colors.

You now have the power to add a vibrant splash of color to your Arduino projects! This is a fun component with many creative possibilities. Experiment with different color combinations and fading patterns to see what you can create.
