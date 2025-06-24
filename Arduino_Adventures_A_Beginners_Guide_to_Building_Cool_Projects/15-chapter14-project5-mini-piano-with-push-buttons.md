# Chapter 14: Project 5: Mini Piano with Push Buttons

(Content to be ~1500-2000 words, image integration for circuit, and final project)

Let's make some noise! In this project, we'll build a mini electronic piano using push buttons to trigger different musical notes played through a piezo buzzer or a small speaker. This project is a fun way to learn about generating tones and mapping inputs to outputs.

## Project Overview

We will connect several push buttons to the Arduino. Each button, when pressed, will correspond to a specific musical note. Pressing a button will cause the Arduino to generate a tone of the corresponding frequency using a piezo buzzer.

We will:
1.  Learn how to generate tones with Arduino using the `tone()` function.
2.  Wire multiple push buttons as inputs.
3.  Map button presses to specific musical note frequencies.
4.  Create a simple musical instrument.

**What you'll learn:**

*   Using the `tone()` and `noTone()` functions.
*   Understanding basic musical note frequencies.
*   Handling multiple digital inputs.
*   Creating an interactive sound-producing project.
*   (Optional) Using arrays to store note frequencies.

## Components Needed:

*   Arduino Uno
*   Breadboard
*   4 to 7 x Push Buttons (tactile switches) - Let's aim for 5 for a C-D-E-F-G sequence.
*   (If not using `INPUT_PULLUP` for buttons) 5 x 10kΩ Resistors (for pull-down/pull-up for each button)
*   1 x Piezo Buzzer (a passive buzzer is best for use with `tone()`, but an active one will also make a sound, though it might be fixed to its internal frequency or just click with `tone()`).
*   Jumper Wires (approx. 10-15)

## Understanding Tone Generation

Arduino can generate square wave tones on a digital pin using the `tone()` function.

**`tone(pin, frequency)`:**
*   Generates a square wave of the specified `frequency` (in Hertz - Hz) on the given `pin`.
*   The tone continues to play until you call `noTone(pin)`.

**`tone(pin, frequency, duration)`:**
*   Generates a square wave of the specified `frequency` on the `pin` for the specified `duration` (in milliseconds).
*   The tone stops automatically after the duration. You don't need to call `noTone()` if you use this version.

**`noTone(pin)`:**
*   Stops the generation of a square wave on the specified `pin`. If no tone is playing, it has no effect.

**Piezo Buzzers:**
*   **Passive Buzzer:** These require an oscillating signal (like the one from `tone()`) to produce sound. They can produce different pitches based on the input frequency. They usually have two pins of the same length.
*   **Active Buzzer:** These have a built-in oscillator. They typically produce a fixed tone when DC power is applied (e.g., by `digitalWrite(buzzerPin, HIGH);`). Using `tone()` with an active buzzer might result in clicks or the buzzer's fixed tone being modulated. For this project, a passive buzzer is more versatile for playing different notes.

```
[Placeholder for images of a passive piezo buzzer and an active piezo buzzer.
 Alt Text Passive: "A passive piezo buzzer, typically a small black disc with two pins."
 Alt Text Active: "An active piezo buzzer, often slightly larger or with clearer + marking, which produces a sound when DC voltage is applied."]
```
*(Image Passive: `images/piezo_passive.png`)*
*(Image Active: `images/piezo_active.png`)*

We'll write the code assuming a passive buzzer, as it's better for varied notes.

## Musical Note Frequencies

Each musical note corresponds to a specific frequency. Here are some approximate frequencies for notes in the C4 octave (Middle C):

*   C4:  262 Hz
*   C#4/Db4: 277 Hz
*   D4:  294 Hz
*   D#4/Eb4: 311 Hz
*   E4:  330 Hz
*   F4:  349 Hz
*   F#4/Gb4: 370 Hz
*   G4:  392 Hz
*   G#4/Ab4: 415 Hz
*   A4:  440 Hz (common tuning reference)
*   A#4/Bb4: 466 Hz
*   B4:  494 Hz
*   C5:  523 Hz (octave higher)

