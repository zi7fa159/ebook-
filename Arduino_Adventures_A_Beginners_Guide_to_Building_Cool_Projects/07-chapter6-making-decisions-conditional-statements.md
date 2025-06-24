# Chapter 6: Making Decisions: Conditional Statements (if, else if, else)

(Content to be ~2000-2500 words)

So far, your Arduino sketches have executed commands sequentially, one after another. But what if you want your program to behave differently based on certain conditions? For example, turn on an LED *if* a button is pressed, or sound an alarm *if* a sensor reading exceeds a threshold. This is where conditional statements come in. They allow your Arduino to make decisions and control the flow of your program.

The primary conditional statement in C++ (and thus Arduino) is the **`if` statement**. It can be extended with `else if` and `else` to handle multiple conditions and default actions.

## The `if` Statement

The `if` statement is the simplest way to make a decision. Its basic structure is:

```cpp
if (condition) {
  // Code to execute if the condition is true
}
```

*   **`condition`:** This is an expression that evaluates to either `true` or `false` (a boolean value). It typically involves comparison operators (`==`, `!=`, `<`, `>`, `<=`, `>=`) and logical operators (`&&`, `||`, `!`) that we learned about in the previous chapter.
*   **Curly Braces `{}`:** The code that you want to execute if the `condition` is `true` is placed inside the curly braces. This is called a "block" of code.
*   **How it works:** If the `condition` evaluates to `true`, the code block inside the curly braces is executed. If the `condition` evaluates to `false`, the code block is skipped, and the program continues with the next statement after the `if` block.

**Example: Turning on an LED if a button is pressed**

Let's imagine you have a button connected to digital pin 2 and an LED connected to digital pin 13.

```cpp
const int buttonPin = 2;
const int ledPin = 13;
int buttonState = 0; // Variable to store the button's state

void setup() {
  pinMode(ledPin, OUTPUT);
  pinMode(buttonPin, INPUT); // Set buttonPin as an input
}

void loop() {
  // Read the state of the button
  buttonState = digitalRead(buttonPin); // digitalRead() returns HIGH or LOW

  // Check if the button is pressed
  // Assuming the button connects the pin to HIGH when pressed (common for some setups)
  // Or, if using a pull-down resistor, HIGH means pressed.
  // If using a pull-up resistor (common), LOW means pressed. Let's assume HIGH for pressed for now.
  if (buttonState == HIGH) {
    digitalWrite(ledPin, HIGH); // Turn the LED on
  }

  // (We'll add an 'else' soon to turn it off when not pressed)
}
```
In this example:
*   `buttonState = digitalRead(buttonPin);` reads the voltage on `buttonPin`. If the button is pressed and wired to send a HIGH signal, `buttonState` becomes `HIGH`.
*   `if (buttonState == HIGH)` checks if the `buttonState` variable is equal to `HIGH`.
*   If it is `true` (the button is pressed), then `digitalWrite(ledPin, HIGH);` is executed, turning the LED on.
*   If it's `false` (button not pressed), the `digitalWrite` line is skipped.

**Single Statement `if` (Optional Braces):**

If the code block for an `if` statement contains only a single statement, the curly braces are optional (though many programmers prefer to always use them for clarity and to avoid errors if they later add more statements).

```cpp
if (buttonState == HIGH)
  digitalWrite(ledPin, HIGH); // Still works, but only for one statement

// If you add another statement without braces, it's NOT part of the if:
if (buttonState == HIGH)
  digitalWrite(ledPin, HIGH); // This is part of the if
  Serial.println("Button Pressed!"); // This will ALWAYS execute, regardless of the if! (WRONG INDENTATION)

// Correct way with multiple statements:
if (buttonState == HIGH) {
  digitalWrite(ledPin, HIGH);
  Serial.println("Button Pressed!");
}
```
**Best practice:** Always use curly braces `{}` for `if` statements, even for single lines, to improve readability and prevent bugs when adding more code later.

## The `if...else` Statement

The `if` statement executes code if a condition is true. But what if you want to do something else if the condition is *false*? That's what `else` is for.

