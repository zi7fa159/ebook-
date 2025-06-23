## Chapter 8: Project 1: Interactive To-Do List

Now it's time to apply what we've learned by building our first complete web application: an Interactive To-Do List. This project will consolidate your understanding of HTML structure, CSS styling (basic), JavaScript fundamentals, DOM manipulation, and event handling. We'll also use `localStorage` to make our to-do items persist even if the user closes the browser.

### 8.1 Project Overview and Features

Our To-Do List application will allow users to:

1.  **Add new tasks:** Enter a task in an input field and add it to the list.
2.  **View tasks:** Display the list of tasks.
3.  **Mark tasks as complete:** Click on a task to toggle its completion status (e.g., strike-through text).
4.  **Delete tasks:** Remove tasks from the list.
5.  **Persist tasks:** Tasks will be saved in the browser's `localStorage` so they are still there when the user revisits the page.

This project will focus primarily on JavaScript logic. We'll keep the HTML structure and CSS styling relatively simple.

### 8.2 HTML Structure and CSS Styling (Basic)

Let's start with the basic HTML structure for our To-Do List.

Create an `index.html` file in a new project folder (e.g., `Project1_TodoList`):

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Interactive To-Do List</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <h1>My To-Do List</h1>

        <form id="todoForm">
            <input type="text" id="taskInput" placeholder="Enter a new task..." autocomplete="off">
            <button type="submit">Add Task</button>
        </form>

        <ul id="taskList">
            <!-- Tasks will be added here by JavaScript -->
        </ul>
    </div>

    <script src="script.js"></script>
</body>
</html>
```

Now, create a `style.css` file in the same folder for some basic styling:

```css
body {
    font-family: Arial, sans-serif;
    background-color: #f4f4f4;
    color: #333;
    line-height: 1.6;
    margin: 0;
    padding: 20px;
    display: flex;
    justify-content: center;
    align-items: flex-start; /* Align to top for longer lists */
    min-height: 100vh;
}

.container {
    background-color: #fff;
    padding: 25px;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    width: 100%;
    max-width: 500px;
}

h1 {
    text-align: center;
    color: #5a67d8; /* A nice blue/purple */
    margin-bottom: 20px;
}

#todoForm {
    display: flex;
    margin-bottom: 20px;
}

#taskInput {
    flex-grow: 1;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 4px 0 0 4px;
    font-size: 1em;
}

#todoForm button {
    padding: 10px 15px;
    background-color: #5a67d8;
    color: white;
    border: none;
    border-radius: 0 4px 4px 0;
    cursor: pointer;
    font-size: 1em;
    transition: background-color 0.2s ease;
}

#todoForm button:hover {
    background-color: #434190;
}

#taskList {
    list-style-type: none;
    padding: 0;
    margin: 0;
}

#taskList li {
    background-color: #f9f9f9;
    padding: 12px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: background-color 0.2s ease;
}

#taskList li:last-child {
    border-bottom: none;
}

#taskList li.completed .task-text {
    text-decoration: line-through;
    color: #aaa;
}

.task-text {
    cursor: pointer;
    flex-grow: 1;
}

.delete-btn {
    background-color: #e53e3e; /* Red */
    color: white;
    border: none;
    padding: 6px 10px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.9em;
    margin-left: 10px;
    transition: background-color 0.2s ease;
}

.delete-btn:hover {
    background-color: #c53030;
}
```
This CSS provides a clean and functional look for our application.

### 8.3 JavaScript Logic: Adding, Completing, Deleting Tasks

Now for the core JavaScript logic. Create a `script.js` file.

We'll break down the JavaScript into several parts:

1.  **Selecting DOM Elements:** Get references to the form, input field, and task list.
2.  **Storing Tasks:** We'll use an array to store our task objects. Each task object might have properties like `id`, `text`, and `completed`.
3.  **Rendering Tasks:** A function to take the array of tasks and display them in the HTML list.
4.  **Adding a New Task:** Handle form submission to add a new task.
5.  **Toggling Task Completion:** Handle clicks on a task to mark it as complete/incomplete.
6.  **Deleting a Task:** Handle clicks on a delete button.
7.  **Local Storage:** Functions to save tasks to and load tasks from `localStorage`.

Let's start with the basic setup and rendering.

```javascript
// script.js

