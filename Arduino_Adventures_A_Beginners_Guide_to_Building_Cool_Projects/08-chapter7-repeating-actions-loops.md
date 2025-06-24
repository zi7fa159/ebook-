# Chapter 7: Repeating Actions: Loops (for, while)

(Content to be ~2000-2500 words)

In many programming scenarios, you need to perform the same action or a similar set of actions multiple times. For example, blinking an LED 10 times, reading a sensor value every second for a minute, or processing a list of items. Instead of writing the same code over and over again, you can use **loops**. Loops allow you to execute a block of code repeatedly until a certain condition is met.

Arduino C++ primarily uses two types of loops: the **`for` loop** and the **`while` loop**. (There's also a `do...while` loop, which is less common but we'll touch on it). The main `loop()` function in every Arduino sketch is itself a type of infinite loop, but `for` and `while` loops give you more fine-grained control over repetitions within `loop()` or `setup()`.

## The `for` Loop

The `for` loop is ideal when you know in advance how many times you want to repeat a block of code. Its structure looks a bit more complex at first, but it's very powerful:

```cpp
for (initialization; condition; increment) {
  // Code block to be executed repeatedly
}
```

Let's break down its three parts, which are separated by semicolons:

1.  **`initialization`:**
    *   This part is executed **once** at the very beginning of the `for` loop.
    *   It's typically used to declare and initialize a counter variable (often called `i`, `j`, or `k`).
    *   Example: `int i = 0;`

2.  **`condition`:**
    *   This part is evaluated **before each iteration** (repetition) of the loop.
    *   If the condition is `true`, the code block inside the loop is executed.
    *   If the condition becomes `false`, the loop terminates, and the program continues with the code immediately following the `for` loop.
    *   Example: `i < 10;` (loop as long as `i` is less than 10)

3.  **`increment` (or update):**
    *   This part is executed **at the end of each iteration**, after the code block inside the loop has run.
    *   It's typically used to modify the counter variable, moving it closer to the termination condition.
    *   Example: `i++` (increment `i` by 1) or `i = i + 2` (increment `i` by 2) or `i--` (decrement `i`).

**How it Works - Step by Step:**

1.  The `initialization` statement is executed.
2.  The `condition` is checked.
3.  If the `condition` is `true`:
    a.  The code block inside the `for` loop is executed.
    b.  The `increment` statement is executed.
    c.  Go back to step 2 (check the `condition` again).
4.  If the `condition` is `false`, the loop ends.

**Example: Blinking an LED 5 times using a `for` loop**

```cpp
const int ledPin = 13;

void setup() {
  pinMode(ledPin, OUTPUT);

  // Blink the LED 5 times in setup
  for (int i = 0; i < 5; i++) { // i will go from 0, 1, 2, 3, 4
    digitalWrite(ledPin, HIGH);
    delay(250);
    digitalWrite(ledPin, LOW);
    delay(250);
  }
  // After the loop, the LED will remain off (as set in the last iteration)
}

void loop() {
  // Nothing happening in the main loop for this example
}
```
Let's trace the `for` loop:
*   **Initialization:** `int i = 0;` (variable `i` is created and set to 0)
*   **Iteration 1:**
    *   Condition: `i < 5` (0 < 5 is `true`)
    *   Code block runs: LED blinks once.
    *   Increment: `i++` (`i` becomes 1)
*   **Iteration 2:**
    *   Condition: `i < 5` (1 < 5 is `true`)
    *   Code block runs: LED blinks once.
    *   Increment: `i++` (`i` becomes 2)
*   **Iteration 3:**
    *   Condition: `i < 5` (2 < 5 is `true`)
    *   Code block runs: LED blinks once.
    *   Increment: `i++` (`i` becomes 3)
*   **Iteration 4:**
    *   Condition: `i < 5` (3 < 5 is `true`)
    *   Code block runs: LED blinks once.
    *   Increment: `i++` (`i` becomes 4)