The structure is:
```cpp
if (condition) {
  // Code to execute if the condition is true
} else {
  // Code to execute if the condition is false
}
```

**Example: Turning LED on if button pressed, off otherwise**

Let's improve our previous example:

```cpp
const int buttonPin = 2;
const int ledPin = 13;
int buttonState = 0;

void setup() {
  pinMode(ledPin, OUTPUT);
  pinMode(buttonPin, INPUT);
}

void loop() {
  buttonState = digitalRead(buttonPin);

  if (buttonState == HIGH) {
    digitalWrite(ledPin, HIGH); // Turn LED on
  } else {
    digitalWrite(ledPin, LOW);  // Turn LED off
  }
}
```
Now:
*   If `buttonState` is `HIGH`, the first block executes (`digitalWrite(ledPin, HIGH);`).
*   If `buttonState` is `LOW` (i.e., not `HIGH`), the `else` block executes (`digitalWrite(ledPin, LOW);`).

This creates a clear on/off behavior for the LED based on the button.

## The `if...else if...else` Statement

Sometimes you have more than two possibilities. You might want to test several conditions in sequence. This is where `else if` comes in handy.

The structure is:
```cpp
if (condition1) {
  // Code to execute if condition1 is true
} else if (condition2) {
  // Code to execute if condition1 is false AND condition2 is true
} else if (condition3) {
  // Code to execute if condition1 and condition2 are false AND condition3 is true
} else {
  // Code to execute if ALL preceding conditions are false
}
```

*   The conditions are evaluated from top to bottom.
*   As soon as one condition is found to be `true`, its corresponding code block is executed, and the rest of the `else if` and `else` blocks in that chain are skipped.
*   The final `else` block is optional. If present, it acts as a "default" case if none of the `if` or `else if` conditions are met.

**Example: Controlling LED brightness based on a sensor reading**

Imagine a sensor (like a potentiometer or light sensor) connected to analog pin A0, and you want to control an LED's state:
*   If sensor reading is high (e.g., > 700), LED is fully ON.
*   If sensor reading is medium (e.g., > 400 but <= 700), LED is half-bright (using PWM).
*   Otherwise (sensor reading is low, <= 400), LED is OFF.

```cpp
const int sensorPin = A0;
const int ledPin = 9; // A PWM pin
int sensorValue = 0;

void setup() {
  pinMode(ledPin, OUTPUT);
  Serial.begin(9600); // For printing sensor values
}

void loop() {
  sensorValue = analogRead(sensorPin); // Reads a value between 0 and 1023
  Serial.print("Sensor Value: ");
  Serial.println(sensorValue);

  if (sensorValue > 700) {
    analogWrite(ledPin, 255); // Full brightness (PWM value 0-255)
    Serial.println("LED: Full ON");
  } else if (sensorValue > 400) { // This condition is only checked if sensorValue <= 700
    analogWrite(ledPin, 127); // Half brightness
    Serial.println("LED: Half Bright");
  } else { // This executes if sensorValue <= 400
    analogWrite(ledPin, 0);   // LED Off
    Serial.println("LED: OFF");
  }
  delay(100); // Small delay
}
```
In this sketch:
1.  If `sensorValue` is 800, the first `if (sensorValue > 700)` is true. The LED is set to full brightness, and the `else if` and `else` blocks are skipped.
2.  If `sensorValue` is 550, the first `if` is false. The program moves to `else if (sensorValue > 400)`. This is true. The LED is set to half brightness, and the final `else` block is skipped.
3.  If `sensorValue` is 300, both the first `if` and the `else if` are false. The program executes the final `else` block, turning the LED off.

## Nested `if` Statements

You can also place `if` statements inside other `if` statements. This is called "nesting."

```cpp
if (conditionA) {
  // Code A
  if (conditionB) {
    // Code B (executes if conditionA AND conditionB are true)
  } else {
    // Code C (executes if conditionA is true AND conditionB is false)
  }
} else {
  // Code D (executes if conditionA is false)
}
```
Nesting can be useful for more complex logic, but be careful not to make it too deep, as it can become hard to read and understand. Often, combining conditions with logical operators (`&&`, `||`) can simplify nested structures.