// --- DOM Elements ---
const todoForm = document.getElementById("todoForm");
const taskInput = document.getElementById("taskInput");
const taskList = document.getElementById("taskList");

// --- Application State ---
// An array to store our tasks. Each task will be an object.
// Example task: { id: Date.now(), text: "Learn JavaScript", completed: false }
let tasks = [];

// --- Functions ---

/**
 * Renders the current tasks to the DOM.
 * Clears the existing list and rebuilds it from the 'tasks' array.
 */
function renderTasks() {
    // Clear current list items
    taskList.innerHTML = "";

    if (tasks.length === 0) {
        taskList.innerHTML = "<li>No tasks yet. Add one!</li>";
        return;
    }

    tasks.forEach(function(task) {
        const listItem = document.createElement("li");
        listItem.setAttribute("data-id", task.id); // Store task ID on the element

        // Add 'completed' class if task is completed
        if (task.completed) {
            listItem.classList.add("completed");
        }

        // Task text span
        const taskTextSpan = document.createElement("span");
        taskTextSpan.classList.add("task-text");
        taskTextSpan.textContent = task.text;
        taskTextSpan.addEventListener("click", function() {
            toggleTaskCompletion(task.id);
        });

        // Delete button
        const deleteButton = document.createElement("button");
        deleteButton.classList.add("delete-btn");
        deleteButton.textContent = "Delete";
        deleteButton.addEventListener("click", function() {
            deleteTask(task.id);
        });

        listItem.appendChild(taskTextSpan);
        listItem.appendChild(deleteButton);
        taskList.appendChild(listItem);
    });
}

/**
 * Handles adding a new task.
 * @param {Event} event - The form submission event.
 */
function handleAddTask(event) {
    event.preventDefault(); // Prevent page reload on form submit

    const taskText = taskInput.value.trim();

    if (taskText === "") {
        alert("Please enter a task!");
        return;
    }

    const newTask = {
        id: Date.now(), // Simple unique ID using timestamp
        text: taskText,
        completed: false
    };

    tasks.push(newTask);
    taskInput.value = ""; // Clear the input field

    saveTasksToLocalStorage();
    renderTasks();
    console.log("Tasks after adding:", tasks);
}

/**
 * Toggles the completion status of a task.
 * @param {number} taskId - The ID of the task to toggle.
 */
function toggleTaskCompletion(taskId) {
    tasks = tasks.map(task => {
        if (task.id === taskId) {
            return { ...task, completed: !task.completed }; // Create new object with toggled status
        }
        return task;
    });

    saveTasksToLocalStorage();
    renderTasks();
    console.log("Tasks after toggle:", tasks);
}

/**
 * Deletes a task from the list.
 * @param {number} taskId - The ID of the task to delete.
 */
function deleteTask(taskId) {
    tasks = tasks.filter(task => task.id !== taskId); // Keep all tasks EXCEPT the one with taskId

    saveTasksToLocalStorage();
    renderTasks();
    console.log("Tasks after deleting:", tasks);
}


// --- Event Listeners ---
todoForm.addEventListener("submit", handleAddTask);