*   **Iteration 5:**
    *   Condition: `i < 5` (4 < 5 is `true`)
    *   Code block runs: LED blinks once.
    *   Increment: `i++` (`i` becomes 5)
*   **Next Check:**
    *   Condition: `i < 5` (5 < 5 is `false`)
    *   The loop terminates.

The LED blinks exactly 5 times.

**Variations in `for` loops:**

*   **Counting Down:**
    ```cpp
    for (int i = 10; i > 0; i--) {
      Serial.println(i); // Prints 10, 9, 8, ..., 1
    }
    ```
*   **Different Step Sizes:**
    ```cpp
    for (int i = 0; i <= 20; i = i + 2) { // Or i += 2
      Serial.println(i); // Prints 0, 2, 4, ..., 20
    }
    ```
*   **Using the loop variable inside the loop:**
    Often, the counter variable (`i`) is used within the loop's code block. For instance, to control the brightness of an LED:
    ```cpp
    const int ledPin = 9; // A PWM pin

    void setup() {
      pinMode(ledPin, OUTPUT);
    }

    void loop() {
      // Fade LED up
      for (int brightness = 0; brightness <= 255; brightness++) {
        analogWrite(ledPin, brightness);
        delay(10); // Small delay to see the fade
      }

      // Fade LED down
      for (int brightness = 255; brightness >= 0; brightness--) {
        analogWrite(ledPin, brightness);
        delay(10);
      }
    }
    ```

**Scope of the `for` loop counter variable:**
If you declare the counter variable inside the `for` loop's initialization part (e.g., `for (int i = 0; ...)`), that variable `i` is **local** to the `for` loop. It only exists and can only be accessed from within the loop. This is generally good practice.

## The `while` Loop

The `while` loop is used when you want to repeat a block of code as long as a certain condition remains `true`. You might not know in advance how many times it will iterate; it depends on when the condition becomes `false`.

The structure is:
```cpp
while (condition) {
  // Code block to be executed repeatedly
  // IMPORTANT: Something inside this block should eventually make the condition false,
  // otherwise you'll have an infinite loop (if that's not intended).
}
```
*   **`condition`:** Evaluated **before each iteration**. If `true`, the code block runs. If `false`, the loop terminates.

**How it Works - Step by Step:**

1.  The `condition` is checked.
2.  If the `condition` is `true`:
    a.  The code block inside the `while` loop is executed.
    b.  Go back to step 1 (check the `condition` again).
3.  If the `condition` is `false`, the loop ends.

**Example: Waiting for a button press using a `while` loop**

Let's say you want your program to wait until a button (connected to pin 2, pulled LOW by default, goes HIGH when pressed) is pressed before proceeding.

```cpp
const int buttonPin = 2;
const int ledPin = 13;

void setup() {
  pinMode(buttonPin, INPUT);
  pinMode(ledPin, OUTPUT);
  Serial.begin(9600);

  Serial.println("Press the button to continue...");

  // Wait while the button is NOT pressed (buttonState is LOW)
  while (digitalRead(buttonPin) == LOW) {
    // Do nothing here, just wait. This is called "busy-waiting".
    // You could add a small delay(10); if you want to be slightly less CPU intensive.
  }

  // Once the loop exits, it means digitalRead(buttonPin) was HIGH (button pressed)
  Serial.println("Button pressed! Continuing...");
  digitalWrite(ledPin, HIGH); // Turn on an LED to indicate continuation
}

void loop() {
  // Main program logic would go here, after the button press
}
```
In this `setup()`:
*   The `while (digitalRead(buttonPin) == LOW)` loop keeps checking the button.
*   As long as the button is not pressed (`digitalRead(buttonPin)` returns `LOW`), the condition `LOW == LOW` is `true`, and the loop continues (doing nothing inside its block).
*   Once you press the button, `digitalRead(buttonPin)` returns `HIGH`. The condition `HIGH == LOW` becomes `false`, and the `while` loop terminates. The program then proceeds to print "Button pressed!" and turn on the LED.

