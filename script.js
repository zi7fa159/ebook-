document.addEventListener('DOMContentLoaded', function () {
    const fullPageInstance = new fullpage('#fullpage', {
        // Navigation
        anchors: ['lesson-1', 'lesson-2', 'lesson-3', 'lesson-4', 'lesson-5', 'lesson-6', 'lesson-7', 'lesson-8'],
        navigation: false, // We use custom navigation
        navigationPosition: 'right',
        // Scrolling
        autoScrolling: true,
        fitToSection: true,
        fitToSectionDelay: 1000,
        scrollOverflow: false, // Important: Set to false to prevent internal scrollbars if content fits
        scrollingSpeed: 700,
        // Accessibility
        keyboardScrolling: false, // Disable keyboard scrolling, use buttons
        // Design
        controlArrows: false, // Disable default arrows
        verticalCentered: true, // Handled by custom CSS flex now
        // Callbacks
        afterLoad: function(origin, destination, direction){
            updateProgressBar(destination.index, fullPageInstance.getActiveSection().last);
            updateNavButtons(destination.index, fullPageInstance.getActiveSection().last);
            checkQuizStateForSection(destination.anchor);
        },
        onLeave: function(origin, destination, direction){
            // Can be used if needed
        }
    });

    const prevButton = document.getElementById('prevButton');
    const nextButton = document.getElementById('nextButton');
    const progressBarFill = document.getElementById('progressBarFill');

    // --- Quiz Data (Correct Answers) ---
    // In a real scenario, this might come from a backend or be more securely stored.
    const correctAnswers = {
        'quiz-1': { 'q1_1': 'c', 'q1_2': 'c' },
        'quiz-2': { 'q2_1': 'b', 'q2_2': 'b' },
        'quiz-3': { 'q3_1': 'b', 'q3_2': 'c' },
        'quiz-4': { 'q4_1': 'c', 'q4_2': 'b' },
        'quiz-5': { 'q5_1': 'b', 'q5_2': 'c' },
        'quiz-6': { 'q6_1': 'c', 'q6_2': 'b' }
    };

    // Store quiz completion status
    const quizCompletionStatus = {}; // e.g., {'quiz-1': true}

    // --- Navigation Button Logic ---
    prevButton.addEventListener('click', function () {
        fullpage_api.moveSectionUp();
    });

    nextButton.addEventListener('click', function () {
        fullpage_api.moveSectionDown();
    });

    function updateNavButtons(currentIndex, totalSections) {
        prevButton.disabled = currentIndex === 0;
        // Next button is more complex due to quiz logic, handled by checkQuizStateForSection
        if (currentIndex === totalSections) { // Last section (conclusion)
             nextButton.disabled = true;
        }
    }

    function updateProgressBar(currentIndex, totalSections) {
        const progress = totalSections > 0 ? ((currentIndex) / totalSections) * 100 : 0;
        progressBarFill.style.width = progress + '%';
    }


    // --- Quiz Logic ---
    const quizContainers = document.querySelectorAll('.quiz-container');

    quizContainers.forEach(container => {
        const questions = container.querySelectorAll('.question');
        const submitButton = container.querySelector('.submit-quiz-button');
        const quizId = submitButton ? submitButton.dataset.quizId : null;

        if (!submitButton) { // For intro/conclusion sections without quizzes
            if (container.id === "quiz-intro" || container.id === "quiz-conclusion") {
                // Mark as "complete" for navigation purposes
                quizCompletionStatus[container.id] = true;
            }
            return;
        }

        quizCompletionStatus[quizId] = false; // Initialize as not completed

        questions.forEach(question => {
            const radios = question.querySelectorAll('input[type="radio"]');
            radios.forEach(radio => {
                radio.addEventListener('change', () => {
                    // Check if all questions in this quiz have an answer
                    let allAnswered = true;
                    questions.forEach(q => {
                        if (!q.querySelector('input[type="radio"]:checked')) {
                            allAnswered = false;
                        }
                    });
                    submitButton.disabled = !allAnswered;
                });
            });
        });

        submitButton.addEventListener('click', () => {
            if (!quizId || !correctAnswers[quizId]) return;

            let allCorrect = true;
            questions.forEach(question => {
                const questionId = question.dataset.questionId;
                const selectedOption = question.querySelector('input[type="radio"]:checked');
                const feedbackEl = question.querySelector('.feedback');
                const labels = question.querySelectorAll('label');

                labels.forEach(l => {
                    l.classList.remove('correct-answer', 'incorrect-answer', 'reveal-correct');
                });

                if (selectedOption) {
                    const userAnswer = selectedOption.value;
                    const correctAnswer = correctAnswers[quizId][questionId];

                    const selectedLabel = selectedOption.closest('label');

                    if (userAnswer === correctAnswer) {
                        feedbackEl.textContent = 'Correct!';
                        feedbackEl.className = 'feedback correct';
                        if(selectedLabel) selectedLabel.classList.add('correct-answer');
                    } else {
                        feedbackEl.textContent = `Incorrect. The correct answer was '${correctAnswer.toUpperCase()}'.`;
                        feedbackEl.className = 'feedback incorrect';
                        if(selectedLabel) selectedLabel.classList.add('incorrect-answer');
                        allCorrect = false;

                        // Highlight the actual correct answer
                        const correctRadio = question.querySelector(`input[type="radio"][value="${correctAnswer}"]`);
                        if (correctRadio) {
                            const correctLabel = correctRadio.closest('label');
                            if (correctLabel) correctLabel.classList.add('reveal-correct');
                        }
                    }
                }
                // Disable radio buttons after submission for this question
                question.querySelectorAll('input[type="radio"]').forEach(rb => rb.disabled = true);
            });

            submitButton.disabled = true; // Disable after submission
            submitButton.textContent = "Submitted";
            quizCompletionStatus[quizId] = true;
            checkQuizStateForSection(fullpage_api.getActiveSection().anchor); // Re-check nav button state
        });
    });

    function checkQuizStateForSection(sectionAnchor) {
        const currentSection = document.querySelector(`.section[data-anchor="${sectionAnchor}"]`);
        if (!currentSection) return;

        const quizContainer = currentSection.querySelector('.quiz-container');
        if (!quizContainer) { // No quiz in this section (e.g. an error or unexpected structure)
            nextButton.disabled = (fullPageInstance.getActiveSection().index === fullPageInstance.getActiveSection().last);
            return;
        }

        const quizId = quizContainer.id; // e.g. "quiz-1", "quiz-intro"

        if (quizId === "quiz-intro" || quizId === "quiz-conclusion") { // Intro and Conclusion have no blocking quiz
            quizCompletionStatus[quizId] = true; // Mark as "complete" for navigation
        }

        const isQuizCompleted = quizCompletionStatus[quizId] === true;
        const isLastSection = fullPageInstance.getActiveSection().index === fullPageInstance.getActiveSection().last;

        nextButton.disabled = !isQuizCompleted && !isLastSection;

        // Special handling for the very last section (conclusion)
        if (isLastSection) {
            nextButton.disabled = true;
        }
    }

    // Initial check for the first loaded section
    if (fullPageInstance.getActiveSection()) {
        updateProgressBar(fullPageInstance.getActiveSection().index, fullPageInstance.getActiveSection().last);
        updateNavButtons(fullPageInstance.getActiveSection().index, fullPageInstance.getActiveSection().last);
        checkQuizStateForSection(fullPageInstance.getActiveSection().anchor);
    }

});
