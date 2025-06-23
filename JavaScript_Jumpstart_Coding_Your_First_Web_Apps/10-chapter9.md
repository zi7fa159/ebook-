## Chapter 9: Project 2: Simple Quiz App

In this chapter, we'll build our second project: a Simple Quiz App. This application will present users with multiple-choice questions, allow them to select answers, provide feedback on their choices, and display a final score. This project will reinforce your understanding of DOM manipulation, event handling, working with arrays and objects for data structures, and implementing conditional logic.

### 9.1 Project Overview and Features

Our Simple Quiz App will have the following features:

1.  **Display Questions:** Show one multiple-choice question at a time.
2.  **Answer Selection:** Allow users to click on an option to select their answer.
3.  **Immediate Feedback (Optional but good):** Indicate if the selected answer was correct or incorrect.
4.  **Next Question:** A button to move to the next question.
5.  **Score Calculation:** Keep track of the user's score.
6.  **Display Final Score:** Show the total score at the end of the quiz.
7.  **Restart Quiz (Optional):** Allow the user to try the quiz again.

### 9.2 Data Structure for Questions and Answers

First, we need a way to store our quiz questions. An array of objects is a suitable data structure for this. Each object will represent a question and contain:
*   The question text.
*   An array of answer options (strings).
*   The correct answer (either the text of the correct option or its index in the options array).

```javascript
// Example data structure for quiz questions (in script.js or a separate data.js file)
const quizData = [
    {
        question: "What does HTML stand for?",
        options: [
            "Hyper Trainer Marking Language",
            "Hyper Text Markup Language",
            "Hyperlinks and Text Markup Language",
            "Home Tool Markup Language"
        ],
        correctAnswer: "Hyper Text Markup Language"
    },
    {
        question: "Which language is used for styling web pages?",
        options: ["HTML", "JQuery", "CSS", "XML"],
        correctAnswer: "CSS"
    },
    {
        question: "What is the purpose of JavaScript?",
        options: [
            "To structure web content",
            "To style web pages",
            "To add interactivity and dynamic behavior to web pages",
            "To manage databases"
        ],
        correctAnswer: "To add interactivity and dynamic behavior to web pages"
    },
    {
        question: "Which of the following is a JavaScript framework/library?",
        options: ["Laravel", "React", "Django", "Flask"],
        correctAnswer: "React"
    },
    {
        question: "What does `console.log()` do in JavaScript?",
        options: [
            "Displays a modal dialog",
            "Writes a message to the web page",
            "Writes a message to the browser's console",
            "Sends a message to the server"
        ],
        correctAnswer: "Writes a message to the browser's console"
    }
];
```

### 9.3 HTML Structure and CSS Styling (Basic)

Let's set up the HTML for our quiz. Create an `index.html` file in a new project folder (e.g., `Project2_QuizApp`):

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Simple Quiz App</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="quiz-container">
        <div id="questionContainer" class="hide">
            <h2 id="questionText">Question text goes here</h2>
            <div id="answerButtons" class="btn-grid">
                <!-- Answer buttons will be generated here -->
            </div>
        </div>

        <div class="controls">
            <div id="feedbackText" class="feedback"></div>
            <button id="nextButton" class="next-btn hide">Next</button>
        </div>

        <div id="scoreContainer" class="hide">
            <h2>Your Score</h2>
            <p id="scoreText">You got X out of Y correct!</p>
            <button id="restartButton">Restart Quiz</button>
        </div>
    </div>

    <script src="script.js"></script>
</body>
</html>
```

And a `style.css` file:

```css
body {
    font-family: Arial, sans-serif;
    background-color: #e0f7fa; /* Light cyan background */
    color: #333;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    margin: 0;
    padding: 20px;
}

.quiz-container {
    background-color: #ffffff;
    padding: 30px;
    border-radius: 10px;
    box-shadow: 0 4px 15px rgba(0,0,0,0.15);
    width: 100%;
    max-width: 600px;
    text-align: center;
}

#questionText {
    color: #00796b; /* Teal color for question */
    margin-bottom: 25px;
    font-size: 1.4em;
}

