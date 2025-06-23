# Part 3: Building Your First Web Apps

## Chapter 7: Asynchronous JavaScript: Callbacks, Promises, and Async/Await

So far, most of the JavaScript code we've written has been **synchronous**. This means that code executes line by line, one statement at a time. If one statement takes a long time to complete (like fetching data from a remote server), it blocks the execution of subsequent code, potentially freezing the browser and creating a poor user experience.

**Asynchronous JavaScript** allows your program to perform long-running tasks (like network requests, timers, or file operations in Node.js) without blocking the main thread. The task is initiated, and the rest of your code continues to run. When the task eventually completes, a callback function is executed, or a Promise is resolved, to handle the result or error.

### 7.1 Understanding Synchronous vs. Asynchronous Code

**Synchronous Code (Blocking):**

```javascript
console.log("First task: Start");

// Simulate a long-running synchronous task
function synchronousTask() {
    let sum = 0;
    for (let i = 0; i < 1000000000; i++) { // This loop takes time
        sum += i;
    }
    console.log("Synchronous task complete. Sum:", sum);
    return sum;
}
synchronousTask(); // The browser will likely freeze here until this is done

console.log("Third task: End");
// "Third task: End" only prints after synchronousTask is fully complete.
```
In this example, `synchronousTask()` blocks everything. If this were happening in a browser in response to a button click, the UI would become unresponsive.

**Asynchronous Code (Non-Blocking):**

A common example of an asynchronous operation is `setTimeout`.

```javascript
console.log("First task: Start");

setTimeout(function() {
    // This function is a callback. It runs LATER.
    console.log("Second task: Asynchronous operation complete (after 2 seconds)");
}, 2000); // Wait for 2 seconds

console.log("Third task: End (this runs BEFORE the timeout completes)");

/*
Output will be:
First task: Start
Third task: End (this runs BEFORE the timeout completes)
(after 2 seconds)
Second task: Asynchronous operation complete (after 2 seconds)
*/
```
Here, `setTimeout` schedules the callback function to run after 2 seconds but doesn't block the main thread. "Third task: End" is logged almost immediately. The JavaScript engine, with the help of the browser's event loop, handles executing the callback when its time comes.

**Why Asynchronous?**
*   **Responsiveness:** Prevents the browser UI from freezing during long operations like fetching data, handling user input smoothly.
*   **Efficiency:** Allows the browser to do other things (like rendering updates, responding to other events) while waiting for an asynchronous task to finish.

JavaScript achieves asynchronicity through mechanisms like:
1.  **Callbacks** (the traditional way)
2.  **Promises** (introduced in ES6, a more robust way)
3.  **Async/Await** (ES2017, syntactic sugar over Promises, making async code look more synchronous)

### 7.2 Callbacks and Callback Hell

A **callback function** is a function that is passed as an argument to another function and is executed after some operation has been completed. We've already seen callbacks with `setTimeout` and event listeners.

```javascript
function fetchDataFromServer(url, callback) {
    console.log(`Fetching data from ${url}...`);
    // Simulate a network request (takes time)
    setTimeout(function() {
        const mockData = { message: "Data received successfully!", source: url };
        const error = null; // Simulate no error for now

        // When the "network request" is "done", call the callback
        if (error) {
            callback(error, null); // Pass error first by convention
        } else {
            callback(null, mockData); // Pass data as the second argument
        }
    }, 1500);
}

// Define the callback function to handle the result
function handleData(error, data) {
    if (error) {
        console.error("Error fetching data:", error);
        return;
    }
    console.log("Callback executed!");
    console.log("Received data:", data);
}

// Call fetchDataFromServer and pass handleData as the callback
fetchDataFromServer("https://api.example.com/data", handleData);
console.log("Request initiated. Waiting for data...");
```

**Callback Hell (Pyramid of Doom):**
When you have multiple dependent asynchronous operations, you might end up nesting callbacks within callbacks. This can lead to deeply indented, hard-to-read, and hard-to-maintain code, often referred to as "Callback Hell" or the "Pyramid of Doom."