**Example: Nested `if`**
```cpp
int temperature = 25;
boolean isRaining = false;

if (temperature > 20) {
  Serial.println("It's warm.");
  if (!isRaining) { // !isRaining means "isRaining is false"
    Serial.println("And it's not raining. Good weather!");
  } else {
    Serial.println("But it's raining. Warm rain!");
  }
} else {
  Serial.println("It's not very warm.");
}
```

## Common Pitfalls with `if` Statements

1.  **Using `=` instead of `==`:**
    *   `if (x = 5)`: This *assigns* 5 to `x`. The expression `(x = 5)` itself evaluates to the value assigned, which is 5. In C++, any non-zero value is treated as `true` in a boolean context. So, this `if` statement will *always* be true, and it will change the value of `x`. This is a very common bug.
    *   `if (x == 5)`: This correctly *compares* `x` to 5.

2.  **Missing Curly Braces `{}`:**
    As mentioned, if you have multiple lines of code that should only execute if the condition is true, you *must* use curly braces.
    ```cpp
    // WRONG:
    if (x > 10)
      Serial.println("x is greater than 10");
      x = 0; // This line will ALWAYS execute, not just when x > 10!

    // CORRECT:
    if (x > 10) {
      Serial.println("x is greater than 10");
      x = 0;
    }
    ```

3.  **Semicolon after `if(condition);`:**
    ```cpp
    // WRONG:
    if (x > 10); { // The semicolon here ends the if statement prematurely!
      Serial.println("This will always print!");
    }
    ```
    The semicolon after `if (x > 10);` makes the `if` statement have an empty body. The block of code ` { Serial.println("This will always print!"); }` that follows is then treated as a separate, unconditional block.

4.  **Floating Point Comparisons:**
    Comparing floating-point numbers (`float`, `double`) for exact equality (`==` or `!=`) can be problematic due to the way these numbers are represented in memory (they can have tiny precision errors).
    ```cpp
    float a = 0.1 + 0.1 + 0.1; // Might not be exactly 0.3
    float b = 0.3;
    if (a == b) { // This might be false!
      // ...
    }
    ```
    Instead of checking for exact equality, it's often better to check if the absolute difference between two floats is within a small tolerance (epsilon):
    ```cpp
    float tolerance = 0.0001;
    if (abs(a - b) < tolerance) { // abs() gives absolute value
      // Consider a and b to be "equal enough"
    }
    ```

## The `switch...case` Statement (Alternative for Multiple Conditions)

While `if...else if...else` is very flexible, if you have many conditions that all depend on the value of a *single integer variable*, a `switch...case` statement can sometimes be cleaner and more readable.

**Structure:**
```cpp
switch (variable) {
  case value1:
    // Code to execute if variable == value1
    break; // IMPORTANT!
  case value2:
    // Code to execute if variable == value2
    break; // IMPORTANT!
  // ... more cases
  default: // Optional
    // Code to execute if variable doesn't match any of the cases
    break;
}
```
*   **`variable`:** Must be an integer type (e.g., `int`, `char`). You cannot use `float` or `String` directly in a `switch` statement's control expression.
*   **`case valueX:`:** Each `case` tests if `variable` is equal to `valueX`. `valueX` must be a constant integer expression.
*   **`break;`:** This is crucial! When a `case` matches, its code is executed. If there's no `break` statement at the end of that `case`'s code, the program will "fall through" and also execute the code in the *next* `case` block(s) until a `break` is encountered or the `switch` statement ends. This is sometimes intentional for advanced use, but usually a bug for beginners.
*   **`default:`:** This is optional. If none of the `case` values match `variable`, the code under `default:` is executed. If there's no `default` and no match, the entire `switch` statement is skipped.

**Example: `switch...case`**
Imagine you have a variable `mode` that can be 1, 2, or 3, each corresponding to a different LED blinking pattern.

