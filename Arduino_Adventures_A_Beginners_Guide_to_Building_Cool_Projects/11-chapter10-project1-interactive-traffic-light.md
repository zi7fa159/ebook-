# Chapter 10: Project 1: Interactive Traffic Light

(Content to be ~1500-2000 words, image integration for circuit and final project)

Welcome to your first full-fledged project in "Arduino Adventures"! We're going to build an interactive traffic light system. This project will solidify your understanding of digital outputs, `delay()`, and basic sequencing. We'll then add a push button to make it "interactive," simulating a pedestrian crossing button.

## Project Overview

We'll create a miniature traffic light with Red, Yellow, and Green LEDs. Initially, it will cycle through the standard light sequence automatically. Then, we'll add a button that, when pressed, will trigger a pedestrian crossing sequence (e.g., make the lights change to allow pedestrians to cross).

**What you'll learn:**

*   Controlling multiple LEDs independently.
*   Creating timed sequences using `delay()`.
*   Reading digital input from a button.
*   Using `if` statements to change program behavior based on input.
*   Structuring a slightly more complex sketch.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   1 x Red LED
*   1 x Yellow LED
*   1 x Green LED
*   3 x 220Ω or 330Ω Resistors (for the LEDs)
*   1 x Push Button (tactile switch)
*   (Optional, if not using `INPUT_PULLUP` for the button: 1 x 10kΩ Resistor for pull-down/pull-up)
*   Jumper Wires (approx. 7-8)

## Part 1: Automatic Traffic Light Sequence

Let's start by making the traffic light cycle automatically.

### Wiring the LEDs:

1.  **Disconnect Power:** Ensure your Arduino is not connected to power.
2.  **Place LEDs:**
    *   Insert the Red, Yellow, and Green LEDs into the breadboard, spaced apart. Remember the anode (longer leg, +) and cathode (shorter leg, -). Keep all cathodes pointing in the same direction (e.g., towards the ground rail) for easier wiring.
3.  **Connect Resistors:**
    *   For each LED, connect a 220Ω (or 330Ω) resistor to its **cathode** (shorter leg).
    *   Connect the other end of each resistor to a common ground point on the breadboard (e.g., the blue (-) power rail).
4.  **Connect to Arduino Ground:**
    *   Use a jumper wire to connect the breadboard's ground rail (where the resistors are connected) to one of the `GND` pins on the Arduino.
5.  **Connect to Arduino Digital Pins:**
    *   Connect the **anode** (longer leg, +) of the Red LED to digital pin 12 on the Arduino.
    *   Connect the **anode** of the Yellow LED to digital pin 11 on the Arduino.
    *   Connect the **anode** of the Green LED to digital pin 10 on the Arduino.

```
[Placeholder for a Fritzing diagram or clear photo of the traffic light LED circuit.
 Alt Text: "Circuit diagram: Red LED anode to Arduino D12, Yellow LED anode to D11, Green LED anode to D10. Each LED's cathode connected through a 220 Ohm resistor to a common GND rail, which is then connected to Arduino GND."]
```
*(Image: `images/traffic_light_leds_circuit.png` - This image will be searched and integrated later)*

### The Code (Automatic Sequence):

```cpp
// Define LED pins
const int redLedPin = 12;
const int yellowLedPin = 11;
const int greenLedPin = 10;

void setup() {
  // Initialize LED pins as outputs
  pinMode(redLedPin, OUTPUT);
  pinMode(yellowLedPin, OUTPUT);
  pinMode(greenLedPin, OUTPUT);

  // Optional: Start with all lights off or red on
  digitalWrite(redLedPin, HIGH); // Start with red light on
  digitalWrite(yellowLedPin, LOW);
  digitalWrite(greenLedPin, LOW);
  delay(1000); // Initial red light duration
}

void loop() {
  // Standard traffic light sequence: Green -> Yellow -> Red

  // 1. Green Light ON
  digitalWrite(greenLedPin, HIGH);
  digitalWrite(redLedPin, LOW);    // Ensure red is off
  digitalWrite(yellowLedPin, LOW); // Ensure yellow is off
  delay(5000); // Green light duration (5 seconds)

  // 2. Yellow Light ON
  digitalWrite(greenLedPin, LOW);
  digitalWrite(yellowLedPin, HIGH);
  // Red remains off
  delay(2000); // Yellow light duration (2 seconds)

  // 3. Red Light ON
  digitalWrite(yellowLedPin, LOW);
  digitalWrite(redLedPin, HIGH);
  // Green remains off
  delay(5000); // Red light duration (5 seconds)

  // The loop will now repeat, going back to Green
}
```