You can find more extensive lists online. For our simple piano, we'll pick a few. Let's aim for C, D, E, F, G.

## Wiring the Mini Piano

We'll use 5 buttons for 5 notes and connect them using `INPUT_PULLUP`.

1.  **Disconnect Power.**
2.  **Wire the Piezo Buzzer:**
    *   Connect one pin of the (passive) piezo buzzer to Arduino digital pin **8** (or another PWM pin, though `tone()` works on most digital pins).
    *   Connect the other pin of the piezo buzzer to **GND** on the Arduino.
3.  **Wire the Push Buttons:**
    We'll connect 5 buttons to digital pins 2, 3, 4, 5, and 6.
    *   **Button 1 (Note C):**
        *   Connect one leg to Arduino digital pin **2**.
        *   Connect the diagonally opposite leg (or adjacent on same side) to **GND**.
    *   **Button 2 (Note D):**
        *   Connect one leg to Arduino digital pin **3**.
        *   Connect the other leg to **GND**.
    *   **Button 3 (Note E):**
        *   Connect one leg to Arduino digital pin **4**.
        *   Connect the other leg to **GND**.
    *   **Button 4 (Note F):**
        *   Connect one leg to Arduino digital pin **5**.
        *   Connect the other leg to **GND**.
    *   **Button 5 (Note G):**
        *   Connect one leg to Arduino digital pin **6**.
        *   Connect the other leg to **GND**.

```
[Placeholder for a Fritzing diagram of the 5 buttons and piezo buzzer circuit.
 Alt Text: "Circuit: Passive Piezo Buzzer: one pin to Arduino D8, other to GND. Five Push Buttons: Button1 to D2 & GND, Button2 to D3 & GND, Button3 to D4 & GND, Button4 to D5 & GND, Button5 to D6 & GND."]
```
*(Image: `images/piano_buttons_buzzer_circuit.png`)*

## The Code: Simple Piano

```cpp
// Define buzzer pin
const int buzzerPin = 8;

// Define button pins
const int buttonPinC = 2;
const int buttonPinD = 3;
const int buttonPinE = 4;
const int buttonPinF = 5;
const int buttonPinG = 6;

// Define note frequencies (Hz)
// C4, D4, E4, F4, G4
const int noteC = 262;
const int noteD = 294;
const int noteE = 330;
const int noteF = 349;
const int noteG = 392;

// Duration for the tone when a button is pressed (milliseconds)
const int noteDuration = 150; // Play note for a short time

void setup() {
  Serial.begin(9600); // For debugging if needed

  // Set buzzer pin as output
  pinMode(buzzerPin, OUTPUT);

  // Set button pins as inputs with internal pull-up resistors
  pinMode(buttonPinC, INPUT_PULLUP);
  pinMode(buttonPinD, INPUT_PULLUP);
  pinMode(buttonPinE, INPUT_PULLUP);
  pinMode(buttonPinF, INPUT_PULLUP);
  pinMode(buttonPinG, INPUT_PULLUP);

  Serial.println("Mini Piano Ready!");
}

void loop() {
  // Check Button C (Pin 2)
  if (digitalRead(buttonPinC) == LOW) { // LOW because of INPUT_PULLUP
    Serial.println("Note C Pressed");
    tone(buzzerPin, noteC, noteDuration);
    // Add a small delay to prevent rapid re-triggering if button held
    // and to allow the tone to play before checking other buttons.
    delay(noteDuration + 50); // Wait for tone to finish plus a bit
  }

  // Check Button D (Pin 3)
  else if (digitalRead(buttonPinD) == LOW) {
    Serial.println("Note D Pressed");
    tone(buzzerPin, noteD, noteDuration);
    delay(noteDuration + 50);
  }

  // Check Button E (Pin 4)
  else if (digitalRead(buttonPinE) == LOW) {
    Serial.println("Note E Pressed");
    tone(buzzerPin, noteE, noteDuration);
    delay(noteDuration + 50);
  }

  // Check Button F (Pin 5)
  else if (digitalRead(buttonPinF) == LOW) {
    Serial.println("Note F Pressed");
    tone(buzzerPin, noteF, noteDuration);
    delay(noteDuration + 50);
  }

  // Check Button G (Pin 6)
  else if (digitalRead(buttonPinG) == LOW) {
    Serial.println("Note G Pressed");
    tone(buzzerPin, noteG, noteDuration);
    delay(noteDuration + 50);
  }

  // If no button is pressed, no tone is played (noTone() is not strictly needed
  // here because tone() with duration stops automatically. If we used tone() without
  // duration, we would need noTone() when button is released).
  // A small delay can be good even if no button is pressed, to make the loop
  // not run excessively fast, though for this input checking it's often fine.
  // delay(10);
}
```

