## Chapter 6: Introduction to Browser APIs

Beyond the core JavaScript language and DOM manipulation, web browsers provide a rich set of **Application Programming Interfaces (APIs)** that your JavaScript code can use. These APIs expose browser functionalities and data, allowing you to create more powerful and integrated web applications.

In this chapter, we'll explore a few common and useful Browser APIs: Timers for scheduling code execution, Web Storage for saving data in the user's browser, a brief look at the Geolocation API for finding the user's location, and more about the Console API for debugging.

### 6.1 Timers: `setTimeout`, `setInterval`, `clearTimeout`, `clearInterval`

Timer functions allow you to schedule the execution of a function at some point in the future or repeatedly at a specific interval.

**1. `setTimeout(function, delayInMilliseconds, param1, param2, ...)`:**
Executes a function *once* after a specified delay (in milliseconds).

*   `function`: The function to execute.
*   `delayInMilliseconds`: The time, in milliseconds (1000 ms = 1 second), to wait before executing the function.
*   `param1, param2, ...`: (Optional) Additional arguments to pass to the `function` when it's executed.

`setTimeout` returns a **timer ID** (a number), which can be used to cancel the timeout before it executes using `clearTimeout()`.

```javascript
function sayHello(name) {
    console.log(`Hello, ${name}! This message was delayed.`);
}

// Execute sayHello after 2 seconds (2000 milliseconds)
const timeoutId1 = setTimeout(sayHello, 2000, "Alice"); // Pass "Alice" as an argument to sayHello
console.log("Timeout 1 scheduled with ID:", timeoutId1);

const timeoutId2 = setTimeout(function() { // Using an anonymous function
    const messageDiv = document.getElementById("delayedMessage");
    if (messageDiv) {
        messageDiv.textContent = "This message appeared after 3 seconds!";
        messageDiv.style.color = "green";
    }
}, 3000);
console.log("Timeout 2 scheduled with ID:", timeoutId2);

// To cancel a timeout before it executes:
// clearTimeout(timeoutId1);
// console.log("Timeout 1 has been canceled.");
```
```html
<!-- Add to your HTML for the second example -->
<div id="delayedMessage" style="color: red;">Waiting for message...</div>
```

**2. `setInterval(function, intervalInMilliseconds, param1, param2, ...)`:**
Repeatedly executes a function at a specified interval (in milliseconds).

*   `function`: The function to execute.
*   `intervalInMilliseconds`: The time interval, in milliseconds, between executions.
*   `param1, param2, ...`: (Optional) Additional arguments to pass to the `function`.

`setInterval` also returns a **timer ID**, which can be used with `clearInterval()` to stop the repeated executions.

```javascript
let count = 0;
const maxCount = 5;

function updateCounter() {
    count++;
    console.log(`Counter: ${count}`);
    const counterDiv = document.getElementById("intervalCounter");
    if (counterDiv) {
        counterDiv.textContent = `Count: ${count}`;
    }

    if (count >= maxCount) {
        console.log("Reached max count. Stopping interval.");
        clearInterval(intervalId); // Stop the interval
        if (counterDiv) {
            counterDiv.textContent += " - Interval Stopped!";
            counterDiv.style.color = "orange";
        }
    }
}

// Execute updateCounter every 1 second (1000 milliseconds)
const intervalId = setInterval(updateCounter, 1000);
console.log("Interval scheduled with ID:", intervalId);

// To stop the interval externally (e.g., with a button):
// const stopButton = document.getElementById("stopIntervalBtn");
// if (stopButton) {
//     stopButton.addEventListener("click", function() {
//         clearInterval(intervalId);
//         console.log("Interval stopped by button.");
//         const counterDiv = document.getElementById("intervalCounter");
//         if (counterDiv) counterDiv.textContent += " - Manually Stopped!";
//     });
// }
```
```html
<!-- Add to your HTML for the interval example -->
<div id="intervalCounter">Count: 0</div>
<!-- <button id="stopIntervalBtn">Stop Interval</button> -->
```

**3. `clearTimeout(timerId)`:**
Cancels a timeout that was previously scheduled with `setTimeout()`.

**4. `clearInterval(timerId)`:**
Stops the repeated execution of a function that was scheduled with `setInterval()`.

**Important Notes on Timers:**

