## Chapter 3: Working with Data Structures: Arrays and Objects

In the previous chapter, we learned about primitive data types like strings, numbers, and booleans. These are great for storing individual pieces of information. However, we often need to work with collections of related data or more complex data entities. That's where JavaScript's primary data structures come in: **Arrays** for ordered lists of items, and **Objects** for collections of key-value pairs. We'll also touch upon **JSON**, a common data format closely related to JavaScript objects.

### 3.1 Arrays: Ordered Collections

An array is an ordered list of values. Each value in an array is called an **element**, and each element has a numerical **index** (position), starting from 0. Arrays can store elements of any data type, including other arrays or objects.

**Creating Arrays:**

There are two main ways to create arrays:

1.  **Array Literal (preferred and most common):**
    Uses square brackets `[]`.
    ```javascript
    // An empty array
    let emptyArray = [];
    console.log(emptyArray); // Output: []

    // An array of strings (colors)
    let colors = ["red", "green", "blue"];
    console.log(colors); // Output: ["red", "green", "blue"]

    // An array of numbers
    let numbers = [1, 2, 3, 4, 5];
    console.log(numbers); // Output: [1, 2, 3, 4, 5]

    // An array with mixed data types
    let mixedArray = ["apple", 100, true, null];
    console.log(mixedArray); // Output: ["apple", 100, true, null]
    ```

2.  **`Array` Constructor (less common for simple arrays):**
    Uses the `new Array()` syntax.
    ```javascript
    let fruits = new Array("apple", "banana", "cherry");
    console.log(fruits); // Output: ["apple", "banana", "cherry"]

    // If you pass a single number to the constructor, it creates an empty array with that length
    let sizedArray = new Array(3); // Creates an empty array with length 3
    console.log(sizedArray);       // Output: [ <3 empty items> ]
    console.log(sizedArray.length);// Output: 3
    ```
    For clarity and simplicity, array literals `[]` are generally preferred.

**Accessing Array Elements:**

You access elements in an array using their zero-based index within square brackets.

```javascript
let colors = ["red", "green", "blue", "yellow"];

console.log(colors[0]); // Output: "red" (first element)
console.log(colors[1]); // Output: "green" (second element)
console.log(colors[3]); // Output: "yellow" (last element in this case)

// Accessing an index that doesn't exist returns undefined
console.log(colors[10]); // Output: undefined
```

**Modifying Array Elements:**

You can change the value of an array element by assigning a new value to its index.

```javascript
let seasons = ["Spring", "Summer", "Autum", "Winter"]; // Typo in "Autum"
console.log(seasons); // Output: ["Spring", "Summer", "Autum", "Winter"]

seasons[2] = "Autumn"; // Correct the typo
console.log(seasons);    // Output: ["Spring", "Summer", "Autumn", "Winter"]

seasons[4] = "Another Season"; // Adds a new element if index is at the end or creates empty spots
console.log(seasons);          // Output: ["Spring", "Summer", "Autumn", "Winter", "Another Season"]
console.log(seasons.length);   // Output: 5
```

**Array Properties and Methods:**

Arrays come with built-in properties and methods that allow you to manipulate and work with them effectively.

*   **`length` Property:**
    Returns the number of elements in the array.
    ```javascript
    let numbers = [10, 20, 30, 40];
    console.log(numbers.length); // Output: 4

    numbers[10] = 100; // Assigning to a higher index increases length
    console.log(numbers.length); // Output: 11 (indices 4-9 are empty/undefined)
    console.log(numbers);        // Output: [10, 20, 30, 40, <6 empty items>, 100]
    ```

**Common Array Methods (Modifying the original array):**

These methods change the array they are called on.

*   **`push()`: Adds one or more elements to the end of an array.** Returns the new length of the array.
    ```javascript
    let tasks = ["Write code"];
    let newLength = tasks.push("Test code", "Deploy code");
    console.log(tasks);     // Output: ["Write code", "Test code", "Deploy code"]
    console.log(newLength); // Output: 3
    ```