// --- Initial Load ---
// (We'll add localStorage loading here later)
renderTasks(); // Initial render to show "No tasks yet" or saved tasks
```

**Explanation of the JavaScript so far:**

1.  **DOM Elements:** We get references to the HTML elements we'll interact with.
2.  **`tasks` array:** This array will hold our to-do items. Each item is an object with an `id` (we use `Date.now()` for a simple unique ID, though for more robust apps a proper UUID generator would be better), the `text` of the task, and a `completed` boolean status.
3.  **`renderTasks()`:**
    *   Clears any existing items in the `taskList` (`taskList.innerHTML = "";`).
    *   If there are no tasks, it displays a placeholder message.
    *   It iterates over the `tasks` array using `forEach`.
    *   For each task, it creates an `<li>` element.
    *   A `data-id` attribute is set on the `<li>` to easily identify the task later.
    *   If a task is `completed`, the `completed` CSS class is added to the `<li>`.
    *   A `<span>` is created for the task text. An event listener is added to this span to call `toggleTaskCompletion()` when clicked.
    *   A "Delete" button is created. An event listener is added to it to call `deleteTask()` when clicked.
    *   The span and button are appended to the `<li>`, and the `<li>` is appended to the main `taskList` `<ul>`.
4.  **`handleAddTask(event)`:**
    *   `event.preventDefault()` stops the default form submission behavior (which would reload the page).
    *   It gets the trimmed text from the `taskInput`.
    *   If the text is empty, it shows an alert.
    *   A `newTask` object is created.
    *   The `newTask` is added (pushed) to the `tasks` array.
    *   The input field is cleared.
    *   `renderTasks()` is called to update the display.
5.  **`toggleTaskCompletion(taskId)`:**
    *   It receives the `id` of the task to be toggled.
    *   It uses the `map()` method to create a *new* `tasks` array. If a task's `id` matches `taskId`, it creates a new task object with the `completed` status flipped (`!task.completed`). Otherwise, it returns the original task object. Using `map` and creating new objects helps with immutability, which can be beneficial in larger applications (though not strictly necessary for this simple example).
    *   `renderTasks()` is called to update the display.
6.  **`deleteTask(taskId)`:**
    *   It receives the `id` of the task to be deleted.
    *   It uses the `filter()` method to create a *new* `tasks` array containing all tasks *except* the one whose `id` matches `taskId`.
    *   `renderTasks()` is called to update the display.
7.  **Event Listener:** An event listener is attached to the `todoForm` for the `submit` event, calling `handleAddTask`.
8.  **Initial Render:** `renderTasks()` is called once when the script loads to display any initial state (currently "No tasks yet").

At this point, you should be able to add tasks, see them appear, click on them to toggle a (currently invisible) completed state, and delete them. The visual feedback for "completed" will come from the CSS class `.completed .task-text` we defined earlier.

### 8.4 Persisting Tasks using Local Storage

To make our tasks persist even when the browser is closed and reopened, we'll use `localStorage`.

Add these functions to your `script.js`:

```javascript
// (Add these functions to the existing script.js)

// --- Local Storage Functions ---

const STORAGE_KEY = "todoApp.tasks"; // A key for storing our tasks in localStorage

/**
 * Saves the current 'tasks' array to localStorage.
 * Tasks are stored as a JSON string.
 */
function saveTasksToLocalStorage() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks));
    console.log("Tasks saved to localStorage");
}

/**
 * Loads tasks from localStorage.
 * If tasks are found, they parse them and update the 'tasks' array.
 */
function loadTasksFromLocalStorage() {
    const storedTasks = localStorage.getItem(STORAGE_KEY);
    if (storedTasks) {
        tasks = JSON.parse(storedTasks);
        console.log("Tasks loaded from localStorage:", tasks);
    } else {
        tasks = []; // Initialize with an empty array if nothing is stored
        console.log("No tasks found in localStorage. Initializing empty.");
    }
}

// --- Modify Initial Load ---
// Replace the existing `renderTasks();` at the end of the script with this:

