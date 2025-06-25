document.addEventListener('DOMContentLoaded', () => {
    // --- Element References ---
    const postListElement = document.getElementById('post-list');
    const postContentElement = document.getElementById('post-content');
    const copyMarkdownButton = document.getElementById('copy-markdown-btn');
    const searchInput = document.getElementById('search-input');
    const contentArea = document.getElementById('content-area'); // Reference to the scrollable content area

    // --- State Variables ---
    let loadedMarkdownContent = ''; // Stores the raw Markdown of the currently displayed post
    let allPosts = []; // Cache for all posts to enable filtering and URL navigation
    let currentPostFilename = ''; // Stores the filename of the currently displayed post

    // --- UI Utility Functions ---

    // Shows/hides a loading indicator in the content area
    const showLoading = (isLoading) => {
        if (isLoading) {
            postContentElement.innerHTML = `<p class="loading-indicator">Loading...</p>`;
            copyMarkdownButton.style.display = 'none';
        }
    };

    // Updates the 'active' class on sidebar links
    const updateActiveLink = (activeFilename) => {
        const links = postListElement.querySelectorAll('a');
        links.forEach(link => {
            if (link.dataset.filename === activeFilename) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    };

    // Updates the browser's URL hash for direct linking and back/forward navigation
    const updateUrlHash = (filename) => {
        if (filename) {
            const hash = filename.replace(/\.md$/, ''); // Clean hash like #001-post-title
            history.pushState(null, null, `#${hash}`); // Update URL without page reload
        } else {
            history.pushState(null, null, '#'); // Clear hash if no file
        }
    };

    // --- Core Functionality ---

    // Fetches and displays a specific blog post
    const displayPost = async (filename, title) => {
        if (!filename || !title) {
            // Default message if no post is selected or found
            postContentElement.innerHTML = `<p>Select a post from the left to view its content.</p> <p>Make sure you're running a local web server (e.g., Python's http.server) from the project's root directory for the posts to load correctly.</p>`;
            copyMarkdownButton.style.display = 'none';
            currentPostFilename = '';
            loadedMarkdownContent = '';
            updateActiveLink(null);
            // Do not update hash here, let initial load or popstate handle it
            return;
        }

        showLoading(true); // Show loading state immediately
        currentPostFilename = filename; // Update current post filename

        try {
            const response = await fetch(`../posts/${filename}`);
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status} - Could not load ${filename}`);
            }
            // Extract YAML front matter and markdown body
            const rawText = await response.text();
            const parts = rawText.split('---');
            if (parts.length >= 3) {
                 loadedMarkdownContent = parts.slice(2).join('---').trim(); // Get content after the second '---'
            } else {
                 loadedMarkdownContent = rawText.trim(); // Assume no front matter or malformed
            }

            // Convert Markdown to HTML using marked.js
            // Ensure marked.js is loaded (it's included in index.html via CDN)
            const renderedHtml = marked.parse(loadedMarkdownContent);

            // Use the title from the manifest, as it's more reliable than parsing from raw text here
            postContentElement.innerHTML = `<h1>${title}</h1>${renderedHtml}`;

            copyMarkdownButton.style.display = 'block';
            updateActiveLink(filename);
            // Update URL hash when post loads, only if it's different from current hash
            const currentHash = window.location.hash.substring(1);
            if (filename.replace(/\.md$/, '') !== currentHash) {
                updateUrlHash(filename);
            }

            contentArea.scrollTop = 0; // Scroll to the top of the content area

        } catch (error) {
            postContentElement.innerHTML = `<p class="loading-indicator">Error loading post: ${error.message}</p><p class="loading-indicator">Please ensure you are running a local web server from the project's root directory and the file exists.</p>`;
            copyMarkdownButton.style.display = 'none';
            console.error('Failed to load post:', filename, error);
        }
    };

    // Handles copying Markdown to clipboard with user feedback
    copyMarkdownButton.addEventListener('click', async () => {
        if (loadedMarkdownContent && currentPostFilename) {
            // To get the full markdown including frontmatter, we need to fetch it again
            // Or, if we decide the button copies ONLY the body, `loadedMarkdownContent` is fine.
            // For this implementation, let's assume it copies the raw file content (including frontmatter)
            try {
                const response = await fetch(`../posts/${currentPostFilename}`);
                if (!response.ok) throw new Error('Failed to re-fetch for copy.');
                const fullMarkdown = await response.text();

                await navigator.clipboard.writeText(fullMarkdown);
                // Provide temporary visual feedback
                const originalText = copyMarkdownButton.textContent;
                copyMarkdownButton.textContent = 'Copied!';
                copyMarkdownButton.classList.add('copied');

                setTimeout(() => {
                    copyMarkdownButton.textContent = originalText;
                    copyMarkdownButton.classList.remove('copied');
                }, 2000); // Revert back after 2 seconds
            } catch (err) {
                console.error('Failed to copy markdown: ', err);
                alert('Failed to copy markdown. Please try again or copy manually.');
            }
        }
    });

    // Filters the sidebar post list based on search input
    const renderPostList = (postsToRender) => {
        postListElement.innerHTML = ''; // Clear current list
        if (postsToRender.length === 0) {
            const messageItem = document.createElement('li');
            messageItem.className = 'no-results';
            messageItem.textContent = 'No posts found.';
            postListElement.appendChild(messageItem);
        } else {
            postsToRender.forEach(post => {
                const listItem = document.createElement('li');
                const link = document.createElement('a');
                link.href = `#${post.filename.replace(/\.md$/, '')}`;
                link.textContent = post.title;
                link.dataset.filename = post.filename; // Store filename for displayPost

                link.addEventListener('click', (e) => {
                    e.preventDefault(); // Prevent default anchor behavior
                    displayPost(post.filename, post.title);
                });

                listItem.appendChild(link);
                postListElement.appendChild(listItem);
            });
        }
        // After rendering, update active link based on currentPostFilename
        if (currentPostFilename) {
            updateActiveLink(currentPostFilename);
        }
    };

    const filterPosts = (query) => {
        const lowerCaseQuery = query.toLowerCase();
        const filteredPosts = allPosts.filter(post =>
            post.title.toLowerCase().includes(lowerCaseQuery)
        );
        renderPostList(filteredPosts);
    };


    // Loads the posts manifest and populates the sidebar
    const loadPostsManifest = async () => {
        try {
            const response = await fetch('../posts/posts-manifest.json');
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status} - Could not load posts-manifest.json`);
            }
            allPosts = await response.json(); // Cache the posts

            if (!Array.isArray(allPosts) || allPosts.length === 0) {
                postListElement.innerHTML = '<li>No posts found in manifest.</li>';
                allPosts = []; // Ensure it's an empty array if invalid
                return;
            }

            // Initially populate sidebar with all posts
            renderPostList(allPosts);

            // Logic to load post from URL hash on initial page load or default
            const initialHash = window.location.hash.substring(1);
            let postToLoad = null;

            if (initialHash) {
                postToLoad = allPosts.find(p => p.filename.replace(/\.md$/, '') === initialHash);
            }

            if (postToLoad) {
                displayPost(postToLoad.filename, postToLoad.title);
            } else if (allPosts.length > 0) {
                // If no hash or hash doesn't match, display first post by default
                // but do not change the URL hash if it was empty initially
                // displayPost(allPosts[0].filename, allPosts[0].title);
                // NO - only display if hash explicitly points or if popstate navigates
                // For initial load, if no valid hash, show default message.
                 displayPost(null, null); // Show default message
            } else {
                 displayPost(null, null); // Show default if no posts and no hash
            }

        } catch (error) {
            postListElement.innerHTML = `<li><p class="loading-indicator">Error loading blog index: ${error.message}</p><p class="loading-indicator">Ensure 'posts-manifest.json' exists in 'posts/' and is accessible.</p></li>`;
            console.error('Failed to load posts manifest:', error);
        }
    };

    // --- Event Listeners ---

    // Search input listener
    searchInput.addEventListener('input', (e) => {
        filterPosts(e.target.value);
    });

    // Handle browser back/forward buttons using popstate event
    window.addEventListener('popstate', () => {
        const hash = window.location.hash.substring(1);
        if (allPosts.length > 0) { // Ensure posts are loaded
            if (hash) {
                const postToLoad = allPosts.find(p => p.filename.replace(/\.md$/, '') === hash);
                if (postToLoad) {
                    displayPost(postToLoad.filename, postToLoad.title);
                } else {
                    // Hash doesn't match any known post, show default message
                    displayPost(null, null);
                }
            } else {
                // Hash is empty (e.g., navigated to base URL)
                // Display default message, don't auto-load first post.
                displayPost(null, null);
            }
        }
    });

    // Initialize the blog
    loadPostsManifest();
});