*   **`pop()`: Removes the last element from an array.** Returns the removed element.
    ```javascript
    let items = ["A", "B", "C"];
    let removedItem = items.pop();
    console.log(items);       // Output: ["A", "B"]
    console.log(removedItem); // Output: "C"
    ```

*   **`shift()`: Removes the first element from an array.** Returns the removed element. (Note: This can be less efficient for large arrays as other elements need to be re-indexed).
    ```javascript
    let queue = ["First", "Second", "Third"];
    let firstInQueue = queue.shift();
    console.log(queue);          // Output: ["Second", "Third"]
    console.log(firstInQueue); // Output: "First"
    ```

*   **`unshift()`: Adds one or more elements to the beginning of an array.** Returns the new length of the array. (Note: Can also be less efficient for large arrays).
    ```javascript
    let messages = ["Hello"];
    messages.unshift("Hi", "Greetings");
    console.log(messages); // Output: ["Hi", "Greetings", "Hello"]
    ```

*   **`splice()`: A powerful method to add, remove, or replace elements at any position.**
    Syntax: `array.splice(startIndex, deleteCount, item1, item2, ...)`
    *   `startIndex`: The index at which to start changing the array.
    *   `deleteCount`: (Optional) The number of elements to remove from `startIndex`.
    *   `item1, item2, ...`: (Optional) Elements to add to the array, beginning at `startIndex`.
    Returns an array containing the deleted elements.

    ```javascript
    let letters = ["a", "b", "c", "d", "e"];

    // Remove 2 elements starting from index 1 ("b", "c")
    let deletedLetters = letters.splice(1, 2);
    console.log(letters);         // Output: ["a", "d", "e"]
    console.log(deletedLetters);  // Output: ["b", "c"]

    // Remove 1 element at index 2 ("e") and insert "X", "Y"
    letters = ["a", "b", "c", "d", "e"];
    let replaced = letters.splice(2, 1, "X", "Y");
    console.log(letters);    // Output: ["a", "b", "X", "Y", "d", "e"]
    console.log(replaced); // Output: ["c"]

    // Insert elements without deleting any
    letters = ["a", "b", "c"];
    letters.splice(1, 0, "Z"); // At index 1, delete 0, insert "Z"
    console.log(letters);    // Output: ["a", "Z", "b", "c"]
    ```

**Common Array Methods (Returning a new array - non-mutating):**

These methods do *not* change the original array but return a new array or value.

*   **`slice()`: Returns a shallow copy of a portion of an array into a new array object.**
    Syntax: `array.slice(startIndex, endIndex)`
    *   `startIndex`: (Optional) Index at which to begin extraction. If negative, it indicates an offset from the end.
    *   `endIndex`: (Optional) Index *before* which to end extraction. `slice` extracts up to but not including `endIndex`. If omitted, extracts through the end.
    ```javascript
    let numbers = [10, 20, 30, 40, 50];
    let subArray1 = numbers.slice(1, 4); // Elements from index 1 up to (but not including) index 4
    console.log(subArray1);         // Output: [20, 30, 40]
    console.log(numbers);           // Output: [10, 20, 30, 40, 50] (original is unchanged)

    let subArray2 = numbers.slice(2); // From index 2 to the end
    console.log(subArray2);         // Output: [30, 40, 50]

    let subArray3 = numbers.slice(-2); // Last 2 elements
    console.log(subArray3);          // Output: [40, 50]
    ```

*   **`concat()`: Merges two or more arrays (or values) and returns a new array.**
    ```javascript
    let arr1 = [1, 2];
    let arr2 = [3, 4];
    let arr3 = [5, 6];
    let combined = arr1.concat(arr2, arr3, 7);
    console.log(combined); // Output: [1, 2, 3, 4, 5, 6, 7]
    console.log(arr1);     // Output: [1, 2] (original unchanged)
    ```