```javascript
// Imaginary functions that take callbacks
function step1(value, callback) {
    setTimeout(() => {
        console.log("Step 1 completed with:", value);
        callback(null, value + 1);
    }, 500);
}

function step2(value, callback) {
    setTimeout(() => {
        console.log("Step 2 completed with:", value);
        callback(null, value * 2);
    }, 500);
}

function step3(value, callback) {
    setTimeout(() => {
        console.log("Step 3 completed with:", value);
        callback(null, `Final result: ${value}`);
    }, 500);
}

// Callback Hell example
step1(10, function(err1, result1) {
    if (err1) {
        console.error("Error in step 1:", err1);
    } else {
        step2(result1, function(err2, result2) {
            if (err2) {
                console.error("Error in step 2:", err2);
            } else {
                step3(result2, function(err3, finalResult) {
                    if (err3) {
                        console.error("Error in step 3:", err3);
                    } else {
                        console.log(finalResult);
                    }
                });
            }
        });
    }
});
```
The nested structure and repeated error handling make this code difficult to follow. Promises were introduced to solve this problem.

### 7.3 Promises: Introduction, Chaining

A **Promise** is an object representing the eventual completion (or failure) of an asynchronous operation and its resulting value. A Promise can be in one of three states:

1.  **Pending:** The initial state; the operation has not completed yet.
2.  **Fulfilled (Resolved):** The operation completed successfully, and the Promise has a resulting value.
3.  **Rejected:** The operation failed, and the Promise has a reason for the failure (an error).

**Creating a Promise:**
You typically create a Promise using the `new Promise()` constructor, which takes a function (called the "executor") as an argument. The executor function itself takes two arguments: `resolve` and `reject`, which are functions.

*   Call `resolve(value)` when the asynchronous operation is successful.
*   Call `reject(error)` when it fails.

```javascript
function fetchDataWithPromise(url) {
    console.log(`Fetching data from ${url} (Promise)...`);
    return new Promise(function(resolve, reject) {
        // Simulate a network request
        setTimeout(function() {
            const success = Math.random() > 0.3; // Simulate occasional failure
            if (success) {
                const mockData = { message: "Data received successfully (Promise)!", source: url };
                resolve(mockData); // Fulfill the promise with data
            } else {
                const error = new Error(`Failed to fetch from ${url}`);
                reject(error);     // Reject the promise with an error
            }
        }, 1500);
    });
}
```

**Consuming a Promise: `.then()`, `.catch()`, `.finally()`**

Once you have a Promise, you can use its methods to handle its outcome:

*   **`.then(onFulfilled, onRejected)`:**
    *   `onFulfilled`: A function called if the Promise is fulfilled. It receives the resolved value as an argument.
    *   `onRejected`: (Optional) A function called if the Promise is rejected. It receives the rejection reason (error) as an argument.
    The `.then()` method itself returns a *new* Promise, which allows for chaining.

*   **`.catch(onRejected)`:**
    A shorthand for `.then(null, onRejected)`. It's used specifically to handle errors (rejections) from the Promise or any preceding `.then()` in the chain.

*   **`.finally(onFinally)`:** (ES2018)
    Registers a function to be called when the Promise is settled (either fulfilled or rejected). It's useful for cleanup operations, regardless of the outcome.

```javascript
const dataPromise = fetchDataWithPromise("https://api.example.com/promise-data");

dataPromise
    .then(function(data) { // Handle fulfillment
        console.log("Promise fulfilled!");
        console.log("Data:", data);
        // You can return a value here, which will be the resolved value of the next .then()
        return data.message.toUpperCase();
    })
    .then(function(uppercasedMessage) { // Chained .then()
        console.log("Uppercased message:", uppercasedMessage);
    })
    .catch(function(error) { // Handle rejection
        console.error("Promise rejected!");
        console.error("Error:", error.message);
    })
    .finally(function() {
        console.log("Promise settled (fulfilled or rejected). Cleanup can happen here.");
    });

console.log("Promise request initiated. Waiting for resolution...");
```

**Promise Chaining:**
Because `.then()` and `.catch()` return new Promises, you can chain them to handle sequences of asynchronous operations in a much cleaner way than callback hell.

If a `.then()` callback returns a value, the next `.then()` in the chain receives that value. If it returns another Promise, the next `.then()` waits for that new Promise to settle.