// Initial Load
loadTasksFromLocalStorage(); // Load tasks from storage first
renderTasks();               // Then render them
```

**Changes Explained:**

1.  **`STORAGE_KEY`:** A constant string used as the key for storing our tasks array in `localStorage`. This helps avoid conflicts if other scripts on the same domain use `localStorage`.
2.  **`saveTasksToLocalStorage()`:**
    *   This function takes the current `tasks` array.
    *   It converts the `tasks` array into a JSON string using `JSON.stringify()`.
    *   It saves this JSON string into `localStorage` using the `STORAGE_KEY`.
3.  **`loadTasksFromLocalStorage()`:**
    *   This function retrieves the JSON string from `localStorage` using `STORAGE_KEY`.
    *   If `storedTasks` is not null (meaning something was found), it parses the JSON string back into a JavaScript array using `JSON.parse()` and assigns it to our global `tasks` array.
    *   If nothing is found, it initializes `tasks` as an empty array.
4.  **Updated Initial Load:**
    *   When the script first runs, we now call `loadTasksFromLocalStorage()` *before* `renderTasks()`. This ensures that any previously saved tasks are loaded into our `tasks` array before we try to display them.
5.  **Integration:**
    *   `saveTasksToLocalStorage()` is now called inside `handleAddTask()`, `toggleTaskCompletion()`, and `deleteTask()` right after the `tasks` array is modified. This ensures that any change to the tasks is immediately saved.

With these additions, your To-Do List will now remember tasks across browser sessions!

### 8.5 Step-by-Step Implementation Guide (Summary)

1.  **Create Project Folder:** `Project1_TodoList`.
2.  **Create `index.html`:** Add the HTML structure provided in section 8.2.
3.  **Create `style.css`:** Add the CSS rules from section 8.2.
4.  **Create `script.js`:**
    *   Start by selecting DOM elements (`todoForm`, `taskInput`, `taskList`).
    *   Initialize an empty `tasks` array.
    *   Implement `renderTasks()` to display tasks in the `taskList` UL.
    *   Implement `handleAddTask()` to add new tasks to the `tasks` array and re-render. Attach it to the form's `submit` event.
    *   Implement `toggleTaskCompletion()` (called from `renderTasks` when task text is clicked).
    *   Implement `deleteTask()` (called from `renderTasks` when a delete button is clicked).
    *   Implement `saveTasksToLocalStorage()` and `loadTasksFromLocalStorage()`.
    *   Call `loadTasksFromLocalStorage()` and then `renderTasks()` at the end of your script for the initial page load.
    *   Ensure `saveTasksToLocalStorage()` is called whenever `tasks` array is modified (in add, toggle, delete functions).

**Testing and Debugging:**
*   Open `index.html` in your browser.
*   Use the Developer Tools (Console) to check for errors and log messages.
*   Test adding tasks, marking them complete (they should get a line-through), and deleting them.
*   Close the browser tab and reopen `index.html`. Your tasks should still be there.
*   Check the "Application" (or "Storage") tab in your Developer Tools to see the data stored in `localStorage`.

### 8.6 Chapter Summary & Action Steps (Further Enhancements)

**Summary:**

*   We built a functional Interactive To-Do List application.
*   Key JavaScript concepts used: DOM element selection, DOM manipulation (creating, appending, modifying elements), event handling (`submit`, `click`), array manipulation (`push`, `map`, `filter`), and `localStorage` for data persistence.
*   The application allows adding, viewing, completing, and deleting tasks, with changes saved locally.

**Action Steps (Further Enhancements - Optional Challenges):**

1.  **Edit Task Text:**
    *   Add an "Edit" button next to each task.
    *   When "Edit" is clicked, allow the user to modify the task text (e.g., replace the task text span with an input field, or use a `prompt()`).
    *   Save the updated text and re-render. Don't forget to update `localStorage`.
2.  **Clear All Completed Tasks:**
    *   Add a "Clear Completed" button.
    *   When clicked, remove all tasks from the `tasks` array that have `completed: true`.
    *   Update `localStorage` and re-render.
3.  **Filter Tasks:**
    *   Add filter buttons (e.g., "All", "Active", "Completed").
    *   Modify `renderTasks()` to display only the tasks that match the current filter. You might need an additional state variable to store the current filter.
4.  **Better Unique IDs:**
    *   Instead of `Date.now()`, research and implement a more robust way to generate unique IDs (e.g., a simple counter combined with a prefix, or a basic UUID function if you want to explore further).
5.  **Improved Styling:**
    *   Spend some time enhancing the CSS to make the application visually more appealing.
6.  **Confirmation for Delete:**
    *   Before deleting a task, use `confirm("Are you sure you want to delete this task?")` to ask the user for confirmation. Only delete if they confirm.

This project provides a solid foundation. By tackling these enhancements, you can further solidify your JavaScript skills. Congratulations on building your first interactive web application! In the next chapter, we'll build another project, a Simple Quiz App, to explore different aspects of JavaScript and DOM interaction.
