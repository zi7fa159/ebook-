package com.alsclone.astrostacker;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean running;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final MainActivity activity;

    public WebServer(int port, MainActivity activity) {
        this.port = port;
        this.activity = activity;
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (running) {
                    Socket socket = serverSocket.accept();
                    executor.execute(() -> handleRequest(socket));
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) {}
    }

    private void handleRequest(Socket socket) {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String line = reader.readLine();
            if (line == null) return;

            String[] parts = line.split(" ");
            String path = parts.length > 1 ? parts[1] : "/";

            String response;
            if (path.equals("/status")) {
                response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" + activity.getStatusJson();
            } else {
                response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + getIndexHtml();
            }
            output.write(response.getBytes());
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private String getIndexHtml() {
        return "<html><head><title>A2LS Remote</title>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
               "<style>body{font-family:sans-serif;background:#222;color:#eee;text-align:center} .stat{font-size:2em;margin:20px}</style>" +
               "<script>setInterval(() => { fetch('/status').then(r=>r.json()).then(j=>{ " +
               "document.getElementById('frames').innerText = j.frames + ' Frames'; " +
               "document.getElementById('status').innerText = j.status; " +
               "}); }, 2000);</script></head>" +
               "<body><h1>A2LS Remote Monitor</h1>" +
               "<div id='status' class='stat'>CONNECTING...</div>" +
               "<div id='frames' class='stat'>0 Frames</div>" +
               "</body></html>";
    }
}