**Explanation:**

*   **Constants:** Pins for the buzzer and each button are defined. Frequencies for C, D, E, F, G notes are defined. `noteDuration` sets how long each note plays.
*   **`setup()`:**
    *   Initializes Serial communication.
    *   Sets `buzzerPin` to `OUTPUT`.
    *   Sets all button pins to `INPUT_PULLUP`. This means when a button is *not* pressed, `digitalRead()` will return `HIGH`. When a button *is* pressed, it connects the pin to GND, so `digitalRead()` returns `LOW`.
*   **`loop()`:**
    *   It uses a series of `if...else if` statements to check each button.
    *   `if (digitalRead(buttonPinX) == LOW)`: Checks if the button is pressed.
    *   `tone(buzzerPin, noteFrequency, noteDuration);`: If a button is pressed, it plays the corresponding note for `noteDuration` milliseconds.
    *   `delay(noteDuration + 50);`: This delay serves two purposes:
        1.  It allows the `tone()` with duration to mostly complete before the `loop()` checks buttons again. If this delay is too short or absent, and you hold a button, `tone()` might be called repeatedly, which can sound odd or cut off the previous tone.
        2.  It provides a simple form of "debouncing" or preventing multiple quick triggers if a button is held.
*   **No `noTone()` needed explicitly:** Because we are using `tone(pin, frequency, duration)`, the tone stops automatically after `noteDuration`. If we had used `tone(pin, frequency)` (without duration), we would need a way to call `noTone(buzzerPin)` when the button is released.

**Upload and Test:**
1.  Upload the sketch.
2.  Open the Serial Monitor (optional, for seeing the "Note X Pressed" messages).
3.  Press each button. You should hear the corresponding note (C, D, E, F, or G) play for a short duration. Try playing a simple tune!

## Using Arrays for Pins and Frequencies (More Scalable)

The `if...else if` structure works for a few buttons, but if you wanted more keys (e.g., a full octave), it would become very long and repetitive. Using arrays can make the code cleaner and more scalable.

```cpp
// Define buzzer pin
const int buzzerPin = 8;

// Define button pins in an array
const int numButtons = 5;
const int buttonPins[numButtons] = {2, 3, 4, 5, 6}; // C, D, E, F, G

// Define note frequencies in an array (corresponding to buttonPins)
// C4, D4, E4, F4, G4
const int noteFrequencies[numButtons] = {262, 294, 330, 349, 392};
const char* noteNames[numButtons] = {"C", "D", "E", "F", "G"}; // For printing

const int noteDuration = 150;

// To keep track of which note is currently playing (if any)
// This helps prevent multiple notes if buttons are pressed almost simultaneously
// or if a button is held.
boolean noteIsPlaying = false;
unsigned long noteStopTime = 0;


void setup() {
  Serial.begin(9600);
  pinMode(buzzerPin, OUTPUT);

  // Initialize button pins using a for loop
  for (int i = 0; i < numButtons; i++) {
    pinMode(buttonPins[i], INPUT_PULLUP);
  }
  Serial.println("Mini Piano (Array Version) Ready!");
}

void loop() {
  // If a note is currently playing its duration, don't check for new button presses yet.
  if (noteIsPlaying && millis() < noteStopTime) {
    return; // Wait for the current note to finish
  } else if (noteIsPlaying && millis() >= noteStopTime) {
    noTone(buzzerPin); // Ensure tone is stopped if it was a continuous one
    noteIsPlaying = false;
  }

  // Check each button using a for loop
  for (int i = 0; i < numButtons; i++) {
    if (digitalRead(buttonPins[i]) == LOW) { // Button i is pressed
      Serial.print("Note ");
      Serial.print(noteNames[i]);
      Serial.println(" Pressed");

      tone(buzzerPin, noteFrequencies[i], noteDuration);
      noteIsPlaying = true;
      noteStopTime = millis() + noteDuration + 20; // Add a small buffer

      // Break after playing one note to avoid multiple notes if >1 button pressed
      // This also means only the first detected pressed button in the scan order plays.
      break;
    }
  }
  // A very small delay to make the loop not spin too fast if no buttons pressed.
  // This can help with responsiveness for some button reading scenarios, but
  // for this simple piano, it might not be strictly necessary if the above
  // noteIsPlaying logic handles held buttons.
  // delay(10);
}
```

