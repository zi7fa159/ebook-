document.addEventListener('DOMContentLoaded', () => {
    const postList = document.getElementById('post-list');
    const postContent = document.getElementById('post-content');
    const copyMarkdownBtn = document.getElementById('copy-markdown-btn');
    let currentMarkdownContent = ''; // To store markdown for copying

    // Function to sanitize filenames for display or IDs
    const sanitizeFilename = (filename) => {
        return filename.replace(/[^a-zA-Z0-9-.]/g, '_');
    };

    // Function to load and render a post
    const loadPost = async (filename, title) => {
        try {
            const response = await fetch(`../posts/${filename}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            currentMarkdownContent = await response.text(); // Store raw markdown

            // Use marked.js to convert markdown to HTML
            // Ensure YAML front matter is not rendered as content
            const contentWithoutFrontMatter = currentMarkdownContent.replace(/---[\s\S]*?---/, '').trim();
            const htmlContent = marked.parse(contentWithoutFrontMatter);
            postContent.innerHTML = `<h2>${title}</h2>${htmlContent}`;

            // Show copy button and update active link
            copyMarkdownBtn.style.display = 'block';
            updateActiveLink(filename);

        } catch (error) {
            postContent.innerHTML = `<p>Error loading post: ${error.message}</p>`;
            copyMarkdownBtn.style.display = 'none';
            console.error('Failed to load post:', filename, error);
        }
    };

    // Function to update active link in sidebar
    const updateActiveLink = (activeFilename) => {
        const links = postList.querySelectorAll('a');
        links.forEach(link => {
            if (link.dataset.filename === activeFilename) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    };

    // Copy Markdown to clipboard
    copyMarkdownBtn.addEventListener('click', async () => {
        if (currentMarkdownContent) {
            try {
                await navigator.clipboard.writeText(currentMarkdownContent);
                alert('Markdown copied to clipboard!');
            } catch (err) {
                console.error('Failed to copy markdown: ', err);
                alert('Failed to copy markdown. Please try again or copy manually.');
            }
        }
    });

    // Load posts manifest and populate the sidebar
    const loadPostsManifest = async () => {
        try {
            const response = await fetch('../posts/posts-manifest.json');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const posts = await response.json();

            if (posts.length === 0) {
                postList.innerHTML = '<li>No posts found.</li>';
                return;
            }

            posts.forEach(post => {
                const listItem = document.createElement('li');
                const link = document.createElement('a');
                link.href = '#'; // Use fragment identifier if needed, or just prevent default
                link.textContent = post.title;
                link.dataset.filename = post.filename; // Store filename in data attribute
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                    loadPost(post.filename, post.title);
                });
                listItem.appendChild(link);
                postList.appendChild(listItem);
            });

            // Optionally, load the first post by default
            if (posts.length > 0) {
                loadPost(posts[0].filename, posts[0].title);
            }

        } catch (error) {
            postList.innerHTML = `<li>Error loading posts manifest: ${error.message}</li>`;
            console.error('Failed to load posts manifest:', error);
        }
    };

    loadPostsManifest();
});