**Explanation:**

*   **Constants for Pins:** We define `const int` variables for our LED pins for readability and easy modification.
*   **`setup()`:**
    *   We set all LED pins to `OUTPUT`.
    *   Optionally, we start the sequence with the red light on for a second.
*   **`loop()`:**
    *   **Green Phase:** Turns green LED HIGH, ensures red and yellow are LOW. Waits for 5 seconds.
    *   **Yellow Phase:** Turns green LOW, yellow HIGH. Waits for 2 seconds.
    *   **Red Phase:** Turns yellow LOW, red HIGH. Waits for 5 seconds.
    *   The `loop()` then repeats, starting the sequence again from green.

**Upload and Test:**
Upload this sketch to your Arduino. You should see your traffic light cycle through green, yellow, and red, with the specified timings. Adjust the `delay()` values to change the duration of each light.

## Part 2: Adding an Interactive Pedestrian Button

Now, let's add a push button. When a "pedestrian" presses this button, the traffic light should (after a reasonable delay if it's green or yellow for cars) turn red for cars, and then we'd ideally have a "walk" signal (we can simulate this by blinking the green LED or turning it on steadily for a short period, as we don't have a separate "walk" light in this basic setup).

### Wiring the Button:

We'll use the `INPUT_PULLUP` method for simplicity.

1.  **Disconnect Power.**
2.  **Place Button:** Insert the push button onto your breadboard, straddling the central divide if it's a 4-pin tactile switch.
3.  **Connect to Arduino Digital Pin:**
    *   Connect one leg of the push button to digital pin 2 on the Arduino.
4.  **Connect to Arduino Ground:**
    *   Connect the leg diagonally opposite (or adjacent on the same side, depending on the button's internal connections – test if unsure) to `GND` on the Arduino (or the breadboard's ground rail).

```
[Placeholder for an updated Fritzing diagram showing the LEDs and the button connected. Button to D2 and GND.
 Alt Text: "Circuit diagram updated to include a push button. One leg of button to Arduino D2, other leg to GND. LED wiring remains the same."]
```
*(Image: `images/traffic_light_button_circuit.png`)*

### The Code (Interactive Sequence):

This will be more complex as we need to manage states and respond to the button. We'll create a simplified pedestrian phase.

