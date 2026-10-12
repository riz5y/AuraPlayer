package com.auraplayer.core.transfer

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * Lightweight zero-dependency Wi-Fi HTTP Server.
 * Allows users to open a web browser on their PC/Mac on the local Wi-Fi
 * and upload MP3, FLAC, WAV files directly into AuraPlayer.
 */
class WifiTransferServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var isServerRunning = false

    private val _statusFlow = MutableStateFlow<String?>(null)
    val statusFlow: StateFlow<String?> = _statusFlow.asStateFlow()

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WifiTransferServer", "Error finding IP", e)
        }
        return null
    }

    fun isRunning(): Boolean = isServerRunning

    fun startServer(port: Int = 8080, onFileSaved: (File) -> Unit): String? {
        if (isServerRunning) return _statusFlow.value

        val ip = getLocalIpAddress() ?: return null
        try {
            serverSocket = ServerSocket(port)
            isServerRunning = true
            val url = "http://$ip:$port"
            _statusFlow.value = url

            Thread {
                while (isServerRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        Thread { handleClient(client, onFileSaved) }.start()
                    } catch (e: Exception) {
                        if (!isServerRunning) break
                    }
                }
            }.start()

            return url
        } catch (e: Exception) {
            Log.e("WifiTransferServer", "Failed to start server", e)
            stopServer()
            return null
        }
    }

    fun stopServer() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _statusFlow.value = null
    }

    private fun handleClient(socket: Socket, onFileSaved: (File) -> Unit) {
        try {
            val inputStream = BufferedInputStream(socket.getInputStream())
            val outputStream = socket.getOutputStream()

            // Read HTTP request line and headers
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            var contentLength = 0L
            var fileName = "Aura_Track_${System.currentTimeMillis()}.mp3"

            var headerLine: String? = reader.readLine()
            while (!headerLine.isNullOrEmpty()) {
                val colon = headerLine.indexOf(':')
                if (colon > 0) {
                    val key = headerLine.substring(0, colon).trim().lowercase()
                    val value = headerLine.substring(colon + 1).trim()
                    if (key == "content-length") {
                        contentLength = value.toLongOrNull() ?: 0L
                    } else if (key == "x-filename") {
                        runCatching {
                            fileName = URLDecoder.decode(value, "UTF-8")
                        }
                    }
                }
                headerLine = reader.readLine()
            }

            if (method.equals("GET", ignoreCase = true) && (path == "/" || path.startsWith("/index"))) {
                val html = getWebPageHtml()
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Content-Length: ${html.toByteArray(Charsets.UTF_8).size}\r\n" +
                        "Connection: close\r\n\r\n$html"
                outputStream.write(response.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } else if (method.equals("POST", ignoreCase = true) && path.startsWith("/upload")) {
                val musicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "AuraPlayer"
                ).apply { mkdirs() }

                val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val destFile = File(musicDir, sanitizedName)

                // Read binary body
                val buffer = ByteArray(8192)
                var bytesRemaining = contentLength
                FileOutputStream(destFile).use { fos ->
                    while (bytesRemaining > 0) {
                        val toRead = if (bytesRemaining > buffer.size) buffer.size else bytesRemaining.toInt()
                        val read = inputStream.read(buffer, 0, toRead)
                        if (read <= 0) break
                        fos.write(buffer, 0, read)
                        bytesRemaining -= read
                    }
                }

                onFileSaved(destFile)

                val jsonResponse = "{\"status\":\"success\",\"file\":\"${destFile.name}\"}"
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: ${jsonResponse.toByteArray(Charsets.UTF_8).size}\r\n" +
                        "Connection: close\r\n\r\n$jsonResponse"
                outputStream.write(response.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } else {
                val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                outputStream.write(response.toByteArray())
                outputStream.flush()
            }
        } catch (e: Exception) {
            Log.e("WifiTransferServer", "Error processing request", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun getWebPageHtml(): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>AuraPlayer Wi-Fi Transfer</title>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                body { background: #0c0d11; color: #fff; min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; }
                .card { background: #181920; border-radius: 24px; padding: 40px; width: 100%; max-width: 520px; text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.6); border: 1px solid rgba(255,255,255,0.08); }
                .logo { font-size: 40px; margin-bottom: 12px; }
                h1 { font-size: 24px; font-weight: 700; margin-bottom: 8px; letter-spacing: -0.5px; }
                p { color: #8e8e93; font-size: 14px; margin-bottom: 28px; line-height: 1.5; }
                .dropzone { border: 2px dashed #6366f1; border-radius: 18px; padding: 40px 20px; cursor: pointer; transition: all 0.2s ease; background: rgba(99,102,241,0.04); }
                .dropzone.hover { border-color: #a855f7; background: rgba(168,85,247,0.1); transform: scale(1.02); }
                .drop-icon { font-size: 48px; color: #6366f1; margin-bottom: 12px; }
                .drop-text { font-size: 15px; font-weight: 600; color: #fff; }
                .drop-sub { font-size: 12px; color: #8e8e93; margin-top: 6px; }
                #fileInput { display: none; }
                .status-box { margin-top: 24px; text-align: left; }
                .item { background: #22232c; border-radius: 12px; padding: 12px 16px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
                .badge { color: #10b981; font-weight: 600; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="logo">🎵</div>
                <h1>AuraPlayer Wi-Fi Transfer</h1>
                <p>Drag and drop audio files from your computer.<br>They will instantly sync to your phone in high fidelity.</p>
                <div class="dropzone" id="dropzone" onclick="document.getElementById('fileInput').click()">
                    <div class="drop-icon">☁️</div>
                    <div class="drop-text">Click or drop music files here</div>
                    <div class="drop-sub">Supports MP3, FLAC, WAV, AAC, M4A</div>
                </div>
                <input type="file" id="fileInput" multiple accept="audio/*">
                <div class="status-box" id="statusBox"></div>
            </div>
            <script>
                const dropzone = document.getElementById('dropzone');
                const fileInput = document.getElementById('fileInput');
                const statusBox = document.getElementById('statusBox');

                ['dragenter', 'dragover'].forEach(e => {
                    dropzone.addEventListener(e, (evt) => { evt.preventDefault(); dropzone.classList.add('hover'); });
                });
                ['dragleave', 'drop'].forEach(e => {
                    dropzone.addEventListener(e, (evt) => { evt.preventDefault(); dropzone.classList.remove('hover'); });
                });

                dropzone.addEventListener('drop', (e) => {
                    const files = e.dataTransfer.files;
                    if (files.length) uploadFiles(files);
                });

                fileInput.addEventListener('change', (e) => {
                    if (e.target.files.length) uploadFiles(e.target.files);
                });

                async function uploadFiles(files) {
                    for (let i = 0; i < files.length; i++) {
                        const file = files[i];
                        const item = document.createElement('div');
                        item.className = 'item';
                        item.innerHTML = '<span>' + file.name + '</span><span>Uploading...</span>';
                        statusBox.prepend(item);

                        try {
                            const res = await fetch('/upload', {
                                method: 'POST',
                                headers: { 'X-Filename': encodeURIComponent(file.name) },
                                body: file
                            });
                            if (res.ok) {
                                item.innerHTML = '<span>' + file.name + '</span><span class="badge">✓ Synced</span>';
                            } else {
                                item.innerHTML = '<span>' + file.name + '</span><span style="color:#ef4444">Failed</span>';
                            }
                        } catch (err) {
                            item.innerHTML = '<span>' + file.name + '</span><span style="color:#ef4444">Error</span>';
                        }
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
