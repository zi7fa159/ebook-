## Chapter 5: Handling Events

Web pages become truly interactive when they can respond to user actions like mouse clicks, key presses, page loading, and more. JavaScript achieves this through **event handling**. An **event** is an action or occurrence that happens in the browser, such as a user clicking a button or the page finishing loading. An **event handler** (or **event listener**) is a piece of JavaScript code (usually a function) that runs in response to a specific event.

### 5.1 Introduction to Events

There are many types of events that can occur in a browser. Some common ones include:

**Mouse Events:**
*   `click`: The user clicks an element.
*   `dblclick`: The user double-clicks an element.
*   `mousedown`: A mouse button is pressed down on an element.
*   `mouseup`: A mouse button is released over an element.
*   `mousemove`: The mouse pointer moves over an element.
*   `mouseover` / `mouseenter`: The mouse pointer moves onto an element.
*   `mouseout` / `mouseleave`: The mouse pointer moves off an element.

**Keyboard Events:**
*   `keydown`: A key is pressed down.
*   `keyup`: A key is released.
*   `keypress`: A key that produces a character is pressed (deprecated in favor of `keydown` for most uses).

**Form Events:**
*   `submit`: A form is submitted.
*   `change`: The value of an input element, select box, or textarea changes (and loses focus).
*   `input`: The value of an `<input>` or `<textarea>` element changes (fires immediately).
*   `focus`: An element gains focus.
*   `blur`: An element loses focus.

**Document/Window Events:**
*   `load`: The entire page (including all resources like images and scripts) has finished loading. Usually used on the `window` object.
*   `DOMContentLoaded`: The HTML document has been completely loaded and parsed, without waiting for stylesheets, images, and subframes to finish loading. This fires earlier than `load`.
*   `unload`: The page is being unloaded (e.g., user navigates away).
*   `resize`: The browser window is resized.
*   `scroll`: The document view or an element has been scrolled.

### 5.2 Event Listeners: Adding and Removing

To make an element respond to an event, you need to attach an event listener to it. There are a few ways to do this:

**1. Inline Event Handlers (Generally Discouraged):**
You can add event handlers directly within HTML attributes. This mixes HTML and JavaScript and is considered bad practice for maintainability and separation of concerns.

```html
<!-- In index.html - Avoid this method for larger applications -->
<button onclick="alert('Button was clicked inline!'); console.log('Inline click');">Click Me Inline</button>

<script>
    function showAlert() {
        alert('Button clicked via function call from inline handler!');
    }
</script>
<button onclick="showAlert()">Click Me (Calls Function)</button>
```
While simple for very small things, it becomes hard to manage for more complex interactions.

**2. DOM Element Properties (Slightly Better, but Still Limited):**
You can assign a function to an element's `on<eventtype>` property (e.g., `onclick`, `onmouseover`).

```html
<!-- In index.html -->
<button id="propButton">Click Me (Property)</button>
```

```javascript
// In script.js
const propBtn = document.getElementById("propButton");

propBtn.onclick = function() { // Assign a function to the onclick property
    console.log("Button clicked via property assignment!");
    alert("Property handler fired!");
};

// You can only have ONE handler per event type this way.
// This would overwrite the previous onclick handler:
// propBtn.onclick = function() { console.log("New handler!"); };
```
This is better than inline handlers as it keeps JavaScript in your `.js` file. However, you can only assign one function per event type to an element using this method. If you assign another function to `element.onclick`, it will overwrite the previous one.

