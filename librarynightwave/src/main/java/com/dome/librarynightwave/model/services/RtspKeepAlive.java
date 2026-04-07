package com.dome.librarynightwave.model.services;


import static com.dome.librarynightwave.model.repository.TCPRepository.isStartedReceivingPackets;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtspKeepAlive {

  private static final String TAG = "RTSPKeepAlive";
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final String rtspUrl;
  private final long intervalMs;
  private boolean isRunning = false;
  private int cSeq = 1;

  @Nullable
  private String sessionId = null;

  public RtspKeepAlive(String rtspUrl, long intervalMs) {
    this.rtspUrl = rtspUrl;
    this.intervalMs = intervalMs;
  }

  private final Runnable keepAliveRunnable = new Runnable() {
    @Override
    public void run() {
      if (!isRunning) return;
      executor.execute(() -> sendRtspRequest("OPTIONS"));
      handler.postDelayed(this, intervalMs);
    }
  };

  public void start() {
    if (!isRunning) {
      isRunning = true;
      handler.post(keepAliveRunnable);
    }
  }

  public void stop() {
    isRunning = false;
    handler.removeCallbacks(keepAliveRunnable);
    executor.shutdownNow();
  }

  public void play() {
    executor.execute(() -> sendRtspRequest("PLAY"));
  }

  public void pause() {
    executor.execute(() -> sendRtspRequest("PAUSE"));
  }

  public void teardown() {
    if(isStartedReceivingPackets){
      executor.execute(() -> sendRtspRequest("TEARDOWN"));
    }
  }

  private void sendRtspRequest(String method) {
    if(isStartedReceivingPackets){
      Socket socket = null;
      PrintWriter writer = null;
      BufferedReader reader = null;

      try {
        URI uri = new URI(rtspUrl);
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 554;
        String path = uri.getRawPath();
        if (uri.getRawQuery() != null) {
          path += "?" + uri.getRawQuery();
        }

        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        StringBuilder request = new StringBuilder();
        request.append(method).append(" rtsp://").append(host).append(path).append(" RTSP/1.0\r\n");
        request.append("CSeq: ").append(cSeq++).append("\r\n");
        request.append("User-Agent: KeepAliveClient\r\n");
        if (sessionId != null && !method.equals("OPTIONS")) {
          request.append("Session: ").append(sessionId).append("\r\n");
        }
        request.append("\r\n");

        writer.print(request);
        writer.flush();

        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) {
          response.append(line).append("\n");
          if (line.trim().isEmpty()) break;
        }

        Log.w(TAG, method + " response:\n" + response);

        // Extract session ID if not set yet
        if (sessionId == null && response.toString().contains("Session:")) {
          Pattern pattern = Pattern.compile("Session: (\\S+)");
          Matcher matcher = pattern.matcher(response.toString());
          if (matcher.find()) {
            sessionId = matcher.group(1).split(";")[0];
            Log.d(TAG, "Session ID acquired: " + sessionId);
          }
        }

      } catch (Exception e) {
        Log.e(TAG, method + " request failed: " + e.getMessage());
      } finally {
        try {
          if (reader != null) reader.close();
          if (writer != null) writer.close();
          if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
      }
    }
  }
}