```javascript
function step1Promise(value) {
    return new Promise((resolve) => {
        setTimeout(() => {
            console.log("Step 1 (Promise) completed with:", value);
            resolve(value + 1);
        }, 500);
    });
}

function step2Promise(value) {
    return new Promise((resolve) => {
        setTimeout(() => {
            console.log("Step 2 (Promise) completed with:", value);
            resolve(value * 2);
        }, 500);
    });
}

function step3Promise(value) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            if (value < 50) { // Simulate a potential error
                console.log("Step 3 (Promise) completed with:", value);
                resolve(`Final result (Promise): ${value}`);
            } else {
                reject(new Error("Value too high in step 3!"));
            }
        }, 500);
    });
}

// Chaining promises
step1Promise(10)
    .then(result1 => { // result1 is the resolved value from step1Promise
        return step2Promise(result1); // Return the next promise
    })
    .then(result2 => { // result2 is the resolved value from step2Promise
        return step3Promise(result2); // Return the next promise
    })
    .then(finalResult => { // finalResult is the resolved value from step3Promise
        console.log(finalResult);
    })
    .catch(error => { // Single .catch() for errors in any preceding step
        console.error("An error occurred in the promise chain:", error.message);
    })
    .finally(() => {
        console.log("Promise chain finished.");
    });
```
This is much more readable and manageable than the nested callback example.

**`Promise.all()` and `Promise.race()`**

*   **`Promise.all(iterableOfPromises)`:**
    Takes an iterable (e.g., an array) of Promises and returns a single new Promise. This new Promise fulfills when *all* input Promises have fulfilled, and it resolves with an array of their fulfillment values (in the same order as the input Promises). If *any* of the input Promises reject, `Promise.all()` immediately rejects with the reason of the first rejected Promise.

    ```javascript
    const p1 = Promise.resolve(10); // A promise that resolves to 10
    const p2 = new Promise(resolve => setTimeout(() => resolve(20), 100));
    const p3 = fetchDataWithPromise("https://api.example.com/data1"); // Our function from before

    Promise.all([p1, p2, p3])
        .then(results => {
            console.log("Promise.all results:", results); // [10, 20, {message: ..., source: ...}]
            const sum = results[0] + results[1];
            console.log("Sum of p1 and p2:", sum);
        })
        .catch(error => {
            console.error("Promise.all failed:", error);
        });
    ```

*   **`Promise.race(iterableOfPromises)`:**
    Takes an iterable of Promises and returns a new Promise. This new Promise settles (fulfills or rejects) as soon as *one* of the input Promises settles, with the value or reason from that first-settled Promise.

    ```javascript
    const promiseFast = new Promise(resolve => setTimeout(() => resolve("Fast one wins!"), 50));
    const promiseSlow = new Promise(resolve => setTimeout(() => resolve("Slow one :("), 200));

    Promise.race([promiseFast, promiseSlow])
        .then(winner => {
            console.log("Promise.race winner:", winner); // Output: Fast one wins!
        })
        .catch(error => {
            console.error("Promise.race error:", error);
        });
    ```

### 7.4 Async/Await: Simplifying Asynchronous Code

ES2017 introduced `async` and `await` keywords, which provide syntactic sugar on top of Promises, making asynchronous code look and behave a bit more like synchronous code. This can greatly improve readability.

*   **`async` function:**
    You declare an asynchronous function using the `async` keyword before `function` or before the parameters of an arrow function.
    An `async` function *always* implicitly returns a Promise.
    If the `async` function explicitly returns a value, the Promise it returns will fulfill with that value.
    If the `async` function throws an error, the Promise it returns will reject with that error.

*   **`await` operator:**
    The `await` operator can only be used *inside* an `async` function.
    It pauses the execution of the `async` function until the Promise it's waiting for settles (fulfills or rejects).
    If the Promise fulfills, `await` returns the fulfilled value.
    If the Promise rejects, `await` throws the rejection reason (error), which can be caught using `try...catch`.

