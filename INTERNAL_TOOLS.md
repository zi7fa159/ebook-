# Internal Tools Documentation

This document provides a highly detailed, comprehensive guide to the internal tools available within the agent sandbox environment. Each tool is described with its endpoints/names, exact parameters, data types, behaviors, edge cases, and best-practice usage instructions.

---

## Table of Contents
1. [File Operations](#1-file-operations)
   - `list_files`
   - `read_file`
   - `write_file`
   - `delete_file`
   - `rename_file`
   - `replace_with_git_merge_diff`
   - `restore_file`
   - `reset_all`
2. [Task Planning & Progression](#2-task-planning--progression)
   - `set_plan`
   - `plan_step_complete`
   - `pre_commit_instructions`
   - `submit`
3. [User & Review Collaboration](#3-user--review-collaboration)
   - `message_user`
   - `request_user_input`
   - `record_user_approval_for_plan`
   - `request_code_review`
   - `read_pr_comments`
   - `reply_to_pr_comments`
4. [External Information & Search](#4-external-information--search)
   - `google_search`
   - `view_text_website`
   - `view_image`
   - `knowledgebase_lookup`
5. [Frontend & Media Verification](#5-frontend--media-verification)
   - `frontend_verification_instructions`
   - `frontend_verification_complete`
   - `start_live_preview_instructions`
   - `read_image_file`
   - `read_media_file`
6. [Execution & Sandbox Tools](#6-execution--sandbox-tools)
   - `run_in_bash_session`
   - `initiate_memory_recording`
   - `call_hello_world_agent`
   - `done`
7. [Deprecated Tools](#7-deprecated-tools)
   - `grep`
   - `create_file_with_block`
   - `overwrite_file_with_block`

---

## 1. File Operations

### `default_api:list_files`
* **Description**: Lists all files and directories under the specified directory path. If no path is provided, it defaults to the repository root. Directories in the output are denoted with a trailing slash (e.g., `src/`). The output format mirrors the Unix command: `ls -a -1F --group-directories-first <path>`.
* **Parameters**:
  - `path` (STRING, Optional): The directory path to list files from, relative to the repository root. Defaults to the repository root.
* **Return/Response**: A string containing the listed files and directories, each on a new line, grouped with directories first.
* **Usage Tip**: Useful for initial exploration and verifying that newly created files or directories exist.

---

### `default_api:read_file`
* **Description**: Reads and returns the complete text content of the specified file in the repository.
* **Parameters**:
  - `filepath` (STRING, Required): The relative path of the file to read, starting from the repository root.
* **Return/Response**: The raw text contents of the file. Returns an error if the file does not exist or if it is a directory.
* **Usage Tip**: Always read the files you are about to edit to understand their structure and context fully.

---

### `default_api:write_file`
* **Description**: Creates a new file at the specified path or overwrites an existing file with the provided contents.
* **Parameters**:
  - `filepath` (STRING, Required): The relative path where the file will be written.
  - `content` (STRING, Required): The complete content to write into the file.
* **Return/Response**: Confirmation of successful write or an error if the path/parent directories are invalid or inaccessible.
* **Usage Tip**: Ensure all parent directories exist, or write files to existing structures. Always read the written file with `read_file` to verify successful execution and correct contents.

---

### `default_api:delete_file`
* **Description**: Deletes a specified file from the sandbox environment.
* **Parameters**:
  - `filepath` (STRING, Required): The relative path of the file to delete.
* **Return/Response**: A confirmation message, or an error message if the file does not exist.
* **Usage Tip**: Verify file deletion using `list_files` or checking that `read_file` on the path now returns an error.

---

### `default_api:rename_file`
* **Description**: Renames and/or moves files and directories from an old path to a new path.
* **Parameters**:
  - `filepath` (STRING, Required): The original path of the file or directory.
  - `new_filepath` (STRING, Required): The target path for the file or directory.
* **Return/Response**: A confirmation message, or an error if `filepath` is missing, `new_filepath` already exists, or the target parent directory does not exist.
* **Usage Tip**: Make sure the target parent directory exists before calling this tool.

---

### `default_api:replace_with_git_merge_diff`
* **Description**: Performs a highly targeted search-and-replace on an existing file using a Git merge diff conflict marker format.
* **Parameters**:
  - `filepath` (STRING, Required): The path of the file to modify.
  - `merge_diff` (STRING, Required): A string detailing the changes using exact Git merge diff syntax.
* **Syntax Format**:
  ```
  <<<<<<< SEARCH
  [exact block of existing code to search for]
  =======
  [replacement block of code]
  >>>>>>> REPLACE
  ```
* **Return/Response**: Confirmation of modification, or an error if the search block could not be found or matched, or if syntax is incorrect.
* **Usage Tip**: The `<<<<<<< SEARCH`, `=======`, and `>>>>>>> REPLACE` markers must be exact and on their own lines. Ensure whitespace and indentation inside the SEARCH block match the target file exactly.

---

### `default_api:restore_file`
* **Description**: Restores a modified file to its original state (as it was when the session started).
* **Parameters**:
  - `filepath` (STRING, Required): The relative path of the file to restore.
* **Return/Response**: Confirmation of restoration or error.
* **Usage Tip**: Use this tool to undo accidental changes to a specific file without affecting other modifications in your workspace.

---

### `default_api:reset_all`
* **Description**: Resets the entire codebase and sandbox state to its original baseline, discarding all files created, deleted, or modified.
* **Parameters**: None.
* **Return/Response**: Confirmation of reset.
* **Usage Tip**: Use this as a last-resort escape hatch if your workspace becomes highly corrupted or tests are broken beyond repair and you need to restart your approach.

---

## 2. Task Planning & Progression

### `default_api:set_plan`
* **Description**: Sets or updates the execution plan for the active task. This must be formatted as a numbered list in Markdown. It must include a pre-commit step which describes its purpose as: "ensure proper testing, verification, review, and reflection are done".
* **Parameters**:
  - `plan` (STRING, Required): The plan in Markdown format.
* **Return/Response**: Confirmation of current plan update.
* **Usage Tip**: Use this immediately after initial exploration and anytime your approach significantly changes.

---

### `default_api:plan_step_complete`
* **Description**: Marks the current step of the plan as completed.
* **Parameters**:
  - `message` (STRING, Required): A succinct, descriptive message explaining what actions were taken to complete the step. This message is visible to the user.
* **Return/Response**: Confirmation of step completion.
* **Requirement**: You must already have verified that your changes were applied correctly (e.g., via `read_file` or running tests) before calling this tool.

---

### `default_api:pre_commit_instructions`
* **Description**: Retrieves a list of pre-commit checks and steps that must be performed before submitting changes.
* **Parameters**: None.
* **Return/Response**: Markdown list of instructions to execute.
* **Requirement**: Always call this tool before executing `submit`.

---

### `default_api:submit`
* **Description**: Commits all code changes, specifies a title and description, and requests user approval to push to the branch.
* **Parameters**:
  - `branch_name` (STRING, Required): Name of the branch to commit/push to.
  - `commit_message` (STRING, Required): Standard git-agnostic commit message.
  - `title` (STRING, Required): Title of the submission.
  - `description` (STRING, Required): Detailed description of the changes made and verified.
* **Return/Response**: Confirmation of commit or failure/conflict reports.
* **Requirement**: Call only when confident that code changes are complete, fully verified, and all relevant tests pass.

---

## 3. User & Review Collaboration

### `default_api:message_user`
* **Description**: Sends a message to the user to provide status updates, explain progress, or respond to non-blocking feedback.
* **Parameters**:
  - `message` (STRING, Required): The text message to send.
  - `continue_working` (BOOLEAN, Required): Set to `True` if you intend to perform more tool actions immediately. Set to `False` if you are finished with your turn and are waiting for user input/feedback.
* **Return/Response**: Acknowledgment of message sent.
* **Usage Tip**: Do NOT use this tool to ask questions requiring response—use `request_user_input` for that.

---

### `default_api:request_user_input`
* **Description**: Prompts the user with a specific question or request for information and pauses execution to wait for a response.
* **Parameters**:
  - `message` (STRING, Required): The question or prompt for the user.
* **Return/Response**: The user's text reply.
* **Usage Tip**: Use this if the prompt is ambiguous, if you are stuck after trying multiple approaches, or if a major decision significantly alters the task scope.

---

### `default_api:record_user_approval_for_plan`
* **Description**: Records that the user has approved the proposed plan.
* **Parameters**: None.
* **Return/Response**: Confirmation of approval recording.
* **Usage Tip**: Call this when the user approves your plan for the first time. Revised plans do not need re-approval unless specified.

---

### `default_api:request_code_review`
* **Description**: Submits a request for a code review on the active set of changes.
* **Parameters**: None.
* **Return/Response**: Acknowledgment of request.

---

### `default_api:read_pr_comments`
* **Description**: Retrieves any pending comments or feedback left on your pull request.
* **Parameters**: None.
* **Return/Response**: A JSON structure or list of comments with identifiers and text content.

---

### `default_api:reply_to_pr_comments`
* **Description**: Allows replying to comments left on your pull request.
* **Parameters**:
  - `replies` (STRING, Required): A JSON-serialized string representing a list of reply objects. Each object must have `comment_id` and `reply` keys.
  - *Example format*: `[{"comment_id": "123", "reply": "This is fixed now."}]`
* **Return/Response**: Confirmation of replies successfully sent.

---

## 4. External Information & Search

### `default_api:google_search`
* **Description**: Queries Google to fetch top search results, including page titles and short snippets.
* **Parameters**:
  - `query` (STRING, Required): The query string.
* **Return/Response**: List of relevant web search results.
* **Usage Tip**: Use this to find documentation, dependency info, or solve cryptic errors.

---

### `default_api:view_text_website`
* **Description**: Fetches the text-only representation of a specified webpage URL.
* **Parameters**:
  - `url` (STRING, Required): The target URL.
* **Return/Response**: Text contents of the webpage.
* **Usage Tip**: Perfect for reading online documentation or blog posts referenced in search results. Only works if sandbox has internet access.

---

### `default_api:view_image`
* **Description**: Downloads and processes an image from a public URL to make it readable in the agent's context.
* **Parameters**:
  - `url` (STRING, Required): The URL of the image.
* **Return/Response**: Extracted textual description or image data ready for context parsing.
* **Usage Tip**: Use this whenever you encounter a URL that points to an image (e.g., .jpg, .png, .webp).

---

### `default_api:knowledgebase_lookup`
* **Description**: Performs a semantic search against the internal knowledgebase for information regarding tools, configurations, packages, or specific technical issues.
* **Parameters**:
  - `query` (STRING, Required): The search query.
* **Return/Response**: Matching documentation or articles from the internal database.
* **Usage Tip**: Highly useful when running into tricky dependency, environment, or tool issues.

---

## 5. Frontend & Media Verification

### `default_api:frontend_verification_instructions`
* **Description**: Returns detailed instructions on how to write Playwright scripts to verify frontend web applications and generate screenshots of the interface.
* **Parameters**: None.
* **Return/Response**: Detailed tutorial/guidelines on using Playwright in the sandbox.

---

### `default_api:frontend_verification_complete`
* **Description**: Tells the agent framework that frontend visual verification is complete, attaching the main screenshot and any optional recordings.
* **Parameters**:
  - `screenshot_path` (STRING, Required): Relative path to the generated screenshot file of the frontend.
  - `additional_media_paths` (ARRAY of STRING, Optional): Paths to other media files, such as `.webm` screen recordings.
* **Return/Response**: Acceptance message.

---

### `default_api:start_live_preview_instructions`
* **Description**: Retrieves information on how to initialize a live preview server in the workspace to preview visual changes.
* **Parameters**: None.
* **Return/Response**: Command and port instructions.

---

### `default_api:read_image_file`
* **Description**: Reads a local image file (e.g., a captured screenshot) from the sandbox into the agent's visual/context buffer.
* **Parameters**:
  - `filepath` (STRING, Required): The relative path to the image.
* **Return/Response**: Visual analysis/rendering data of the image.

---

### `default_api:read_media_file`
* **Description**: Reads local media assets (images or video files) into the agent's context. Supported formats include `.png`, `.jpg`, `.jpeg`, `.webp`, and `.webm`.
* **Parameters**:
  - `filepath` (STRING, Required): The relative path to the media asset.
* **Return/Response**: Rendering context or analysis of the media file.

---

## 6. Execution & Sandbox Tools

### `default_api:run_in_bash_session`
* **Description**: Runs commands in a persistent, stateful bash shell in the sandbox. Every invocation uses the same session, although the starting directory for each command is always the repository root.
* **Parameters**:
  - `command` (STRING, Required): The shell command to run.
* **Return/Response**: Combined standard output and standard error from execution.
* **Usage Tip**: Use to install dependencies, run scripts, compile code, execute tests, or perform process operations. To start a long-running server in the background, append `&` and redirect output, e.g., `npm start > app.log 2>&1 &`.

---

### `default_api:initiate_memory_recording`
* **Description**: Activates or records persistent context/information that will be carried forward or utilized in future/successive tasks.
* **Parameters**: None.
* **Return/Response**: Confirmation of recording initialized.

---

### `default_api:call_hello_world_agent`
* **Description**: Communication proxy that calls the Hello World Agency agent with a payload message. Used for testing agency agent handshakes and integrations.
* **Parameters**:
  - `message` (STRING, Required): The message text.
* **Return/Response**: Response payload from the Hello World agent.

---

### `default_api:done`
* **Description**: Signals completion of the current subagent's scope of work.
* **Parameters**:
  - `summary` (STRING, Required): A descriptive summary of what was accomplished and the final state of the task.
* **Return/Response**: Closing acknowledgment.

---

## 7. Deprecated Tools

These tools are deprecated and should not be used. Better, more robust alternatives are listed.

### `default_api:grep`
* **Description**: *Deprecated*. Search for text patterns inside files.
* **Parameters**:
  - `pattern` (STRING, Required): The text pattern or regex.
* **Alternative**: Use standard `grep` commands inside `run_in_bash_session` (e.g., `grep -rn "pattern" src/`).

---

### `default_api:create_file_with_block`
* **Description**: *Deprecated*. Creates a file with content block.
* **Parameters**:
  - `filepath` (STRING, Required), `content` (STRING, Required).
* **Alternative**: Use `write_file`.

---

### `default_api:overwrite_file_with_block`
* **Description**: *Deprecated*. Overwrites a file with a content block.
* **Parameters**:
  - `filepath` (STRING, Required), `content` (STRING, Required).
* **Alternative**: Use `write_file`.
