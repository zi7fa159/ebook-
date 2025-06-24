document.addEventListener('DOMContentLoaded', function () {
    const fullPageInstance = new fullpage('#fullpage', {
        // Navigation
        licenseKey: 'OPEN-SOURCE-GPLV3-LICENSE', // Replace with your key if you have one
        anchors: ['lesson0', 'lesson1', 'lesson2', 'lesson3', 'lesson4', 'lesson5', 'lesson6', 'lesson7', 'lesson8', 'lesson9'],
        navigation: false, // We use custom navigation, so disable default dots

        // Scrolling
        autoScrolling: true,
        fitToSection: true,
        scrollBar: false, // No browser scrollbar
        scrollingSpeed: 700,

        // Accessibility
        keyboardScrolling: false, // Disable keyboard scrolling, we use buttons

        // Design
        controlArrows: false, // Disable default arrows if any

        // Callbacks
        afterLoad: function(origin, destination, direction){
            updateNavigationButtons(destination);
            updateProgressBar(destination);
        },
        onLeave: function(origin, destination, direction){
            // You could add effects here if needed
        }
    });

    // Disable scroll and touch initially after fullPage.js is ready
    // Using methods directly on the instance if available, or the global API
    if (typeof fullpage_api !== 'undefined') {
        fullpage_api.setAllowScrolling(false, 'all');
        fullpage_api.setKeyboardScrolling(false, 'all');
        // For touch, fullPage.js's internal handling is usually managed by autoScrolling and fitToSection
        // but if more aggressive blocking is needed, you might explore options like normalScrollElements.
        // However, for button-only navigation, the above should suffice.
    }


    const prevButton = document.getElementById('prevLesson');
    const nextButton = document.getElementById('nextLesson');
    const progressBar = document.getElementById('progressBar');
    const totalSections = document.querySelectorAll('#fullpage .section').length;

    function updateNavigationButtons(destination) {
        if (destination.isFirst) {
            prevButton.disabled = true;
        } else {
            prevButton.disabled = false;
        }

        if (destination.isLast) {
            nextButton.disabled = true;
            // Optionally change text: nextButton.textContent = 'Finish Course';
        } else {
            nextButton.disabled = false;
            // Optionally change text back: nextButton.textContent = 'Next Lesson';
        }
    }

    function updateProgressBar(destination) {
        const currentSectionIndex = destination.index; // 0-based index
        const progressPercentage = ((currentSectionIndex + 1) / totalSections) * 100;
        if (progressBar) {
            progressBar.style.width = progressPercentage + '%';
        }
    }

    // Event Listeners for custom navigation
    if (prevButton) {
        prevButton.addEventListener('click', function() {
            if (typeof fullpage_api !== 'undefined') {
                fullpage_api.moveSectionUp();
            }
        });
    }

    if (nextButton) {
        nextButton.addEventListener('click', function() {
            if (typeof fullpage_api !== 'undefined') {
                fullpage_api.moveSectionDown();
            }
        });
    }

    // Initial state update for buttons and progress bar (in case starting not on first section due to anchor)
    // This is typically handled by afterLoad on initial load as well if an anchor is present.
    // However, to be safe, especially if no anchor is in URL:
    if (typeof fullpage_api !== 'undefined' && fullpage_api.getActiveSection) {
        const currentSection = fullpage_api.getActiveSection();
        if(currentSection) {
             updateNavigationButtons(currentSection);
             updateProgressBar(currentSection);
        }
    } else if (totalSections > 0) { // Fallback if API not ready or no active section initially
        // Assume starting at the first section if API isn't ready for getActiveSection
        // This part might be redundant if afterLoad fires correctly on init
        prevButton.disabled = true;
        nextButton.disabled = totalSections === 1;
        if (progressBar) progressBar.style.width = (1 / totalSections) * 100 + '%';
    }
});