**Important: Avoiding Unintended Infinite Loops with `while`**

With `while` loops, you must ensure that something *inside* the loop (or an external event that the condition checks) will eventually cause the `condition` to become `false`. If the condition always remains `true`, the `while` loop will run forever, and your program will get stuck.

```cpp
// POTENTIAL INFINITE LOOP (if 'sensorValue' never gets above 100 from an external source)
int sensorValue = 0;
while (sensorValue <= 100) {
  Serial.println("Waiting for sensor value to increase...");
  // If sensorValue is not updated here or by some other process the condition checks,
  // this loop might never end.
  // You would typically read the sensor inside the loop:
  // sensorValue = analogRead(A0);
  delay(100);
}
```

## The `do...while` Loop

The `do...while` loop is similar to the `while` loop, but with one key difference: the code block is executed **at least once** *before* the condition is checked.

The structure is:
```cpp
do {
  // Code block to be executed
  // This block always runs at least once.
} while (condition);
```

**How it Works - Step by Step:**

1.  The code block inside the `do...while` loop is executed.
2.  Then, the `condition` is checked.
3.  If the `condition` is `true`, go back to step 1.
4.  If the `condition` is `false`, the loop ends.

**Example: Asking for input until valid input is given**

This is less common in basic Arduino without direct keyboard input, but conceptually:
```cpp
int value;
char inputChar;

void setup() {
  Serial.begin(9600);
}

void loop() {
  Serial.println("Enter a number between 1 and 5:");
  do {
    while (Serial.available() == 0) {
      // Wait for user to type something in Serial Monitor
    }
    inputChar = Serial.read(); // Read one character
    // Attempt to convert char to int (simple version, not robust for multi-digit or non-digit)
    if (inputChar >= '1' && inputChar <= '5') {
      value = inputChar - '0'; // Convert char '1' to int 1, etc.
      Serial.print("You entered: ");
      Serial.println(value);
    } else {
      value = 0; // Invalid input
      Serial.println("Invalid input. Try again.");
    }
  } while (value < 1 || value > 5); // Loop as long as value is not between 1 and 5

  Serial.println("Valid input received. Proceeding...");
  // ... do something with the valid 'value' ...
  delay(2000); // Wait before asking again in the main loop
}
```
In this example, the code inside the `do` block (prompting and reading input) will always run once. Then, the `while` condition checks if the input was valid. If not, it loops back to ask again.

The `do...while` loop is useful when you need to perform an action first and then decide if it needs to be repeated.

## `break` and `continue` Statements in Loops

Sometimes you need more control over how a loop executes or terminates.

### `break` Statement

The `break` statement immediately **terminates the innermost loop** (`for`, `while`, or `do...while`) it is in, or a `switch` statement. Program execution continues with the statement immediately following the terminated loop.

**Example: Using `break` in a `for` loop**
Find the first multiple of 7 within a range, then stop.
```cpp
void setup() {
  Serial.begin(9600);
  int foundNumber = -1; // Initialize to a value indicating not found

  for (int i = 1; i <= 20; i++) {
    Serial.print("Checking: ");
    Serial.println(i);
    if (i % 7 == 0) { // If i is a multiple of 7
      foundNumber = i;
      break; // Exit the for loop immediately
    }
  }

  if (foundNumber != -1) {
    Serial.print("First multiple of 7 found: ");
    Serial.println(foundNumber); // Will print 7
  } else {
    Serial.println("No multiple of 7 found in the range.");
  }
}

void loop() {}
```
Without `break`, the loop would continue to check all numbers up to 20. With `break`, as soon as `i` is 7, `foundNumber` is set, and the loop stops.

### `continue` Statement

The `continue` statement **skips the rest of the current iteration** of the innermost loop (`for`, `while`, or `do...while`) and proceeds to the **next iteration** of the loop.

*   In a `for` loop, `continue` jumps to the `increment` step and then the `condition` check.
*   In a `while` or `do...while` loop, `continue` jumps directly to the `condition` check.

