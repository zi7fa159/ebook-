# Chapter 5: Understanding Code: Variables, Data Types, and Operators

(Content to be ~2000-2500 words)

In the previous chapter, you successfully made an LED blink. While doing so, you used a variable (`externalLEDPin`) even if we didn't dive deep into the concept. Now, it's time to formally explore some of the fundamental building blocks of programming: **variables**, **data types**, and **operators**. These are essential tools that allow your Arduino sketches to store information, perform calculations, and make decisions.

## What is a Variable?

Think of a variable as a container in your Arduino's memory that can hold a piece of information. This information can change (it can *vary*) as your program runs. Each variable has:

1.  **A Name:** You give your variable a descriptive name so you can refer to it in your code (e.g., `ledPin`, `sensorValue`, `buttonState`).
2.  **A Data Type:** This specifies what kind of information the variable can hold (e.g., a whole number, a number with a decimal point, a single character).
3.  **A Value:** The actual piece of information stored in the variable.

**Why Use Variables?**

*   **Store Data:** To remember sensor readings, user inputs, calculations, or the state of something (like whether an LED is on or off).
*   **Make Code Readable:** Using a descriptive variable name (like `temperatureReading`) makes your code much easier to understand than just using a raw number.
*   **Make Code Flexible:** If you need to change a value that's used in many places (like a pin number), you only need to change it once where the variable is defined.

**Declaring Variables**

Before you can use a variable, you must "declare" it. Declaring a variable tells the Arduino IDE (and the compiler) its name and its data type.

The basic syntax for declaring a variable is:
`dataType variableName;`

For example:
`int counter;`  // Declares an integer variable named 'counter'
`float temperature;` // Declares a floating-point variable named 'temperature'

You can also **initialize** a variable (give it an initial value) when you declare it:
`dataType variableName = initialValue;`

For example:
`int ledPin = 13;`      // Declares an integer 'ledPin' and sets its initial value to 13
`char command = 'A';`   // Declares a character 'command' and sets its initial value to 'A'

**Variable Naming Rules and Conventions:**

*   **Allowed Characters:** Names can contain letters, numbers, and underscores (`_`).
*   **Start:** Must start with a letter or an underscore. They cannot start with a number.
*   **Case Sensitivity:** Variable names are case-sensitive. `myVariable` is different from `MyVariable`.
*   **Keywords:** You cannot use Arduino keywords (like `int`, `void`, `if`, `for`, `HIGH`, `LOW`, etc.) as variable names.
*   **Descriptive Names:** Choose names that clearly indicate what the variable is used for (e.g., `pushButtonPin` is better than `p` or `pin1`).
*   **Camel Case:** A common convention is "camel case," where the first word is lowercase, and subsequent words start with an uppercase letter (e.g., `sensorReadingValue`, `isActiveFlag`). Underscores are also common (e.g., `sensor_reading_value`). Pick a style and be consistent.

## Common Data Types in Arduino

The data type of a variable determines the kind of values it can store and how much memory it occupies. Choosing the right data type is important for efficiency and correctness. Here are some of the most common data types you'll use with Arduino:

1.  **`int` (Integer):**
    *   **Description:** Stores whole numbers (no decimal points).
    *   **Size:** On most Arduinos (like Uno, Nano, Mega, which are AVR-based), an `int` is a 16-bit value.
    *   **Range:** Can store values from -32,768 to 32,767.
    *   **Example:** `int score = 100;` `int buttonPresses = 0;`
    *   **Note:** On 32-bit Arduinos (like Due, MKR series), an `int` is typically 32-bit (-2,147,483,648 to 2,147,483,647). This difference is important for portability if you're writing code for multiple Arduino types.

2.  **`unsigned int`:**
    *   **Description:** Stores only non-negative whole numbers (0 and positive values).
    *   **Size:** 16-bit on AVR Arduinos.
    *   **Range:** 0 to 65,535.
    *   **Use Case:** When you know a value will never be negative, like a counter for events.
    *   **Example:** `unsigned int positiveCounter = 50000;`

3.  **`long`:**
    *   **Description:** Stores larger whole numbers.
    *   **Size:** 32-bit value (on all Arduinos).
    *   **Range:** -2,147,483,648 to 2,147,483,647.
    *   **Use Case:** When you need to store numbers larger than an `int` can hold, such as the value returned by `millis()` (which counts milliseconds since the Arduino started).
    *   **Example:** `long timeSinceStart = millis();`

4.  **`unsigned long`:**
    *   **Description:** Stores very large non-negative whole numbers.
    *   **Size:** 32-bit value.
    *   **Range:** 0 to 4,294,967,295.
    *   **Use Case:** Also commonly used with `millis()` or for timers that need to run for a long time.
    *   **Example:** `unsigned long eventTimestamp;`