```cpp
// Define LED pins
const int redLedPin = 12;
const int yellowLedPin = 11;
const int greenLedPin = 10;

// Define Button pin
const int buttonPin = 2;

// Variables for button state
int buttonState = 0;
int lastButtonState = HIGH; // Assume button is not pressed initially (due to INPUT_PULLUP)
boolean pedestrianRequest = false;

// Timing variables (in milliseconds)
unsigned long greenDuration = 5000;
unsigned long yellowDuration = 2000;
unsigned long redDuration = 5000;
unsigned long pedestrianWalkTime = 3000; // Time for "pedestrians to walk"

// State machine variables (optional, but good for more complex logic)
enum LightState { GREEN, YELLOW_TO_RED, RED, PED_YELLOW, PED_WALK };
LightState currentLightState = RED; // Start with red

unsigned long lastStateChangeTime = 0; // To manage timing without blocking delays too much

void setup() {
  pinMode(redLedPin, OUTPUT);
  pinMode(yellowLedPin, OUTPUT);
  pinMode(greenLedPin, OUTPUT);
  pinMode(buttonPin, INPUT_PULLUP); // Enable internal pull-up for the button

  Serial.begin(9600); // For debugging

  // Initial state: Red light on
  digitalWrite(redLedPin, HIGH);
  digitalWrite(yellowLedPin, LOW);
  digitalWrite(greenLedPin, LOW);
  lastStateChangeTime = millis(); // Initialize timer
}

void loop() {
  // Read the button (simple debounce might be needed for real projects)
  buttonState = digitalRead(buttonPin);
  if (buttonState == LOW && lastButtonState == HIGH) { // Button pressed (LOW due to INPUT_PULLUP)
    pedestrianRequest = true;
    Serial.println("Pedestrian request received!");
  }
  lastButtonState = buttonState;

  // --- Main Traffic Light State Machine ---
  unsigned long currentTime = millis();

  switch (currentLightState) {
    case GREEN:
      digitalWrite(greenLedPin, HIGH);
      digitalWrite(yellowLedPin, LOW);
      digitalWrite(redLedPin, LOW);
      if (pedestrianRequest || (currentTime - lastStateChangeTime >= greenDuration)) {
        currentLightState = YELLOW_TO_RED;
        lastStateChangeTime = currentTime;
        Serial.println("State: GREEN -> YELLOW_TO_RED");
      }
      break;

    case YELLOW_TO_RED:
      digitalWrite(greenLedPin, LOW);
      digitalWrite(yellowLedPin, HIGH);
      digitalWrite(redLedPin, LOW);
      if (currentTime - lastStateChangeTime >= yellowDuration) {
        currentLightState = RED;
        lastStateChangeTime = currentTime;
        Serial.println("State: YELLOW_TO_RED -> RED");
      }
      break;

    case RED:
      digitalWrite(greenLedPin, LOW);
      digitalWrite(yellowLedPin, LOW);
      digitalWrite(redLedPin, HIGH);
      if (pedestrianRequest && (currentTime - lastStateChangeTime >= 1000)) { // Small delay before ped walk
        // Transition to pedestrian walk sequence
        pedestrianRequest = false; // Service the request
        currentLightState = PED_WALK; // Or a PED_YELLOW first if desired
        lastStateChangeTime = currentTime;
        Serial.println("State: RED -> PED_WALK");
      } else if (!pedestrianRequest && (currentTime - lastStateChangeTime >= redDuration)) {
        currentLightState = GREEN;
        lastStateChangeTime = currentTime;
        Serial.println("State: RED -> GREEN (No ped request)");
      }
      break;

    // Pedestrian phase (simplified: green light acts as "walk" signal)
    case PED_WALK:
      // For cars: Red light stays ON
      digitalWrite(redLedPin, HIGH);
      digitalWrite(yellowLedPin, LOW);
      // For pedestrians: Green light simulates "WALK"
      digitalWrite(greenLedPin, HIGH);
      if (currentTime - lastStateChangeTime >= pedestrianWalkTime) {
        // After walk time, go back to normal red for cars before switching to green
        // Or add a "flashing don't walk" (e.g. blink yellow)
        currentLightState = RED; // Return to a normal RED state
        lastStateChangeTime = currentTime;
        Serial.println("State: PED_WALK -> RED (Ped phase end)");
        // Ensure green (ped walk) is off for the next car red phase
        digitalWrite(greenLedPin, LOW);
      }
      break;

      // PED_YELLOW could be added here for flashing "don't walk"
  }
}
```

**Explanation of Interactive Code:**

*   **Button Handling:**
    *   `pinMode(buttonPin, INPUT_PULLUP);` sets up the button.
    *   We read `buttonState`. If it's `LOW` (pressed) and `lastButtonState` was `HIGH` (meaning it was just pressed, a simple way to detect a press event rather than holding), we set `pedestrianRequest = true;`.