*   **Minimum Delay:** Browsers have a minimum delay for timers (often around 4ms, or higher for inactive tabs) to optimize performance and battery life. So, `setTimeout(fn, 0)` or `setTimeout(fn, 1)` doesn't mean the function will execute immediately but rather as soon as the browser can process it after the current task queue is empty.
*   **`this` Context:** When using `setTimeout` or `setInterval` with object methods, the `this` keyword inside the callback function might not refer to the object you expect (it often defaults to the `window` object in non-strict mode). Arrow functions can help preserve the lexical `this`, or you can use `function.bind(this)`.

    ```javascript
    const myApp = {
        appName: "Timer App",
        start: function() {
            console.log(`Starting ${this.appName}`); // 'this' is myApp

            // Problematic 'this' with traditional function in setTimeout
            // setTimeout(function() {
            //     console.log(`App name inside timeout: ${this.appName}`); // 'this' is likely window here
            // }, 100);

            // Solution 1: Using an arrow function
            setTimeout(() => {
                console.log(`App name (arrow fn): ${this.appName}`); // 'this' is lexically bound to myApp
            }, 500);

            // Solution 2: Using bind
            // setTimeout(function() {
            //     console.log(`App name (bind): ${this.appName}`);
            // }.bind(this), 1000);
        }
    };
    myApp.start();
    ```

### 6.2 Working with Web Storage: `localStorage` and `sessionStorage`

Web Storage provides mechanisms for web applications to store data locally within the user's browser. This data persists even after the browser window is closed (for `localStorage`) or until the session ends (for `sessionStorage`).

**Key Features:**

*   **Key-Value Pairs:** Data is stored as key-value pairs, and both keys and values *must be strings*. If you want to store objects or arrays, you need to convert them to JSON strings first (using `JSON.stringify()`) and parse them back (using `JSON.parse()`) when retrieving.
*   **Storage Limits:** Typically around 5-10 MB per domain, much larger than cookies.
*   **Same-Origin Policy:** Storage is specific to the protocol, domain, and port (the "origin"). Code from one origin cannot access storage from another.
*   **Synchronous API:** The Web Storage API is synchronous, meaning operations block other code execution until they complete. For large amounts of data, this could potentially impact performance (though usually negligible for typical use cases).

**1. `localStorage`:**
Stores data with no expiration time. The data remains available until explicitly cleared by the web application or the user (e.g., by clearing browser data). Data in `localStorage` is shared across all tabs and windows from the same origin.

*   **`localStorage.setItem(key, value)`:** Stores a key-value pair.
*   **`localStorage.getItem(key)`:** Retrieves the value for a given key. Returns `null` if the key doesn't exist.
*   **`localStorage.removeItem(key)`:** Removes a key-value pair.
*   **`localStorage.clear()`:** Removes all key-value pairs for that origin.
*   **`localStorage.length`:** Returns the number of stored items.
*   **`localStorage.key(index)`:** Returns the key at a given numerical index.

```javascript
// Storing a simple string
localStorage.setItem("username", "JohnDoe");

// Retrieving the string
let storedUsername = localStorage.getItem("username");
console.log("Stored username:", storedUsername); // Output: JohnDoe

// Storing an object (must be stringified)
const userPreferences = {
    theme: "dark",
    fontSize: 16,
    notifications: true
};
localStorage.setItem("preferences", JSON.stringify(userPreferences));

// Retrieving and parsing the object
const storedPrefsString = localStorage.getItem("preferences");
if (storedPrefsString) {
    const retrievedPrefs = JSON.parse(storedPrefsString);
    console.log("Retrieved preferences:", retrievedPrefs);
    console.log("Theme:", retrievedPrefs.theme); // Output: dark
}

// Removing an item
// localStorage.removeItem("username");
// console.log("Username after removal:", localStorage.getItem("username")); // Output: null

// Clearing all localStorage for this origin
// localStorage.clear();
// console.log("localStorage length after clear:", localStorage.length); // Output: 0
```
You can inspect `localStorage` in your browser's Developer Tools (usually under the "Application" or "Storage" tab).

**2. `sessionStorage`:**
Stores data for the duration of the page session. A page session lasts as long as the browser is open and survives page reloads and restores.
*   Opening a new tab or window creates a new session with new `sessionStorage`.
*   Closing a tab or window ends the session and clears `sessionStorage` for that session.

The API for `sessionStorage` is identical to `localStorage`:
*   `sessionStorage.setItem(key, value)`
*   `sessionStorage.getItem(key)`
*   `sessionStorage.removeItem(key)`
*   `sessionStorage.clear()`
*   `sessionStorage.length`
*   `sessionStorage.key(index)`

```javascript
// Storing data for the current session
sessionStorage.setItem("sessionInfo", "User is currently active in this tab.");
sessionStorage.setItem("tempCart", JSON.stringify(["item1", "item2"]));

console.log(sessionStorage.getItem("sessionInfo"));

// This data will be gone if you close the tab and reopen it,
// or open the same page in a new tab.
```

**Use Cases:**

*   **`localStorage`:** User preferences (theme, layout), remembering if a user has dismissed a notification, storing application state that needs to persist across sessions.
*   **`sessionStorage`:** Storing temporary data for a single session, like user input in a multi-step form before final submission, current scroll position, or data specific to a single tab's workflow.