5.  **`float` (Floating-Point):**
    *   **Description:** Stores numbers with decimal points (real numbers).
    *   **Size:** 32-bit value.
    *   **Precision:** About 6-7 decimal digits of precision.
    *   **Use Case:** For sensor readings that aren't whole numbers (e.g., temperature like 23.45°C), or for calculations involving fractions.
    *   **Example:** `float temperature = 25.7;` `float piValue = 3.14159;`
    *   **Note:** Floating-point arithmetic can be slower on Arduinos than integer arithmetic. If high precision isn't critical and speed is, sometimes developers will work with integers by scaling values (e.g., store temperature as `257` instead of `25.7` and then divide by 10 when displaying).

6.  **`double`:**
    *   **Description:** Double-precision floating-point number.
    *   **Size:** On AVR Arduinos (Uno, etc.), `double` is the same size as `float` (32-bit) and offers no extra precision. On 32-bit Arduinos (Due, etc.), `double` is typically 64-bit and offers higher precision.
    *   **Use Case:** For AVR Arduinos, `float` is generally preferred. For 32-bit Arduinos, use `double` if you need more precision than `float` provides.
    *   **Example:** `double veryPreciseValue = 123.456789012;` (only truly double precision on 32-bit boards)

7.  **`char` (Character):**
    *   **Description:** Stores a single character.
    *   **Size:** 8-bit value (1 byte).
    *   **Range:** Internally stored as a number (ASCII value), typically from -128 to 127.
    *   **Use Case:** Storing letters, numbers as characters, or symbols. Characters are enclosed in single quotes.
    *   **Example:** `char initial = 'J';` `char command = 's';` `char digit = '7';`

8.  **`unsigned char` (or `byte`):**
    *   **Description:** Stores a single byte of data, representing a non-negative number.
    *   **Size:** 8-bit value.
    *   **Range:** 0 to 255.
    *   **`byte`** is a convenient alias for `unsigned char` in Arduino.
    *   **Use Case:** Working with raw byte data, or when you need a small positive number.
    *   **Example:** `byte smallValue = 150;` `unsigned char dataPacket = 0xFF;` (0xFF in hexadecimal is 255)

9.  **`boolean` (or `bool`):**
    *   **Description:** Stores a truth value, either `true` or `false`.
    *   **Size:** Occupies 1 byte of memory, but only uses one bit logically.
    *   **Use Case:** For flags, states, or results of comparisons.
    *   **`bool`** is the standard C++ type, `boolean` is an Arduino-specific alias. `bool` is generally preferred for wider C++ compatibility.
    *   **Example:** `boolean isLedOn = true;` `bool switchPressed = false;`
    *   **Note:** In C++, non-zero numbers evaluate to `true` in a boolean context, and zero evaluates to `false`. `true` is often represented as 1, and `false` as 0.

10. **`String` (Arduino String Object):**
    *   **Description:** Not a fundamental C++ data type, but an Arduino class that allows you to work with sequences of characters (text) more easily than traditional C-style strings (arrays of `char`).
    *   **Use Case:** Manipulating text, like messages for an LCD or data received over serial communication.
    *   **Example:** `String message = "Hello, Arduino!";`
    *   **Caution:** `String` objects can be very convenient, but they use more memory and can sometimes lead to memory fragmentation on small microcontrollers like the ATmega328P, especially if you create and destroy many `String` objects frequently. For simple text or when memory is tight, C-style character arrays are often more efficient.

**Choosing the Right Data Type:**
*   Use the smallest data type that can comfortably hold the range of values you expect. This saves memory.
*   Use `unsigned` types if you know the value will never be negative.
*   Use `float` or `double` for numbers with decimal points.
*   Use `boolean` or `bool` for true/false states.

## Operators in Arduino

Operators are special symbols that perform operations on variables and values (operands).

### 1. Arithmetic Operators

These perform mathematical calculations:

*   **`+` (Addition):** `result = value1 + value2;`
*   **`-` (Subtraction):** `result = value1 - value2;`
*   **`*` (Multiplication):** `result = value1 * value2;`
*   **`/` (Division):** `result = value1 / value2;`
    *   **Integer Division:** If both `value1` and `value2` are integers, the result will be an integer, and any fractional part is truncated (discarded). E.g., `5 / 2` results in `2`.
    *   **Floating-Point Division:** If at least one operand is a `float` or `double`, floating-point division is performed. E.g., `5.0 / 2.0` results in `2.5`. To force floating-point division with integers, you can "cast" one of them: `(float)5 / 2` results in `2.5`.
*   **`%` (Modulo):**
    *   **Description:** Returns the remainder of an integer division.
    *   **Example:** `7 % 3` results in `1` (because 7 divided by 3 is 2 with a remainder of 1). `10 % 5` results in `0`.
    *   **Use Case:** Useful for checking divisibility, making counters wrap around, or creating patterns.