*   **State Machine:**
    *   We introduce an `enum LightState` to define different states of our traffic light system: `GREEN`, `YELLOW_TO_RED`, `RED`, `PED_WALK`. (A `PED_YELLOW` for flashing "don't walk" could be added).
    *   `currentLightState` holds the active state.
    *   `lastStateChangeTime` and `millis()` are used for non-blocking timing. Instead of long `delay()` calls that freeze the program, we check if enough time has passed since the last state change. This allows the Arduino to still check for button presses frequently.
*   **`switch (currentLightState)`:** This structure handles the logic for each state.
    *   **`GREEN`:** Green light is on. If `pedestrianRequest` is true OR the `greenDuration` has passed, it transitions to `YELLOW_TO_RED`.
    *   **`YELLOW_TO_RED`:** Yellow light is on. After `yellowDuration`, it transitions to `RED`.
    *   **`RED`:** Red light is on.
        *   If `pedestrianRequest` is true (and a small safety delay has passed), it services the request by setting `pedestrianRequest = false;` and transitioning to `PED_WALK`.
        *   If there's no pedestrian request and `redDuration` has passed, it transitions to `GREEN`.
    *   **`PED_WALK`:** This is our simplified pedestrian phase. The car red light remains ON. The green LED is turned ON to simulate a "WALK" signal for pedestrians. After `pedestrianWalkTime`, it transitions back to `RED` (to ensure cars still see red before the cycle might eventually go to green for cars).
*   **Non-Blocking Timing:** Using `millis() - lastStateChangeTime >= duration` is a common non-blocking way to handle time intervals. It allows the `loop()` to run very quickly, constantly checking the button and updating states.

**Upload and Test:**
1.  Upload this more complex sketch.
2.  Observe the normal traffic light cycle.
3.  Press the button.
    *   If the light is green for cars, it should soon turn yellow, then red. After a moment on red, the green LED (our "walk" signal) should turn on for `pedestrianWalkTime`.
    *   If the light is already red for cars when you press the button, after a short delay, the green LED ("walk") should turn on.
    *   After the "walk" period, the system returns to a normal red state for cars, and then eventually proceeds to green for cars if no further pedestrian requests are made.

This version is more advanced due to the non-blocking timing and state machine concept. It's a more robust way to handle multiple timed events and user input simultaneously.

## Further Extensions & Challenges:

1.  **Add a "Don't Walk" Signal:**
    *   Use the yellow LED to flash for a few seconds after the `PED_WALK` phase before returning to a solid red for cars. This would involve adding another state like `PED_FLASHING_DONT_WALK`.
2.  **Separate Pedestrian Lights:**
    *   If you have more LEDs (e.g., another red and green, or a white one for "walk"), create dedicated pedestrian signals.
3.  **Debounce the Button:**
    *   Push buttons can "bounce," meaning they might register multiple presses very quickly when you only press them once. For more critical applications, you'd add debouncing logic (either with `delay()` or a more advanced `millis()`-based technique) to ensure only one press is registered. (Search "Arduino button debounce" for examples).
4.  **Adjust Timings:** Play with the `greenDuration`, `yellowDuration`, `redDuration`, and `pedestrianWalkTime` variables to simulate different traffic scenarios.
5.  **Night Mode:**
    *   Add a photoresistor (LDR). If it's dark, make the traffic light switch to a flashing yellow (for cars) or flashing red mode, and perhaps disable the pedestrian button or change its behavior.

## Summary

This interactive traffic light project has taken you through:
*   Controlling multiple digital outputs (LEDs).
*   Implementing timed sequences.
*   Reading digital input from a button with `INPUT_PULLUP`.
*   Using conditional logic (`if`, `switch`) to create responsive behavior.
*   Introducing a basic state machine and non-blocking timing with `millis()` for more complex interactions.

You've built a system that not only runs automatically but also reacts to user input, a core concept in many Arduino projects. This project forms a great foundation for understanding how to manage multiple tasks and events in your sketches. Keep experimenting and building upon it!
