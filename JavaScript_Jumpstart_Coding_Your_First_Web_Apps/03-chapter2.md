## Chapter 2: Core JavaScript Syntax

Now that you've had a taste of JavaScript and set up your environment, it's time to delve into the core syntax of the language. These are the fundamental rules and structures that you'll use to write any JavaScript program. We'll cover variables, different types of data, operators for performing actions, control flow for making decisions and repeating tasks, and functions for organizing your code.

### 2.1 Variables, Data Types

**Variables: Storing Information**

Think of variables as named containers or labels for storing data values. You can assign a value to a variable and then refer to that value using the variable's name.

In JavaScript, you declare variables using keywords: `var`, `let`, and `const`.

*   **`var` (Older way, generally avoid in modern JavaScript):**
    Historically, `var` was the only way to declare variables. It has some quirks related to "scope" (where the variable is accessible) that can lead to confusion.
    ```javascript
    var myName = "Alice";
    var userAge = 30;
    console.log(myName); // Output: Alice
    ```

*   **`let` (Modern way, preferred for variables that might change):**
    Introduced in ES6 (ECMAScript 2015), `let` provides block-scoping, which is more intuitive and helps prevent common bugs associated with `var`. Use `let` when you expect the variable's value to change during your program.
    ```javascript
    let score = 100;
    console.log(score); // Output: 100
    score = 150;        // Value can be reassigned
    console.log(score); // Output: 150

    let currentMessage; // Can declare without assigning a value initially
    currentMessage = "Processing...";
    console.log(currentMessage); // Output: Processing...
    ```