**Iterating Over Arrays (Looping):**

There are several ways to loop through the elements of an array.

*   **`for` loop (traditional):**
    ```javascript
    let fruits = ["Apple", "Banana", "Cherry"];
    for (let i = 0; i < fruits.length; i++) {
        console.log(fruits[i]);
    }
    // Output: Apple, Banana, Cherry (each on a new line)
    ```

*   **`for...of` loop (ES6 - preferred for iterating values):**
    This loop iterates directly over the values of an iterable object, like an array.
    ```javascript
    let tools = ["Hammer", "Screwdriver", "Wrench"];
    for (const tool of tools) {
        console.log(tool);
    }
    // Output: Hammer, Screwdriver, Wrench
    ```

*   **`forEach()` method:**
    Executes a provided function once for each array element.
    Syntax: `array.forEach(function(currentValue, index, array) { ... });`
    ```javascript
    let names = ["Alice", "Bob", "Charlie"];
    names.forEach(function(name, idx) {
        console.log(`Index ${idx}: ${name}`);
    });
    // Output:
    // Index 0: Alice
    // Index 1: Bob
    // Index 2: Charlie

    // Using an arrow function with forEach
    names.forEach((name, idx) => console.log(`${idx} -> ${name.toUpperCase()}`));
    // Output:
    // 0 -> ALICE
    // 1 -> BOB
    // 2 -> CHARLIE
    ```

**Higher-Order Array Methods (ES5+):**
JavaScript provides powerful higher-order functions for arrays that take other functions as arguments (callbacks). These allow for more declarative and concise array manipulation.

*   **`map()`: Creates a new array populated with the results of calling a provided function on every element in the calling array.**
    ```javascript
    let numbers = [1, 2, 3, 4];
    let doubled = numbers.map(function(num) {
        return num * 2;
    });
    console.log(doubled); // Output: [2, 4, 6, 8]
    console.log(numbers); // Output: [1, 2, 3, 4] (original unchanged)

    let squared = numbers.map(num => num * num);
    console.log(squared); // Output: [1, 4, 9, 16]
    ```

*   **`filter()`: Creates a new array with all elements that pass the test implemented by the provided function.**
    ```javascript
    let values = [10, 25, 8, 42, 15, 30];
    let above20 = values.filter(function(val) {
        return val > 20;
    });
    console.log(above20); // Output: [25, 42, 30]

    let evenNumbers = values.filter(val => val % 2 === 0);
    console.log(evenNumbers); // Output: [10, 8, 42, 30]
    ```

*   **`reduce()`: Executes a "reducer" function on each element of the array, resulting in a single output value.**
    Syntax: `array.reduce(function(accumulator, currentValue, currentIndex, array) { ... }, initialValue);`
    *   `accumulator`: The value resulting from the previous call to the callback function.
    *   `currentValue`: The current element being processed.
    *   `initialValue`: (Optional) A value to use as the first argument to the first call of the callback.
    ```javascript
    let numsToSum = [1, 2, 3, 4, 5];
    let sum = numsToSum.reduce(function(total, currentNum) {
        console.log(`Total: ${total}, Current: ${currentNum}`);
        return total + currentNum;
    }, 0); // 0 is the initial value for 'total'
    console.log("Sum:", sum); // Output: Sum: 15

    /*
    Output of console.log inside reduce:
    Total: 0, Current: 1
    Total: 1, Current: 2
    Total: 3, Current: 3
    Total: 6, Current: 4
    Total: 10, Current: 5
    */

    // Example: Flattening an array of arrays
    let nested = [[1, 2], [3, 4], [5, 6]];
    let flat = nested.reduce((acc, current) => acc.concat(current), []);
    console.log(flat); // Output: [1, 2, 3, 4, 5, 6]
    ```

Other useful methods include `find()`, `findIndex()`, `some()`, `every()`, `includes()`, `join()`, `sort()`, `reverse()`. We encourage you to look these up on MDN (Mozilla Developer Network) as you need them.