### 6.3 Introduction to Geolocation API (Simple Example)

The Geolocation API allows web applications to retrieve the geographical position of the user's device, with their permission.

**Key Method: `navigator.geolocation.getCurrentPosition(successCallback, errorCallback, options)`**

*   `navigator.geolocation`: The entry point to the API.
*   `getCurrentPosition()`: Asynchronously attempts to get the device's current position.
    *   `successCallback`: A function that is called if the position is successfully retrieved. It receives a `GeolocationPosition` object as an argument.
    *   `errorCallback`: (Optional) A function that is called if an error occurs (e.g., user denies permission, location unavailable). It receives a `GeolocationPositionError` object.
    *   `options`: (Optional) An object with options like `enableHighAccuracy` (boolean), `timeout` (milliseconds), `maximumAge` (milliseconds).

**The `GeolocationPosition` Object (passed to success callback):**
*   `coords`: A `GeolocationCoordinates` object containing:
    *   `latitude`: Decimal degrees.
    *   `longitude`: Decimal degrees.
    *   `accuracy`: Accuracy of the position in meters.
    *   `altitude`: Height in meters above the ellipsoid (often `null`).
    *   `altitudeAccuracy`: Accuracy of altitude in meters (often `null`).
    *   `heading`: Direction of travel in degrees (often `null`).
    *   `speed`: Speed in meters per second (often `null`).
*   `timestamp`: Time when the position was retrieved.

```html
<!-- In index.html -->
<button id="getLocationBtn">Get My Location</button>
<div id="locationInfo"></div>
```

```javascript
// In script.js
const getLocationBtn = document.getElementById("getLocationBtn");
const locationInfoDiv = document.getElementById("locationInfo");

getLocationBtn.addEventListener("click", function() {
    locationInfoDiv.textContent = "Attempting to get location...";

    if (navigator.geolocation) { // Check if Geolocation is supported
        navigator.geolocation.getCurrentPosition(showPosition, showError, { enableHighAccuracy: true, timeout: 10000 });
    } else {
        locationInfoDiv.textContent = "Geolocation is not supported by this browser.";
        locationInfoDiv.style.color = "red";
    }
});

function showPosition(position) {
    const lat = position.coords.latitude;
    const lon = position.coords.longitude;
    const acc = position.coords.accuracy;

    locationInfoDiv.innerHTML = `
        Latitude: ${lat.toFixed(4)} &deg;<br>
        Longitude: ${lon.toFixed(4)} &deg;<br>
        Accuracy: ${acc.toFixed(0)} meters
    `;
    locationInfoDiv.style.color = "green";
    console.log("Position object:", position);
}

function showError(error) {
    let message = "An error occurred while trying to get location: ";
    switch(error.code) {
        case error.PERMISSION_DENIED:
            message += "User denied the request for Geolocation.";
            break;
        case error.POSITION_UNAVAILABLE:
            message += "Location information is unavailable.";
            break;
        case error.TIMEOUT:
            message += "The request to get user location timed out.";
            break;
        case error.UNKNOWN_ERROR:
            message += "An unknown error occurred.";
            break;
    }
    locationInfoDiv.textContent = message;
    locationInfoDiv.style.color = "red";
    console.error("Geolocation Error:", error);
}
```
**Important Considerations for Geolocation:**
*   **User Permission:** Browsers will always ask the user for permission before sharing their location.
*   **HTTPS Required:** Most modern browsers require pages to be served over HTTPS to use the Geolocation API for security and privacy reasons.
*   **Accuracy Varies:** The accuracy depends on the device and available location sources (GPS, Wi-Fi, cellular networks).

Other Geolocation API methods include `watchPosition()` (to get updated positions as the user moves) and `clearWatch()`.

### 6.4 Console Methods for Debugging

We've been using `console.log()` extensively. The `console` object provides several other useful methods for debugging and logging information.

*   **`console.log(...data)`:** General output of logging information.
*   **`console.info(...data)`:** Informational messages. Often styled similarly to `log` but can be filtered separately in some browser consoles.
*   **`console.warn(...data)`:** Outputs a warning message. Typically displayed with a yellow warning icon.
*   **`console.error(...data)`:** Outputs an error message. Typically displayed with a red error icon and may include a stack trace.
*   **`console.table(data, [columns])`:** Displays tabular data as a table. Very useful for arrays of objects or objects.
    ```javascript
    const users = [
        { id: 1, name: "Alice", role: "Admin" },
        { id: 2, name: "Bob", role: "Editor" },
        { id: 3, name: "Charlie", role: "Viewer" }
    ];
    console.table(users);
    console.table(users, ["name", "role"]); // Display only specific columns
    ```