**Example: Using `continue` in a `for` loop**
Print only odd numbers in a range.
```cpp
void setup() {
  Serial.begin(9600);
  for (int i = 1; i <= 10; i++) {
    if (i % 2 == 0) { // If i is even
      continue; // Skip the rest of this iteration (the Serial.println)
    }
    // This line is only reached if i is odd
    Serial.println(i);
  }
  // Output: 1, 3, 5, 7, 9
}

void loop() {}
```
When `i` is even, `continue` is executed, and the `Serial.println(i);` for that iteration is skipped. The loop then proceeds with `i++` and checks the condition again.

## Choosing the Right Loop

*   **`for` loop:** Best when you know (or can easily calculate) the number of iterations beforehand. Excellent for iterating over a known range or a fixed number of times. Commonly used with counter variables.
*   **`while` loop:** Best when the number of iterations is not known in advance and depends on a condition that might change during execution (e.g., waiting for sensor input, user action, or until a certain state is reached). You need to ensure the condition will eventually become false.
*   **`do...while` loop:** Use when you need the loop body to execute at least once, regardless of the condition. Less common than `for` or `while`.

Often, you can achieve similar results with different types of loops, but one type might lead to more readable or logical code for a specific situation.

## Summary

Loops are fundamental for making your programs efficient and capable of performing repetitive tasks.
*   **`for (initialization; condition; increment)`:** Repeats a known number of times.
*   **`while (condition)`:** Repeats as long as a condition is true. Condition is checked *before* each iteration.
*   **`do { ... } while (condition);`:** Repeats as long as a condition is true. Condition is checked *after* each iteration, so the body always runs at least once.
*   **`break;`:** Exits the current loop immediately.
*   **`continue;`:** Skips the current iteration and proceeds to the next.

By combining loops with conditional statements (`if`, `else`), you can create sophisticated programs that perform complex tasks, respond to changing inputs, and manage sequences of operations effectively.

### Action Steps/Challenges:

1.  **Countdown:** Write a sketch that uses a `for` loop to print a countdown from 10 down to 1 to the Serial Monitor, followed by "Liftoff!".
2.  **Sum of Numbers:** Write a sketch that uses a `for` loop to calculate the sum of all numbers from 1 to 100. Print the final sum to the Serial Monitor.
    (Hint: `int sum = 0; for (...) { sum = sum + i; }`)
3.  **User-Controlled Blink Count:**
    *   Connect a potentiometer to A0.
    *   Read the potentiometer value in `setup()`.
    *   Map the potentiometer value (0-1023) to a smaller range for blink count, say 1 to 10 blinks. (The `map()` function is great for this: `int numBlinks = map(potValue, 0, 1023, 1, 10);`)
    *   Use a `for` loop in `setup()` to blink the onboard LED (`LED_BUILTIN`) `numBlinks` times.
4.  **`while` loop with a Threshold:**
    *   Connect a potentiometer to A0.
    *   Write a sketch where the `loop()` function continuously reads the potentiometer.
    *   Inside `loop()`, use a `while` loop that only allows the program to proceed (e.g., print "Threshold reached!" to Serial Monitor and turn on an LED) *while* the potentiometer reading is below a certain threshold (e.g., 500). Once the reading goes above 500, the `while` loop should terminate, the message printed, LED turned on, and then perhaps the `loop()` function does nothing further or waits for a reset. (Think about how to structure this so the `while` loop actually blocks until the condition is met).
5.  **`break` and `continue` Exploration:**
    Modify the "Fade LED Up and Down" `for` loop example.
    *   Add a condition inside the "Fade LED up" loop: if `brightness` becomes exactly 128, use `continue` to skip setting that specific brightness (so it jumps from 127 to 129).
    *   Add a condition inside the "Fade LED down" loop: if `brightness` becomes exactly 64, use `break` to stop the fading down process at that point. Observe the effects.

Loops are your workhorses for repetition. Practice them, and you'll find them indispensable! Next, we'll look at how to organize your code even better with functions.
