import os
import pty
import select
import subprocess
import struct
import fcntl
import termios
from flask import Flask, render_template
from flask_socketio import SocketIO

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*", async_mode='eventlet')

fd = None
child_pid = None

def set_winsize(fd, row, col, xpix=0, ypix=0):
    """Set the window size of the pseudo-terminal."""
    winsize = struct.pack("HHHH", row, col, xpix, ypix)
    fcntl.ioctl(fd, termios.TIOCSWINSZ, winsize)

def read_and_forward_pty_output():
    """Continuously read from the PTY and forward output to the web client."""
    global fd
    max_read_bytes = 1024 * 20  # Read up to 20KB at a time
    while True:
        socketio.sleep(0.01)  # Small delay to prevent CPU overuse
        if fd:
            timeout_sec = 0
            (data_ready, _, _) = select.select([fd], [], [], timeout_sec)
            if data_ready:
                try:
                    output = os.read(fd, max_read_bytes).decode(errors='ignore')
                    socketio.emit("pty-output", {"output": output})
                except OSError:
                    break

@app.route("/")
def index():
    """Serve the main HTML page."""
    return render_template("index.html")

@socketio.on("pty-input")
def pty_input(data):
    """Handle keyboard input from the web client."""
    global fd
    if fd:
        os.write(fd, data["input"].encode())

@socketio.on("resize")
def resize(data):
    """Handle terminal resize events from the web client."""
    global fd
    if fd:
        set_winsize(fd, data["rows"], data["cols"])

@socketio.on("connect")
def connect():
    """Handle new WebSocket connections - spawn a new shell process."""
    global fd, child_pid
    if child_pid:
        return

    # Create a new pseudo-terminal for the shell
    (child_pid, fd) = pty.fork()

    if child_pid == 0:
        # This code runs in the child process (the shell)
        subprocess.run(["/bin/bash", "-i"])
    else:
        # This code runs in the parent process (the server)
        set_winsize(fd, 50, 50)  # Set initial terminal size
        # Start background task to read shell output
        socketio.start_background_task(target=read_and_forward_pty_output)

if __name__ == "__main__":
    # Run the Flask-SocketIO server
    # host="0.0.0.0" makes it accessible on the network
    # port=7860 is the port number (you can change this)
    socketio.run(app, host="0.0.0.0", port=7860, debug=False, allow_unsafe_werkzeug=True)