*   **`console.group(label)` and `console.groupEnd()`:** Groups related log messages together, creating a collapsible section in the console.
    ```javascript
    console.group("User Processing");
    console.log("Fetching user data...");
    console.warn("User has limited permissions.");
    console.group("Address Info"); // Nested group
    console.log("Street: 123 Main St");
    console.log("City: Anytown");
    console.groupEnd(); // End "Address Info" group
    console.log("Processing complete.");
    console.groupEnd(); // End "User Processing" group
    ```
*   **`console.groupCollapsed(label)`:** Same as `console.group()` but starts collapsed.
*   **`console.time(label)` and `console.timeEnd(label)`:** Starts a timer with a given label. When `console.timeEnd(label)` is called with the same label, it logs the elapsed time in milliseconds.
    ```javascript
    console.time("myLoopTimer");
    for (let i = 0; i < 1000000; i++) {
        // Some operation
    }
    console.timeEnd("myLoopTimer"); // Output: myLoopTimer: XXX.XXXms
    ```
*   **`console.assert(assertion, ...data)`:** Logs a message and stack trace if the first argument (`assertion`) is `false`. If `true`, nothing happens.
    ```javascript
    let x = 5;
    let y = 10;
    console.assert(x > y, "Assertion failed: x is not greater than y", {x, y});
    // Output: Assertion failed: x is not greater than y {x: 5, y: 10}
    ```
*   **`console.clear()`:** Clears the console.
*   **`console.count(label)`:** Logs the number of times this line has been called with that label.
*   **`console.trace()`:** Outputs a stack trace.

Using these console methods effectively can significantly improve your debugging workflow.

### 6.5 Chapter Summary & Action Steps

**Summary:**

*   **Timers:**
    *   `setTimeout(fn, delay)`: Executes `fn` once after `delay`. Returns ID to cancel with `clearTimeout()`.
    *   `setInterval(fn, interval)`: Executes `fn` repeatedly every `interval`. Returns ID to cancel with `clearInterval()`.
*   **Web Storage:**
    *   `localStorage`: Persistent key-value string storage (5-10MB per origin). API: `setItem()`, `getItem()`, `removeItem()`, `clear()`.
    *   `sessionStorage`: Session-only key-value string storage. Same API as `localStorage`.
    *   Store objects/arrays by converting to/from JSON strings (`JSON.stringify()`, `JSON.parse()`).
*   **Geolocation API:**
    *   `navigator.geolocation.getCurrentPosition(successFn, errorFn, options)` to get user's location (requires permission and HTTPS).
*   **Console API:**
    *   Beyond `console.log()`, use `info()`, `warn()`, `error()`, `table()`, `group()`, `time()`, `assert()` for enhanced debugging.

**Action Steps:**

1.  **Delayed Greeting:**
    *   Write a script that asks the user for their name using `prompt()`.
    *   Then, use `setTimeout` to display a greeting message (e.g., "Hello, [Name]! Welcome!") in a `<div>` on the page after 3 seconds.
2.  **Simple Slideshow with `setInterval`:**
    *   Create an array of image URLs (use placeholders like `https://via.placeholder.com/300/FF0000?Text=Img1`).
    *   Display an `<img>` element on the page.
    *   Use `setInterval` to change the `src` of the `<img>` element every 2 seconds to cycle through the images in your array.
    *   Add a "Stop Slideshow" button that uses `clearInterval` to stop it.
3.  **Theme Preference with `localStorage`:**
    *   Create two buttons: "Light Theme" and "Dark Theme".
    *   When "Light Theme" is clicked, set `document.body.style.backgroundColor = "white"` and `document.body.style.color = "black"`. Store `"light"` in `localStorage` under a key like `"theme"`.
    *   When "Dark Theme" is clicked, set `document.body.style.backgroundColor = "black"` and `document.body.style.color = "white"`. Store `"dark"` in `localStorage`.
    *   When the page loads, check `localStorage` for the saved theme. If a theme is found, apply it immediately.
4.  **Geolocation Display:**
    *   Implement the Geolocation example from the chapter.
    *   Try to display not just latitude and longitude, but also the accuracy.
    *   Handle the cases where the user denies permission or location is unavailable by showing appropriate messages.
5.  **Console Debugging Practice:**
    *   Create an array of simple objects (e.g., a list of products with `name` and `price`). Use `console.table()` to display it.
    *   Write a function that performs a potentially slow calculation (e.g., a loop). Use `console.time()` and `console.timeEnd()` to measure its execution time.
    *   Use `console.group()` to group related log messages while simulating a multi-step process.

These Browser APIs greatly expand what your JavaScript can do, moving beyond simple DOM manipulation to interact with browser features, store data, and improve your development process. The next part of our journey will focus on a core concept for handling asynchronous operations: Promises and Async/Await.