```cpp
int x = 10;
int y = 3;
int sum = x + y;      // sum is 13
int difference = x - y; // difference is 7
int product = x * y;    // product is 30
int quotient = x / y;   // quotient is 3 (integer division)
int remainder = x % y;  // remainder is 1

float a = 10.0;
float b = 3.0;
float floatQuotient = a / b; // floatQuotient is 3.333...
```

### 2. Comparison Operators (Relational Operators)

These compare two values and return a boolean result (`true` or `false`). They are crucial for making decisions in `if` statements (which we'll cover in the next chapter).

*   **`==` (Equal to):** `true` if operands are equal. `x == 10`
*   **`!=` (Not equal to):** `true` if operands are not equal. `x != 5`
*   **`<` (Less than):** `true` if the left operand is less than the right. `x < 20`
*   **`>` (Greater than):** `true` if the left operand is greater than the right. `x > 5`
*   **`<=` (Less than or equal to):** `true` if the left operand is less than or equal to the right. `x <= 10`
*   **`>=` (Greater than or equal to):** `true` if the left operand is greater than or equal to the right. `x >= 10`

**Common Mistake:** Using a single `=` (assignment operator) instead of `==` (comparison operator) in an `if` statement. `if (x = 5)` will *assign* 5 to x and then evaluate to true (because 5 is non-zero), which is usually not what you want. `if (x == 5)` correctly *compares* x to 5.

```cpp
int age = 25;
boolean isAdult = (age >= 18); // isAdult will be true
boolean isTen = (age == 10);   // isTen will be false
```

### 3. Logical Operators

These combine or modify boolean expressions.

*   **`&&` (Logical AND):** `true` only if *both* expressions are `true`.
    `if (temperature > 20 && humidity < 60)`
*   **`||` (Logical OR):** `true` if *at least one* of the expressions is `true`.
    `if (buttonPressed == true || emergencyStop == true)`
*   **`!` (Logical NOT):** Inverts a boolean value. `true` becomes `false`, and `false` becomes `true`.
    `boolean ledState = false; if (!ledState) { /* do something if ledState is false */ }`

```cpp
boolean sunny = true;
boolean warm = false;
boolean goToBeach = sunny && warm; // goToBeach is false
boolean stayInside = !sunny || !warm; // stayInside is true (because !warm is true)
```

### 4. Assignment Operators

*   **`=` (Assignment):** Assigns the value on the right to the variable on the left.
    `int count = 0;`
    `count = 10;`

### 5. Compound Assignment Operators

These provide a shorthand for common operations where a variable is modified by an operator and then the result is assigned back to the same variable.

*   **`+=` (Addition assignment):** `x += y;` is equivalent to `x = x + y;`
*   **`-=` (Subtraction assignment):** `x -= y;` is equivalent to `x = x - y;`
*   **`*=` (Multiplication assignment):** `x *= y;` is equivalent to `x = x * y;`
*   **`/=` (Division assignment):** `x /= y;` is equivalent to `x = x / y;`
*   **`%=` (Modulo assignment):** `x %= y;` is equivalent to `x = x % y;`

```cpp
int score = 100;
score += 10; // score is now 110
score -= 20; // score is now 90
score *= 2;  // score is now 180
score /= 3;  // score is now 60
```

### 6. Increment and Decrement Operators

Used to increase or decrease an integer variable by 1.

*   **`++` (Increment):**
    *   **Prefix:** `++variable;` (increments `variable` then uses its new value in the expression)
    *   **Postfix:** `variable++;` (uses `variable`'s current value in the expression, then increments `variable`)
*   **`--` (Decrement):**
    *   **Prefix:** `--variable;`
    *   **Postfix:** `variable--;`

The difference between prefix and postfix matters when used within a larger expression. When used on a line by itself, `x++;` and `++x;` have the same effect.

```cpp
int counter = 5;
counter++; // counter is now 6
++counter; // counter is now 7

int a = 3;
int b = ++a; // a becomes 4, then b is assigned 4. So a is 4, b is 4.

int c = 3;
int d = c++; // d is assigned 3 (c's original value), then c becomes 4. So c is 4, d is 3.
```

## Variable Scope

The **scope** of a variable defines where in your program the variable can be accessed.

*   **Global Variables:**
    *   Declared outside of any function (e.g., before `setup()`).
    *   Can be accessed from *any* function in your sketch (`setup()`, `loop()`, or any custom functions you write).
    *   Exist for the entire duration of the program.
    *   **Caution:** While sometimes necessary, overuse of global variables can make code harder to understand and debug, as they can be modified from anywhere.

*   **Local Variables:**
    *   Declared inside a function (like `setup()` or `loop()`) or a block of code (like inside an `if` statement or `for` loop).
    *   Can only be accessed from *within* that function or block where they are declared.
    *   Are created when the function/block is entered and destroyed when it's exited.
    *   Local variables with the same name in different functions are distinct and do not interfere with each other.

```cpp
int globalVar = 10; // This is a global variable

void setup() {
  int localVarSetup = 20; // This is a local variable to setup()
  Serial.begin(9600);
  Serial.print("Global var in setup: ");
  Serial.println(globalVar);
  Serial.print("Local var in setup: ");
  Serial.println(localVarSetup);
  // Serial.println(localVarLoop); // ERROR! localVarLoop is not in scope here
}

void loop() {
  int localVarLoop = 30; // This is a local variable to loop()
  globalVar++; // We can access and modify globalVar here
  Serial.print("Global var in loop: ");
  Serial.println(globalVar);
  // Serial.print("Local var in setup: "); // ERROR! localVarSetup is not in scope here
  Serial.print("Local var in loop: ");
  Serial.println(localVarLoop);
  delay(1000);
}
```
In this example:
*   `globalVar` can be used in both `setup()` and `loop()`.
*   `localVarSetup` can only be used within `setup()`.
*   `localVarLoop` can only be used within `loop()`.

It's generally good practice to use local variables whenever possible and limit the use of global variables to only what's truly necessary to share between functions.

## `const` Keyword

We used `const` in the last chapter: `const int externalLEDPin = 8;`
The `const` keyword declares a variable as a **constant**. This means its value cannot be changed after it's initialized.

**Benefits of using `const`:**
*   **Prevents Accidental Modification:** If you try to change the value of a `const` variable later in your code, the compiler will give you an error. This helps catch bugs.
*   **Readability:** It clearly signals to anyone reading the code that this value is fixed and not meant to change.
*   **Potential Compiler Optimizations:** The compiler might be able to optimize code better if it knows a value is constant.

It's good practice to use `const` for values that are truly constant, like pin numbers or fixed configuration parameters.

## Summary

Variables, data types, and operators are the bedrock of programming.
*   **Variables** are named containers for storing data that can change.
*   **Data types** define what kind of data a variable can hold (e.g., `int`, `float`, `boolean`, `char`). Choosing the correct type is important for memory and correctness.
*   **Operators** perform actions:
    *   **Arithmetic:** `+`, `-`, `*`, `/`, `%`
    *   **Comparison:** `==`, `!=`, `<`, `>`, `<=`, `>=`
    *   **Logical:** `&&`, `||`, `!`
    *   **Assignment:** `=`, `+=`, `-=`, etc.
    *   **Increment/Decrement:** `++`, `--`
*   **Variable scope** (global vs. local) determines where a variable can be accessed.
*   `const` makes a variable's value unchangeable after initialization.

Understanding these concepts will allow you to write much more powerful and flexible Arduino sketches. As we move on to control structures like `if` statements and loops, you'll see how variables and operators are used to make decisions and control the flow of your program.

### Action Steps/Challenges:

1.  **Data Type Sizes:** On the Arduino website or by a quick search, find out the exact memory size (in bytes) for `char`, `int`, `long`, and `float` on an Arduino Uno (AVR architecture).
2.  **Integer Division vs. Float Division:**
    Write a small sketch that does the following:
    *   Declares two `int` variables, `a = 7` and `b = 2`.
    *   Calculates `int result1 = a / b;`
    *   Calculates `float result2 = (float)a / b;`
    *   Uses `Serial.begin(9600);` in `setup()` and `Serial.println()` in `loop()` (or `setup()` if you just want to print once) to display the values of `result1` and `result2`. What's the difference? (We'll cover `Serial` communication in more detail later, but `Serial.println(variableName);` will print the value of `variableName` to the Serial Monitor in the IDE).
3.  **Modulo Operator Fun:**
    Think about how the modulo operator (`%`) could be used to make an LED blink a different color every second, if you had 3 different colored LEDs (e.g., red, green, blue) and a counter variable that increments every second. (You don't have to write the full LED code, just the logic for using modulo to cycle through 0, 1, 2, 0, 1, 2...).
4.  **Scope Experiment:** Try to access a variable declared locally within `setup()` from inside `loop()`. What error message does the Arduino IDE give you when you try to Verify/Compile? This will help solidify your understanding of scope.
5.  **Variable Naming:** Come up with good, descriptive variable names for the following:
    *   A variable to store the reading from a light sensor.
    *   A variable to store whether a door is open or closed.
    *   A variable to store the current speed of a motor.
    *   A variable to count how many times a button has been pressed.

Keep practicing with these fundamentals. They are the keys to unlocking more complex and interesting Arduino projects!