.btn-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); /* Responsive grid */
    gap: 10px;
    margin-bottom: 20px;
}

.btn {
    background-color: #009688; /* Teal */
    color: white;
    border: none;
    padding: 12px 15px;
    border-radius: 5px;
    cursor: pointer;
    font-size: 1em;
    transition: background-color 0.2s ease, transform 0.1s ease;
}

.btn:hover {
    background-color: #00796b;
}

.btn:active {
    transform: scale(0.98);
}

.btn.correct {
    background-color: #4caf50; /* Green */
}

.btn.wrong {
    background-color: #f44336; /* Red */
}

.btn:disabled { /* Style for buttons after an answer is selected */
    cursor: not-allowed;
    opacity: 0.7;
}

.controls {
    margin-top: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
}

.next-btn, #restartButton {
    background-color: #ff9800; /* Orange */
    color: white;
    border: none;
    padding: 10px 25px;
    border-radius: 5px;
    cursor: pointer;
    font-size: 1.1em;
    transition: background-color 0.2s ease;
}

.next-btn:hover, #restartButton:hover {
    background-color: #f57c00;
}

.hide {
    display: none !important; /* Utility class to hide elements */
}

.feedback {
    font-size: 1.1em;
    font-weight: bold;
    min-height: 1.5em; /* Reserve space to prevent layout shift */
}

.feedback.correct {
    color: #4caf50;
}

.feedback.wrong {
    color: #f44336;
}

#scoreContainer h2 {
    color: #00796b;
    margin-bottom: 15px;
}
#scoreText {
    font-size: 1.2em;
    margin-bottom: 20px;
}
```

### 9.4 Displaying Questions and Options / 9.5 Checking Answers and Providing Feedback / 9.6 Calculating and Displaying Score / 9.7 Step-by-Step Implementation Guide

Let's implement the JavaScript logic in `script.js`.

```javascript
// script.js

const quizData = [
    {
        question: "What does HTML stand for?",
        options: [
            "Hyper Trainer Marking Language",
            "Hyper Text Markup Language",
            "Hyperlinks and Text Markup Language",
            "Home Tool Markup Language"
        ],
        correctAnswer: "Hyper Text Markup Language"
    },
    {
        question: "Which language is used for styling web pages?",
        options: ["HTML", "JQuery", "CSS", "XML"],
        correctAnswer: "CSS"
    },
    {
        question: "What is the purpose of JavaScript?",
        options: [
            "To structure web content",
            "To style web pages",
            "To add interactivity and dynamic behavior to web pages",
            "To manage databases"
        ],
        correctAnswer: "To add interactivity and dynamic behavior to web pages"
    },
    {
        question: "Which of the following is a JavaScript framework/library?",
        options: ["Laravel", "React", "Django", "Flask"],
        correctAnswer: "React"
    },
    {
        question: "What does `console.log()` do in JavaScript?",
        options: [
            "Displays a modal dialog",
            "Writes a message to the web page",
            "Writes a message to the browser's console",
            "Sends a message to the server"
        ],
        correctAnswer: "Writes a message to the browser's console"
    }
];

// --- DOM Elements ---
const questionContainer = document.getElementById("questionContainer");
const questionTextElement = document.getElementById("questionText");
const answerButtonsElement = document.getElementById("answerButtons");
const nextButton = document.getElementById("nextButton");
const feedbackTextElement = document.getElementById("feedbackText");
const scoreContainer = document.getElementById("scoreContainer");
const scoreTextElement = document.getElementById("scoreText");
const restartButton = document.getElementById("restartButton");

// --- Quiz State ---
let currentQuestionIndex = 0;
let score = 0;
let shuffledQuestions = []; // To hold shuffled questions for each quiz attempt

// --- Functions ---

/**
 * Shuffles an array in place (Fisher-Yates shuffle) and returns it.
 */
function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]]; // Swap elements
    }
    return array;
}

/**
 * Starts the quiz: shuffles questions, resets state, and shows the first question.
 */