**3. `addEventListener()` (Recommended and Most Flexible):**
This is the modern and preferred way to handle events. It allows you to:
*   Attach multiple listener functions to the same element for the same event type.
*   Have more control over the event handling process (e.g., event capturing vs. bubbling, which we'll touch on).
*   Remove event listeners specifically.

Syntax: `element.addEventListener(eventType, listenerFunction, useCapture);`
*   `eventType`: A string representing the event type (e.g., `"click"`, `"mouseover"`, `"keydown"` - without the "on" prefix).
*   `listenerFunction`: The function to be called when the event occurs. This function automatically receives an **Event object** as its first argument.
*   `useCapture`: (Optional) A boolean value indicating whether to use event capturing (true) or event bubbling (false, default). We'll discuss this later.

```html
<!-- In index.html -->
<button id="listenerButton">Click Me (Listener)</button>
<div id="hoverDiv" style="width:100px; height:50px; background-color:lightgray; margin-top:10px;">Hover over me</div>
```

```javascript
// In script.js
const listenerBtn = document.getElementById("listenerButton");
const hoverDiv = document.getElementById("hoverDiv");

function handleClick() {
    console.log("addEventListener: Button clicked!");
    alert("Listener fired!");
}

function anotherClickHandler() {
    console.log("addEventListener: Second click handler on the same button!");
}

// Add the first click listener
listenerBtn.addEventListener("click", handleClick);

// Add a second click listener to the same button
listenerBtn.addEventListener("click", anotherClickHandler);

// Mouseover event for the div
hoverDiv.addEventListener("mouseover", function() { // Using an anonymous function
    this.style.backgroundColor = "lightblue"; // 'this' refers to hoverDiv here
    console.log("Mouse is over the div!");
});

// Mouseout event for the div
hoverDiv.addEventListener("mouseout", function() {
    this.style.backgroundColor = "lightgray";
    console.log("Mouse left the div!");
});
```

**Removing Event Listeners with `removeEventListener()`:**
To remove an event listener, you *must* pass the exact same function reference that was originally added. You cannot remove listeners added as anonymous functions directly unless you store a reference to that anonymous function.

Syntax: `element.removeEventListener(eventType, listenerFunction, useCapture);`

```javascript
// To remove the 'handleClick' listener from listenerBtn:
// listenerBtn.removeEventListener("click", handleClick);
// console.log("handleClick listener removed.");

// You cannot easily remove an anonymous function listener like this:
// hoverDiv.removeEventListener("mouseover", function() { ... }); // This WON'T work
// because the function() { ... } here is a *new* function object.

// To remove an anonymous function, you need to use a named function or store its reference:
const handleDivMouseOver = function() {
    this.style.backgroundColor = "lightcoral";
    console.log("Named function: Mouse is over the div!");
};
hoverDiv.addEventListener("mouseover", handleDivMouseOver);

// Sometime later, to remove it:
// hoverDiv.removeEventListener("mouseover", handleDivMouseOver);
// console.log("handleDivMouseOver listener removed.");
```

### 5.3 The Event Object

When an event occurs and an event listener function is called, the browser automatically passes an **Event object** as the first argument to that function. This object contains useful information and methods related to the event.

```javascript
const myButton = document.getElementById("listenerButton"); // Assuming this button exists

myButton.addEventListener("click", function(event) { // 'event' is the Event object
    console.log("Event type:", event.type);         // Output: click
    console.log("Target element:", event.target);   // Output: <button id="listenerButton">...</button>
    console.log("Current target:", event.currentTarget); // Usually same as target, see bubbling section
    console.log("Timestamp:", event.timeStamp);   // Time the event occurred

    // For mouse events:
    if (event.type === "click" || event.type.startsWith("mouse")) {
        console.log("Mouse X (relative to window):", event.clientX);
        console.log("Mouse Y (relative to window):", event.clientY);
        console.log("Ctrl key pressed?", event.ctrlKey); // true or false
        console.log("Shift key pressed?", event.shiftKey); // true or false
    }

    // For keyboard events:
    if (event.type.startsWith("key")) {
        console.log("Key pressed:", event.key);       // e.g., "a", "Enter", "Shift"
        console.log("Key code:", event.code);     // e.g., "KeyA", "Enter", "ShiftLeft"
        // console.log("keyCode:", event.keyCode); // Deprecated, use .key or .code
    }

    // Prevent default action (e.g., form submission, link navigation)
    // if (event.target.tagName === 'A') {
    //     event.preventDefault();
    //     console.log("Link navigation prevented!");
    // }
});

// Example with a form
document.getElementById('myForm')?.addEventListener('submit', function(event) {
    event.preventDefault(); // Stops the form from actually submitting (and reloading the page)
    console.log("Form submission prevented!");
    const nameInput = document.getElementById('nameInput');
    if (nameInput) {
        console.log("Name submitted:", nameInput.value);
    }
});
```

**Common Properties of the Event Object:**

*   `type`: The type of event (e.g., `"click"`).
*   `target`: The DOM element on which the event originally occurred (the innermost element that was clicked, typed into, etc.).
*   `currentTarget`: The DOM element to which the event listener is currently attached. (This can differ from `target` during event bubbling/capturing).
*   `timeStamp`: A timestamp indicating when the event was created.
*   `preventDefault()`: A method that, if called, cancels the browser's default action for that event (e.g., stops a link from navigating, stops a form from submitting).
*   `stopPropagation()`: A method that stops the event from "bubbling" up or "capturing" down the DOM tree (discussed next).

**Mouse Event Specific Properties:**
*   `clientX`, `clientY`: Coordinates of the mouse pointer relative to the visible part of the browser window.
*   `pageX`, `pageY`: Coordinates relative to the entire document.
*   `screenX`, `screenY`: Coordinates relative to the user's screen.
*   `altKey`, `ctrlKey`, `metaKey` (Cmd on Mac, Windows key on Windows), `shiftKey`: Boolean values indicating if these modifier keys were pressed during the event.
*   `button`: Which mouse button was pressed (0 for main/left, 1 for middle, 2 for right).

**Keyboard Event Specific Properties:**
*   `key`: The value of the key pressed (e.g., "a", "A", "Enter", "ArrowUp", "Escape").
*   `code`: The "physical" key code (e.g., "KeyA", "Enter", "ArrowUp", "Escape"). This is useful because `key` can change based on Shift or locale, while `code` usually represents the physical key.
*   `altKey`, `ctrlKey`, `metaKey`, `shiftKey`: Booleans for modifier keys.

### 5.4 Event Bubbling and Capturing (Conceptual)

When an event occurs on an element, it doesn't just happen on that one element. The event actually travels through the DOM tree in two phases:

1.  **Capturing Phase:** The event travels *down* from the `window` object to the document root, and then down to the target element's parent, and finally to the target element itself. Listeners attached in the capturing phase are triggered first.
2.  **Bubbling Phase:** After reaching the target element, the event then "bubbles" *up* from the target element back up through its ancestors to the document root, and finally to the `window` object. Listeners attached in the bubbling phase (the default) are triggered during this upward journey.

Most of the time, you'll work with **event bubbling**, which is the default behavior for `addEventListener()` (when `useCapture` is `false` or omitted).

**Example of Bubbling:**

```html
<div id="outer" style="padding: 20px; background-color: lightblue;">
    Outer Div
    <div id="middle" style="padding: 20px; background-color: lightgreen;">
        Middle Div
        <button id="innerButton" style="padding: 10px;">Inner Button</button>
    </div>
</div>
```

```javascript
const outerDiv = document.getElementById("outer");
const middleDiv = document.getElementById("middle");
const innerButton = document.getElementById("innerButton");

// All listeners are in the bubbling phase (default)
outerDiv.addEventListener("click", function(event) {
    console.log("Outer div clicked. Target:", event.target.id);
});

middleDiv.addEventListener("click", function(event) {
    console.log("Middle div clicked. Target:", event.target.id);
});

innerButton.addEventListener("click", function(event) {
    console.log("Inner button clicked. Target:", event.target.id);
});

// If you click "Inner Button":
// Output will be:
// Inner button clicked. Target: innerButton
// Middle div clicked. Target: innerButton
// Outer div clicked. Target: innerButton
```
Notice how clicking the inner button also triggers the click listeners on its parent elements (middle and outer divs) because the event "bubbles up." The `event.target` always refers to the element where the event originated (the `innerButton` in this case).

**`event.stopPropagation()`:**
You can stop an event from bubbling further up (or capturing further down) by calling `event.stopPropagation()` within an event listener.

```javascript
middleDiv.addEventListener("click", function(event) {
    console.log("Middle div clicked (stopPropagation). Target:", event.target.id);
    event.stopPropagation(); // Prevents the event from bubbling to outerDiv
});

// If you click "Inner Button" now:
// Output will be:
// Inner button clicked. Target: innerButton
// Middle div clicked (stopPropagation). Target: innerButton
// (The outerDiv's listener will NOT fire)
```
Use `stopPropagation()` judiciously, as it can sometimes make debugging harder if other parts of your application expect to hear about an event that has been stopped.

**Event Capturing:**
To listen for an event in the capturing phase, set the third argument of `addEventListener()` to `true`.

```javascript
outerDiv.addEventListener("click", function(event) {
    console.log("Outer div clicked (CAPTURING). Target:", event.target.id);
}, true); // true for capturing phase

// If you click "Inner Button" now, and the previous bubbling listeners are still active:
// Output might be (order can depend on browser specifics for same-element capture/bubble):
// Outer div clicked (CAPTURING). Target: innerButton
// Inner button clicked. Target: innerButton
// Middle div clicked. Target: innerButton
// Outer div clicked. Target: innerButton (bubbling listener)
```
Capturing is less commonly used than bubbling but can be useful for specific scenarios, like intercepting an event before it reaches its target or for global event logging.

### 5.5 Practical Examples: Interactive Forms, Menus

Let's put some of this into practice.

**Example 1: Simple Form Validation and Interaction**

```html
<!-- In index.html -->
<form id="contactForm">
    <div>
        <label for="name">Name:</label>
        <input type="text" id="name" name="name" required>
        <span class="error-message" id="nameError"></span>
    </div>
    <div>
        <label for="email">Email:</label>
        <input type="email" id="email" name="email" required>
        <span class="error-message" id="emailError"></span>
    </div>
    <button type="submit">Submit</button>
</form>
<div id="formFeedback"></div>
<style> .error-message { color: red; font-size: 0.9em; } </style>
```

```javascript
// In script.js
const contactForm = document.getElementById("contactForm");
const nameInput = document.getElementById("name");
const emailInput = document.getElementById("email");
const nameError = document.getElementById("nameError");
const emailError = document.getElementById("emailError");
const formFeedback = document.getElementById("formFeedback");

contactForm.addEventListener("submit", function(event) {
    event.preventDefault(); // Prevent default form submission
    formFeedback.textContent = ""; // Clear previous feedback

    let isValid = true;

    // Validate name
    if (nameInput.value.trim() === "") {
        nameError.textContent = "Name is required.";
        isValid = false;
    } else {
        nameError.textContent = "";
    }

    // Validate email
    if (emailInput.value.trim() === "") {
        emailError.textContent = "Email is required.";
        isValid = false;
    } else if (!emailInput.value.includes("@") || !emailInput.value.includes(".")) {
        emailError.textContent = "Please enter a valid email.";
        isValid = false;
    } else {
        emailError.textContent = "";
    }

    if (isValid) {
        formFeedback.style.color = "green";
        formFeedback.textContent = "Form submitted successfully! (Not really, we prevented it)";
        console.log("Form Data:", { name: nameInput.value, email: emailInput.value });
        contactForm.reset(); // Clear the form
    } else {
        formFeedback.style.color = "red";
        formFeedback.textContent = "Please correct the errors above.";
    }
});

// Real-time input feedback (optional)
nameInput.addEventListener("input", function() {
    if (nameInput.value.trim() !== "") {
        nameError.textContent = ""; // Clear error as user types
    }
});
emailInput.addEventListener("input", function() {
    if (emailInput.value.includes("@") && emailInput.value.includes(".")) {
        emailError.textContent = "";
    }
});
```

**Example 2: Simple Dropdown Menu (Click to Toggle)**

```html
<!-- In index.html -->
<style>
    .dropdown { position: relative; display: inline-block; }
    .dropdown-content {
        display: none; /* Hidden by default */
        position: absolute;
        background-color: #f9f9f9;
        min-width: 160px;
        box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
        z-index: 1;
    }
    .dropdown-content a {
        color: black;
        padding: 12px 16px;
        text-decoration: none;
        display: block;
    }
    .dropdown-content a:hover { background-color: #f1f1f1; }
    .show { display: block !important; } /* Class to show the dropdown */
</style>

<div class="dropdown">
    <button id="menuButton">Menu</button>
    <div id="myDropdown" class="dropdown-content">
        <a href="#home">Home</a>
        <a href="#about">About</a>
        <a href="#contact">Contact</a>
    </div>
</div>
```

```javascript
// In script.js
const menuButton = document.getElementById("menuButton");
const dropdownContent = document.getElementById("myDropdown");

menuButton.addEventListener("click", function(event) {
    dropdownContent.classList.toggle("show");
    event.stopPropagation(); // Prevent click from immediately closing due to window listener
});

// Close the dropdown if the user clicks outside of it
window.addEventListener("click", function(event) {
    // Check if the click was NOT on the menu button AND the dropdown is currently shown
    if (!menuButton.contains(event.target) && dropdownContent.classList.contains("show")) {
        dropdownContent.classList.remove("show");
    }
});
```
This menu example shows how `event.stopPropagation()` can be useful, and how listening for events on `window` can help manage UI elements like dropdowns or modals.

### 5.6 Chapter Summary & Action Steps

**Summary:**

*   **Events** are actions in the browser (clicks, key presses, page load).
*   **Event Listeners** (handlers) are functions that respond to events.
*   `element.addEventListener(type, function)` is the preferred way to attach listeners. `removeEventListener()` removes them.
*   The listener function receives an **Event object** with details about the event (e.g., `event.target`, `event.key`, `event.preventDefault()`, `event.stopPropagation()`).
*   Events travel in **Capturing** (down) and **Bubbling** (up, default) phases through the DOM. `stopPropagation()` can halt this.
*   Practical applications include form validation, interactive menus, and responding to any user input.

**Action Steps:**

1.  **Button Color Changer:**
    *   Create an HTML page with three buttons: "Red", "Green", "Blue".
    *   Add a `<div>` below the buttons.
    *   Write JavaScript to add event listeners to each button. When a button is clicked, change the background color of the `<div>` to the respective color.
2.  **Keyboard Input Display:**
    *   Create an `<input type="text">` field.
    *   Add an empty `<div>` below it.
    *   Add an event listener to the input field for the `keyup` event.
    *   Inside the listener, display the `event.key` and `event.code` of the pressed key in the `<div>`.
3.  **Simple Image Cycler:**
    *   Include two or three images in your HTML (you can use placeholder image URLs like `https://via.placeholder.com/150/FF0000/FFFFFF?Text=Image1` for testing). Give them IDs or a common class. Initially, display only the first one (e.g., by setting `style.display = 'none'` on others).
    *   Add "Next" and "Previous" buttons.
    *   Write JavaScript that, when "Next" is clicked, hides the current image and shows the next. When "Previous" is clicked, hide the current and show the previous. Make it loop around.
4.  **Form Character Counter:**
    *   Create a `<textarea>` and a `<span>` below it.
    *   Add an event listener to the `textarea` for the `input` event.
    *   Inside the listener, get the length of the text in the `textarea` and display it in the `<span>` (e.g., "Characters: 55").
    *   Optional: Add a maximum character limit and provide visual feedback or prevent further typing if it's exceeded.

Event handling is the cornerstone of dynamic web applications. By mastering how to listen for and respond to user interactions and browser events, you can create rich and engaging experiences. In the next chapter, we'll look at some built-in Browser APIs that extend JavaScript's capabilities even further.
