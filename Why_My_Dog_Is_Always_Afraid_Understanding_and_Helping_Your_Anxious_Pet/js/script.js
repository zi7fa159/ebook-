document.addEventListener('DOMContentLoaded', function () {
    const fullPageContainer = document.getElementById('fullpage');
    const progressBar = document.getElementById('progress-bar');
    const prevButton = document.getElementById('prev-lesson');
    const nextButton = document.getElementById('next-lesson');

    // Define course content structure (lessons and quizzes)
    // In a real application, this might be fetched from a JSON file or API
    const courseData = [
        {
            anchor: 'introduction',
            title: 'Course Introduction',
            file: 'content_source/00-course-introduction.md',
            quiz: null // No quiz for introduction
        },
        {
            anchor: 'lesson1',
            title: 'Lesson 1: Recognizing Fear vs. Anxiety',
            file: 'content_source/01-recognizing-fear-vs-anxiety.md',
            quiz: {
                questions: [
                    {
                        text: "Which of the following best describes FEAR in dogs?",
                        options: [
                            { text: "A persistent state of worry about unknown future events.", value: "A" },
                            { text: "An adaptive emotional response to a specific, immediate threat.", value: "B" },
                            { text: "A behavior learned through excessive punishment.", value: "C" },
                            { text: "Always a sign of a poorly socialized dog.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Fear is a natural and often helpful reaction to something specific and currently happening that the dog perceives as dangerous."
                    },
                    {
                        text: "A dog that starts panting, pacing, and whining 30 minutes before their owner usually leaves for work, even with no other departure cues, is most likely exhibiting:",
                        options: [
                            { text: "Fear of the front door.", value: "A" },
                            { text: "Anxiety about being left alone.", value: "B" },
                            { text: "Excitement for a walk.", value: "C" },
                            { text: "A learned begging behavior.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "This describes an anticipation of a stressful event (being left alone), which is characteristic of anxiety, specifically separation anxiety."
                    },
                    {
                        text: "\"Whale eye\" (showing the whites of the eyes) is typically a sign of:",
                        options: [
                            { text: "Dominance.", value: "A" },
                            { text: "Contentment and relaxation.", value: "B" },
                            { text: "Fear or stress.", value: "C" },
                            { text: "Playfulness.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "\"Whale eye\" is a common canine body language signal indicating that a dog is feeling stressed, anxious, or fearful about a situation."
                    },
                    {
                        text: "True or False: Anxiety in dogs is always triggered by a clearly identifiable, present danger.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "Anxiety is often characterized by a lack of a clear, immediate trigger, or it's an anticipation of a future threat. Fear is typically linked to a present danger."
                    }
                ]
            }
        },
        {
            anchor: 'lesson2',
            title: 'Lesson 2: Common Triggers and Causes',
            file: 'content_source/02-common-triggers-causes.md',
            quiz: {
                questions: [
                    {
                        text: "Which of the following is NOT considered a common *trigger* for acute fear or anxiety episodes in dogs?",
                        options: [
                            { text: "Thunderstorms", value: "A" },
                            { text: "A consistent, balanced daily diet", value: "B" },
                            { text: "Unfamiliar visitors to the home", value: "C" },
                            { text: "Car rides for a dog with past motion sickness", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "While diet can be a *contributing factor* to overall well-being, a consistent, balanced diet itself is not an acute *trigger* for a fear episode."
                    },
                    {
                        text: "The critical socialization period for puppies, crucial for preventing adult fearfulness, is generally considered to be:",
                        options: [
                            { text: "The first week of life.", value: "A" },
                            { text: "3 to 16 weeks of age.", value: "B" },
                            { text: "6 months to 1 year of age.", value: "C" },
                            { text: "Only after they have all their vaccinations.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "This is widely recognized as the most critical period for puppies to have varied, positive exposures."
                    },
                    {
                        text: "True or False: A dog's anxiety can sometimes be linked to an undiagnosed medical condition or chronic pain.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "A",
                        feedback: "Pain and certain medical issues can significantly impact a dog's behavior. A vet check is essential."
                    },
                    {
                        text: "If an owner consistently tries to soothe their dog with excessive coddling *during* a fearful episode, this might unintentionally:",
                        options: [
                            { text: "Immediately cure the dog's fear.", value: "A" },
                            { text: "Teach the dog that the trigger is very dangerous.", value: "B" },
                            { text: "Reinforce the fearful behavior by rewarding it with attention.", value: "C" },
                            { text: "Have no impact on the dog's fear.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "Excessive soothing during fear can be interpreted as a reward for acting fearful. It's better to act calm and confident."
                    }
                ]
            }
        },
        {
            anchor: 'lesson3',
            title: 'Lesson 3: The Anxious Dog\'s Brain',
            file: 'content_source/03-anxious-dog-brain.md',
            quiz: {
                questions: [
                    {
                        text: "What is the primary fear processing center in the dog's brain?",
                        options: [
                            { text: "The Prefrontal Cortex", value: "A" },
                            { text: "The Cerebellum", value: "B" },
                            { text: "The Amygdala", value: "C" },
                            { text: "The Hippocampus", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "The amygdala is the key structure in the brain responsible for detecting threats and initiating the fear response."
                    },
                    {
                        text: "Which hormone is known as the \"stress hormone\" and can have negative long-term effects if chronically elevated?",
                        options: [
                            { text: "Serotonin", value: "A" },
                            { text: "Dopamine", value: "B" },
                            { text: "Adrenaline", value: "C" },
                            { text: "Cortisol", value: "D" }
                        ],
                        correctAnswer: "D",
                        feedback: "Cortisol is released for a more sustained period during stress and chronic elevation can lead to health and behavioral issues."
                    },
                    {
                        text: "When a dog is experiencing an \"amygdala hijack,\" what typically happens to their ability to learn or respond to commands?",
                        options:
                            [
                                { text: "It improves significantly as they are more alert.", value: "A" },
                                { text: "It is greatly diminished as the \"emotional brain\" takes over.", value: "B" },
                                { text: "It remains unchanged.", value: "C" },
                                { text: "They become more eager to please.", value: "D" }
                            ],
                        correctAnswer: "B",
                        feedback: "During an amygdala hijack, the dog is in survival mode, and the \"thinking brain\" is largely offline."
                    },
                    {
                        text: "True or False: Trying to train a dog or teach them new behaviors when they are highly fearful and over their stress threshold is generally very effective.",
                        options:
                            [
                                { text: "True", value: "A" },
                                { text: "False", value: "B" }
                            ],
                        correctAnswer: "B",
                        feedback: "When a dog is over their stress threshold, their capacity for learning is severely impaired. Training should occur \"below threshold.\""
                    }
                ]
            }
        },
        {
            anchor: 'lesson4',
            title: 'Lesson 4: Creating a Safe Haven',
            file: 'content_source/04-creating-safe-haven.md',
            quiz: {
                questions: [
                    {
                        text: "What is a primary goal of creating a \"safe den\" for an anxious dog?",
                        options: [
                            { text: "To teach the dog to be more independent and ignore the owner.", value: "A" },
                            { text: "To provide a secure place where the dog can retreat and feel safe when overwhelmed.", value: "B" },
                            { text: "To use as a time-out area when the dog misbehaves.", value: "C" },
                            { text: "To keep the dog confined and out of the way at all times.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "A safe den is a sanctuary, a place of security and comfort that the dog chooses to go to."
                    },
                    {
                        text: "If a dog is highly reactive to people walking past the front window, which environmental management strategy would be most appropriate?",
                        options: [
                            { text: "Leaving the window wide open to help the dog \"get used to it.\"", value: "A" },
                            { text: "Using blinds, curtains, or frosted window film to block the view.", value: "B" },
                            { text: "Taking the dog to the window and yelling \"No!\" each time they bark.", value: "C" },
                            { text: "Installing a louder doorbell to distract the dog.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Blocking visual access to triggers is a key environmental management technique to reduce arousal and reactivity."
                    },
                    {
                        text: "True or False: When managing greetings with visitors for an anxious dog, it's best to have guests immediately try to pet and comfort the dog to show they are friendly.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "Forcing interaction can increase a dog's anxiety. It's better to have guests ignore the dog initially."
                    },
                    {
                        text: "Which of the following is an example of using environmental management to reduce exposure to a known trigger?",
                        options: [
                            { text: "Forcing a dog terrified of thunder to stay outside during a storm.", value: "A" },
                            { text: "Putting a dog in a quiet room with a chew toy before vacuuming if they fear the vacuum.", value: "B" },
                            { text: "Taking a dog fearful of other dogs to a crowded dog park daily.", value: "C" },
                            { text: "Ignoring a dog's fear of car rides and taking them on long trips frequently.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "This is a proactive step to prevent the dog from experiencing the full intensity of a known fear trigger."
                    }
                ]
            }
        },
        {
            anchor: 'lesson5',
            title: 'Lesson 5: Basic Behavior Modification',
            file: 'content_source/05-behavior-modification.md',
            quiz: {
                questions: [
                    {
                        text: "What is the primary goal of Desensitization and Counter-Conditioning (DSCC)?",
                        options: [
                            { text: "To force the dog to confront their fears at full intensity.", value: "A" },
                            { text: "To punish the dog whenever they show signs of fear.", value: "B" },
                            { text: "To change the dog's underlying emotional response to a trigger from negative to positive or neutral.", value: "C" },
                            { text: "To quickly teach the dog to ignore all environmental stimuli.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "DSCC is about changing feelings, not just suppressing behaviors. The aim is for the dog to no longer feel scared by the trigger."
                    },
                    {
                        text: "In Counter-Conditioning, what is the correct order of events?",
                        options: [
                            { text: "High-value treat is given, then the trigger appears.", value: "A" },
                            { text: "The trigger appears, then a punishment is given if the dog reacts.", value: "B" },
                            { text: "The trigger appears, then a high-value treat is given immediately.", value: "C" },
                            { text: "The trigger and the treat are presented at the exact same time.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "This timing is crucial. The trigger needs to become a reliable predictor that something wonderful (the treat) is about to happen."
                    },
                    {
                        text: "\"Flooding\" in dog behavior modification refers to:",
                        options: [
                            { text: "Giving the dog too many treats.", value: "A" },
                            { text: "Exposing the dog to a fear trigger at an intensity that overwhelms them.", value: "B" },
                            { text: "Gradually increasing exposure to a trigger while the dog remains calm.", value: "C" },
                            { text: "A type of water therapy for anxious dogs.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Flooding is counterproductive and can worsen fear and anxiety. DSCC focuses on gradual, non-stressful exposure."
                    },
                    {
                        text: "True or False: When doing DSCC, it's important to always work with the dog \"below threshold.\"",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "A",
                        feedback: "Staying below threshold is fundamental to successful and humane DSCC. If the dog is scared, they are not in a state to learn a new, positive association."
                    }
                ]
            }
        },
        {
            anchor: 'lesson6',
            title: 'Lesson 6: Calming Techniques and Tools',
            file: 'content_source/06-calming-techniques-tools.md',
            quiz: {
                questions: [
                    {
                        text: "Dog Appeasing Pheromone (DAP), as found in products like Adaptil, is designed to mimic:",
                        options: [
                            { text: "The scent of a dog's favorite food.", value: "A" },
                            { text: "Pheromones released by a mother dog to comfort her puppies.", value: "B" },
                            { text: "A territorial marking scent to deter other animals.", value: "C" },
                            { text: "An alarm pheromone to warn of danger.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "DAP is a synthetic version of this maternal pheromone, intended to provide a sense of security."
                    },
                    {
                        text: "What is the proposed mechanism by which anxiety wraps (e.g., Thundershirts) might help calm a dog?",
                        options: [
                            { text: "By restricting movement so the dog cannot panic.", value: "A" },
                            { text: "By applying gentle, constant pressure, similar to swaddling.", value: "B" },
                            { text: "By emitting a high-frequency sound only dogs can hear.", value: "C" },
                            { text: "By making the dog too warm, inducing lethargy.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "The maintained pressure is thought to have a calming effect on the nervous system for many dogs."
                    },
                    {
                        text: "When considering nutritional supplements for calming your dog, what is the most important first step?",
                        options: [
                            { text: "Ordering the cheapest option online.", value: "A" },
                            { text: "Asking for recommendations on social media.", value: "B" },
                            { text: "Consulting with your veterinarian.", value: "C" },
                            { text: "Doubling the recommended dosage for faster results.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "This is crucial to ensure safety, rule out underlying medical issues, and get professional advice."
                    },
                    {
                        text: "True or False: Most calming tools are standalone cures for severe anxiety and don't require behavior modification.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "These tools are best used as adjunctive aids alongside a comprehensive plan. They are rarely standalone cures."
                    }
                ]
            }
        },
        {
            anchor: 'lesson7',
            title: 'Lesson 7: Exercise, Diet, and Routine',
            file: 'content_source/07-exercise-diet-routine.md',
            quiz: {
                questions: [
                    {
                        text: "Which type of walk is often particularly beneficial for an anxious dog's mental well-being?",
                        options: [
                            { text: "A very fast-paced run through a crowded city street.", value: "A" },
                            { text: "A short, controlled walk focused heavily on obedience drills.", value: "B" },
                            { text: "A \"sniffari\" where the dog is allowed ample time to explore scents.", value: "C" },
                            { text: "A walk where the dog is constantly corrected for pulling.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "Sniffing is a naturally calming and mentally enriching activity for dogs."
                    },
                    {
                        text: "The \"gut-brain axis\" refers to the connection between:",
                        options: [
                            { text: "The dog's stomach size and their intelligence.", value: "A" },
                            { text: "The quality of dog food and its price.", value: "B" },
                            { text: "Gut health (microbiome) and brain function, including mood and anxiety.", value: "C" },
                            { text: "How quickly a dog eats and how fast they can run.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "Emerging research highlights the significant impact of the gut microbiome on neurological and psychological health."
                    },
                    {
                        text: "Why is a predictable daily routine generally beneficial for anxious dogs?",
                        options: [
                            { text: "It teaches them to be more demanding of their owner's attention.", value: "A" },
                            { text: "It reduces uncertainty and provides a sense of security.", value: "B" },
                            { text: "It makes them less adaptable to any changes in the future.", value: "C" },
                            { text: "It eliminates the need for any physical exercise.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Knowing what to expect helps anxious dogs feel safer and more in control."
                    },
                    {
                        text: "True or False: For anxious dogs, only extreme physical exhaustion matters in exercise, not the environment.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "The quality and context of exercise matter greatly. Stressful exercise can be counterproductive."
                    }
                ]
            }
        },
        {
            anchor: 'lesson8',
            title: 'Lesson 8: When and How to Seek Professional Help',
            file: 'content_source/08-when-how-seek-professional-help.md',
            quiz: {
                questions: [
                    {
                        text: "If your dog suddenly develops severe anxiety, who is the recommended first professional to consult?",
                        options: [
                            { text: "A local dog groomer.", value: "A" },
                            { text: "Your primary care veterinarian (DVM).", value: "B" },
                            { text: "An online dog training forum.", value: "C" },
                            { text: "A friend who owns multiple dogs.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "A veterinarian can rule out underlying medical conditions causing or contributing to anxiety."
                    },
                    {
                        text: "Which professional is a veterinarian with specialized board certification in animal behavior?",
                        options: [
                            { text: "Certified Professional Dog Trainer (CPDT-KA)", value: "A" },
                            { text: "Certified Applied Animal Behaviorist (CAAB)", value: "B" },
                            { text: "Veterinary Behaviorist (e.g., DACVB)", value: "C" },
                            { text: "Animal Control Officer", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "Veterinary behaviorists are top-tier specialists for complex animal behavior problems."
                    },
                    {
                        text: "When seeking a dog trainer for fear/anxiety, it is crucial they use:",
                        options: [
                            { text: "Dominance-based methods.", value: "A" },
                            { text: "Punishment to stop unwanted behaviors.", value: "B" },
                            { text: "Positive reinforcement, force-free methods and have experience with anxiety.", value: "C" },
                            { text: "The cheapest methods available.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "Aversive methods can worsen fear. Positive, science-based approaches are essential."
                    },
                    {
                        text: "True or False: It is generally safe to self-treat serious aggression in dogs without professional help.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "Aggression is a serious safety concern. Always seek expert help."
                    }
                ]
            }
        },
        {
            anchor: 'lesson9',
            title: 'Lesson 9: Building Confidence',
            file: 'content_source/09-building-confidence.md',
            quiz: {
                questions: [
                    {
                        text: "How does positive reinforcement (+R) training primarily help build a dog's confidence?",
                        options: [
                            { text: "By showing the dog who is dominant.", value: "A" },
                            { text: "By allowing the dog to experience success and learn they can influence outcomes positively.", value: "B" },
                            { text: "By using loud verbal corrections.", value: "C" },
                            { text: "By keeping the dog slightly hungry.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Positive reinforcement focuses on rewarding success, which builds a sense of accomplishment and control."
                    },
                    {
                        text: "Which is an example of cognitive/occupational enrichment for a dog?",
                        options: [
                            { text: "Leaving the TV on when the dog is alone.", value: "A" },
                            { text: "A food-dispensing puzzle toy.", value: "B" },
                            { text: "A short leash walk with no sniffing allowed.", value: "C" },
                            { text: "Yelling at the dog for barking.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Puzzle toys require dogs to think and problem-solve, providing mental stimulation."
                    },
                    {
                        text: "\"Nose work\" or scent games are good for confidence because they:",
                        options: [
                            { text: "Require the dog to be physically very strong.", value: "A" },
                            { text: "Are very difficult for most dogs to learn.", value: "B" },
                            { text: "Tap into a dog's natural abilities and allow them to succeed independently.", value: "C" },
                            { text: "Primarily teach the dog to be more aggressive.", value: "D" }
                        ],
                        correctAnswer: "C",
                        feedback: "Using their sense of smell is inherently rewarding and allows dogs to solve problems."
                    },
                    {
                        text: "True or False: To build confidence, constantly push anxious dogs into very scary situations.",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "Pushing anxious dogs into overwhelming situations (flooding) can increase fear. Confidence is built through positive experiences."
                    }
                ]
            }
        },
        {
            anchor: 'lesson10',
            title: 'Lesson 10: Living with Long-Term Anxiety',
            file: 'content_source/10-living-with-long-term-anxiety.md',
            quiz: { // Quiz for lesson 10
                questions: [
                    {
                        text: "When managing long-term anxiety in a dog, what is a more realistic expectation than a complete \"cure\"?",
                        options: [
                            { text: "The dog will never again experience any form of fear.", value: "A" },
                            { text: "Significant improvement in quality of life and ability to cope, with ongoing management.", value: "B" },
                            { text: "The dog will no longer need any special routines or environmental considerations.", value: "C" },
                            { text: "All anxious behaviors will vanish within a few weeks of starting a plan.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "For many dogs with chronic anxiety, the goal is to reduce the severity and frequency of anxious episodes and improve their overall well-being through consistent, long-term strategies."
                    },
                    {
                        text: "Which of the following is LEAST likely to be a component of successful long-term anxiety management?",
                        options: [
                            { text: "Regular veterinary check-ups and medication management (if applicable).", value: "A" },
                            { text: "Sporadically applying behavior modification techniques only when problems become severe.", value: "B" },
                            { text: "Continued environmental management and maintaining predictable routines.", value: "C" },
                            { text: "Advocating for the dog's needs in various situations.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Consistency is key in long-term management. Waiting for problems to become severe before intervening is less effective than ongoing, proactive management."
                    },
                    {
                        text: "Why is it important for owners of anxious dogs to also prioritize their own well-being?",
                        options: [
                            { text: "Because the dog's anxiety is usually the owner's fault.", value: "A" },
                            { text: "Because living with an anxious dog can be emotionally challenging, and owner stress can affect the dog.", value: "B" },
                            { text: "So the owner can spend less time with the dog.", value: "C" },
                            { text: "It's not important; only the dog's well-being matters.", value: "D" }
                        ],
                        correctAnswer: "B",
                        feedback: "Owner stress and frustration can be picked up by sensitive dogs. Taking care of your own emotional health allows you to be a more patient and effective support for your dog."
                    },
                    {
                        text: "True or False: If a dog has shown improvement with their anxiety, it means all management strategies can be stopped, as they are \"cured.\"",
                        options: [
                            { text: "True", value: "A" },
                            { text: "False", value: "B" }
                        ],
                        correctAnswer: "B",
                        feedback: "Improvement is wonderful, but for many dogs with long-term anxiety, some level of ongoing management is often necessary to maintain that improvement and prevent regression."
                    }
                ]
            }
        },
        {
            anchor: 'conclusion',
            title: 'Course Conclusion',
            file: 'content_source/11-conclusion.md',
            quiz: null // No quiz for conclusion
        }
    ];

    let currentQuizData = null;
    let quizAttempted = false;
    let quizCorrectAnswers = 0;
    let totalQuizQuestions = 0;

    // Basic Markdown to HTML converter (simplified)
    function markdownToHtml(markdown) {
        // Replace headings
        markdown = markdown.replace(/^# (.*$)/gim, '<h1>$1</h1>');
        markdown = markdown.replace(/^## (.*$)/gim, '<h2>$1</h2>');
        markdown = markdown.replace(/^### (.*$)/gim, '<h3>$1</h3>');
        // Replace bold
        markdown = markdown.replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>');
        markdown = markdown.replace(/__(.*)__/gim, '<strong>$1</strong>');
        // Replace italic
        markdown = markdown.replace(/\*(.*)\*/gim, '<em>$1</em>');
        markdown = markdown.replace(/_(.*)_/gim, '<em>$1</em>');
        // Replace list items
        markdown = markdown.replace(/^\s*[\-\*] (.*$)/gim, '<li>$1</li>');
        markdown = markdown.replace(/<\/li>\n<li>/gim, '</li><li>'); // Fix consecutive list items
        markdown = markdown.replace(/^(<li>.*<\/li>)$/gim, '<ul>$1</ul>'); // Wrap in ul if it's a list block
        // Handle multiple list blocks
        markdown = markdown.replace(/<\/ul>\s*<ul>/gim, '');

        // Replace paragraphs (any line not starting with a special char or tag)
        markdown = markdown.split('\n').map(line => {
            if (line.trim() === '' || line.startsWith('<') || line.startsWith('#') || line.startsWith('---')) {
                return line;
            }
            return `<p>${line}</p>`;
        }).join('\n');

        // Clean up empty paragraphs that might result from split/join
        markdown = markdown.replace(/<p>\s*<\/p>/gim, '');
        // Remove ---
        markdown = markdown.replace(/^---$/gim, '<hr class="quiz-divider">');

        // Image placeholder handling (basic)
        markdown = markdown.replace(/!\[(.*?)\]\((images\/.*?)\)/gim, '<div class="content-image-placeholder">Image: $1 ($2) - Placeholder</div>');


        return markdown;
    }

    async function fetchAndConvertMarkdown(filePath) {
        try {
            const response = await fetch(filePath);
            if (!response.ok) {
                throw new Error(`Failed to fetch ${filePath}: ${response.statusText}`);
            }
            const markdown = await response.text();
            // Split content and quiz based on "---" separator specifically for quiz
            const parts = markdown.split('--- \n**Quiz'); // Ensure specific split
            let contentHtml = markdownToHtml(parts[0]);

            if (parts.length > 1 && filePath.includes('lesson')) { // Quiz part exists
                // The quiz questions are now in courseData, so we don't parse them from MD here
                // We just need a placeholder for the quiz container
                contentHtml += `<div id="quiz-section-${filePath.split('/').pop().split('.')[0]}" class="quiz-container"></div>`;
            }
            return contentHtml;
        } catch (error) {
            console.error("Error fetching or converting markdown:", error);
            return `<p>Error loading content for this lesson. Please try refreshing. Details: ${error.message}</p>`;
        }
    }


    async function loadSections() {
        for (let i = 0; i < courseData.length; i++) {
            const lesson = courseData[i];
            const sectionDiv = document.createElement('div');
            sectionDiv.className = 'section';
            sectionDiv.setAttribute('data-anchor', lesson.anchor);

            const contentHtml = await fetchAndConvertMarkdown(lesson.file);
            sectionDiv.innerHTML = contentHtml;
            fullPageContainer.appendChild(sectionDiv);
        }
        initializeFullPage(); // Call this after all sections are added
    }

    function renderQuiz(quizData, sectionAnchor) {
        const quizContainerId = `quiz-section-${sectionAnchor}`;
        const quizContainer = document.getElementById(quizContainerId);

        if (!quizContainer || !quizData || !quizData.questions) {
            console.log("No quiz for this section or container not found:", sectionAnchor);
            if (nextButton) nextButton.disabled = false; // Enable next if no quiz
            quizAttempted = true; // Consider it "attempted" if no quiz
            return;
        }

        currentQuizData = quizData;
        quizAttempted = false;
        quizCorrectAnswers = 0;
        totalQuizQuestions = quizData.questions.length;

        let quizHtml = `<h3>Lesson Quiz</h3>`;
        quizData.questions.forEach((q, index) => {
            quizHtml += `
                <div class="quiz-question" id="question-${sectionAnchor}-${index}">
                    <p>${index + 1}. ${q.text}</p>
                    <div class="quiz-options">
            `;
            q.options.forEach(opt => {
                quizHtml += `
                    <label>
                        <input type="radio" name="question-${sectionAnchor}-${index}" value="${opt.value}">
                        ${opt.text}
                    </label>
                `;
            });
            quizHtml += `</div><div class="quiz-feedback" id="feedback-${sectionAnchor}-${index}"></div></div>`;
        });
        quizHtml += `<button id="submit-quiz-button-${sectionAnchor}" class="submit-quiz-btn">Submit Quiz</button>`;
        quizContainer.innerHTML = quizHtml;

        const submitButton = document.getElementById(`submit-quiz-button-${sectionAnchor}`);
        if (submitButton) {
            submitButton.addEventListener('click', () => handleQuizSubmit(sectionAnchor));
        }
        if (nextButton) nextButton.disabled = true; // Disable next until quiz is submitted
    }

    function handleQuizSubmit(sectionAnchor) {
        if (!currentQuizData) return;
        quizCorrectAnswers = 0;

        currentQuizData.questions.forEach((q, index) => {
            const questionContainer = document.getElementById(`question-${sectionAnchor}-${index}`);
            const feedbackEl = document.getElementById(`feedback-${sectionAnchor}-${index}`);
            const selectedOption = questionContainer.querySelector(`input[name="question-${sectionAnchor}-${index}"]:checked`);

            if (selectedOption) {
                if (selectedOption.value === q.correctAnswer) {
                    quizCorrectAnswers++;
                    feedbackEl.textContent = `Correct! ${q.feedback || ''}`;
                    feedbackEl.className = 'quiz-feedback correct';
                } else {
                    feedbackEl.textContent = `Incorrect. The correct concept: ${q.feedback || 'Review the lesson material.'}`;
                    feedbackEl.className = 'quiz-feedback incorrect';
                }
            } else {
                feedbackEl.textContent = "Please select an answer.";
                feedbackEl.className = 'quiz-feedback incorrect';
            }
        });

        quizAttempted = true;
        if (nextButton) nextButton.disabled = false; // Enable next button after quiz attempt
        const submitButton = document.getElementById(`submit-quiz-button-${sectionAnchor}`);
        if (submitButton) submitButton.disabled = true; // Disable submit after one attempt

        // Optional: Display overall score
        const quizContainerId = `quiz-section-${sectionAnchor}`;
        const quizContainer = document.getElementById(quizContainerId);
        let scoreDisplay = quizContainer.querySelector('.quiz-score');
        if (!scoreDisplay) {
            scoreDisplay = document.createElement('p');
            scoreDisplay.className = 'quiz-score';
            quizContainer.appendChild(scoreDisplay);
        }
        scoreDisplay.textContent = `You got ${quizCorrectAnswers} out of ${totalQuizQuestions} correct.`;

    }


    function updateProgressBar(sectionIndex, sectionsAmount) {
        const progress = sectionIndex > 0 ? ((sectionIndex) / (sectionsAmount - 1)) * 100 : 0;
        progressBar.style.width = progress + '%';
    }

    function updateNavigationButtons(destination) {
        // Handle prevButton
        if (destination.isFirst) {
            prevButton.disabled = true;
        } else {
            prevButton.disabled = false;
        }

        // Handle nextButton
        if (destination.isLast) {
            nextButton.disabled = true;
        } else {
            // Check if there's a quiz for the current section
            const currentLessonData = courseData[destination.index];
            if (currentLessonData.quiz) {
                // If there's a quiz, disable next until it's attempted
                // We need to check if the quiz for *this* section has been attempted
                // This logic will be refined in afterLoad
                const quizContainerId = `quiz-section-${currentLessonData.anchor}`;
                const quizSubmitButton = document.getElementById(`submit-quiz-button-${currentLessonData.anchor}`);
                if (quizSubmitButton && !quizSubmitButton.disabled) { // Quiz exists and not yet submitted
                    nextButton.disabled = true;
                } else { // No quiz or quiz already submitted
                     nextButton.disabled = false;
                }

            } else {
                // No quiz for this section, enable next button
                nextButton.disabled = false;
            }
        }
    }


    function initializeFullPage() {
        if (!fullPageContainer.children.length) {
            console.error("Fullpage container has no sections. Initialization aborted.");
            return;
        }
        new fullpage('#fullpage', {
            licenseKey: 'YOUR_KEY_HERE', // Replace with your actual key if you have one for commercial use
            anchors: courseData.map(lesson => lesson.anchor),
            navigation: false, // We use custom navigation
            autoScrolling: true,
            fitToSection: true,
            scrollingSpeed: 700,
            keyboardScrolling: false, // Strictly button controlled
            touchSensitivity: 15, // Default, but effectively blocked by button control logic
            scrollOverflow: false, // Keep it simple, no internal scrollbars unless really needed by content

            onLeave: function(origin, destination, direction) {
                // Logic for preventing scroll if quiz not done could go here,
                // but better handled by disabling the 'next' button.
                // If there's a quiz in the 'origin' section and it's not attempted,
                // and we are moving 'down', prevent moving.
                const originLessonData = courseData[origin.index];
                if (direction === 'down' && originLessonData.quiz && !quizAttempted) {
                    // This alert is for debugging, a more user-friendly message would be better.
                    // alert("Please complete the quiz before proceeding.");
                    // return false; // Prevent moving
                }
            },
            afterLoad: function(origin, destination, direction) {
                updateProgressBar(destination.index, courseData.length);
                updateNavigationButtons(destination);

                const lessonData = courseData[destination.index];
                if (lessonData.quiz) {
                    renderQuiz(lessonData.quiz, lessonData.anchor);
                } else {
                    // No quiz for this section, ensure 'Next Lesson' is enabled if not last page
                    quizAttempted = true; // Treat as "attempted" for navigation purposes
                    if (!destination.isLast) {
                        nextButton.disabled = false;
                    }
                }
                 // Ensure fullPage.js re-evaluates scrolling if content was dynamically added
                if (typeof fullpage_api !== 'undefined' && typeof fullpage_api.reBuild === 'function') {
                    fullpage_api.reBuild();
                }
            }
        });

        // Initial state for buttons (on page load, usually first section)
        if (courseData.length > 0) {
            updateProgressBar(0, courseData.length); // For first section
            prevButton.disabled = true; // Can't go back from first
            // Check quiz for first section
            const firstLessonData = courseData[0];
            if (firstLessonData.quiz) {
                renderQuiz(firstLessonData.quiz, firstLessonData.anchor);
                nextButton.disabled = true; // Quiz exists, disable next until submitted
            } else {
                quizAttempted = true; // No quiz, so it's "attempted"
                nextButton.disabled = courseData.length <= 1; // Disable if only one section
            }
        } else {
            prevButton.disabled = true;
            nextButton.disabled = true;
        }
    }

    // Custom Navigation Button Event Listeners
    prevButton.addEventListener('click', function() {
        if (typeof fullpage_api !== 'undefined') {
            fullpage_api.moveSectionUp();
        }
    });

    nextButton.addEventListener('click', function() {
        if (typeof fullpage_api !== 'undefined') {
            // Check if current section has a quiz and if it's attempted
            const currentSectionIndex = fullpage_api.getActiveSection().index;
            const currentLessonData = courseData[currentSectionIndex];

            if (currentLessonData.quiz && !quizAttempted) {
                // This is a fallback, primary control is disabling the button
                alert("Please complete the quiz before proceeding to the next lesson.");
                return;
            }
            fullpage_api.moveSectionDown();
        }
    });

    // Load sections and then initialize FullPage.js
    loadSections();

});