function startQuiz() {
    shuffledQuestions = shuffleArray([...quizData]); // Shuffle a copy of quizData
    currentQuestionIndex = 0;
    score = 0;
    questionContainer.classList.remove("hide");
    scoreContainer.classList.add("hide");
    nextButton.classList.add("hide"); // Hide next button initially
    feedbackTextElement.textContent = "";
    feedbackTextElement.className = 'feedback'; // Reset feedback class
    showQuestion(shuffledQuestions[currentQuestionIndex]);
}

/**
 * Displays a question and its answer options.
 * @param {object} questionObject - The question object from quizData.
 */
function showQuestion(questionObject) {
    resetState(); // Clear previous question's buttons and feedback
    questionTextElement.textContent = questionObject.question;

    // Shuffle answer options for the current question
    const currentOptions = shuffleArray([...questionObject.options]);

    currentOptions.forEach(optionText => {
        const button = document.createElement("button");
        button.textContent = optionText;
        button.classList.add("btn");
        button.addEventListener("click", selectAnswer);
        answerButtonsElement.appendChild(button);
    });
}

/**
 * Resets the state for showing a new question (clears answer buttons, hides next button).
 */
function resetState() {
    nextButton.classList.add("hide");
    feedbackTextElement.textContent = "";
    feedbackTextElement.className = 'feedback';
    while (answerButtonsElement.firstChild) {
        answerButtonsElement.removeChild(answerButtonsElement.firstChild);
    }
}

/**
 * Handles the event when an answer button is clicked.
 * @param {Event} event - The click event.
 */
function selectAnswer(event) {
    const selectedButton = event.target;
    const correctAnswer = shuffledQuestions[currentQuestionIndex].correctAnswer;
    const isCorrect = selectedButton.textContent === correctAnswer;

    if (isCorrect) {
        score++;
        selectedButton.classList.add("correct");
        feedbackTextElement.textContent = "Correct!";
        feedbackTextElement.classList.add("correct");
    } else {
        selectedButton.classList.add("wrong");
        feedbackTextElement.textContent = `Wrong! Correct answer: ${correctAnswer}`;
        feedbackTextElement.classList.add("wrong");
    }

    // Disable all answer buttons after selection & show correct one if wrong
    Array.from(answerButtonsElement.children).forEach(button => {
        button.disabled = true; // Disable button
        if (button.textContent === correctAnswer && !isCorrect) {
            button.classList.add("correct"); // Also highlight the correct one if user was wrong
        }
    });

    // Show next button or finish quiz
    if (shuffledQuestions.length > currentQuestionIndex + 1) {
        nextButton.classList.remove("hide");
    } else {
        // End of quiz
        showScore();
    }
}

/**
 * Displays the final score.
 */
function showScore() {
    questionContainer.classList.add("hide");
    nextButton.classList.add("hide");
    feedbackTextElement.textContent = "";
    feedbackTextElement.className = 'feedback';
    scoreContainer.classList.remove("hide");
    scoreTextElement.textContent = `You scored ${score} out of ${shuffledQuestions.length}!`;
}

/**
 * Handles click on the "Next" button to show the next question.
 */
function handleNextButton() {
    currentQuestionIndex++;
    if (currentQuestionIndex < shuffledQuestions.length) {
        showQuestion(shuffledQuestions[currentQuestionIndex]);
    } else {
        // This case should ideally be handled by selectAnswer,
        // but as a fallback or if logic changes:
        showScore();
    }
}

// --- Event Listeners ---
nextButton.addEventListener("click", handleNextButton);
restartButton.addEventListener("click", startQuiz);