```javascript
// Using our Promise-based functions from before:
// step1Promise, step2Promise, step3Promise

async function processStepsAsync(initialValue) {
    console.log("Async function started with:", initialValue);
    try {
        const result1 = await step1Promise(initialValue);
        // Code here pauses until step1Promise resolves
        console.log("After await step1:", result1);

        const result2 = await step2Promise(result1);
        // Pauses until step2Promise resolves
        console.log("After await step2:", result2);

        const finalResult = await step3Promise(result2);
        // Pauses until step3Promise resolves
        console.log("After await step3:", finalResult);

        return finalResult; // This will be the resolved value of the promise returned by processStepsAsync
    } catch (error) {
        console.error("Error in async function:", error.message);
        // If an error is thrown by an awaited promise, it's caught here.
        // You can re-throw or return a specific error value if needed.
        throw error; // Re-throwing will cause the promise returned by processStepsAsync to reject
    } finally {
        console.log("Async function processing finished (finally block).");
    }
}

// Calling the async function and handling its returned Promise
processStepsAsync(5)
    .then(result => {
        console.log("Async function success:", result);
    })
    .catch(error => {
        console.error("Async function failed:", error.message);
    });

// Example with an API call
async function fetchAndDisplayUserData(userId) {
    try {
        // Assume fetchUser is an async function or returns a Promise
        // const response = await fetch(`https://api.example.com/users/${userId}`);
        // if (!response.ok) {
        //     throw new Error(`HTTP error! status: ${response.status}`);
        // }
        // const userData = await response.json(); // .json() also returns a Promise

        // Using our fetchDataWithPromise for simulation:
        const userData = await fetchDataWithPromise(`https://api.example.com/users/${userId}`);

        console.log("User Data:", userData);
        // Update UI with userData here
        const userInfoDiv = document.getElementById("userInfo");
        if(userInfoDiv) userInfoDiv.textContent = `User: ${userData.message}`;

    } catch (error) {
        console.error("Could not fetch user data:", error);
        const userInfoDiv = document.getElementById("userInfo");
        if(userInfoDiv) userInfoDiv.textContent = `Error: ${error.message}`;
    }
}

fetchAndDisplayUserData(123);
```
```html
<!-- Add to HTML for the fetchAndDisplayUserData example -->
<div id="userInfo">Loading user data...</div>
```

**Error Handling with `try...catch`:**
In `async/await` code, you use standard `try...catch` blocks to handle errors from awaited Promises. If an awaited Promise rejects, it throws an error that the `catch` block can capture.

Async/await significantly simplifies writing and reading sequential asynchronous operations, making them appear more linear while still being non-blocking under the hood.

### 7.5 Fetch API: Making HTTP Requests

The **Fetch API** is a modern browser interface for making network requests (e.g., to get data from a server API). It's Promise-based, making it integrate well with `async/await`.

**Basic `fetch()` usage:**
`fetch(url, options)`
*   `url`: The URL to request.
*   `options`: (Optional) An object to configure the request (e.g., method, headers, body).

`fetch()` returns a Promise that resolves to a `Response` object. This `Response` object doesn't directly contain the data in the format you want (like JSON); it's a representation of the entire HTTP response.

To get the actual data, you need to use one of the `Response` object's methods, which also return Promises:
*   `response.json()`: Parses the response body as JSON.
*   `response.text()`: Returns the response body as plain text.
*   `response.blob()`: Returns the response body as a `Blob` (binary data).
*   `response.formData()`: Returns the response body as `FormData`.

```javascript
const apiUrl = "https://jsonplaceholder.typicode.com/todos/1"; // A public test API

