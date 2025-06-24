# Chapter 8: Organizing Your Code: Functions

(Content to be ~2000-2500 words)

As your Arduino sketches grow larger and more complex, you'll find that putting all your code inside `setup()` and `loop()` can become messy and hard to manage. Imagine a project with multiple sensors, motors, and display elements – your `loop()` function could become hundreds of lines long! This is where **functions** come to the rescue.

Functions are self-contained blocks of code that perform a specific task. They help you break down your program into smaller, manageable, and reusable pieces. You've already been using functions: `setup()`, `loop()`, `pinMode()`, `digitalWrite()`, `delay()`, `Serial.println()` are all functions! Now, you'll learn how to create your own.

## Why Use Functions?

1.  **Organization and Readability:** Functions allow you to give a name to a block of code that performs a clear action (e.g., `readTemperatureSensor()`, `blinkLedMultipleTimes()`, `displayErrorMessage()`). This makes your main `loop()` function cleaner and easier to understand, as it can be written as a sequence of calls to these descriptive functions.
2.  **Reusability (DRY Principle - Don't Repeat Yourself):** If you have a piece of code that you need to execute from multiple places in your sketch, you can write it once as a function and then "call" that function whenever you need it. This avoids duplicating code, which makes your sketch shorter, easier to debug, and easier to modify (if you need to change the behavior, you only change it in one place – the function definition).
3.  **Modularity:** Functions help create modular code. Each function can be thought of as a "black box" that takes some inputs (optional), performs its task, and possibly returns an output (optional). You can develop and test functions independently.
4.  **Abstraction:** Functions hide the complex details of a task behind a simple name. For example, when you call `digitalWrite(ledPin, HIGH);`, you don't need to know the intricate details of how the microcontroller sets the voltage on that pin; you just know the function does its job.

## Anatomy of a Function

A function definition in C++/Arduino typically has these parts:

1.  **Return Type:** Specifies the data type of the value that the function will "send back" or "return" to the part of the code that called it. If the function doesn't return any value, its return type is `void`.
2.  **Function Name:** A unique name that identifies the function. Follows the same naming rules as variables (letters, numbers, underscore; start with letter or underscore; case-sensitive; no keywords). Choose descriptive names!
3.  **Parameters (Arguments):** Enclosed in parentheses `()`. Parameters are like local variables that receive values passed into the function when it's called. They allow you to pass data into the function for it to work with. If a function doesn't need any input, the parentheses are empty.
    *   Each parameter has a data type and a name (e.g., `int pinNumber`, `int delayTime`).
    *   Multiple parameters are separated by commas.
4.  **Function Body:** Enclosed in curly braces `{}`. This is where the actual code that performs the function's task resides.
5.  **`return` Statement (Optional):** If the function has a return type other than `void`, it must use a `return` statement to send a value back to the caller. The data type of the returned value must match the function's declared return type. If the return type is `void`, a `return;` statement can be used to exit the function early, but it's often omitted if the function simply runs to its end.

**General Structure:**

```cpp
returnType functionName(dataType parameter1Name, dataType parameter2Name, ...) {
  // Code block (function body)
  // ... perform actions ...
  if (functionHasAReturnTypeOtherThanVoid) {
    return valueToReturn; // 'valueToReturn' must match 'returnType'
  }
}
```

## Defining and Calling Functions

There are two main steps to using functions you create:

1.  **Defining the Function (Function Definition / Implementation):**
    This is where you write the actual code for the function, as shown in the "Anatomy" section above. Function definitions are usually placed outside of `setup()` and `loop()`, either before them or after them.

2.  **Calling the Function (Function Call / Invocation):**
    This is where you tell your program to execute the function. To call a function, you use its name followed by parentheses. If the function expects parameters (arguments), you provide values for those parameters inside the parentheses.

    `functionName(argument1, argument2, ...);`

    If the function returns a value, you can assign that returned value to a variable:
    `returnType resultVariable = functionName(argument1, argument2, ...);`

**Example 1: A Simple `void` Function (No Return Value, No Parameters)**

Let's create a function to print a greeting.

```cpp
// Function Definition
void printGreeting() {
  Serial.println("Hello from your custom function!");
}

void setup() {
  Serial.begin(9600);
  printGreeting(); // Calling the function
}

void loop() {
  // Call it again in loop if you want
  // printGreeting();
  // delay(1000);
}
```
*   **Definition:** `void printGreeting() { ... }` defines a function named `printGreeting` that returns nothing (`void`) and takes no parameters (`()`).
*   **Call:** `printGreeting();` in `setup()` executes the code inside `printGreeting`.

**Example 2: A `void` Function with Parameters**

Let's make a function to blink an LED a specific number of times, on a specific pin, with a specific delay.

```cpp
// Function Definition
// Parameters:
//   pin: The digital pin number for the LED
//   numBlinks: How many times to blink
//   blinkDelay: The delay in milliseconds for on and off states
void blinkLedCustom(int pin, int numBlinks, int blinkDelay) {
  pinMode(pin, OUTPUT); // Set the pin mode inside the function
  for (int i = 0; i < numBlinks; i++) {
    digitalWrite(pin, HIGH);
    delay(blinkDelay);
    digitalWrite(pin, LOW);
    delay(blinkDelay);
  }
}

void setup() {
  Serial.begin(9600); // For any serial output if needed

  // Call the function to blink LED on pin 13, 3 times, with 500ms delay
  blinkLedCustom(13, 3, 500);

  // Call it again for a different pin and parameters
  blinkLedCustom(12, 5, 100); // Assuming you have an LED on pin 12
}

void loop() {
  // The blinking happened in setup, loop is empty for now
}
```
*   **Definition:** `void blinkLedCustom(int pin, int numBlinks, int blinkDelay)`
    *   Takes three integer parameters: `pin`, `numBlinks`, `blinkDelay`.
*   **Call 1:** `blinkLedCustom(13, 3, 500);`
    *   When this is called: `pin` inside `blinkLedCustom` becomes `13`, `numBlinks` becomes `3`, and `blinkDelay` becomes `500`.
*   **Call 2:** `blinkLedCustom(12, 5, 100);`
    *   `pin` becomes `12`, `numBlinks` becomes `5`, `blinkDelay` becomes `100`.

This demonstrates reusability. We defined the blinking logic once and can use it with different settings.

**Example 3: A Function that Returns a Value**

Let's create a function that calculates the average of two numbers.

```cpp
// Function Definition
// Parameters:
//   a, b: The two numbers to average
// Returns: The average as a float
float calculateAverage(float num1, float num2) {
  float sum = num1 + num2;
  float average = sum / 2.0; // Use 2.0 for float division
  return average; // Return the calculated average
}

void setup() {
  Serial.begin(9600);

  float val1 = 10.5;
  float val2 = 20.0;
  float avgResult;

  // Call the function and store the returned value
  avgResult = calculateAverage(val1, val2);
  Serial.print("The average of ");
  Serial.print(val1);
  Serial.print(" and ");
  Serial.print(val2);
  Serial.print(" is: ");
  Serial.println(avgResult); // Output: 15.25

  // You can also use the function call directly in an expression
  Serial.print("Average of 3 and 4 is: ");
  Serial.println(calculateAverage(3.0, 4.0)); // Output: 3.5
}

void loop() {
}
```
*   **Definition:** `float calculateAverage(float num1, float num2)`
    *   Return type is `float`.
    *   Takes two `float` parameters.
*   **`return average;`:** This statement sends the value stored in the `average` variable back to where the function was called.
*   **Call:** `avgResult = calculateAverage(val1, val2);`
    *   `calculateAverage(val1, val2)` is executed. It calculates 15.25.
    *   The `return` statement sends 15.25 back.
    *   This returned value (15.25) is then assigned to the `avgResult` variable.

## Function Prototypes (Declarations)

In C++, the compiler reads your code from top to bottom. If you try to call a function *before* you've defined it, the compiler won't know what that function is and will give an error ("function not declared in this scope").

There are two ways to handle this:

1.  **Define functions before they are called:** Place all your custom function definitions *before* `setup()` and `loop()` (or any other function that calls them). This is often the simplest way for smaller sketches.

2.  **Use Function Prototypes (Declarations):**
    A function prototype (or declaration) tells the compiler about the function's existence, its name, return type, and parameter types *before* the actual definition appears. The prototype looks like the first line of the function definition, but with a semicolon at the end and without the function body.

    **Syntax:** `returnType functionName(dataType parameter1Type, dataType parameter2Type, ...);`
    (Parameter names in the prototype are optional, but their types are required).

    You can then place the full function definitions later in the file, even after `loop()`.

**Example with Function Prototype:**

```cpp
// Function Prototype (Declaration)
float calculateAverage(float num1, float num2); // Note the semicolon and parameter names are optional here, types are enough
// float calculateAverage(float, float); // This also works as a prototype

void setup() {
  Serial.begin(9600);
  float result = calculateAverage(5.0, 7.0); // We can call it because it's been declared
  Serial.print("Average: ");
  Serial.println(result);
}

void loop() {
}

// Function Definition (Implementation) - can be after setup() and loop()
float calculateAverage(float num1, float num2) {
  float sum = num1 + num2;
  float average = sum / 2.0;
  return average;
}
```
The Arduino IDE is sometimes "helpful" and tries to automatically generate prototypes for you behind the scenes if your functions are defined after `setup()` and `loop()`. However, relying on this can sometimes lead to confusion or issues in more complex scenarios or when moving code to other C++ environments. Explicitly writing prototypes or defining functions before use is generally more robust.

**Best Practice:** For clarity, especially in larger projects, it's common to:
1.  Include necessary libraries (`#include ...`).
2.  Define global constants and variables.
3.  Write function prototypes for all your custom functions.
4.  Write the `setup()` function.
5.  Write the `loop()` function.
6.  Write the full definitions (implementations) of your custom functions.

This structure allows someone reading your code to quickly see all available functions (from the prototypes) before diving into their implementations.

## Variable Scope within Functions

*   **Parameters:** Parameters passed to a function are **local** to that function. They are copies of the values (or references, for more advanced usage) passed in during the call. Changing a parameter inside a function does *not* change the original variable that was passed as an argument (unless you use pointers or references, which are more advanced topics).
*   **Local Variables:** Variables declared inside a function (including its parameters) are local to that function. They are created when the function is called and destroyed when the function exits. They cannot be accessed from outside the function.
*   **Global Variables:** Functions can access and modify global variables (declared outside any function). However, relying too much on global variables can make functions less reusable and harder to debug, as their behavior might depend on external state. It's often better to pass necessary data into functions via parameters and get results back via return values.

**Example: Parameter Scope**
```cpp
void changeValue(int x) {
  x = 100; // x here is a LOCAL copy.
  Serial.print("Inside changeValue, x = ");
  Serial.println(x); // Prints 100
}

void setup() {
  Serial.begin(9600);
  int myNumber = 10;
  Serial.print("Before calling changeValue, myNumber = ");
  Serial.println(myNumber); // Prints 10

  changeValue(myNumber); // Pass myNumber (value 10) to the function

  Serial.print("After calling changeValue, myNumber = ");
  Serial.println(myNumber); // Still prints 10! The original myNumber was not changed.
}

void loop() {}
```
This demonstrates "pass-by-value." The `changeValue` function received a *copy* of `myNumber`'s value.

## Benefits of Well-Designed Functions

*   **Simpler `loop()`:** Your main `loop()` can become a high-level overview of what your program does, calling various functions to handle the details.
    ```cpp
    void loop() {
      readSensors();
      processSensorData();
      updateDisplay();
      controlActuators();
      checkForUserInput();
    }
    ```
*   **Easier Debugging:** If there's a problem with reading sensors, you know to look inside the `readSensors()` function. It narrows down where bugs might be.
*   **Testability:** You can test individual functions more easily, perhaps by writing small sketches that just call that one function with different inputs.

## Summary

Functions are a cornerstone of structured programming. They allow you to:
*   Break your code into logical, manageable, and reusable blocks.
*   Improve readability and organization.
*   Avoid code duplication (DRY principle).
*   Hide complexity (abstraction).

Key components of a function:
*   **Return Type** (e.g., `void`, `int`, `float`)
*   **Function Name**
*   **Parameters** (inputs, optional)
*   **Function Body** (code to execute)
*   **`return` statement** (to send a value back, if not `void`)

Remember function **prototypes** if you define functions after they are called. Pay attention to **variable scope** (local vs. global) when working with functions.

Start thinking about how you can break down the tasks in your future Arduino projects into smaller, dedicated functions. This will significantly improve the quality and maintainability of your code as your projects grow.

### Action Steps/Challenges:

1.  **Custom Blinker Function:**
    *   Write a function called `blinkPin(int targetPin, int duration, int repetitions)` that takes a pin number, a duration for each on/off cycle, and a number of repetitions.
    *   Inside `setup()`, call this function to blink `LED_BUILTIN` 3 times with a 200ms on/off duration.
    *   Then call it again to blink an external LED on pin 7 (if you have one wired) 5 times with a 100ms on/off duration.

2.  **Temperature Converter Function:**
    *   Write a function called `celsiusToFahrenheit(float celsiusTemp)` that takes a temperature in Celsius and returns the equivalent temperature in Fahrenheit. The formula is: F = (C * 9/5) + 32.
    *   In `setup()`, call this function with a few Celsius values (e.g., 0, 25, 100) and print the returned Fahrenheit values to the Serial Monitor. Make sure your calculations use floating-point numbers (e.g., `9.0/5.0` instead of `9/5`).

3.  **`isButtonPressed()` Function:**
    *   Wire a push button to digital pin 2 (use a pull-down resistor, so HIGH is pressed, or a pull-up, so LOW is pressed – choose one and stick to it).
    *   Write a function `boolean checkButton(int buttonPinNumber)` that reads the state of the specified button pin and returns `true` if the button is pressed, and `false` otherwise. (Adapt the logic inside for pull-up vs. pull-down).
    *   In `loop()`, call `checkButton(2)` and use an `if` statement to print "Button is Pressed!" or "Button is Not Pressed" to the Serial Monitor based on the returned value.

4.  **Function Prototypes Practice:**
    Take one of the functions you wrote above (e.g., `celsiusToFahrenheit`). Move its full definition *after* your `loop()` function. Then, add a function prototype for it *before* `setup()`. Verify that your sketch still compiles and runs correctly.

Learning to write and use functions effectively is a major step towards becoming a proficient programmer!