// --- Initial Load ---
startQuiz(); // Start the quiz when the page loads
```

**JavaScript Breakdown:**

1.  **`quizData`:** Our array of question objects.
2.  **DOM Elements:** References to all necessary HTML elements.
3.  **Quiz State Variables:**
    *   `currentQuestionIndex`: Tracks which question the user is on.
    *   `score`: Tracks the user's score.
    *   `shuffledQuestions`: An array to hold the questions in a random order for each quiz attempt. This prevents users from memorizing the question sequence.
4.  **`shuffleArray(array)`:** A utility function (Fisher-Yates algorithm) to shuffle an array. We use this to shuffle both the questions and the answer options for each question to make the quiz less predictable.
5.  **`startQuiz()`:**
    *   Resets `currentQuestionIndex` and `score`.
    *   Shuffles a *copy* of `quizData` using `shuffleArray([...quizData])` (the spread operator `...` creates a shallow copy).
    *   Hides the score container and shows the question container.
    *   Calls `showQuestion()` to display the first (shuffled) question.
6.  **`showQuestion(questionObject)`:**
    *   Calls `resetState()` to clear previous buttons/feedback.
    *   Sets the `questionTextElement`'s content.
    *   Shuffles the `options` for the current `questionObject`.
    *   Dynamically creates a button for each answer option:
        *   Sets its text.
        *   Adds a CSS class (`btn`).
        *   Attaches an event listener (`selectAnswer`) to it.
        *   Appends the button to `answerButtonsElement`.
7.  **`resetState()`:**
    *   Hides the `nextButton`.
    *   Clears old answer buttons from `answerButtonsElement`.
    *   Clears feedback text.
8.  **`selectAnswer(event)`:**
    *   This function is called when an answer button is clicked.
    *   `event.target` is the button that was clicked.
    *   It checks if `selectedButton.textContent` matches the `correctAnswer` for the `currentQuestionIndex`.
    *   Updates the score and provides visual feedback by adding `.correct` or `.wrong` classes to the button and updating `feedbackTextElement`.
    *   **Crucially**, it disables all answer buttons for the current question after an answer is selected to prevent multiple attempts. It also highlights the correct answer if the user chose incorrectly.
    *   If there are more questions, it shows the `nextButton`.
    *   If it's the last question, it calls `showScore()`.
9.  **`showScore()`:**
    *   Hides the question container and next button.
    *   Shows the `scoreContainer`.
    *   Displays the final score.
10. **`handleNextButton()`:**
    *   Increments `currentQuestionIndex`.
    *   Calls `showQuestion()` for the next question.
11. **Event Listeners:**
    *   The `nextButton` listens for clicks to call `handleNextButton`.
    *   The `restartButton` listens for clicks to call `startQuiz`.
12. **Initial Load:** `startQuiz()` is called when the script loads to begin the quiz immediately.

**To Make it Work:**
Save `index.html`, `style.css`, and `script.js` in the same folder (`Project2_QuizApp`). Open `index.html` in your browser.

### 8.7 Chapter Summary & Action Steps (Further Enhancements)

**Summary:**

*   We built a Simple Quiz App that presents questions, allows answer selection, provides feedback, and calculates a score.
*   Utilized an array of objects to store quiz data.
*   Dynamically created and updated DOM elements for questions and answers.
*   Handled click events for answer selection and navigation.
*   Implemented logic for scoring and displaying results.
*   Used array shuffling to randomize question and option order.

**Action Steps (Further Enhancements - Optional Challenges):**

1.  **More Questions:** Add more diverse questions to the `quizData` array.
2.  **Timer per Question (Advanced):**
    *   Add a timer that counts down for each question.
    *   If the timer runs out, automatically mark the question as incorrect and move to the next one or end the quiz.
    *   This would involve `setInterval` and `clearInterval`.
3.  **Store High Score:**
    *   Use `localStorage` to store the user's highest score for the quiz.
    *   Display the high score when the quiz ends or on a separate high score page/section.
4.  **Different Question Types:**
    *   Extend the `quizData` structure and logic to support other question types, like "fill in the blank" or "true/false". This would require more complex rendering and answer checking logic.
5.  **Visual Feedback for Progress:**
    *   Add a progress bar or text indicating "Question X of Y".
6.  **Review Answers:** At the end of the quiz, allow the user to review their answers, showing which ones they got right or wrong and what the correct answers were.
7.  **Improved Styling and Animations:** Enhance the visual appeal with more sophisticated CSS, transitions, or simple animations.

This quiz app demonstrates how to manage application state and create a more involved interactive experience. By tackling the enhancements, you can further practice your problem-solving and JavaScript skills. In the next chapter, we'll build a Weather Dashboard that involves fetching data from an external API.