// Using .then() chaining
fetch(apiUrl)
    .then(response => {
        console.log("Fetch Response object:", response);
        if (!response.ok) { // Check if HTTP status code is 200-299
            // If not ok (e.g., 404, 500), throw an error to be caught by .catch()
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json(); // This returns another Promise
    })
    .then(data => { // This .then() handles the Promise from response.json()
        console.log("Fetched data (Todo item 1):", data);
        // Example: Display the title
        const todoTitleDiv = document.getElementById("todoTitle");
        if (todoTitleDiv) todoTitleDiv.textContent = `Todo Title: ${data.title}`;
    })
    .catch(error => {
        console.error("Fetch error:", error);
        const todoTitleDiv = document.getElementById("todoTitle");
        if (todoTitleDiv) todoTitleDiv.textContent = `Error fetching todo: ${error.message}`;
    });

// Using async/await (often cleaner)
async function getTodoById(id) {
    const todoApiUrl = `https://jsonplaceholder.typicode.com/todos/${id}`;
    const todoDetailsDiv = document.getElementById("todoDetails");

    try {
        const response = await fetch(todoApiUrl);
        console.log("Async Fetch Response object:", response);

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status} for ID ${id}`);
        }

        const todoData = await response.json(); // Parse JSON data
        console.log(`Fetched Todo ${id}:`, todoData);

        if (todoDetailsDiv) {
            todoDetailsDiv.innerHTML = `
                <h3>Todo ID: ${todoData.id}</h3>
                <p>Title: ${todoData.title}</p>
                <p>Completed: ${todoData.completed}</p>
            `;
        }
    } catch (error) {
        console.error(`Error fetching todo ${id}:`, error);
        if (todoDetailsDiv) {
            todoDetailsDiv.textContent = `Error: ${error.message}`;
        }
    }
}

getTodoById(5); // Fetch and display details for todo item 5
```
```html
<!-- Add to HTML for Fetch API examples -->
<div id="todoTitle">Loading todo title...</div>
<hr>
<div id="todoDetails">Loading todo details...</div>
```

**Making POST Requests (and other methods):**
You can specify the HTTP method, headers, and body in the `options` object.

```javascript
async function createPost(postData) {
    const postsApiUrl = "https://jsonplaceholder.typicode.com/posts";
    const postResultDiv = document.getElementById("postResult");

    try {
        const response = await fetch(postsApiUrl, {
            method: "POST", // Specify the HTTP method
            headers: {
                "Content-Type": "application/json; charset=UTF-8" // Tell the server we're sending JSON
            },
            body: JSON.stringify(postData) // Convert JavaScript object to JSON string for the body
        });

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const createdPost = await response.json(); // Get the server's response (often the created object with an ID)
        console.log("Post created successfully:", createdPost);
        if (postResultDiv) {
            postResultDiv.textContent = `Post created! ID: ${createdPost.id}, Title: ${createdPost.title}`;
        }
        return createdPost;

    } catch (error) {
        console.error("Error creating post:", error);
        if (postResultDiv) postResultDiv.textContent = `Error: ${error.message}`;
    }
}

const newBlogPost = {
    title: "My Awesome New Post",
    body: "This is the content of my new post.",
    userId: 1
};
createPost(newBlogPost);
```
```html
<!-- Add to HTML for POST example -->
<div id="postResult">Submitting post...</div>
```
The Fetch API is a powerful tool for modern web development, enabling communication with backend services and APIs.

### 7.6 Chapter Summary & Action Steps

**Summary:**

*   **Synchronous** code blocks, **asynchronous** code doesn't. Asynchronicity is key for responsive UIs.
*   **Callbacks** are functions passed to be executed later. Nested callbacks can lead to "Callback Hell."
*   **Promises** represent the eventual outcome of an async operation (pending, fulfilled, or rejected). Consumed with `.then()`, `.catch()`, `.finally()`. Chaining Promises improves readability.
*   **`async/await`** provides syntactic sugar over Promises, allowing async code to be written in a more synchronous-looking style using `async` functions and the `await` operator. Error handling is done with `try...catch`.
*   The **Fetch API** (`fetch()`) is a modern, Promise-based way to make HTTP requests (GET, POST, etc.) to servers.

**Action Steps:**

1.  **Callback to Promise Conversion:**
    *   Take the `fetchDataFromServer` function (the one using a callback) from section 7.2.
    *   Rewrite it to return a Promise instead of taking a callback. Call this new function `fetchDataAsPromise`.
    *   Consume `fetchDataAsPromise` using `.then()` and `.catch()`.
2.  **Chained Async Operations:**
    *   Create three simple functions, each returning a Promise that resolves after a short delay (e.g., `getUserData(userId)`, `getUserPosts(userId)`, `getPostComments(postId)`).
    *   Chain these operations: Get user data, then use the user ID to get their posts, then pick a post ID to get its comments.
    *   Implement this chain using both `.then()` chaining and `async/await`.
3.  **Fetch Multiple Todos:**
    *   Use the `https://jsonplaceholder.typicode.com/todos/` API.
    *   Write an `async` function that fetches details for 3 different todo items simultaneously using `Promise.all()` and `fetch()`.
    *   Log the results of all three todos once they are all fetched. Handle potential errors.
4.  **Simple "Delayed Quote" Display:**
    *   Find a simple quotes API (e.g., `https://api.quotable.io/random` or `https://dummyjson.com/quotes/random`).
    *   Write an `async` function that:
        *   Fetches a random quote using `fetch()`.
        *   After successfully fetching, waits an additional 2 seconds using `await new Promise(resolve => setTimeout(resolve, 2000));`.
        *   Then, displays the quote and its author on the webpage in designated `div` elements.
        *   Include error handling.
5.  **POST to JSONPlaceholder:**
    *   Using the `https://jsonplaceholder.typicode.com/posts` endpoint and the `fetch()` API:
    *   Write an `async` function that sends a POST request to create a new post. The post object should have `title`, `body`, and `userId` properties.
    *   Log the response from the server (which usually includes the newly created post with an `id`).
    *   Display a success or error message on the page.

Understanding and effectively using asynchronous JavaScript is crucial for building modern web applications that interact with servers, handle user input efficiently, and provide a smooth user experience. Promises and async/await are your primary tools for managing this complexity. The projects in the following chapters will heavily rely on these concepts.