*   **`const` (Modern way, for variables that won't change - constants):**
    Also introduced in ES6, `const` is used to declare variables whose values are intended to remain constant throughout the program. Once a value is assigned to a `const` variable, it cannot be reassigned. This helps make your code safer and easier to understand.
    ```javascript
    const pi = 3.14159;
    console.log(pi); // Output: 3.14159
    // pi = 3.14; // This would cause an error: Assignment to constant variable.

    const siteName = "My Awesome Website";
    // const siteName; // Error: const declarations must be initialized.
    ```
    **Best Practice:** Prefer `const` by default. If you know a variable's value needs to change, then use `let`. Avoid `var` in new code.

**Variable Naming Rules & Conventions:**

*   Names can contain letters, digits, underscores (`_`), and dollar signs (`$`).
*   Names must begin with a letter, an underscore (`_`), or a dollar sign (`$`). They cannot start with a digit.
*   Names are case-sensitive (`myVariable` and `myvariable` are different).
*   Reserved keywords (like `let`, `const`, `var`, `function`, `if`, `for`, etc.) cannot be used as variable names.
*   **Convention (Camel Case):** It's a common convention in JavaScript to use "camelCase" for variable names. This means the first word is lowercase, and subsequent words start with an uppercase letter (e.g., `firstName`, `totalAmount`, `isUserLoggedIn`).

**Data Types**

JavaScript is a dynamically typed language. This means you don't explicitly specify the type of data a variable will hold. The JavaScript engine infers the type at runtime based on the value assigned.

JavaScript has several primitive data types and one complex data type (Object).

**Primitive Data Types:**

1.  **String:** Represents textual data. Strings are enclosed in single quotes (`'...'`), double quotes (`"..."`), or backticks (`` `...` `` - template literals, covered later).
    ```javascript
    let greeting = "Hello, JavaScript!";
    let userName = 'Bob';
    let message = `Welcome, ${userName}!`; // Template literal
    console.log(greeting);
    console.log(message); // Output: Welcome, Bob!
    ```

2.  **Number:** Represents both integer and floating-point (decimal) numbers.
    ```javascript
    let age = 25;
    let price = 19.99;
    let temperature = -5;
    console.log(age);
    console.log(price);
    ```
    There are also special numeric values: `Infinity`, `-Infinity`, and `NaN` (Not a Number - results from invalid math operations like `0/0`).

3.  **Boolean:** Represents a logical entity and can have only two values: `true` or `false`. Booleans are crucial for decision-making in code.
    ```javascript
    let isActive = true;
    let isLoggedIn = false;
    console.log(isActive); // Output: true
    ```

4.  **Null:** Represents the intentional absence of any object value. It's a special value that means "no value" or "empty". It's often assigned explicitly.
    ```javascript
    let userProfile = null; // User profile data is not yet loaded
    console.log(userProfile); // Output: null
    ```

5.  **Undefined:** A variable that has been declared but has not yet been assigned a value is `undefined`. Also, if a function doesn't explicitly return a value, it returns `undefined`.
    ```javascript
    let country;
    console.log(country); // Output: undefined
    ```

6.  **Symbol (ES6):** Represents a unique identifier. Symbols are mainly used to create unique property keys for objects, preventing naming collisions. We won't focus heavily on Symbols in this introductory book.
    ```javascript
    const id = Symbol('uniqueId');
    console.log(id.toString());
    ```

7.  **BigInt (ES2020):** Represents integers with arbitrary precision. Standard `Number` types can safely represent integers up to `2^53 - 1`. `BigInt` is used for numbers larger than that. You create a `BigInt` by appending `n` to the end of an integer or by calling the `BigInt()` constructor.
    ```javascript
    const veryLargeNumber = 9007199254740991n; // Note the 'n' at the end
    const anotherLargeNumber = BigInt("9007199254740992");
    console.log(veryLargeNumber);
    ```

**The `typeof` Operator:**
You can use the `typeof` operator to find out the data type of a variable or value.
```javascript
console.log(typeof "Hello");   // Output: "string"
console.log(typeof 100);       // Output: "number"
console.log(typeof true);      // Output: "boolean"
console.log(typeof undefined); // Output: "undefined"
console.log(typeof null);      // Output: "object" (This is a long-standing quirk in JavaScript!)
console.log(typeof Symbol('id'));// Output: "symbol"
console.log(typeof 9007199254740991n); // Output: "bigint"

let myVar;
console.log(typeof myVar);     // Output: "undefined"
myVar = "Now I'm a string";
console.log(typeof myVar);     // Output: "string"
```
Note the `typeof null` returning `"object"`. This is a historical bug that can't be fixed due to backward compatibility reasons. Remember that `null` is a primitive type, despite what `typeof` says.

### 2.2 Operators

Operators are special symbols used to perform operations on operands (values and variables).

**1. Arithmetic Operators:**
Perform mathematical calculations.
*   `+` (Addition)
*   `-` (Subtraction)
*   `*` (Multiplication)
*   `/` (Division)
*   `%` (Modulus - remainder of a division)
*   `**` (Exponentiation - ES2016)
*   `++` (Increment - adds 1 to a number)
*   `--` (Decrement - subtracts 1 from a number)

```javascript
let x = 10;
let y = 5;

console.log(x + y);  // Output: 15
console.log(x - y);  // Output: 5
console.log(x * y);  // Output: 50
console.log(x / y);  // Output: 2
console.log(10 % 3); // Output: 1 (10 divided by 3 is 3 with a remainder of 1)
console.log(2 ** 3); // Output: 8 (2 to the power of 3)

let count = 0;
count++; // count is now 1
console.log(count);
count--; // count is now 0
console.log(count);

// Increment/Decrement can be prefix or postfix
let a = 5;
let b = ++a; // a becomes 6, then b is assigned 6. (a=6, b=6)
console.log(`a: ${a}, b: ${b}`);

let c = 5;
let d = c++; // d is assigned 5, then c becomes 6. (c=6, d=5)
console.log(`c: ${c}, d: ${d}`);
```

**String Concatenation with `+`:**
The `+` operator can also be used to concatenate (join) strings.
```javascript
let firstName = "Jane";
let lastName = "Doe";
let fullName = firstName + " " + lastName;
console.log(fullName); // Output: "Jane Doe"

console.log("The year is " + 2024); // Output: "The year is 2024" (number is converted to string)
```

**2. Assignment Operators:**
Assign values to variables.
*   `=` (Assignment)
*   `+=` (Add and assign: `x += y` is `x = x + y`)
*   `-=` (Subtract and assign)
*   `*=` (Multiply and assign)
*   `/=` (Divide and assign)
*   `%=` (Modulus and assign)
*   `**=` (Exponentiation and assign)

```javascript
let num = 10;
num += 5; // num is now 15 (10 + 5)
console.log(num);

let productPrice = 20;
productPrice *= 1.1; // Increase price by 10% (productPrice = productPrice * 1.1)
console.log(productPrice); // Output: 22
```

**3. Comparison Operators:**
Compare two values and return a boolean (`true` or `false`).
*   `==` (Equal to - performs type coercion, try to avoid)
*   `===` (Strictly equal to - compares value and type, preferred)
*   `!=` (Not equal to - performs type coercion)
*   `!==` (Strictly not equal to - compares value and type, preferred)
*   `>` (Greater than)
*   `<` (Less than)
*   `>=` (Greater than or equal to)
*   `<=` (Less than or equal to)

```javascript
let val1 = 5;
let val2 = "5";

console.log(val1 == val2);  // Output: true (string "5" is coerced to number 5)
console.log(val1 === val2); // Output: false (different types: number vs string) - Recommended!

console.log(val1 != val2);  // Output: false
console.log(val1 !== val2); // Output: true - Recommended!

console.log(10 > 5);   // Output: true
console.log(10 <= 10); // Output: true
```
**Always prefer strict equality (`===`) and strict inequality (`!==`) to avoid unexpected behavior due to type coercion.**

**4. Logical Operators:**
Perform logical operations, usually on boolean values.
*   `&&` (Logical AND - returns `true` if both operands are `true`)
*   `||` (Logical OR - returns `true` if at least one operand is `true`)
*   `!` (Logical NOT - inverts the boolean value)

```javascript
let isAdult = true;
let hasTicket = false;

console.log(isAdult && hasTicket); // Output: false (true && false)
console.log(isAdult || hasTicket); // Output: true  (true || false)
console.log(!isAdult);            // Output: false (not true)
console.log(!hasTicket);          // Output: true  (not false)

// Short-circuiting:
// For &&, if the first operand is false, the second is not evaluated.
// For ||, if the first operand is true, the second is not evaluated.
let result = (10 > 20) && (console.log("This won't print")); // "This won't print" is not executed
let anotherResult = (10 < 20) || (console.log("This also won't print")); // "This also won't print" is not executed
```

**Truthy and Falsy Values:**
In JavaScript, values other than `true` and `false` can also behave like booleans in logical contexts.
*   **Falsy values:** `false`, `0`, `""` (empty string), `null`, `undefined`, `NaN`. These evaluate to `false` in a boolean context.
*   **Truthy values:** Everything else (including non-empty strings, non-zero numbers, objects, arrays) evaluates to `true`.

```javascript
if (0) { console.log("0 is truthy"); } else { console.log("0 is falsy"); } // Output: 0 is falsy
if ("hello") { console.log("'hello' is truthy"); } // Output: 'hello' is truthy
if ([]) { console.log("[] (empty array) is truthy"); } // Output: [] (empty array) is truthy
```

**5. Ternary Operator (Conditional Operator):**
A shorthand for a simple `if-else` statement.
Syntax: `condition ? valueIfTrue : valueIfFalse`

```javascript
let userAge = 18;
let accessMessage = (userAge >= 18) ? "Access Granted" : "Access Denied";
console.log(accessMessage); // Output: "Access Granted"

let temperature = 15;
let weatherType = (temperature > 25) ? "Hot" : (temperature < 10) ? "Cold" : "Moderate";
console.log(weatherType); // Output: Moderate (nested ternary)
```
While ternary operators can be concise, overly complex or nested ones can reduce readability.

**Operator Precedence:**
JavaScript has a defined order of operator precedence, similar to mathematical rules (PEMDAS/BODMAS). For example, multiplication and division are performed before addition and subtraction. Parentheses `()` can be used to override the default precedence or to improve clarity.
```javascript
let calculation = 5 + 10 * 2; // 10 * 2 is 20, then 5 + 20 = 25
console.log(calculation);     // Output: 25

let anotherCalc = (5 + 10) * 2; // 5 + 10 is 15, then 15 * 2 = 30
console.log(anotherCalc);       // Output: 30
```
When in doubt, use parentheses to make your intentions clear.

### 2.3 Control Flow

Control flow statements allow you to control the order in which your code executes, making decisions and repeating actions.

**1. `if`, `else if`, `else` Statements:**
Used to execute different blocks of code based on conditions.

```javascript
let hour = 14;

if (hour < 12) {
    console.log("Good morning!");
} else if (hour < 18) {
    console.log("Good afternoon!");
} else {
    console.log("Good evening!");
}
// Output: Good afternoon!

let num = 0;
if (num > 0) {
    console.log("Number is positive.");
} else if (num < 0) {
    console.log("Number is negative.");
} else {
    console.log("Number is zero.");
}
// Output: Number is zero.
```

**2. `switch` Statement:**
Used to perform different actions based on different conditions (cases). It's often an alternative to a long chain of `if-else if` statements when comparing a single value against multiple possibilities.

```javascript
let dayOfWeek = "Monday";
let activity;

switch (dayOfWeek) {
    case "Monday":
        activity = "Start the work week";
        break; // Important! Exits the switch statement
    case "Tuesday":
    case "Wednesday":
    case "Thursday":
        activity = "Keep working";
        break;
    case "Friday":
        activity = "Almost weekend!";
        break;
    case "Saturday":
    case "Sunday":
        activity = "Weekend fun!";
        break;
    default: // Optional: code to run if no case matches
        activity = "Invalid day";
}
console.log(activity); // Output: Start the work week

// Without break, execution "falls through" to the next case
let fruit = "apple";
switch (fruit) {
    case "apple":
        console.log("Apples are red or green."); // This will run
    case "banana":
        console.log("Bananas are yellow."); // This will also run if no break above
        break;
    default:
        console.log("Some other fruit.");
}
```
The `break` statement is crucial in `switch` blocks. Without it, the code will continue executing the statements in the subsequent `case` blocks (fall-through behavior), which is usually not intended.

**Loops: Repeating Code**

Loops are used to execute a block of code multiple times.

**3. `for` Loop:**
Repeats a block of code a specific number of times.
Syntax: `for (initialization; condition; increment/decrement)`

```javascript
// Print numbers from 0 to 4
for (let i = 0; i < 5; i++) { // i = 0, 1, 2, 3, 4
    console.log("The number is " + i);
}
/* Output:
The number is 0
The number is 1
The number is 2
The number is 3
The number is 4
*/

// Loop backwards
for (let j = 3; j >= 0; j--) {
    console.log("Countdown: " + j);
}
/* Output:
Countdown: 3
Countdown: 2
Countdown: 1
Countdown: 0
*/
```
*   **Initialization (`let i = 0`):** Executed once before the loop starts. Often used to declare and initialize a loop counter.
*   **Condition (`i < 5`):** Checked before each iteration. If `true`, the loop body executes. If `false`, the loop terminates.
*   **Increment/Decrement (`i++`):** Executed after each iteration. Typically used to update the loop counter.

**4. `while` Loop:**
Repeats a block of code as long as a specified condition is `true`. The condition is checked *before* each iteration.

```javascript
let counter = 0;
while (counter < 3) {
    console.log("While loop iteration: " + counter);
    counter++;
}
/* Output:
While loop iteration: 0
While loop iteration: 1
While loop iteration: 2
*/

// Be careful with while loops to avoid infinite loops!
// Ensure the condition eventually becomes false.
// let stuck = true;
// while (stuck) {
//   console.log("Stuck in an infinite loop!"); // This would run forever
// }
```

**5. `do...while` Loop:**
Similar to a `while` loop, but the condition is checked *after* each iteration. This means the loop body will always execute at least once, even if the condition is initially `false`.

```javascript
let k = 0;
do {
    console.log("Do...while iteration: " + k);
    k++;
} while (k < 3);
/* Output:
Do...while iteration: 0
Do...while iteration: 1
Do...while iteration: 2
*/

let m = 5; // Condition (m < 3) is initially false
do {
    console.log("This will run once: " + m); // Executes once
    m++;
} while (m < 3);
// Output: This will run once: 5
```

**`break` and `continue` in Loops:**

*   **`break`:** Immediately terminates the current loop (for, while, do...while, or switch).
    ```javascript
    for (let i = 0; i < 10; i++) {
        if (i === 5) {
            console.log("Breaking at 5");
            break; // Exit the loop
        }
        console.log(i);
    }
    // Output: 0, 1, 2, 3, 4, Breaking at 5
    ```

*   **`continue`:** Skips the current iteration of the loop and proceeds to the next iteration.
    ```javascript
    for (let i = 0; i < 5; i++) {
        if (i === 2) {
            console.log("Skipping 2");
            continue; // Skip the rest of this iteration
        }
        console.log(i);
    }
    // Output: 0, 1, Skipping 2, 3, 4
    ```

### 2.4 Functions: Declaration, Expression, Arrow Functions, Scope

Functions are reusable blocks of code that perform a specific task. They help organize your code, make it more readable, and reduce repetition.

**1. Function Declaration (Function Statement):**
This is the classic way to define a function. Function declarations are "hoisted," meaning they can be called before they are defined in the code.

```javascript
// Function declaration
function greet(name) { // 'name' is a parameter
    console.log("Hello, " + name + "!");
}

greet("Alice"); // Calling the function with an argument "Alice"
greet("Bob");   // Output: Hello, Alice!
                // Output: Hello, Bob!

function add(num1, num2) {
    let sum = num1 + num2;
    return sum; // Returns the result
}

let result = add(5, 10);
console.log("The sum is: " + result); // Output: The sum is: 15
console.log("Direct sum: " + add(3, 7)); // Output: Direct sum: 10

// Function without parameters and without an explicit return
function showMessage() {
    console.log("This is a message.");
    // Implicitly returns undefined
}
let msgReturn = showMessage(); // Output: This is a message.
console.log(msgReturn);      // Output: undefined
```
*   **Parameters:** Variables listed inside the parentheses `()` in the function definition (e.g., `name`, `num1`, `num2`). They act as placeholders for values that will be passed into the function.
*   **Arguments:** The actual values passed to the function when it is called (e.g., `"Alice"`, `5`, `10`).
*   **`return` statement:** Specifies the value the function should output. If a function doesn't have a `return` statement, or has a `return` statement without a value, it implicitly returns `undefined`. Once a `return` statement is executed, the function immediately stops executing.

**2. Function Expression:**
A function can also be defined as an expression and assigned to a variable. Function expressions are *not* hoisted, meaning you must define them before you can call them.

```javascript
const sayGoodbye = function(name) {
    console.log("Goodbye, " + name + "!");
}; // Note the semicolon here, as it's an assignment

sayGoodbye("Charlie"); // Output: Goodbye, Charlie!

const multiply = function(x, y) {
    return x * y;
};
let product = multiply(4, 6);
console.log("Product: " + product); // Output: Product: 24

// Anonymous functions: Function expressions are often anonymous (they don't have a name after the `function` keyword).
// The variable name (`sayGoodbye`, `multiply`) is used to call them.
```

**3. Arrow Functions (ES6):**
Arrow functions provide a more concise syntax for writing function expressions. They are always anonymous and have some differences in how the `this` keyword behaves (which we'll explore later).

```javascript
// Traditional function expression
const addOld = function(a, b) {
    return a + b;
};

// Arrow function equivalent
const addNew = (a, b) => {
    return a + b;
};
console.log(addNew(2, 3)); // Output: 5

// If the function body is a single expression, `return` and curly braces `{}` are implicit
const subtract = (a, b) => a - b;
console.log(subtract(10, 4)); // Output: 6

// If there's only one parameter, parentheses `()` around it are optional
const square = x => x * x;
console.log(square(5)); // Output: 25

// No parameters requires empty parentheses
const sayHello = () => console.log("Hello from an arrow function!");
sayHello(); // Output: Hello from an arrow function!

// For multi-line statements, curly braces and explicit return are needed
const processData = (data) => {
    console.log("Processing:", data);
    let processed = data.toUpperCase(); // Example processing
    return processed;
};
console.log(processData("test")); // Output: Processing: test
                                  // Output: TEST
```
Arrow functions are widely used in modern JavaScript, especially for short, callback functions.

**Scope: Where Variables Are Accessible**

Scope determines the accessibility (visibility) of variables. JavaScript has:

*   **Global Scope:** Variables declared outside any function or block are in the global scope. They can be accessed from anywhere in your JavaScript code. It's generally good practice to minimize the use of global variables to avoid naming conflicts and make code harder to manage.
    ```javascript
    let globalVar = "I am global";

    function checkGlobal() {
        console.log(globalVar); // Accessible here
    }
    checkGlobal(); // Output: I am global
    console.log(globalVar); // Accessible here too
    ```

*   **Function Scope (Local Scope with `var`):** Variables declared with `var` inside a function are accessible only within that function (and any functions nested inside it). They are not accessible outside the function.
    ```javascript
    function myFunction() {
        var functionScopedVar = "I am local to myFunction (var)";
        console.log(functionScopedVar);
    }
    myFunction(); // Output: I am local to myFunction (var)
    // console.log(functionScopedVar); // Error: functionScopedVar is not defined
    ```

*   **Block Scope (Local Scope with `let` and `const`):** Variables declared with `let` and `const` inside a block (code enclosed in curly braces `{}`, like in an `if` statement or a `for` loop) are accessible only within that block.
    ```javascript
    if (true) {
        let blockScopedLet = "I am in a block (let)";
        const blockScopedConst = "I am also in a block (const)";
        var blockScopedVar = "I am in a block (var - but behaves like function scope)";
        console.log(blockScopedLet);
        console.log(blockScopedConst);
        console.log(blockScopedVar);
    }
    // console.log(blockScopedLet);   // Error: blockScopedLet is not defined
    // console.log(blockScopedConst); // Error: blockScopedConst is not defined
    console.log(blockScopedVar);     // Output: I am in a block (var - but behaves like function scope)
                                     // This demonstrates a key difference of var!

    for (let i = 0; i < 1; i++) {
        let loopVar = "Inside loop";
        console.log(loopVar); // Output: Inside loop
    }
    // console.log(loopVar); // Error: loopVar is not defined
    ```
    Block scope with `let` and `const` is generally preferred because it helps prevent accidental modification of variables and makes code easier to reason about.

**Lexical Scope (Static Scope):**
JavaScript uses lexical scope, meaning the scope of a variable is determined by its position in the source code at the time the code is written (lexed/parsed), not at runtime. Inner functions have access to variables declared in their outer functions. This concept is fundamental to closures, which we'll touch upon later.

```javascript
function outerFunction() {
    let outerVar = "I am from outerFunction";

    function innerFunction() {
        let innerVar = "I am from innerFunction";
        console.log(outerVar); // innerFunction can access outerVar
        console.log(innerVar);
    }
    innerFunction();
    // console.log(innerVar); // Error: innerVar is not defined here
}
outerFunction();
/* Output:
I am from outerFunction
I am from innerFunction
*/
```

### 2.5 Comments and Code Readability

Comments are notes in your code that are ignored by the JavaScript engine. They are for human readers to understand the code's purpose, logic, or how to use it.

**Types of Comments:**

*   **Single-line comments:** Start with `//`. Everything from `//` to the end of the line is a comment.
    ```javascript
    // This is a single-line comment
    let x = 10; // This comment explains the variable
    ```

*   **Multi-line comments:** Start with `/*` and end with `*/`. Everything between `/*` and `*/` is a comment, even across multiple lines.
    ```javascript
    /*
    This is a
    multi-line comment.
    It can span several lines.
    */
    let y = 20;
    ```

**Why Use Comments?**

*   **Explain complex logic:** If a piece of code is particularly tricky or non-obvious, a comment can clarify what it does and why.
*   **Provide context:** Explain the purpose of a function or a block of code.
*   **Leave to-do notes:** Mark areas that need further work (e.g., `// TODO: Implement error handling`).
*   **Temporarily disable code (commenting out):** Useful for debugging.

```javascript
// Function to calculate the area of a rectangle
function calculateArea(width, height) {
    // Ensure both width and height are positive numbers
    if (width <= 0 || height <= 0) {
        return 0; // Or throw an error, or return null
    }
    return width * height;
}

/*
let result = calculateArea(5, 10);
console.log(result);
*/ // This block of code is temporarily commented out
```

**Code Readability:**
Beyond comments, writing readable code is crucial for maintainability.
*   **Consistent Naming:** Use clear and consistent names for variables and functions (e.g., camelCase).
*   **Indentation and Formatting:** Use consistent indentation (usually 2 or 4 spaces) and spacing to structure your code visually. Most code editors can auto-format your code.
*   **Keep Functions Small:** Break down complex tasks into smaller, focused functions.
*   **Avoid overly long lines of code.**

### 2.6 Chapter Summary & Action Steps

**Summary:**

*   **Variables** are declared using `let` (for values that can change) and `const` (for constant values). Avoid `var` in modern JavaScript.
*   JavaScript has **primitive data types**: String, Number, Boolean, Null, Undefined, Symbol, and BigInt. `typeof` can identify most types (with a quirk for `null`).
*   **Operators** perform actions: Arithmetic (`+`, `-`, `*`, `/`, `%`, `**`, `++`, `--`), Assignment (`=`, `+=`, etc.), Comparison (`===`, `!==`, `>`, `<`), Logical (`&&`, `||`, `!`), and Ternary (`condition ? trueVal : falseVal`).
*   **Control Flow** statements direct execution: `if/else if/else` for conditional logic, `switch` for multi-way branching, and loops (`for`, `while`, `do...while`) for repetition. `break` and `continue` modify loop behavior.
*   **Functions** are reusable code blocks defined using declarations, expressions, or arrow functions (`=>`). They help organize code and can take parameters and return values.
*   **Scope** (global, function, block) determines variable accessibility. `let` and `const` provide block scope, which is generally preferred.
*   **Comments** (`//` and `/* ... */`) and good formatting improve code readability.

**Action Steps:**

1.  **Variable Practice:**
    *   Declare a variable using `let` to store your favorite color and print it to the console.
    *   Declare a variable using `const` to store your birth year. Try to reassign it and observe the error in the console.
    *   Experiment with `typeof` on different values (e.g., `typeof "JavaScript"`, `typeof 42`, `typeof (10 < 5)`).
2.  **Operator Challenges:**
    *   Write an expression that calculates the area of a circle with a radius of 5 (Area = π * r²). Use `const pi = 3.14159;`.
    *   Declare two boolean variables, `isRaining` and `isSunny`. Use logical operators to determine if it's a "good day for a walk" (e.g., sunny and not raining). Print the result.
    *   Use the ternary operator to assign a message to a variable: "Adult" if `age >= 18`, otherwise "Minor".
3.  **Control Flow Exercises:**
    *   Write an `if-else if-else` statement that checks a `grade` variable (a number from 0-100) and prints "A" (90-100), "B" (80-89), "C" (70-79), "D" (60-69), or "F" (<60).
    *   Write a `for` loop that prints all even numbers from 0 to 10.
    *   Write a `while` loop that asks the user for input using `prompt("Enter 'quit' to exit:")` and continues looping until the user types "quit". (Note: `prompt` creates a pop-up, use with awareness).
4.  **Function Creation:**
    *   Write a function declaration named `calculateRectanglePerimeter` that takes `width` and `height` as parameters and returns the perimeter. Test it.
    *   Rewrite the above function as a function expression assigned to a variable.
    *   Rewrite it again as an arrow function.
    *   Create a function that takes a name as an argument and returns a personalized greeting string (e.g., "Welcome back, [Name]!").
5.  **Scope Experiment:**
    *   Create a global variable. Inside a function, try to access it.
    *   Inside a function, declare a variable with `let`. Try to access it outside the function and observe the error.
    *   Create an `if` block. Inside it, declare a variable with `let`. Try to access it outside the `if` block.

This chapter covered a lot of ground! These core syntax elements are the bedrock of JavaScript programming. Practice them, experiment, and don't hesitate to look up details as you build more complex programs. In the next chapter, we'll explore how to group data together using Arrays and Objects.
