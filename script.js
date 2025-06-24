document.addEventListener('DOMContentLoaded', function() {
    const isCoursePage = document.getElementById('fullpage') !== null;
    let fullPageInstance;

    // Quiz data: questions, options, correct answer index, user's answer, completed status
    const quizzes = {
        quiz1: {
            questions: [
                { text: "1. What is the typical lifespan of a guppy?", options: ["6 months - 1 year", "1-3 years", "5-7 years"], answer: 1 },
                { text: "2. Guppies are generally peaceful fish. (True/False)", options: ["True", "False"], answer: 0 },
                { text: "3. Which of these is NOT a basic need for guppies?", options: ["Clean water", "Saltwater environment", "Stable temperature"], answer: 1 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz2: {
            questions: [
                { text: "1. What is a good minimum tank size for a small guppy breeding group (1 male, 2-3 females)?", options: ["1 gallon", "5 gallons", "10 gallons"], answer: 2 },
                { text: "2. Why are sponge filters often recommended for guppy breeding tanks?", options: ["They create strong currents.", "They are gentle and won't suck up fry.", "They require no maintenance."], answer: 1 },
                { text: "3. What is the purpose of cycling a new aquarium?", options: ["To make the water crystal clear.", "To establish beneficial bacteria for waste processing.", "To quickly raise the water temperature."], answer: 1 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz3: {
            questions: [
                { text: "1. Which fin is modified in male guppies for mating?", options: ["Dorsal fin", "Caudal fin", "Anal fin (gonopodium)"], answer: 2 },
                { text: "2. A dark area near the anal fin of a female guppy, indicating potential pregnancy, is called:", options: ["The color spot", "The gravid spot", "The breeding mark"], answer: 1 },
                { text: "3. Why is it important to quarantine new fish before adding them to your main tank?", options: ["To help them get used to your water temperature.", "To prevent the introduction of diseases or parasites.", "To make them less shy."], answer: 1 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz4: {
            questions: [
                { text: "1. What is a good male-to-female ratio for a guppy breeding tank?", options: ["1 male to 1 female", "2-3 males to 1 female", "1 male to 2-3 females"], answer: 2 },
                { text: "2. The typical gestation period for guppies is:", options: ["7-10 days", "21-30 days", "60-90 days"], answer: 1 },
                { text: "3. Guppies lay eggs which then hatch into fry. (True/False)", options: ["True", "False"], answer: 1 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz5: {
            questions: [
                { text: "1. Why might a breeder use a breeding box or net?", options: ["To make the mother feel more comfortable.", "To protect newborn fry from being eaten by adult fish.", "To help fry grow faster."], answer: 1 },
                { text: "2. Which of the following is considered an excellent first food for guppy fry?", options: ["Whole adult flake food", "Newly hatched baby brine shrimp (BBS)", "Algae wafers"], answer: 1 },
                { text: "3. How often should you typically feed guppy fry?", options: ["Once a day", "Once a week", "Small amounts, 3-5 times a day"], answer: 2 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz6: {
            questions: [
                { text: "1. At what approximate age/size can guppy fry typically be moved to a larger grow-out tank?", options: ["1 week old / 1/4 inch", "4-6 weeks old / 1/2 - 3/4 inch", "3 months old / 1 inch"], answer: 1 },
                { text: "2. What is culling in fish breeding?", options: ["Feeding fish a special diet.", "Selectively removing fish that don't meet breeding goals.", "Separating males and females."], answer: 1 },
                { text: "3. Young male guppies can be identified by the development of their ________.", options: ["Larger eyes", "Gravid spot", "Gonopodium and brighter colors"], answer: 2 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz7: {
            questions: [
                { text: "1. What is a common reason for low guppy fry survival rates?", options: ["Fry are too independent.", "Adult fish eating the fry.", "Fry tanks are too large."], answer: 1 },
                { text: "2. If male guppies are constantly harassing females, what is a potential solution?", options: ["Add more males to the tank.", "Ensure a ratio of at least 2-3 females per male.", "Lower the water temperature significantly."], answer: 1 },
                { text: "3. True or False: Poor water quality can contribute to diseases and deformities in guppies.", options: ["True", "False"], answer: 0 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        },
        quiz8: {
            questions: [
                { text: "1. In genetics, a trait that requires two copies of a gene to be expressed is called:", options: ["Dominant", "Recessive", "Mutated"], answer: 1 },
                { text: "2. What is line breeding primarily used for in guppy breeding?", options: ["To increase the randomness of traits.", "To fix and stabilize desired traits in a population.", "To ensure all fry are female."], answer: 1 },
                { text: "3. Periodically breeding to an unrelated fish to introduce new genetic material is known as:", options: ["Inbreeding", "Outcrossing", "Culling"], answer: 1 }
            ],
            userAnswers: [null, null, null],
            completed: false,
            score: 0
        }
        // No quiz for conclusion section
    };

    if (isCoursePage) {
        const sections = document.querySelectorAll('#fullpage .section');
        const totalLessons = sections.length -1; // Exclude conclusion for quiz logic
        const progressBar = document.getElementById('progress-bar');
        const prevButton = document.getElementById('prevButton');
        const nextButton = document.getElementById('nextButton');

        fullPageInstance = new fullpage('#fullpage', {
            licenseKey: 'OPEN-SOURCE-GPLV3-LICENSE', // Replace with your actual key if you have one
            autoScrolling: true,
            fitToSection: true,
            fitToSectionDelay: 300,
            scrollOverflow: false, // Important: ensure content wrapper handles scroll
            scrollingSpeed: 700,
            touchSensitivity: 15, // Default is 5, higher means less sensitive
            keyboardScrolling: false, // Disable keyboard scrolling
            navigation: false, // Disable default navigation dots
            controlArrows: true, // Use default slide arrows for now, can be styled or replaced
            controlArrowsHTML: [
                '<div class="fp-arrow"></div>',
                '<div class="fp-arrow"></div>'
            ],
            anchors: ['lesson-1', 'lesson-2', 'lesson-3', 'lesson-4', 'lesson-5', 'lesson-6', 'lesson-7', 'lesson-8', 'conclusion'],
            menu: '#menu', // If you have a menu

            onLeave: function(origin, destination, direction) {
                updateProgressBar(destination.index, sections.length);
                updateNavigationButtons(destination.index, sections.length);
                // Ensure next button is disabled if landing on a section with an incomplete quiz
                if (destination.item.querySelector('.quiz-form')) {
                    const quizId = destination.item.querySelector('.quiz-form').id;
                    if (quizzes[quizId] && !quizzes[quizId].completed) {
                        nextButton.disabled = true;
                    } else {
                        nextButton.disabled = false;
                    }
                } else {
                     nextButton.disabled = false; // No quiz, enable next
                }

                // If leaving a section with a quiz, ensure it's completed before allowing "next" via button
                if (direction === 'down' && origin.item.querySelector('.quiz-form')) {
                    const originQuizId = origin.item.querySelector('.quiz-form').id;
                    if (quizzes[originQuizId] && !quizzes[originQuizId].completed) {
                        // This should ideally not happen if button logic is correct, but as a safeguard:
                        // fullpage_api.moveTo(origin.index + 1); // Stay on current section
                        // alert("Please complete the quiz before proceeding.");
                        // return false; // Prevent moving
                    }
                }
            },
            afterLoad: function(origin, destination, direction) {
                // Initial state for buttons and progress bar
                updateProgressBar(destination.index, sections.length);
                updateNavigationButtons(destination.index, sections.length);

                // Check quiz status on load for the current section
                if (destination.item.querySelector('.quiz-form')) {
                    const quizId = destination.item.querySelector('.quiz-form').id;
                     if (quizzes[quizId] && !quizzes[quizId].completed) {
                        nextButton.disabled = true;
                    } else {
                        nextButton.disabled = destination.index === sections.length - 1; // Disable on last section
                    }
                } else {
                     nextButton.disabled = destination.index === sections.length - 1; // Disable on last section (conclusion)
                }
                 prevButton.disabled = destination.index === 0; // Disable on first section
            },
             // Required for scrollOverflow: false with internal scrolling elements
            normalScrollElements: '.content-wrapper',
            // Try to prevent body scroll on touch devices for scrollable elements
            normalScrollElementTouchThreshold: 5,
        });

        function updateProgressBar(currentIndex, totalSections) {
            const progress = ((currentIndex + 1) / totalSections) * 100;
            progressBar.style.width = progress + '%';
        }

        function updateNavigationButtons(currentIndex, totalSections) {
            prevButton.disabled = currentIndex === 0;
            nextButton.disabled = currentIndex === totalSections - 1; // Initially disable if on last section

            // Re-check quiz status for next button enablement
            const currentSection = sections[currentIndex];
            if (currentSection.querySelector('.quiz-form')) {
                const quizId = currentSection.querySelector('.quiz-form').id;
                if (quizzes[quizId] && !quizzes[quizId].completed) {
                    nextButton.disabled = true;
                } else if (currentIndex === totalSections - 1) {
                    nextButton.disabled = true; // Still disable if on last section
                }
                 else {
                    nextButton.disabled = false;
                }
            }
        }

        // Custom Navigation Button Event Listeners
        prevButton.addEventListener('click', function() {
            if (fullPageInstance) {
                fullpage_api.moveSectionUp();
            }
        });

        nextButton.addEventListener('click', function() {
            if (fullPageInstance) {
                const currentSection = fullpage_api.getActiveSection().item;
                const quizForm = currentSection.querySelector('.quiz-form');

                if (quizForm) {
                    const quizId = quizForm.id;
                    if (quizzes[quizId] && quizzes[quizId].completed) {
                        fullpage_api.moveSectionDown();
                    } else if (quizzes[quizId] && !quizzes[quizId].completed){
                        // This case implies the button should have been disabled.
                        // Or, if it's a "skip quiz" scenario (not implemented), handle here.
                        alert("Please complete the quiz to proceed.");
                    } else {
                        // No quiz or quiz already handled, just move
                         fullpage_api.moveSectionDown();
                    }
                } else {
                    // No quiz in this section, just move
                    fullpage_api.moveSectionDown();
                }
            }
        });

        // Quiz Logic
        document.querySelectorAll('.submit-quiz-button').forEach(button => {
            button.addEventListener('click', function() {
                const quizId = this.dataset.quizid;
                const quizData = quizzes[quizId];
                const form = document.getElementById(quizId);
                const feedbackDiv = document.getElementById('feedback-' + quizId);
                let score = 0;
                let allAnswered = true;

                quizData.questions.forEach((question, index) => {
                    const qName = "q" + (index + 1);
                    const selectedOption = form.querySelector(`input[name="${qName}"]:checked`);

                    if (selectedOption) {
                        quizData.userAnswers[index] = parseInt(selectedOption.value, 10); // Assuming value is option index or specific value
                        // For True/False, let's assume 'a' is true (index 0), 'b' is false (index 1)
                        // For multiple choice, option values should be 0, 1, 2...
                        let answerValue = selectedOption.value;
                        if(answerValue === 'a') answerValue = 0;
                        else if(answerValue === 'b') answerValue = 1;
                        else if(answerValue === 'c') answerValue = 2;
                        // Add more if you have more than 3 options consistently

                        if (parseInt(answerValue) === question.answer) {
                            score++;
                        }
                    } else {
                        allAnswered = false;
                    }
                });

                if (!allAnswered) {
                    feedbackDiv.textContent = "Please answer all questions before submitting.";
                    feedbackDiv.className = 'quiz-feedback incorrect';
                    return;
                }

                quizData.score = score;
                quizData.completed = true; // Mark quiz as completed

                const totalQuestions = quizData.questions.length;
                feedbackDiv.innerHTML = `You scored ${score} out of ${totalQuestions}.<br>`;
                if (score === totalQuestions) {
                    feedbackDiv.innerHTML += "Excellent! All correct!";
                    feedbackDiv.className = 'quiz-feedback correct';
                } else {
                    feedbackDiv.innerHTML += "Review the material if you're unsure about any answers.";
                    feedbackDiv.className = 'quiz-feedback incorrect'; // Or neutral
                }

                // Disable radio buttons after submission
                form.querySelectorAll('input[type="radio"]').forEach(radio => radio.disabled = true);
                this.disabled = true; // Disable submit button
                this.textContent = "Submitted";

                // Enable the "Next Lesson" button for the current section
                const currentSectionIndex = fullpage_api.getActiveSection().index;
                if (currentSectionIndex < sections.length - 1) { // Not the last section
                    nextButton.disabled = false;
                }
            });
        });

        // Initialize buttons and progress bar for the first section
        if (sections.length > 0) {
            // Wait for fullPage.js to be fully initialized
            setTimeout(() => {
                 if (fullPageInstance && typeof fullpage_api !== 'undefined' && fullpage_api.getActiveSection) {
                    const initialSectionIndex = fullpage_api.getActiveSection() ? fullpage_api.getActiveSection().index : 0;
                    updateProgressBar(initialSectionIndex, sections.length);
                    updateNavigationButtons(initialSectionIndex, sections.length);
                    // Check initial quiz state for the first section
                    const firstSection = sections[initialSectionIndex];
                    if (firstSection && firstSection.querySelector('.quiz-form')) {
                        const quizId = firstSection.querySelector('.quiz-form').id;
                        if (quizzes[quizId] && !quizzes[quizId].completed) {
                            nextButton.disabled = true;
                        }
                    }
                } else {
                    // Fallback if fullpage_api is not ready, though unusual with DOMContentLoaded
                    updateProgressBar(0, sections.length);
                    updateNavigationButtons(0, sections.length);
                    if (sections[0] && sections[0].querySelector('.quiz-form')) {
                         nextButton.disabled = true; // Assume quiz incomplete initially
                    }
                }
            }, 100); // Small delay to ensure fullPage.js is ready
        }
    } // end if(isCoursePage)
});