**Explanation of Array Version:**

*   **`buttonPins[]` array:** Stores the pin numbers for each button.
*   **`noteFrequencies[]` array:** Stores the frequency for the note corresponding to each button. The order must match `buttonPins`.
*   **`noteNames[]` array:** Stores string names for notes, just for Serial printing.
*   **`setup()`:** Uses a `for` loop to iterate through the `buttonPins` array and set `pinMode` for each. This is much cleaner than listing them individually.
*   **`loop()`:**
    *   **`noteIsPlaying` and `noteStopTime`:** These variables are added to better handle the duration of a note. When a note starts, `noteIsPlaying` is set to `true`, and `noteStopTime` records when it should end. The loop won't check for new button presses until the current note's time is up. This prevents a held button from retriggering `tone()` constantly.
    *   A `for` loop iterates from `i = 0` to `numButtons - 1`.
    *   Inside the loop, `buttonPins[i]` refers to the current button pin being checked, and `noteFrequencies[i]` refers to its corresponding note.
    *   `if (digitalRead(buttonPins[i]) == LOW)`: Checks the current button in the array.
    *   `tone(buzzerPin, noteFrequencies[i], noteDuration);` plays the note.
    *   `break;`: If a button is found pressed and its note is played, the `break;` statement exits the `for` loop that checks buttons. This means if you press multiple buttons simultaneously, only the one with the lowest index `i` (scanned first) will play. This is a simple way to handle "polyphony" (or lack thereof).

This array-based approach is much more maintainable if you decide to add more keys to your piano. You'd just need to add entries to the `buttonPins` and `noteFrequencies` arrays and increase `numButtons`.

## Further Extensions & Challenges:

1.  **More Notes:** Expand to a full octave (e.g., C, D, E, F, G, A, B, C5) by adding more buttons and their corresponding frequencies.
2.  **Different Durations:** Experiment with `noteDuration`. Or, make some buttons play short notes and others play longer notes.
3.  **`noTone()` for Held Notes:** Modify the first (non-array) version. Instead of using `tone(pin, freq, duration)`, use `tone(pin, freq)`. Then, add logic to call `noTone(buzzerPin)` when the button is *released* (i.e., when `digitalRead()` goes back to `HIGH`). This allows notes to be held as long as the button is pressed. You'll need to keep track of which button was last pressed.
4.  **Simple Song Playback:** Create an array of notes and an array of durations. In `setup()` or triggered by a button, loop through these arrays to play a predefined simple tune (e.g., "Mary Had a Little Lamb").
5.  **Octave Switch:** Add another button that, when pressed, shifts all the note frequencies up by an octave (i.e., doubles them) or down by an octave (halves them).
6.  **Visual Feedback:** Add LEDs that light up corresponding to the button pressed.

## Summary

This mini piano project has introduced you to:
*   Generating sounds using `tone()`, `noTone()`, and a piezo buzzer.
*   Basic musical note frequencies.
*   Handling multiple button inputs efficiently, especially with arrays.
*   Mapping inputs directly to sound outputs.

You've created an interactive musical instrument, however simple! This project highlights how Arduino can bridge the gap between digital inputs and analog-like outputs (sound waves). Feel free to experiment with different notes, timings, and even try to code simple melodies. Making music with code can be very rewarding!