```cpp
int mode = 1; // Can be changed by a button or sensor later
const int ledPin = 13;

void setup() {
  pinMode(ledPin, OUTPUT);
}

void loop() {
  // (Code to change 'mode' would go here, e.g., based on button presses)

  switch (mode) {
    case 1: // Slow blink
      digitalWrite(ledPin, HIGH);
      delay(1000);
      digitalWrite(ledPin, LOW);
      delay(1000);
      break;
    case 2: // Fast blink
      digitalWrite(ledPin, HIGH);
      delay(200);
      digitalWrite(ledPin, LOW);
      delay(200);
      break;
    case 3: // Pulse blink
      digitalWrite(ledPin, HIGH);
      delay(100);
      digitalWrite(ledPin, LOW);
      delay(900);
      break;
    default: // Off if mode is not 1, 2, or 3
      digitalWrite(ledPin, LOW);
      // No delay needed here if it's just staying off
      break;
  }
}
```

**When to use `switch...case` vs. `if...else if`:**
*   **`switch...case`:** Good when you have a single integer variable and multiple distinct constant values to check against. Can be more readable in such scenarios.
*   **`if...else if`:** More flexible. Can handle ranges (e.g., `if (value > 10 && value < 20)`), floating-point numbers, complex conditions, and conditions involving multiple variables.

You cannot do this with a `switch`:
`if (sensorValue > 700)` // `switch` only checks for equality with constants.

## Summary

Conditional statements are the decision-makers in your Arduino code.
*   **`if (condition)`:** Executes code if the condition is true.
*   **`if (condition) { ... } else { ... }`:** Executes one block of code if true, another if false.
*   **`if (condition1) { ... } else if (condition2) { ... } else { ... }`:** Checks multiple conditions in sequence, executing the block for the first true condition found.
*   Always use **curly braces `{}`** for code blocks, especially with multiple statements.
*   Be careful with `=` (assignment) vs. `==` (comparison).
*   **`switch...case`** provides an alternative for checking a single integer variable against multiple constant values. Remember the `break` statements!

Mastering conditional logic is key to creating interactive and intelligent Arduino projects that respond dynamically to their environment and user input.

### Action Steps/Challenges:

1.  **Simple Light Switch:**
    *   Wire a push button to digital pin 2 (use a pull-down resistor so HIGH means pressed, or a pull-up resistor so LOW means pressed – adapt your code accordingly!).
    *   Wire an LED (with its resistor) to digital pin 3.
    *   Write a sketch that turns the LED ON when the button is pressed and OFF when it's not pressed. Use an `if...else` statement.

2.  **Three-Level Brightness:**
    *   Connect a potentiometer to analog pin A0.
    *   Connect an LED (with resistor) to a PWM pin (e.g., pin 9).
    *   Write a sketch using `if...else if...else` to:
        *   If potentiometer reading is < 341, turn the LED OFF (`analogWrite(ledPin, 0);`).
        *   If potentiometer reading is >= 341 AND < 682, set LED to medium brightness (`analogWrite(ledPin, 127);`).
        *   If potentiometer reading is >= 682, set LED to full brightness (`analogWrite(ledPin, 255);`).
    *   Use `Serial.println()` to print the potentiometer reading and the LED state (e.g., "OFF", "MEDIUM", "FULL") to the Serial Monitor to help you debug.

3.  **`switch...case` Grader (Conceptual):**
    Imagine a variable `char grade = 'B';`. Write a `switch...case` statement (just the switch part, you don't need full Arduino code) that would print:
    *   "Excellent" if grade is 'A'
    *   "Good" if grade is 'B'
    *   "Fair" if grade is 'C'
    *   "Needs Improvement" if grade is 'D' or 'F' (hint: you can have cases fall through if you omit `break;` strategically)
    *   "Invalid Grade" for anything else.

4.  **Fix the Bug:** What's wrong with this code snippet? How would you fix it?
    ```cpp
    int lightLevel = analogRead(A0);
    if (lightLevel < 100); // Turn on a night light
      digitalWrite(nightLightPin, HIGH);
    ```

Practice these decision-making structures. They are fundamental to programming logic! Next, we'll look at how to repeat actions using loops.