### 3.2 Objects: Key-Value Collections

Objects are collections of key-value pairs. The keys (also called **properties** or **property names**) are usually strings (or Symbols), and the values can be any data type, including other objects or functions (which are then called **methods**). Objects are used to represent more complex entities with named characteristics.

**Creating Objects:**

1.  **Object Literal (preferred and most common):**
    Uses curly braces `{}`.
    ```javascript
    // An empty object
    let emptyObject = {};
    console.log(emptyObject); // Output: {}

    // A simple person object
    let person = {
        firstName: "John", // "firstName" is the key, "John" is the value
        lastName: "Doe",
        age: 30,
        isStudent: false,
        "favorite color": "blue" // Keys with spaces or special characters need quotes
    };
    console.log(person);
    // Output: {firstName: "John", lastName: "Doe", age: 30, isStudent: false, favorite color: "blue"}
    ```

2.  **`Object` Constructor (less common for simple objects):**
    Uses `new Object()`.
    ```javascript
    let car = new Object();
    car.make = "Toyota";
    car.model = "Camry";
    car.year = 2021;
    console.log(car); // Output: {make: "Toyota", model: "Camry", year: 2021}
    ```

**Accessing Object Properties:**

You can access object properties in two ways:

1.  **Dot Notation (`object.propertyName`):**
    Simpler and generally preferred when the property name is a valid JavaScript identifier (no spaces, doesn't start with a number, etc.).
    ```javascript
    let book = {
        title: "The Great Gatsby",
        author: "F. Scott Fitzgerald",
        pages: 180
    };
    console.log(book.title);   // Output: The Great Gatsby
    console.log(book.author);  // Output: F. Scott Fitzgerald
    ```

2.  **Bracket Notation (`object['propertyName']`):**
    Required when the property name:
    *   Is not a valid identifier (e.g., contains spaces, hyphens, or starts with a number).
    *   Is stored in a variable.
    *   Is a Symbol.
    The property name inside brackets is treated as a string.

    ```javascript
    let student = {
        name: "Alice",
        "student ID": "S12345",
        gradeLevel: 10
    };
    console.log(student["student ID"]); // Output: S12345
    console.log(student.gradeLevel);    // Output: 10 (dot notation still works for valid identifiers)

    let propertyToAccess = "name";
    console.log(student[propertyToAccess]); // Output: Alice (accessing property using a variable)
    ```

**Modifying Object Properties:**

You can change the value of an existing property or add a new property by assigning a value.

```javascript
let user = {
    username: "testuser",
    email: "test@example.com"
};
console.log(user.username); // Output: testuser

// Modify an existing property
user.email = "updated@example.com";
console.log(user.email); // Output: updated@example.com

// Add a new property
user.isAdmin = false;
user.lastLogin = "2024-07-28";
console.log(user);
// Output: {username: "testuser", email: "updated@example.com", isAdmin: false, lastLogin: "2024-07-28"}
```

**Deleting Object Properties:**
Use the `delete` operator.
```javascript
let product = {
    id: 101,
    name: "Laptop",
    price: 1200,
    inStock: true
};
delete product.inStock;
console.log(product); // Output: {id: 101, name: "Laptop", price: 1200}
console.log(product.inStock); // Output: undefined
```

**Object Methods:**
When a property of an object is a function, it's called a method. Methods define the behavior or actions an object can perform.

```javascript
let calculator = {
    operand1: 0,
    operand2: 0,
    add: function() {
        return this.operand1 + this.operand2; // 'this' refers to the calculator object itself
    },
    subtract(val1, val2) { // Shorthand method syntax (ES6)
        return val1 - val2;
    }
};

calculator.operand1 = 10;
calculator.operand2 = 5;
console.log(calculator.add()); // Output: 15

console.log(calculator.subtract(20, 8)); // Output: 12
```
The `this` keyword in JavaScript can be tricky. In an object method (defined with `function` or ES6 method syntax), `this` typically refers to the object the method was called on. Arrow functions handle `this` differently (lexically inheriting it from the surrounding scope), which is an important distinction we'll explore more later.

**Iterating Over Object Properties:**

*   **`for...in` loop:**
    Iterates over the enumerable property names (keys) of an object.
    ```javascript
    let course = {
        title: "JavaScript Basics",
        duration: "4 weeks",
        instructor: "Dr. Code"
    };

    for (let key in course) {
        // It's good practice to check if the property belongs to the object itself
        // and not inherited from its prototype chain.
        if (course.hasOwnProperty(key)) {
            console.log(`${key}: ${course[key]}`);
        }
    }
    /* Output:
    title: JavaScript Basics
    duration: 4 weeks
    instructor: Dr. Code
    */
    ```

*   **`Object.keys()`:** Returns an array of an object's own enumerable property names.
*   **`Object.values()`:** Returns an array of an object's own enumerable property values.
*   **`Object.entries()`:** Returns an array of an object's own enumerable string-keyed property `[key, value]` pairs.

    ```javascript
    let pet = {
        name: "Buddy",
        type: "Dog",
        age: 5
    };

    let keys = Object.keys(pet);
    console.log(keys); // Output: ["name", "type", "age"]
    for (const key of keys) {
        console.log(`${key} -> ${pet[key]}`);
    }

    let values = Object.values(pet);
    console.log(values); // Output: ["Buddy", "Dog", 5]

    let entries = Object.entries(pet);
    console.log(entries); // Output: [["name", "Buddy"], ["type", "Dog"], ["age", 5]]
    for (const [key, value] of entries) { // Using array destructuring
        console.log(`Property ${key} has value ${value}`);
    }
    ```

**Nested Objects and Arrays:**
Objects and arrays can be nested within each other to create complex data structures.
```javascript
let studentProfile = {
    id: "S001",
    name: "Eva Green",
    contact: {
        email: "eva@example.com",
        phone: "555-1234"
    },
    courses: [
        { title: "Math 101", grade: "A" },
        { title: "History 202", grade: "B+" }
    ]
};

console.log(studentProfile.name);                 // Output: Eva Green
console.log(studentProfile.contact.email);        // Output: eva@example.com
console.log(studentProfile.courses[0].title);     // Output: Math 101
console.log(studentProfile.courses[1].grade);     // Output: B+

studentProfile.courses.push({ title: "Art 100", grade: "A-" });
console.log(studentProfile.courses.length);       // Output: 3
```

### 3.3 JSON: JavaScript Object Notation

JSON (JavaScript Object Notation) is a lightweight data-interchange format. It is easy for humans to read and write, and easy for machines to parse and generate. JSON is derived from JavaScript object literal syntax, but it's not exactly the same. It's language-independent but commonly used with JavaScript.

**Key Differences between JSON and JavaScript Object Literals:**

1.  **Keys must be strings in double quotes:** In JSON, all property names (keys) *must* be enclosed in double quotes.
    ```json
    // Valid JSON
    {
      "name": "Widget",
      "id": 123,
      "isAvailable": true
    }
    ```
    In JavaScript object literals, quotes around keys are often optional if the key is a valid identifier.

2.  **Values:** JSON values can be strings (in double quotes), numbers, booleans (`true`/`false`), arrays (using `[]`), or other JSON objects. `null` is also a valid JSON value.
    **JSON does NOT support functions, `undefined`, dates as a distinct type (they are usually represented as strings), or comments.**

3.  **No trailing commas:** Trailing commas (e.g., after the last property or last array element) are generally not allowed in JSON, though some parsers might be lenient.

**Working with JSON in JavaScript:**

JavaScript provides built-in methods to convert between JavaScript objects/arrays and JSON strings:

*   **`JSON.stringify(value)`:** Converts a JavaScript value (object, array, primitive) into a JSON string.
    ```javascript
    let bookData = {
        title: "1984",
        author: "George Orwell",
        year: 1949,
        genres: ["Dystopian", "Sci-Fi"],
        // aFunction: () => console.log("test") // Functions are ignored
        // status: undefined // undefined properties are ignored
    };

    let jsonString = JSON.stringify(bookData);
    console.log(jsonString);
    // Output: {"title":"1984","author":"George Orwell","year":1949,"genres":["Dystopian","Sci-Fi"]}
    // Note: functions and undefined properties are omitted.

    let prettyJsonString = JSON.stringify(bookData, null, 2); // null replacer, 2 spaces for indentation
    console.log(prettyJsonString);
    /* Output (formatted):
    {
      "title": "1984",
      "author": "George Orwell",
      "year": 1949,
      "genres": [
        "Dystopian",
        "Sci-Fi"
      ]
    }
    */
    ```

*   **`JSON.parse(jsonString)`:** Parses a JSON string, constructing the JavaScript value or object described by the string.
    ```javascript
    let productJson = '{ "name": "Laptop", "price": 999.99, "inStock": true, "specs": {"cpu": "i7", "ram": "16GB"} }';

    let productObject = JSON.parse(productJson);
    console.log(productObject);
    // Output: {name: "Laptop", price: 999.99, inStock: true, specs: {cpu: "i7", ram: "16GB"}}

    console.log(productObject.name);      // Output: Laptop
    console.log(productObject.specs.ram); // Output: 16GB
    ```
    If the string is not valid JSON, `JSON.parse()` will throw a `SyntaxError`.

JSON is extensively used for exchanging data between web servers and web applications (clients), in configuration files, and more.

### 3.4 Data Type Conversion (Coercion)

JavaScript is a loosely typed language, and it often performs automatic type conversions (coercion) when operators are used with different data types. This can be convenient but also a source of bugs if not understood.

**String Conversion:**
*   Occurs when the `+` operator is used with a string and another type.
*   The `String()` function can explicitly convert a value to a string.
    ```javascript
    console.log("5" + 3);     // Output: "53" (3 is converted to "3")
    console.log(5 + "3");     // Output: "53" (5 is converted to "5")
    console.log("Value: " + 100); // Output: "Value: 100"

    let num = 123;
    let strNum = String(num);
    console.log(typeof strNum); // Output: "string"
    console.log(String(true));  // Output: "true"
    console.log(String(null));  // Output: "null"
    ```

**Numeric Conversion:**
*   Occurs in mathematical operations (except `+` with a string).
*   The `Number()` function can explicitly convert a value to a number.
*   `parseInt()` and `parseFloat()` are used to parse strings into integers and floating-point numbers, respectively.
    ```javascript
    console.log("10" - "5"); // Output: 5 (strings converted to numbers)
    console.log("10" * "2"); // Output: 20
    console.log("10" / "2"); // Output: 5
    console.log("apple" - 5); // Output: NaN (Not a Number)

    console.log(Number("123"));     // Output: 123
    console.log(Number("  12.3  "));// Output: 12.3
    console.log(Number(""));        // Output: 0
    console.log(Number("hello"));   // Output: NaN
    console.log(Number(true));      // Output: 1
    console.log(Number(false));     // Output: 0
    console.log(Number(null));      // Output: 0
    console.log(Number(undefined)); // Output: NaN

    console.log(parseInt("100px"));   // Output: 100 (stops at non-numeric character)
    console.log(parseFloat("12.5em"));// Output: 12.5
    console.log(parseInt("0xFF", 16)); // Output: 255 (parsing hexadecimal)
    console.log(parseInt("101", 2));   // Output: 5 (parsing binary)
    ```
    The unary plus operator `+` can also be used as a shorthand for `Number()`:
    ```javascript
    let strValue = "42";
    let numValue = +strValue;
    console.log(typeof numValue, numValue); // Output: number 42
    ```

**Boolean Conversion:**
*   Occurs in logical operations or when a value is expected to be boolean (e.g., in an `if` condition).
*   The `Boolean()` function can explicitly convert a value to a boolean.
    Remember the falsy values: `0`, `""`, `null`, `undefined`, `NaN`. All other values are truthy.
    ```javascript
    console.log(Boolean(1));      // Output: true
    console.log(Boolean(0));      // Output: false
    console.log(Boolean("hello"));// Output: true
    console.log(Boolean(""));     // Output: false
    console.log(Boolean(null));   // Output: false
    console.log(Boolean({}));     // Output: true (empty object is truthy)
    console.log(Boolean([]));     // Output: true (empty array is truthy)

    if ("text") { // "text" is truthy
        console.log("This will run");
    }
    if (0) { // 0 is falsy
        console.log("This will NOT run");
    }
    ```
While automatic type coercion can seem handy, it's often better to perform explicit conversions (e.g., using `Number()`, `String()`, `Boolean()`) to make your code clearer and avoid unexpected behavior, especially when dealing with user input or data from external sources.

### 3.5 Chapter Summary & Action Steps

**Summary:**

*   **Arrays** are ordered lists of values, accessed by numerical index (starting from 0). Created with `[]`.
    *   Key properties/methods: `length`, `push()`, `pop()`, `shift()`, `unshift()`, `splice()`, `slice()`, `concat()`, `forEach()`, `map()`, `filter()`, `reduce()`.
*   **Objects** are unordered collections of key-value pairs. Keys are strings (or Symbols), values can be any type. Created with `{}`.
    *   Access properties using dot notation (`.`) or bracket notation (`[]`).
    *   Functions as object properties are called **methods**.
    *   Iterate with `for...in` or `Object.keys()`, `Object.values()`, `Object.entries()`.
*   **JSON (JavaScript Object Notation)** is a text-based data format. Keys must be double-quoted strings. No functions, undefined, or comments.
    *   Use `JSON.stringify()` to convert JavaScript objects to JSON strings.
    *   Use `JSON.parse()` to convert JSON strings to JavaScript objects.
*   **Data Type Conversion (Coercion)** happens automatically in JavaScript but can be done explicitly with `String()`, `Number()`, `Boolean()`, `parseInt()`, `parseFloat()`. Be mindful of truthy/falsy values.

**Action Steps:**

1.  **Array Manipulation:**
    *   Create an array of your favorite hobbies.
    *   Add a new hobby to the end using `push()`.
    *   Remove the first hobby using `shift()`.
    *   Use a `for...of` loop to print each hobby to the console.
    *   Create an array of numbers. Use `map()` to create a new array where each number is multiplied by 3.
    *   Use `filter()` on your numbers array to create a new array containing only numbers greater than 10.
2.  **Object Practice:**
    *   Create an object representing a movie with properties like `title`, `director`, `releaseYear`, and `genres` (an array of strings).
    *   Access and print the `title` and `releaseYear`.
    *   Add a new property `rating` (e.g., 8.5).
    *   Write a method for the movie object called `getMovieInfo` that returns a string like "Title (Release Year) directed by Director."
    *   Use `Object.keys()` to iterate through the movie object's properties and print each key-value pair.
3.  **JSON Challenge:**
    *   Take the movie object you created and convert it into a JSON string using `JSON.stringify()`. Print the string.
    *   Take the JSON string, and parse it back into a JavaScript object using `JSON.parse()`. Verify that you can access its properties.
4.  **Type Conversion Exploration:**
    *   What is the result of `console.log("5" - true);`? Why? (Hint: `true` converts to a number).
    *   Explicitly convert the string `"false"` to a boolean. What is the result?
    *   Use `parseInt()` to get the number from the string `"Product ID: 007"`.

Arrays and Objects are fundamental for structuring data in JavaScript. Mastering their manipulation is key to building any non-trivial application. Next, we'll focus on how JavaScript interacts with the web browser itself through the Document Object Model (DOM).
