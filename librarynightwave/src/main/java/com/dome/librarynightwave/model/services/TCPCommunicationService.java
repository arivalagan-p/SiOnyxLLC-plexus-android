package com.dome.librarynightwave.model.services;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_NONE;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_WIFI_DIALOG;
import static com.dome.librarynightwave.model.repository.TCPRepository.commandRequested;
import static com.dome.librarynightwave.model.repository.TCPRepository.fwMode;
import static com.dome.librarynightwave.model.repository.TCPRepository.fwUpdateFailed;
import static com.dome.librarynightwave.model.repository.TCPRepository.isOpsinLiveStreamingStarted;
import static com.dome.librarynightwave.model.repository.TCPRepository.isRiscvUpdateCompleteSent;
import static com.dome.librarynightwave.utils.Constants.ACTION;
import static com.dome.librarynightwave.utils.Constants.DEFAULT_IP_ADDRESS;
import static com.dome.librarynightwave.utils.Constants.DEFAULT_PORT;
import static com.dome.librarynightwave.utils.Constants.STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.STATE_DISCONNECTED;
import static com.dome.librarynightwave.utils.Constants.STATE_FAILED;
import static com.dome.librarynightwave.utils.Constants.STATE_NONE;
import static com.dome.librarynightwave.utils.Constants.VALUE;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.compareRiscvVersion;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.isSDK13;
import static com.dome.librarynightwave.utils.Constants.isSDK14;
import static com.dome.librarynightwave.utils.Constants.isSDK15;
import static com.dome.librarynightwave.utils.Constants.isSDK16AndAbove;
import static com.dome.librarynightwave.utils.Constants.mState;
import static com.dome.librarynightwave.utils.Constants.mWifiState;
import static com.dome.librarynightwave.utils.Constants.opsinVersionDetails;
import static com.dome.librarynightwave.utils.Constants.opsinWiFiOldFirmwareVersion;
import static com.dome.librarynightwave.utils.SCCPConstants.MAX_SEQUENCE_COUNT;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_ACK_SUCCESS;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.applyPreset;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.isRequestFailed;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.lockApplySettings;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.responseReceived;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.hasAllPacketZero;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.hostUnreachable;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.liveViewErrorMessage;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.opsinCameraErrorMessage;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.dome.librarynightwave.R;
import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.mediaCodec.MediaDecoder;
import com.dome.librarynightwave.model.repository.opsinmodel.LastSentCommand;
import com.dome.librarynightwave.model.repository.pojo.FPSCounter;
import com.dome.librarynightwave.model.repository.pojo.OpsinPeriodicCommand;
import com.dome.librarynightwave.model.repository.pojo.Packets;
import com.dome.librarynightwave.model.repository.pojo.RequestQueue;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.Event;
import com.dome.librarynightwave.utils.SCCPConstants;
import com.dome.librarynightwave.utils.SCCPMessage;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;


import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPCommunicationService extends Service {
    private final String TAG = "TCPCommunicationService";

    private ExecutorService executorServiceConnectTcp = null;
    private ExecutorService executorServiceTcpReceiver = null;
    private ExecutorService executorServiceNightwaveUDP = null;
    private ExecutorService executorServiceOpsinUDP = null;
    private ScheduledExecutorService scheduledESMonitorSOTimeout = null;
    private ScheduledExecutorService scheduledExecutorServiceKeepAlive = null;
    private ScheduledExecutorService scheduledFPSCalculation = null;
    private Callbacks callback;

    private boolean shouldRestartSocketListen = false;
    private boolean isCommandSent = false;
    public static long lastCommandSentTime = 0;
    private short sequence = 0;
    private byte opsinRxSequence = 0;
    private byte opsinTxSequence = 0;
    private int jpegBadStartPacketCount = 0;
    private int jpegBadEndPacketCount = 0;
    private SCCPMessage sccpMessage;

    private final Queue<byte[]> queue = new LinkedList<>();
    public static HashMap<Short, RequestQueue> requestQueue = new HashMap<>();
    private final Handler handlerForError = new Handler(Looper.getMainLooper());
    private int retryCount = 0;

    private final RequestQueue request = new RequestQueue();
    private boolean isAllZero;
    private byte[] inputArray;
    private final FPSCounter fpsCounter = new FPSCounter();
    private int fpsBasedOnGoodPackets = -1;

    private int totalUdpReceivedOpsin = 0;

    // Used to load the 'plexus' library on application startup.
    static {
        System.loadLibrary("plexus");
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("rtsp_stream");
    }

    public native int connectTcpSocket(String ip, int port, boolean isNightwave);

    public native int sendTcpCommand(byte[] data);

    public native void receiveTcpResponse();

    public native void disconnectTcpSocket();

    public native void startStopUdpReceiver(boolean shouldReceiveUDP);

    public native void startStopUdpReceiverNW(boolean shouldReceiveUDP);

    private static int retryKeepAlive = 0;
    private int hostNotReachableCount = 0;
    private boolean shouldStartStreaming;

    public void receiveTcpResponse(byte[] result, int length) {
        if (result != null && result.length > 0) {
            byte[] data = Arrays.copyOfRange(result, 0, length);
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    isTimeOutTriggered = false;
                    /*Get value id based on sequence number*/
                    byte resultSeqA = data[6];
                    byte resultSeqB = data[7];
                    short resultSeq = (short) (((resultSeqB & 0xFF) << 8) | (resultSeqA & 0xFF));

                RequestQueue request = requestQueue.get(resultSeq);
                int isValueEventAction = -1;
                byte valueId = 0;
                if (request != null) {
                    byte[] reqBytes = request.getData();
                    valueId = reqBytes != null ? reqBytes[1] : 0;
                    requestQueue.remove(resultSeq);
                    isValueEventAction = request.getIsValueEventAction();
                }
                broadcastNightwaveResponse(data, data.length, valueId, isValueEventAction);

                    isCommandSent = false;
                    break;
                case OPSIN:
                    isTimeOutTriggered = false;
                    retryKeepAlive = 0;
                    hostNotReachableCount = 0;
                    Log.d(TAG, "BYTE RECEIVED SUCCESS: " + Arrays.toString(result));
                    if (data.length > 0) {
                        //Flag
                        byte flag = data[6];
                        //Command
                        byte[] commandBytesToCheckUnsignedValue = {data[8], data[9], 0, 0};
                        int command = ByteBuffer.wrap(commandBytesToCheckUnsignedValue).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        //Consider flag 1 for protocol level commands, remaining drop the packets
                        if (command >= 64 && command <= 127) {
                            broadcastOpsinResponse(data);
                        } else if (flag != 0x01) {
                            broadcastOpsinResponse(data);
                        }


                        //Consider flag 1 for protocol level commands, remaining drop the packets
                        if (command >= 64 && command <= 127) {
                            isCommandSent = false;
                        } else if (flag != 0x01) {
                            if (command == 63498) {
                                Log.e(TAG, "READ: last command skip");
                            } else {
                                isCommandSent = false;
                            }
                        }
                        if (checkIfOlderVersion()) {
                            checkResponseAndCancelTimer(data, command);
                        }
                    }
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"receiveTCPResponse : NW_Digital");
                    break;
            }
        }
    }

    public boolean checkIfOlderVersion() {
        try {
            String riscv = opsinVersionDetails.getRiscv();
            boolean conditionMet = false;

            if (riscv == null || riscv.isEmpty()) {
                conditionMet = true;
            } else if (riscv.equalsIgnoreCase(compareRiscvVersion)) {
                conditionMet = true;
            }
            return conditionMet;
        } catch (Exception e) {
            // Handle any exceptions here
            e.printStackTrace(); // or log the exception
            return false; // Return false if an exception occurs
        }
    }

    private void checkResponseAndCancelTimer(byte[] data, int command) {
        if (lastSentCommand.getCommand() == command) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(data, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            if (ack == SCCP_ACK_SUCCESS) {
                lastSentCommand.setData(null);
                cancelCountdown();
            } else {
                cancelCountdown();
            }
        }
    }

    public void handleOpsinH264Payload(byte[] sps, byte[] pps, byte[] data) {
        String value = liveViewErrorMessage.getValue();
        if (value != null) {
            liveViewErrorMessage.postValue(null);
        }
        sotimeout_count.set(0);

        if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED && shouldReceiveUDP && mediaDecoder != null) {
            try {
                mediaDecoder.prepareInputBufferQueue(data, true, sps, pps);
                totalUdpReceivedOpsin = totalUdpReceivedOpsin + 1;
                mediaDecoder.setTotalUdpPacketReceived(totalUdpReceivedOpsin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "handleFrameWithSPSPPS: ");
            stopListeningOpsinUdp();
            TCPCommunicationService.shouldReceiveUDP = false;
        }
    }

    public void handleNightWaveUdpData(byte[] data, int length) {
        if (data != null && data.length > 0) {
            try {
                byte[] copiedData = Arrays.copyOfRange(data, 0, length);
                liveViewErrorMessage.postValue(null);
                sotimeout_count.set(0);

                receivedPacketSize = copiedData.length;
                if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED && shouldReceiveUDP) {
                    receivedUdpPacketcount++;
                    int received_length = copiedData.length;
                    createBuffer(copiedData, received_length);
                } else {
                    TCPCommunicationService.shouldReceiveUDP = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "UDP EMPTY ");
        }
    }

    public void handleTcpError(int errorCode, String errorMsg) {
        Log.e(TAG, "handleTcpError: Code: " + errorCode + " : " + errorMsg);
        if (mWifiState == WIFI_STATE_CONNECTED) {
            if (mState == STATE_CONNECTED) {
                switch (errorCode) {
                    case -3://Connection Reset By Peer
                        switch (currentCameraSsid) {
                            case OPSIN:
                                if (fwMode != MODE_NONE) {
                                    if (fwMode == MODE_WIFI_DIALOG) {
                                        String wifi = opsinVersionDetails.getWifi();
                                        Log.e(TAG, "Connection Reset By Peer: WIFI " + wifi);
                                        if (wifi == null) {
                                            Log.e(TAG, "handleTcpError: " + applyOpsinPeriodicRequest + " " + commandRequested);
                                            applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                                            disconnectResetAndConnect();
                                        }
                                    } else {
                                        if (!isRiscvUpdateCompleteSent) {
                                            disconnectTcpSocket();
                                            if (executorServiceConnectTcp != null) {
                                                executorServiceConnectTcp.shutdownNow();
                                                executorServiceConnectTcp = null;
                                            }
                                            if (executorServiceTcpReceiver != null) {
                                                executorServiceTcpReceiver.shutdown();
                                                executorServiceTcpReceiver = null;
                                            }
                                            handlerForError.postDelayed(this::connectSocket, 3000);
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "handleTcpError: " + applyOpsinPeriodicRequest + " " + commandRequested);
                                    applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                                    disconnectResetAndConnect();
                                }
                                break;
                            case NIGHTWAVE:
                                disconnectResetAndConnect();
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG,"handleTCPError : NW_Digital");
                                break;
                        }
                        break;
                    case -6://Resource Temporarily Unavailable
                        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                            tcpResourceUnAvailable();
                        } else {
                            //Need to handle
                        }
                        break;
                    case -8://Socket Closed
                    case -11://Interrupted system call
                        if (fwMode == MODE_NONE) {
                            disconnectResetAndConnect();
                        } else {
                            //display firmware failed dialog
                            CommandError error = new CommandError();
                            error.setError("Failed");
                            fwUpdateFailed.postValue(new Event<>(error));
                        }
                        break;
                    default:
                        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                            Log.e(TAG, "handleTcpError: " + applyOpsinPeriodicRequest + " " + commandRequested);
                            applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                            disconnectResetAndConnect();
                        } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                            disconnectResetAndConnect();
                        } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                            Log.e(TAG,"handleTCPErr : NW_Digital");
                        }
                        break;
                }
            } else {
                Log.e(TAG, "handleTcpError: NO RETRY DUE TO SOCKET DISCONNECTED");
            }
        } else {
            Log.e(TAG, "handleTcpError: NO RETRY DUE TO WIFI DISCONNECTED");
        }
    }

    public void handleUdpError(int errorCode, String errorMessage) {
//        Log.e(TAG, "handleUdpError: Code: " + errorCode + " : " + errorMessage + " shouldReceiveUDP: " + shouldReceiveUDP + " mWifiState: " + mWifiState + " Socket State: " + mState);
        if (mWifiState == WIFI_STATE_CONNECTED && shouldReceiveUDP && mState == STATE_CONNECTED) {
            switch (errorCode) {
                case -1:// Socket error
                    break;
                case -2:// Bind error
                    break;
                case -3:// poll error
                    break;
                case -4:// Socket receive timeout
                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            if (mState == STATE_CONNECTED) {
                                try {
                                    if (InetAddress.getByName(DEFAULT_IP_ADDRESS).isReachable(5000)) {
                                        int incremented = sotimeout_count.incrementAndGet();
                                        if (incremented == 15) {
                                            Log.e(TAG, "handleUdpError: trigger Dialog that no udp packets are received as like NW");
                                            hasAllPacketZero.postValue(new Event<>(true));
                                            sotimeout_count.set(0);
                                        } else {
                                            handlerForError.post(() -> {
                                                Log.e(TAG, "handleUdpError: " + applyOpsinPeriodicRequest + " " + commandRequested);
                                                if (incremented > 3) {
                                                    liveViewErrorMessage.postValue("Buffering");
                                                }
                                            });
                                        }
                                    } else {
                                        Log.e(TAG, "handleUdpError: Host is not Reachable");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e(TAG, "handleUdpError: mState == STATE_DISCONNECTED");
                                disconnectResetAndConnect();
                            }
                            break;
                        case OPSIN:
                            if (mState == STATE_CONNECTED) {
                                try {
                                    if (InetAddress.getByName(DEFAULT_IP_ADDRESS).isReachable(5000)) {
                                        int incremented = sotimeout_count.incrementAndGet();
                                        if (incremented == 5) {
                                            //Get Recording Status
                                            Log.e(TAG, "handleUdpError:  triggerOpsingRecordingState");
                                            liveViewErrorMessage.postValue("ip");
                                            mediaDecoder.triggerOpsingRecordingState();
                                        } else if (incremented == 15) {
                                            Log.e(TAG, "handleUdpError: trigger Dialog that no udp packets are received as like NW");
                                            //Trigger Dialog that no udp packets are received as like NW
                                            handlerForError.post(() -> {
                                                callback.triggerStopOpsinLiveStreaming();
                                            });
                                            hasAllPacketZero.postValue(new Event<>(true));
                                            sotimeout_count.set(0);
                                        } else {
                                            handlerForError.post(() -> {
                                                Log.e(TAG, "handleUdpError: " + applyOpsinPeriodicRequest + " " + commandRequested);
                                                if (incremented > 3) {
                                                    liveViewErrorMessage.postValue("Buffering");
                                                }
                                            });
                                        }
                                    } else {
                                        Log.e(TAG, "handleUdpError: Host is not Reachable");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e(TAG, "handleUdpError: mState == STATE_DISCONNECTED");
                                disconnectResetAndConnect();
                            }
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"handleUDPErr : NW_Digital");
                            break;
                    }
                    break;
                case -5:// Receive error
                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            liveViewErrorMessage.postValue("Buffering");
                            seconds = 0;
                            isAllZero = false;
                            if (executorServiceNightwaveUDP != null) {
                                executorServiceNightwaveUDP.shutdownNow();
                                executorServiceNightwaveUDP = null;
                            }
                            if (scheduleIsAllZero != null) {
                                scheduleIsAllZero.shutdownNow();
                                scheduleIsAllZero = null;
                            }
                            handlerForError.postDelayed(() -> {
                                if (mediaDecoder != null) {
                                    if (!isLiveViewCommand) {
                                        startListenForUDPBroadcast();
                                    }
                                    liveViewErrorMessage.postValue(null);
                                }
                            }, 3000);
                            break;
                        case OPSIN:
                            liveViewErrorMessage.postValue("Buffering");
                            stopListeningOpsinUdp();
                            handlerForError.postDelayed(() -> {
                                if (mediaDecoder != null) {
                                    Log.e(TAG, "handleUdpError: ");
                                    startListenOpsinUdp(mediaDecoder);
                                    liveViewErrorMessage.postValue(null);
                                }
                            }, 3000);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"receiveError : NW_Digital");
                            break;
                    }
                case -6:// Connection Reset By Peer
                    break;
                case -7:// Frame Missing
                    mediaDecoder.increaseMissingCount();
                    break;
                default:
                    // Default error handling
                    break;
            }
        }
    }

    public void sendSameCommand() {
        applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
        applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
        commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;

        short command = lastSentCommand.getCommand();
        Log.e(TAG, "sendSameCommand: called " + command);
        if (command != SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_COMPLETE.getValue()
                && command != SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DATA_COMPLETE.getValue()) {
            if (lastSentCommand.getData() != null && lastSentCommand.isSetCommand()) {
                byte[] data = lastSentCommand.getData();
                boolean responseRequired = lastSentCommand.isResponseRequired();
                boolean keepAlive = lastSentCommand.isKeepAlive();
                sendOpsinSetCommand(command, data, responseRequired, keepAlive);
            } else if (!lastSentCommand.isSetCommand()) {
                boolean responseRequired = lastSentCommand.isResponseRequired();
                sendOpsinGetCommand(command, responseRequired);
            } else {
                Log.e(TAG, "sendSameCommand: data null " + command);
            }
        } else {
            Log.e(TAG, "handleTcpError: Command is SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_COMPLETE.getValue()");
        }
    }

    private void tcpResourceUnAvailable() {
        try {
            if (fwMode != MODE_NONE) {
                if (retryKeepAlive >= 5) {
                    retryKeepAlive = 0;
                    callback.triggerCancelUpgrade();
                } else {
                    retryKeepAlive = retryKeepAlive + 1;
                    if (fwMode == MODE_WIFI_DIALOG) {
                        String wifi = opsinVersionDetails.getWifi();
                        Log.e(TAG, "tcpResourceUnAvailable: WIFI " + wifi + " " + retryKeepAlive);
                        if (wifi != null && !wifi.contains(opsinWiFiOldFirmwareVersion)) {//Skip sending same command if Wifi updating through localhost
                            sendSameCommand();
                        }
                    } else {
                        sendSameCommand();
                    }
                }
            } else {
                if (InetAddress.getByName(DEFAULT_IP_ADDRESS).isReachable(5000)) {
                    String riscv = opsinVersionDetails.getRiscv();
                    if (riscv != null) {
                        String[] split = riscv.split("\\.");
                        if (Integer.parseInt(split[0]) < 24) {
                            liveViewErrorMessage.postValue(null);
                            if (retryKeepAlive == 1) {
                                /*if less than 24*/
                                applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                                commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
                            } else if (retryKeepAlive == 2) {
                                disconnectResetAndConnect();
                                retryKeepAlive = 0;
                            } else {
                                retryKeepAlive = retryKeepAlive + 1;
                            }
                        } else {
                            if (applyPreset != CameraPresetsViewModel.ApplyPreset.NONE) {
                                Log.e(TAG, "tcpResourceUnAvailable: ");
                                if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                                    applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                                    synchronized (lockApplySettings) {
                                        lockApplySettings.notifyAll();
                                        responseReceived = true;
                                        isRequestFailed = true;
                                        Log.e(TAG, "tcpResourceUnAvailable: NOTIFIED: " + applyPreset.name());
                                    }
                                }
                            } else {
                                /*Retry Keep alive if try again occurs 3 times*/
                                if (retryKeepAlive == 1) {
                                    retryKeepAlive = retryKeepAlive + 1;
                                    if (scheduledExecutorServiceKeepAlive != null) {
                                        scheduledExecutorServiceKeepAlive.shutdownNow();
                                        scheduledExecutorServiceKeepAlive = null;
                                    }
                                    applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
                                    applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                                    commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
                                    isTimeOutTriggered = false;
                                    opsinPeriodicRequestDelay = 1;
                                    periodicRequestIndex = 0;
                                    countDownToHitBattery = 0;
                                    countDownToHitKeepAlive = 5;
                                    triggerKeepAlive(shouldStartStreaming);

                                    if (mediaDecoder == null && shouldStartStreaming) {
                                        callback.triggerStartOpsinLiveStreaming();
                                    }
                                } else if (retryKeepAlive == 2) {
                                    disconnectResetAndConnect();
                                    retryKeepAlive = 0;
                                } else {
                                    retryKeepAlive = retryKeepAlive + 1;
                                }
                            }

                        }
                    } else {
                        if (retryKeepAlive == 3) {
                            displaySocketError();
                            retryKeepAlive = 0;
                        } else {
                            applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                            commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
                            sendSameCommand();
                            retryKeepAlive = retryKeepAlive + 1;
                        }
                    }
                } else {
                    Log.e(TAG, "handleTcpError: Host is not Reachable");
                    liveViewErrorMessage.postValue("Buffering");
                    if (hostNotReachableCount < 3) {
                        hostNotReachableCount = hostNotReachableCount + 1;
                        liveViewErrorMessage.postValue("Buffering");
                    } else {
                        hostNotReachableCount = 0;
                        //refresh network
                        hostUnreachable.postValue(new Event<>(true));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAndStartOpsinLiveStreaming() {
        stopListeningOpsinUdp();
        callback.triggerStopOpsinLiveStreaming();
        callback.triggerStartOpsinLiveStreaming();
    }

    private void disconnectResetAndConnect() {
        liveViewErrorMessage.postValue("Buffering");
        shouldReceiveUDP = false;
        disconnectSocket();
        resetSocketState();
        handlerForError.postDelayed(this::connectSocket, 3000);
    }

    private ScheduledExecutorService scheduleIsAllZero;
    private ScheduledExecutorService retry;
    public static boolean isTimeOutTriggered = false;

    public native short calculateCRC16FromJNI(byte[] bytes, int size);

    public native int calculateCRC32FromJNI(byte[] bytes, int size);

    public byte getOpsinRxSequence() {
        return opsinRxSequence;
    }

    public byte getOpsinTxSequence() {
        return opsinTxSequence;
    }


    public interface Callbacks {
        void isTcpConnected(int state);

        void updateClient(byte[] data, int received_length, int valueId, int isValueEventAction);

        void updateLiveView(byte[] data, int received_length);

        void updateGoodBadPackets(int goodFrameCount, int jpeg_last_fragment_offset, int fps, int receiveUdpPacketsCount, int rtpSkippedPacket, int jpegBadStart, int jpegBadEndCount, int jpegCorruptCounts, int fpsBasedOnGoodPackets);

        /*Opsin*/
        void updateOpsinResponse(byte[] result);

        void sendOpsinUpgradeCancel();

        void sendOpsinUpgradeFailed();

        void triggerStopOpsinLiveStreaming();

        void triggerStartOpsinLiveStreaming();

        void triggerNextBlock();

        void triggerCancelUpgrade();
    }

    private final IBinder mBinder = new LocalBinder();

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public TCPCommunicationService getServiceInstance() {
            return TCPCommunicationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind() CALLED");
        startForegroundService();
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: running..");
        sccpMessage = SCCPMessage.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind: ");
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy timer cancelled");
    }

    public void registerClient(TCPRepository activity) {
        this.callback = (Callbacks) activity;
    }

    private static int opsinPeriodicRequestDelay = 5;
    public ArrayList<OpsinPeriodicCommand> opsinPeriodicCommandArrayList = new ArrayList<>();
    public List<OpsinPeriodicCommand> synchronizedPeriodicList = Collections.synchronizedList(opsinPeriodicCommandArrayList);

    public List<OpsinPeriodicCommand> getSynchronizedPeriodicList() {
        return synchronizedPeriodicList;
    }

    private int periodicRequestIndex = 0;

    public enum OpsinPeriodicRequest {
        NONE,
        OPSIN_PERIODIC_PROCEED_NEXT_COMMAND,
        OPSIN_PERIODIC_COMMAND_PROCEEDED,
        APPLY_OPSIN_PERIODIC_VALUES,
    }

    public static OpsinPeriodicRequest applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //    private ScheduledFuture<?> countdownFuture;
    private Future<?> countdownFuture;

    public void startCountdown(Runnable methodToTrigger) {
        cancelCountdown();
//        Log.e(TAG, "startCountdown: Called");
        countdownFuture = scheduler.schedule(methodToTrigger, 3, TimeUnit.SECONDS);
    }

    public void cancelCountdown() {
        if (countdownFuture != null/* && !countdownFuture.isDone()*/) {
//            Log.e(TAG, "cancelCountdown: Called");
            countdownFuture.cancel(true);
        }
    }

    public void connectSocket() {
        Log.e(TAG, "connectSocket: called");
        triggerKeepAlive(shouldStartStreaming);
        if (executorServiceConnectTcp == null || executorServiceConnectTcp.isShutdown()) {
            try {
                ThreadFactory threadFactory = runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setPriority(Thread.MAX_PRIORITY); // Set the desired priority
                    return thread;
                };
                executorServiceConnectTcp = Executors.newCachedThreadPool(threadFactory);
                executorServiceConnectTcp.execute(() -> {
                    try {
                        if (InetAddress.getByName(DEFAULT_IP_ADDRESS).isReachable(5000)) {
                            Log.e(TAG, "connectSocket: Host is reachable!");
                            boolean isNightwave = DEFAULT_PORT != 3915;
                            int sockfd = connectTcpSocket(DEFAULT_IP_ADDRESS, DEFAULT_PORT, isNightwave);
                            if (sockfd >= 0) {
                                shouldRestartSocketListen = true;
                                if (callback != null) {
                                    applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                                    commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
                                    commandStuckCount = 0;
                                    setState(STATE_CONNECTED);
                                    Log.e(TAG, "WIFI_SOCKET CONNECTED " + shouldRestartSocketListen + " " + shouldStartStreaming);
                                }
                                startTcpReceiver();
                                switch (currentCameraSsid) {
                                    case OPSIN:
                                        if (shouldStartStreaming && fwMode == MODE_NONE) {
                                            try {
                                                String riscv = opsinVersionDetails.getRiscv();
                                                if (riscv != null) {
                                                    String[] split = riscv.split("\\.");
                                                    if (Integer.parseInt(split[0]) > 22) {
                                                        callback.triggerStartOpsinLiveStreaming();
                                                    }
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        } else if (fwMode != MODE_NONE) {
                                            if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                                                applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                                                synchronized (lockApplySettings) {
                                                    lockApplySettings.notifyAll();
                                                    responseReceived = true;
                                                }
                                            }
                                            sendSameCommand();
                                        }
                                        break;
                                    case NIGHTWAVE:
                                        if (fwMode != MODE_NONE) {
                                            CommandError error = new CommandError();
                                            error.setError("Failed");
                                            fwUpdateFailed.postValue(new Event<>(error));
                                        }
                                        break;
                                    case NIGHTWAVE_DIGITAL:
                                        Log.e(TAG,"connectSocket : NW_Digital");
                                        break;
                                }
                            } else {
                                switch (sockfd) {
                                    case -2:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Connection has been aborted by the local system.");
                                        break;
                                    case -3:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: The remote host abruptly terminated the connection while data was still being transmitted.");
                                        break;
                                    case -4:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Connection timeout.");
                                        break;
                                    case -5:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Connection refused by the remote host.");
                                        break;
                                    case -6:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Destination host is unreachable.");
                                        break;
                                    case -7:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Network is down.");
                                        break;
                                    case -8:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Network is unreachable.");
                                        break;
                                    case -9:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: Network dropped connection on reset.");
                                        break;
                                    default:
                                        Log.e(TAG, "WIFI_SOCKET FAILED: OTHER REASON " + sockfd);
                                        break;
                                }
                                disconnectSocket();
                                resetSocketState();
                                retrySocketConnection();
                            }
                        } else {
                            if (hostNotReachableCount < 3) {
                                hostNotReachableCount = hostNotReachableCount + 1;
                                if (executorServiceConnectTcp != null) {
                                    executorServiceConnectTcp.shutdownNow();
                                    executorServiceConnectTcp = null;
                                }
                                if (executorServiceTcpReceiver != null) {
                                    executorServiceTcpReceiver.shutdown();
                                    executorServiceTcpReceiver = null;
                                }
                                handlerForError.postDelayed(this::connectSocket, 5000);
                            } else {
                                hostNotReachableCount = 0;
                                //refresh network
                                hostUnreachable.postValue(new Event<>(true));
                            }
                            Log.e(TAG, "connectSocket: Host is not reachable.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    while (shouldRestartSocketListen) {
                        try {
                            if (queue.size() > 0) {
                                inputArray = queue.poll();
                                if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED) {
                                    if (inputArray != null) {
                                        Log.d(TAG, "BYTE SEND: " + Arrays.toString(inputArray) + " Queue Size: " + queue.size());
                                        int bytesSent = sendTcpCommand(inputArray);
                                        if (bytesSent > 0) {
                                            Log.d(TAG, "BYTE SENT: " + bytesSent);
                                            isCommandSent = true;
                                            lastCommandSentTime = System.currentTimeMillis();
                                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                                                if (checkIfOlderVersion()) {
                                                    if (lastSentCommand.getData() != null) {
                                                        if (fwMode != MODE_NONE) {
                                                            startCountdown(() -> callback.triggerNextBlock());
                                                        } else {
                                                            startCountdown(this::sendSameCommand);
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            switch (bytesSent) {
                                                case -2:// Connection has been aborted by the local system.
                                                    Log.e(TAG, "BYTE SEND FAILED: Connection has been aborted by the local system.");
                                                    displaySocketError();
                                                    break;
                                                case -3:// The remote host abruptly terminated the connection while data was still being transmitted.
                                                    Log.e(TAG, "BYTE SEND FAILED: The remote host abruptly terminated the connection while data was still being transmitted.");
                                                    displaySocketError();
                                                    break;
                                                case -4://Connection timeout.
                                                    Log.e(TAG, "BYTE SEND FAILED: Connection timeout.");
                                                    displaySocketError();
                                                    break;
                                                case -5://Connection refused by the remote host.
                                                    Log.e(TAG, "BYTE SEND FAILED: Connection refused by the remote host.");
                                                    displaySocketError();
                                                    break;
                                                case -6://Destination host is unreachable.
                                                    Log.e(TAG, "BYTE SEND FAILED: Destination host is unreachable.");
                                                    displaySocketError();
                                                    break;
                                                case -7://Network is down.
                                                    Log.e(TAG, "BYTE SEND FAILED: Network is down.");
                                                    displaySocketError();
                                                    break;
                                                case -8:// Network is unreachable.
                                                    Log.e(TAG, "BYTE SEND FAILED:  Network is unreachable.");
                                                    displaySocketError();
                                                    break;
                                                case -9://Network dropped connection on reset.
                                                    Log.e(TAG, "BYTE SEND FAILED: Network dropped connection on reset.");
                                                    displaySocketError();
                                                    break;
                                                case -10://Attempt was made to write to a pipe or socket that has been closed on the reading end.
                                                    Log.e(TAG, "BYTE SEND FAILED: Attempt was made to write to a pipe or socket that has been closed on the reading end.");
                                                    displaySocketError();
                                                    break;
                                                case -11://Attempt was made to write to a pipe or socket that has been closed on the reading end.
                                                    Log.e(TAG, "BYTE SEND FAILED: Bad file descriptor");
                                                    break;
                                                case -13://Attempt was made to write to a pipe or socket that has been closed on the reading end.
                                                    Log.e(TAG, "BYTE SEND FAILED: write Null Array");
                                                    break;
                                                default:
                                                    Log.e(TAG, "BYTE SEND FAILED: Generic Exception");
                                                    retryCount = 0;
                                                    disconnectSocket();
                                                    resetSocketState();
                                                    retrySocketConnection();
                                                    break;
                                            }
                                        }
                                    } else {
                                        switch (currentCameraSsid) {
                                            case NIGHTWAVE:
                                                sendCommand();
                                                break;
                                            case OPSIN:
                                                sendSameCommand();
                                                break;
                                            case NIGHTWAVE_DIGITAL:
                                                Log.e(TAG,"sendCmnd() : NW_Digital");
                                                break;
                                        }
                                        Log.e(TAG, "WRITE FAILED : inputArray null " + " queue: " + queue.size());
                                    }
                                } else {
                                    queue.clear();
                                    inputArray = null;
                                    Log.e(TAG, "WRITE FAILED : mWifiState " + mWifiState + " mState: " + mState + " fwMode: " + fwMode);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "connectSocket: " + executorServiceConnectTcp.isShutdown());
        }

    }

    private void startTcpReceiver() {
        if (executorServiceTcpReceiver == null || executorServiceTcpReceiver.isShutdown()) {
            try {
                Log.e(TAG, "startTcpReceiver: true");
                ThreadFactory threadFactory = runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setPriority(Thread.MAX_PRIORITY); // Set the desired priority
                    return thread;
                };
                executorServiceTcpReceiver = Executors.newCachedThreadPool(threadFactory);
                executorServiceTcpReceiver.execute(this::receiveTcpResponse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "startTcpReceiver: false");
        }
    }

    private void displaySocketError() {
        // Handle sending error
        isCommandSent = false;
        commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
        applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;

        Log.d(TAG, "BYTE SEND FAILED: " + Arrays.toString(inputArray) + " Queue Size: " + queue.size());
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                sendSoTimeout();
                break;
            case OPSIN:
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                    applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                    synchronized (lockApplySettings) {
                        lockApplySettings.notifyAll();
                        responseReceived = true;
                    }
                } else {
                    if (scheduledESMonitorSOTimeout != null) {
                        retryCount = 0;
                        scheduledESMonitorSOTimeout.shutdownNow();
                        scheduledESMonitorSOTimeout = null;
                    }
                    opsinCameraErrorMessage.postValue(new Event<>(true));
                }
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"displaySOcketError : NW_Digital");
                break;
        }
    }

    private void broadcastNightwaveResponse(byte[] data, int received_length, int valueId, int isValueEventAction) {
        if (callback != null) {
            callback.updateClient(data, received_length, valueId, isValueEventAction);
        }
    }

    private void broadcastOpsinResponse(byte[] result) {
        if (callback != null) {
            callback.updateOpsinResponse(result);
        }
    }

    private final int DELAY_FOR_PERIODIC_REQUEST = 1;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final OpsinPeriodicCommand keepAliveModel = new OpsinPeriodicCommand();
    private final OpsinPeriodicCommand batteryModel = new OpsinPeriodicCommand();
    private int countDownToHitKeepAlive = 5;
    private int countDownToHitBattery = 20;
    private int commandStuckCount = 0;

    private void sendSoTimeout() {
        try {
            if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                synchronized (lockApplySettings) {
                    lockApplySettings.notifyAll();
                    responseReceived = true;
                }
            } else {
                inputArray[0] = (byte) SCCPConstants.SCCP_MSG_TYPE.SO_READ_TIMEOUT.getValue();
                byte resultSeqA = inputArray[6];
                byte resultSeqB = inputArray[7];
                short resultSeq = (short) (((resultSeqB & 0xFF) << 8) | (resultSeqA & 0xFF));

                RequestQueue request = requestQueue.get(resultSeq);
                byte valueId = 0;
                if (request != null) {
                    byte[] reqBytes = request.getData();
                    valueId = reqBytes != null ? reqBytes[1] : 0;
                    requestQueue.remove(resultSeq);
                }
                broadcastNightwaveResponse(inputArray, inputArray.length, valueId, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendSoTimeoutException: " + e.getLocalizedMessage());
        }
    }

    public void cancelOpsinPeriodicTimer() {
        Log.e(TAG, "cancelOpsinPeriodicTimer: ");
        shouldStartStreaming = false;
        synchronizedPeriodicList.clear();
        if (applyOpsinPeriodicRequest == TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
            applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_PROCEED_NEXT_COMMAND;
        }
        if (scheduledExecutorServiceKeepAlive != null) {
            scheduledExecutorServiceKeepAlive.shutdownNow();
            scheduledExecutorServiceKeepAlive = null;
        }
        countDownToHitKeepAlive = 5;
        countDownToHitBattery = 20;
        applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
        commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
    }

    public void triggerKeepAlive(boolean shouldStartStreamingg) {
        shouldStartStreaming = shouldStartStreamingg;
        if (!shouldStartStreamingg && isOpsinLiveStreamingStarted && mState == STATE_CONNECTED) {
            callback.triggerStopOpsinLiveStreaming();
        }
        Log.e(TAG, "triggerKeepAlive: " + shouldStartStreamingg);
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                /*Keep Alive for every 15 seconds*/
                if (scheduledExecutorServiceKeepAlive == null || scheduledExecutorServiceKeepAlive.isShutdown()) {
                    scheduledExecutorServiceKeepAlive = Executors.newScheduledThreadPool(1);
                    scheduledExecutorServiceKeepAlive.scheduleWithFixedDelay(() -> {
                        if (mState == STATE_CONNECTED && fwMode == MODE_NONE && !isCommandSent) {
                            Log.e("KEEP-ALIVE", "Called");
                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                             /*  here below changed 'LedEnable' command instead of 'CameraInfo' command because while camera name have contain restricted special characters
                             to avoid check firmware versions and show custom dialog with edit text view so, we avoid to call every 15sec(block navigate to live view from home screen)
                              */
                                byte value_id = (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue();
                                getValue(value_id);
                            }
                        }
                    }, 15, 15, TimeUnit.SECONDS);
                }
                break;
            case OPSIN:
                if (scheduledExecutorServiceKeepAlive == null || scheduledExecutorServiceKeepAlive.isShutdown()) {
                    scheduledExecutorServiceKeepAlive = Executors.newScheduledThreadPool(1);
                    scheduledExecutorServiceKeepAlive.scheduleWithFixedDelay(() -> {
                        countDownToHitKeepAlive = countDownToHitKeepAlive + 1;
                        countDownToHitBattery = countDownToHitBattery + 1;
                        if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED && fwMode == MODE_NONE && applyPreset == CameraPresetsViewModel.ApplyPreset.NONE) {
                            if (!isTimeOutTriggered && opsinPeriodicRequestDelay == DELAY_FOR_PERIODIC_REQUEST && periodicRequestIndex == 0) {
                                opsinPeriodicRequestDelay = 0;
                                applyOpsinPeriodicRequest = OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
                                applyPeriodicRequest();
                            } else {
                                if (opsinPeriodicRequestDelay > 5) {
                                    opsinPeriodicRequestDelay = 0;
                                    periodicRequestIndex = 0;
                                }
                                opsinPeriodicRequestDelay = opsinPeriodicRequestDelay + 1;
                            }
                        } else if (applyPreset != CameraPresetsViewModel.ApplyPreset.NONE) {
                            applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                }
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"triggerKeepAlive : NW_Digital");
                break;
        }
    }

    public void applyPeriodicRequest() {
        if (mState == STATE_CONNECTED
                && synchronizedPeriodicList.size() > 0
                && periodicRequestIndex < synchronizedPeriodicList.size()
                && fwMode == MODE_NONE
                && applyPreset == CameraPresetsViewModel.ApplyPreset.NONE) {
            if (applyOpsinPeriodicRequest == OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES) {

                if (commandRequested == TCPRepository.COMMAND_REQUESTED.NONE) {
                    applyOpsinPeriodicRequest = OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED;

                    if (synchronizedPeriodicList.size() != 1) {
                        short sccp_command_keep_alive = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_SCCP_MANAGEMENT.SCCP_CMD_KEEPALIVE.getValue();
                        keepAliveModel.setCommand(sccp_command_keep_alive);

                        short sccp_command_battery = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_POWER_MANAGEMENT.SCCP_CMD_POWER_GET_BATTERY.getValue();
                        batteryModel.setCommand(sccp_command_battery);

                        int keepAliveIndex = getSynchronizedPeriodicList().indexOf(keepAliveModel);
                        int batteryIndex = getSynchronizedPeriodicList().indexOf(batteryModel);
//                        Log.e(TAG, "applyPeriodicRequest: " + keepAliveIndex + " " + batteryIndex + " " + synchronizedPeriodicList.size());

                        try {
                            if (periodicRequestIndex == -1) {
                                periodicRequestIndex = 0;
                            }
                            if (countDownToHitKeepAlive >= 5) {
                                Collections.swap(synchronizedPeriodicList, keepAliveIndex, periodicRequestIndex);
                                countDownToHitKeepAlive = 0;
                            } else if (countDownToHitBattery >= 20) {
                                Collections.swap(synchronizedPeriodicList, batteryIndex, periodicRequestIndex);
                                countDownToHitBattery = 0;
                            }

                            OpsinPeriodicCommand opsinPeriodicCommand = synchronizedPeriodicList.get(periodicRequestIndex);
                            boolean isGetCommand = opsinPeriodicCommand.isGetCommand();
                            short command = opsinPeriodicCommand.getCommand();
                            boolean responseRequired = opsinPeriodicCommand.isResponseRequired();
                            if (isGetCommand) {
                                sendOpsinGetCommand(command, responseRequired);
                            } else {
                                boolean keepAlive = opsinPeriodicCommand.isKeepAlive();
                                byte[] data = opsinPeriodicCommand.getData();
                                sendOpsinSetCommand(command, data, responseRequired, keepAlive);
                            }
                            applyPeriodicRequest();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (countDownToHitKeepAlive >= 5) {
                            OpsinPeriodicCommand opsinPeriodicCommand = synchronizedPeriodicList.get(periodicRequestIndex);
                            boolean isGetCommand = opsinPeriodicCommand.isGetCommand();
                            short command = opsinPeriodicCommand.getCommand();
                            boolean responseRequired = opsinPeriodicCommand.isResponseRequired();
                            if (isGetCommand) {
                                sendOpsinGetCommand(command, responseRequired);
                            } else {
                                boolean keepAlive = opsinPeriodicCommand.isKeepAlive();
                                byte[] data = opsinPeriodicCommand.getData();
                                sendOpsinSetCommand(command, data, responseRequired, keepAlive);
                            }
                            countDownToHitKeepAlive = 0;
                            applyPeriodicRequest();
                        }
                    }

                } else {
                    mHandler.postDelayed(() -> {
                        opsinPeriodicRequestDelay = 0;
                        commandStuckCount = commandStuckCount + 1;
                        applyPeriodicRequest();
                    }, 300);
                    Log.e(TAG, "applyPeriodicRequest: waitPeriodicTimer " + applyOpsinPeriodicRequest.name());
                }
            } else if (applyOpsinPeriodicRequest == OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                mHandler.postDelayed(() -> {
                    opsinPeriodicRequestDelay = 0;
                    applyPeriodicRequest();
                }, 300);
            } else if (applyOpsinPeriodicRequest == OpsinPeriodicRequest.OPSIN_PERIODIC_PROCEED_NEXT_COMMAND) {
                periodicRequestIndex++;
                opsinPeriodicRequestDelay = 0;
                applyOpsinPeriodicRequest = OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
                applyPeriodicRequest();
            }
        } else {
            applyOpsinPeriodicRequest = OpsinPeriodicRequest.NONE;
            periodicRequestIndex = 0;
            opsinPeriodicRequestDelay = 0;
            Log.d("TAG", "periodicRequest: complete");
        }
    }

    public void sendLastOpsinDataBlock() {
        short command = lastSentCommand.getCommand();
        byte[] data = lastSentCommand.getData();
        boolean responseRequired = lastSentCommand.isResponseRequired();
        boolean keepAlive = lastSentCommand.isKeepAlive();
        sendOpsinSetCommand(command, data, responseRequired, keepAlive);
    }


    /*Nightwave Send Commands*/
    public void startWiFiDialogUpdate(String wiFiDialogUrl) {
        byte[] data = wiFiDialogUrl.getBytes();//10
        byte[] newArray = new byte[data.length + 1];//11
        System.arraycopy(data, 0, newArray, 0, data.length);//0-9
        newArray[data.length] = 0x00;


        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.DIALOG_OTA_START.getValue());
        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setBlock_num((byte) 0);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.LOW.getValue());
        sccpMessage.setData_size((short) newArray.length);
        sccpMessage.setData(newArray);
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void startRiscUpdate(int length) {
        Log.e(TAG, "startRiscUpdate: ");
        byte[] data = convertIntToByteArray2(length);

        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.UPDATE_START.getValue());
        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setBlock_num((byte) 0);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.MED.getValue());
        sccpMessage.setData_size((short) data.length);
        sccpMessage.setData(data);
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void sendDataBlockToRiscv(byte[] data, short blockNum) {
        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.DATA_TRANSFER.getValue());
        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setBlock_num(blockNum);
        sccpMessage.setData_size((short) data.length);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.MED.getValue());
        sccpMessage.setData(data);
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void completeUpdate() {
        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.UPDATE_COMPLETE.getValue());
        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.MED.getValue());
        sccpMessage.setData_size((short) 0);
        sccpMessage.setData(new byte[]{0x00});
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void fwUpdateCancel() {
        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.UPDATE_CANCEL.getValue());
        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.MED.getValue());
        sccpMessage.setData_size((short) 0);
        sccpMessage.setData(new byte[]{0x00});
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void resetFpga(boolean isGolden) {
        if (isGolden) {
            sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.FPGA_RESET_GOLD.getValue());
        } else {
            sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.FPGA_RESET.getValue());
        }

        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.LOW.getValue());
        sccpMessage.setData_size((short) 0);
        sccpMessage.setData(new byte[]{0x00});
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void completeUpdateWifiDialog() {
        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.DIALOG_OTA_COMPLETE.getValue());
        sccpMessage.setValueEventActionIds((byte) 0);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.MED.getValue());
        sccpMessage.setData_size((short) 0);
        sccpMessage.setData(new byte[]{0x00});
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void setValue(byte value_id, byte[] data) {
        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.SET_VALUE.getValue());
        sccpMessage.setValueEventActionIds(value_id);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.LOW.getValue());
        sccpMessage.setData_size((short) data.length);
        sccpMessage.setData(data);
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    public void getValue(byte value_id) {
        Log.e(TAG, "getValue: " + value_id);
        sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.GET_VALUE.getValue());
        sccpMessage.setValueEventActionIds(value_id);
        sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.LOW.getValue());
        sccpMessage.setData_size((short) 0);
        sccpMessage.setData(new byte[]{0x00});
        request.setIsValueEventAction(VALUE);
        sendCommand();
    }

    private boolean isLiveViewCommand = false;

    public void startStopLiveView(byte value_id) {
        if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED) {
            sccpMessage.setMsg_type((byte) SCCPConstants.SCCP_MSG_TYPE.DO_ACTION.getValue());
            sccpMessage.setValueEventActionIds(value_id);
            sccpMessage.setPriority((byte) SCCPConstants.SCCP_PRIORITY.LOW.getValue());
            sccpMessage.setData_size((short) 4);
            sccpMessage.setData(new byte[]{0x00, 0x00, 0x00, 0x00});
            request.setIsValueEventAction(ACTION);
            sendCommand();
            if (value_id == (byte) SCCPConstants.SCCP_ACTION.StopLiveView.getValue()) {
                shouldReceiveUDP = false;
                isLiveViewCommand = false;
                safeQueue.clear();
                arrayList.clear();

                startStopUdpReceiverNW(false);


                if (executorServiceNightwaveUDP != null) {
                    executorServiceNightwaveUDP.shutdownNow();
                    executorServiceNightwaveUDP = null;
                }

                if (scheduledFPSCalculation != null) {
                    scheduledFPSCalculation.shutdownNow();
                    scheduledFPSCalculation = null;
                }
                isGoodFrameStarted = false;
            } else {
                isLiveViewCommand = true;
                goodFrameCount = 0;
                totalFrameCount = 0;
                receivedUdpPacketcount = 0;
                jpeg_last_fragment_offset = 0;
                jpegBadStartPacketCount = 0;
                jpegBadEndPacketCount = 0;
                badOffset = 0;
                lastRTPSequenceNumber = -1;
                rtpSkippedPacketCount = 0;
                isOutOfOrder = false;
                safeQueue.clear();
                arrayList.clear();
                previousFrameCount = 0;
                currentFrameCount = 0;
                fps = 0;
                fpsCounter.reset();
            }
        }
    }

    private void sendCommand() {
        if (sequence == MAX_SEQUENCE_COUNT) sequence = 0;
        byte[] sequenceNumber = shortToByteArray(sequence);
        byte[] blockNum = shortToByteArray(sccpMessage.getBlock_num());
        byte[] dataSize = shortToByteArray(sccpMessage.getData_size());

        ByteBuffer bb = ByteBuffer.allocate(sccpMessage.getTotalLength());
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(sccpMessage.getMsg_type());//msg_type
        bb.put(sccpMessage.getValueEventActionIds());//value_id
        bb.put(blockNum);//block_num
        bb.put(dataSize);//data_size
        bb.put(sequenceNumber);//sequence_num
        bb.put(sccpMessage.getPriority());//priority
        bb.put(sccpMessage.getReserved());//Reserved
        bb.put(sccpMessage.getData());//data
        bb.flip();

        request.setData(bb.array());
        requestQueue.put(sequence, request);
        queue.add(bb.array());
        sequence++;
    }


    /*OPSIN SEND COMMANDs*/
    private final byte PROTOCOL_HEADER_VERSION = 3;
    private final byte[] SOF_MAGIC = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xEB, (byte) 0xFE};
    private final byte EOF = (byte) 0xBE;
    private final int HEADER_SIZE = 10;
    private final int CRC16_BUFFER_SIZE = 2;
    private final int CRC32_BUFFER_SIZE = 4;
    private final int EOF_SIZE = 1;
    private final int CRC16_TOTAL_BUFFER = CRC16_BUFFER_SIZE + HEADER_SIZE;
    private final int CRC32_TOTAL_BUFFER = CRC16_TOTAL_BUFFER + CRC32_BUFFER_SIZE + EOF_SIZE;
    public static LastSentCommand lastSentCommand = new LastSentCommand();

    public void sendOpsinGetCommand(short command, boolean isResponseRequired) {
        Log.d(TAG, "SENT COMMAND: OPSIN_GET_COMMAND : " + command + " applyOpsinPeriodicRequest: " + applyOpsinPeriodicRequest.name() + " commandRequested: " + commandRequested.name() + " Queue Size: " + queue.size() + mWifiState + " " + mState);
        lastSentCommand.setIsSetCommand(false);
        lastSentCommand.setCommand(command);
        lastSentCommand.setResponseRequired(isResponseRequired);

        if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED) {
            if (opsinTxSequence == Byte.toUnsignedInt((byte) -1)) opsinTxSequence = 0;

            if (opsinRxSequence == Byte.toUnsignedInt((byte) -1)) opsinRxSequence = 0;

            short header = headerToSend((short) 0, PROTOCOL_HEADER_VERSION);
            byte[] lengthVersion = shortToByteArray(header);

            ByteBuffer bbRequest = ByteBuffer.allocate(HEADER_SIZE);
            bbRequest.put(SOF_MAGIC);//sof+magic
            bbRequest.put(lengthVersion);//data length, Protocol header version
            bbRequest.put(isResponseRequired ? (byte) 0x01 : (byte) 0x00);//flags
            bbRequest.put(opsinTxSequence);//sequence_num
            bbRequest.put(shortToByteArray(command));//command

            //CRC 16
            ByteBuffer bbCRC16 = ByteBuffer.allocate(CRC16_TOTAL_BUFFER);
            byte[] crcArr16 = bbRequest.array();
            short crc16 = calculateCRC16FromJNI(crcArr16, crcArr16.length);
            bbCRC16.put(bbRequest.array());
            bbCRC16.put(shortToByteArray(crc16));

            //CRC 32
            ByteBuffer bbCRC32 = ByteBuffer.allocate(CRC32_TOTAL_BUFFER);
            byte[] crcArr32 = bbCRC16.array();
            int crc32 = calculateCRC32FromJNI(crcArr32, crcArr32.length);
            bbCRC32.put(bbCRC16.array());
            bbCRC32.put(intToBytess(crc32));


            //EOF
            bbCRC32.put(EOF);
            bbCRC32.flip();

            request.setData(bbCRC32.array());
            queue.add(bbCRC32.array());
            opsinTxSequence++;
            opsinRxSequence++;

            Log.e(TAG, "sendOpsinGetCommand: " + queue.size());
        } else {
//            connectSocket();
        }
    }

    public void sendOpsinSetCommand(short command, byte[] data, boolean isResponseRequired, boolean isKeepAlive) {
        Log.d(TAG, "SENT COMMAND: OPSIN_SET_COMMAND : " + command + " applyOpsinPeriodicRequest: " + applyOpsinPeriodicRequest.name() + " commandRequested: " + commandRequested.name() + " Queue Size: " + queue.size());
        lastSentCommand.setIsSetCommand(true);
        lastSentCommand.setCommand(command);
        lastSentCommand.setResponseRequired(isResponseRequired);
        lastSentCommand.setData(data);
        lastSentCommand.setKeepAlive(isKeepAlive);

        if (mWifiState == WIFI_STATE_CONNECTED && mState == STATE_CONNECTED) {
            if (opsinTxSequence == Byte.toUnsignedInt((byte) -1)) opsinTxSequence = 0;

            if (opsinRxSequence == Byte.toUnsignedInt((byte) -1)) opsinRxSequence = 0;

            int dataLength = data.length;
            short header = headerToSend((short) data.length, PROTOCOL_HEADER_VERSION);
            byte[] lengthVersion = shortToByteArray(header);

            ByteBuffer bbRequest = ByteBuffer.allocate(HEADER_SIZE);
            bbRequest.put(SOF_MAGIC);//sof+magic
            bbRequest.put(lengthVersion);//data length, Protocol header version
            bbRequest.put(isResponseRequired ? (byte) 0x01 : (byte) 0x00);//flags
            bbRequest.put(opsinTxSequence);//sequence_num
            bbRequest.put(shortToByteArray(command));//command
            //CRC 16
            ByteBuffer bbCRC16 = ByteBuffer.allocate(CRC16_TOTAL_BUFFER + dataLength);
            byte[] crcArr16 = bbRequest.array();
            short crc16 = calculateCRC16FromJNI(crcArr16, crcArr16.length);
            bbCRC16.put(bbRequest.array());
            bbCRC16.put(shortToByteArray(crc16));
            bbCRC16.put(data);

            //CRC 32
            ByteBuffer bbCRC32 = ByteBuffer.allocate(CRC32_TOTAL_BUFFER + dataLength);
            byte[] crcArr32 = bbCRC16.array();
            int crc32 = calculateCRC32FromJNI(crcArr32, crcArr32.length);
            bbCRC32.put(bbCRC16.array());
            bbCRC32.put(intToBytess(crc32));

            //EOF
            bbCRC32.put(EOF);
            bbCRC32.flip();

            request.setData(bbCRC32.array());
            queue.add(bbCRC32.array());
            opsinTxSequence++;
            if (!isKeepAlive) {
                opsinRxSequence++;
            }
//            Log.e(TAG, "sendOpsinSetCommand: " + queue.size());
        }
    }


    /*Nightwave UDP Listener*/
    public static boolean shouldReceiveUDP = false;
    private int receivedUdpPacketcount = 0;
    public static int seconds = 0;
    private int receivedPacketSize = 0;
    private int noudpRetryCount = 0;
    private int previousFrameCount = 0;
    private int currentFrameCount = 0;
    private int outOfOrderCount = 0;
    private int fps = 0;

    // frame variables
    private final int RTP_HEADER = 12;
    private final int JPEG_HEADER = 8;
    private final int RTP_JPEG_HEADER_SIZE = RTP_HEADER + JPEG_HEADER;
    private final int startOfFrame = 0xFFD8;
    private final int endOfFrame = 0xFFD9;
    private boolean isGoodFrameStarted = false;
    private int goodFrameCount = 0;
    private int totalFrameCount = 0;
    private int jpeg_last_fragment_offset = 0;
    private int lastRTPSequenceNumber = -1;
    private int rtpSkippedPacketCount = 0;
    private int rtpDifferentEndPacketCount = 0;
    private int badOffset = 0;
    private boolean isOutOfOrder = false;
    private int previousSequenceNumber = 0;
    private boolean isReachedMaxSeqNum = false;
    private final ArrayList<byte[]> arrayList = new ArrayList<>();
    private final PriorityQueue<Packets> safeQueue = new PriorityQueue<>(Comparator.comparingInt(Packets::getSequenceNumber));
    private final PriorityQueue<Packets> tempSafeList = new PriorityQueue<>(Comparator.comparingInt(Packets::getSequenceNumber));

    public void startListenForUDPBroadcast() {
        try {
            Log.e(TAG, "startListenForUDPBroadcast: ");
            /* for this up to 30 sec receive all packets are zero*/
            if (scheduleIsAllZero == null) {
                scheduleIsAllZero = Executors.newScheduledThreadPool(1);
                scheduleIsAllZero.scheduleWithFixedDelay(() -> {
                    if (isLiveViewCommand) {
                        if (seconds != 30) {
                            Log.e(TAG, "scheduleIsAllZero: Before 30 seconds false");
                            if (isAllZero) {
                                Log.e(TAG, "scheduleIsAllZero: Before 30 seconds true");
                                scheduleIsAllZero.shutdownNow();
                            }
                        } else {
                            if (!isAllZero) {
                                hasAllPacketZero.postValue(new Event<>(true));
                                Log.e(TAG, "scheduleIsAllZero: After 30 seconds false");
                                scheduleIsAllZero.shutdownNow();
                            } else {
                                Log.e(TAG, "scheduleIsAllZero: After 30 seconds true");
                                scheduleIsAllZero.shutdownNow();
                            }
                        }
                        seconds++;
                    }

                }, 1, 1, TimeUnit.SECONDS);
            }

            /* for this upto 5 sec udp packets not receive*/
            checkNoUdpState();

            if (scheduledFPSCalculation == null || scheduledFPSCalculation.isShutdown()) {
                scheduledFPSCalculation = Executors.newScheduledThreadPool(1);
                scheduledFPSCalculation.scheduleWithFixedDelay(() -> {
                    currentFrameCount = goodFrameCount + jpegBadEndPacketCount + outOfOrderCount;
                    if (previousFrameCount != 0) {
                        fps = (int) (((float) currentFrameCount - (float) previousFrameCount)) / 10;
                    }
                    previousFrameCount = currentFrameCount;
                }, 0, 10, TimeUnit.SECONDS);
            }


            if (executorServiceNightwaveUDP == null || executorServiceNightwaveUDP.isShutdown()) {
                ThreadFactory threadFactory = runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setPriority(Thread.MAX_PRIORITY); // Set the desired priority
                    return thread;
                };

                executorServiceNightwaveUDP = Executors.newCachedThreadPool(threadFactory);
                executorServiceNightwaveUDP.execute(() -> {
                    seconds = 0;
                    receivedPacketSize = 0;
                    startStopUdpReceiverNW(true);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeNWUdp() {
        seconds = 0;
        isAllZero = false;
        if (executorServiceNightwaveUDP != null) {
            executorServiceNightwaveUDP.shutdownNow();
            executorServiceNightwaveUDP = null;
        }
        if (scheduleIsAllZero != null) {
            scheduleIsAllZero.shutdownNow();
            scheduleIsAllZero = null;
        }
        startStopUdpReceiverNW(false);
    }

    private void checkNoUdpState() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (receivedPacketSize == 0) {
                    if (noudpRetryCount == 0) {
                        resetFpga(false);
                        checkNoUdpState();
                    } else {
                        hasAllPacketZero.postValue(new Event<>(true));
                        timer.cancel();
                    }
                    noudpRetryCount = noudpRetryCount + 1;
                }
            }
        }, 5000);
    }

    private void createBuffer(byte[] bytesAddressPair, int received_length) {
        if (callback != null) {
            int rtpSequenceNumber = (bytesAddressPair[2] & 0x00FF) * 256 + (bytesAddressPair[3] & 0x00FF);
            handleMaxSequenceNumber(rtpSequenceNumber);
            if (!isReachedMaxSeqNum) {
                safeQueue.add(new Packets(rtpSequenceNumber, received_length, bytesAddressPair.clone()));
                if (safeQueue.size() > 90) {
                    iterateBuffer();
                }
            } else {
                tempSafeList.add(new Packets(rtpSequenceNumber, received_length, bytesAddressPair.clone()));
                iterateBuffer();
                if (safeQueue.isEmpty()) {
                    safeQueue.addAll(tempSafeList);
                    tempSafeList.clear();
                    isReachedMaxSeqNum = false;
                }
            }
        }
    }

    private void iterateBuffer() {
        try {
            Packets packets = safeQueue.poll();
            int sequenceNumber = packets.getSequenceNumber();
            int receivedLength = packets.getReceivedLength();
            byte[] array = packets.getArray();

            checkSequenceNumber(sequenceNumber);
            broadcastNightwaveResponse(array, receivedLength, sequenceNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastNightwaveResponse(byte[] bytesAddressPair, int received_length, int sequenceNumber) {
        if (!isAllZero) isAllZero = checkIsAllZero(bytesAddressPair, received_length);

        if (callback != null) {
            if (previousSequenceNumber != sequenceNumber) {
                previousSequenceNumber = sequenceNumber;
                int jpeg_fragment_offset = (bytesAddressPair[RTP_HEADER + 1] & 0x00FF) * 65536 + (bytesAddressPair[RTP_HEADER + 2] & 0x00FF) * 256 + (bytesAddressPair[RTP_HEADER + 3] & 0x00FF);
                int jpeg_frame_start = (int) (bytesAddressPair[RTP_HEADER + 8] & 0x00FF) * 256 + (int) (bytesAddressPair[RTP_HEADER + 9] & 0x00FF);

                if (jpeg_fragment_offset == 0) {
                    if (jpeg_frame_start == startOfFrame) {
                        jpeg_last_fragment_offset = 0;
                        arrayList.clear();
                        isOutOfOrder = false;
                        addToList(bytesAddressPair, received_length);
                        isGoodFrameStarted = true;
                    } else {
                        isGoodFrameStarted = false;

                        jpeg_last_fragment_offset = 0;
                        arrayList.clear();
                        isOutOfOrder = false;
                        jpegBadStartPacketCount++;
                    }
                } else if (isGoodFrameStarted && jpeg_fragment_offset == jpeg_last_fragment_offset + 1024) {
                    addToList(bytesAddressPair, received_length);
                    if (received_length < 1044) {
                        int jpeg_frame_end = validateEndOfFrame(bytesAddressPair, received_length);
                        if (jpeg_frame_end == endOfFrame) {
                            if (isGoodFrameStarted && !isOutOfOrder) {
                                byte[] concatenate = concatenate(arrayList);
                                int length = concatenate.length;
                                callback.updateLiveView(concatenate, length);
                                isGoodFrameStarted = false;
                                goodFrameCount = goodFrameCount + 1;
                                totalFrameCount = totalFrameCount + 1;
                                /*FPS calculation for video recording based on good frames only*/
                                fpsCounter.incrementFrameCount();
                                fpsBasedOnGoodPackets = (int) fpsCounter.calculateFPS();

                                updateUdpPacketsInfo();
                            } else if (isGoodFrameStarted) {
                                outOfOrderCount = outOfOrderCount + 1;
                            }
                        } else {
                            jpegBadEndPacketCount++;
                            updateUdpPacketsInfo();
                        }
                    }
                } else if (isGoodFrameStarted) {
                    isOutOfOrder = true;
                    badOffset++;
                }
                if (isGoodFrameStarted) jpeg_last_fragment_offset = jpeg_fragment_offset;
            } else {
                Log.d(TAG, "broadcastIntent: duplicate sequence number");
            }

        }
    }

    private void addToList(byte[] bytesAddressPair, int received_length) {
        int size = received_length - RTP_JPEG_HEADER_SIZE;
        byte[] payLoad = new byte[size];
        System.arraycopy(bytesAddressPair, RTP_JPEG_HEADER_SIZE, payLoad, 0, size);
        arrayList.add(payLoad);
    }

    private boolean checkIsAllZero(byte[] bytesAddressPair, int received_length) {
        boolean isAvailable = false;
        for (int i = RTP_JPEG_HEADER_SIZE; i < received_length; i++) {
            if (bytesAddressPair[i] != 0) {
                isAvailable = true;
                break;
            }
        }
        return isAvailable;
    }

    private int validateEndOfFrame(byte[] bytesAddressPair, int received_length) {
        int eofSize = received_length - RTP_JPEG_HEADER_SIZE;
        int eof_index = 0;
        for (int i = 1; i <= eofSize; i++) {
            if ((int) (bytesAddressPair[received_length - i] & 0x00FF) == 0xFF) {
                eof_index++;
            } else {
                break;
            }
        }
        int jpeg_frame_end = (int) (bytesAddressPair[(received_length - eof_index) - 2] & 0x00FF) * 256 + (int) (bytesAddressPair[(received_length - eof_index) - 1] & 0x00FF);
        int posOfPreviousPacket = arrayList.size() - 2;
        if (eof_index == eofSize) {
            byte[] prevousPackets = arrayList.get(posOfPreviousPacket);
            int length = prevousPackets.length;

            int previousPacketIndex = 0;
            for (int i = 1; i <= prevousPackets.length; i++) {
                if ((int) (prevousPackets[length - i] & 0x00FF) == 0xFF) {
                    previousPacketIndex++;
                } else {
                    break;
                }
            }
            int jpeg_frame_previous_end = (int) (prevousPackets[(length - previousPacketIndex) - 2] & 0x00FF) * 256 + (int) (prevousPackets[(length - previousPacketIndex) - 1] & 0x00FF);
            byte[] bytes = {prevousPackets[(length - previousPacketIndex) - 2], prevousPackets[(length - previousPacketIndex) - 1]};
            if (jpeg_frame_previous_end == endOfFrame) {
                return endOfFrame;
            } else {
                return jpeg_frame_end;
            }
        } else if (eofSize != 1024 && jpeg_frame_end != endOfFrame) {
            if (received_length == 24) {
                String converted = convertBytesToHex(new byte[]{bytesAddressPair[(received_length - 4)], bytesAddressPair[(received_length - 3)], bytesAddressPair[(received_length - 2)], bytesAddressPair[(received_length - 1)]});
                if (converted.contains("d9")) {
                    byte[] previousPacketPayload = arrayList.get(posOfPreviousPacket);
                    int previousPacketLength = previousPacketPayload.length;
                    int jpeg_frame_end1 = (int) (previousPacketPayload[previousPacketLength - 2] & 0x00FF) * 256 + (int) (previousPacketPayload[previousPacketLength - 1] & 0x00FF);
                    int jpeg_frame_end2 = (int) (previousPacketPayload[previousPacketLength - 1] & 0x00FF);
                    if (jpeg_frame_end1 == endOfFrame) {
                        return endOfFrame;
                    } else if (jpeg_frame_end2 == 0xFF) {
                        return endOfFrame;
                    } else {
                        return jpeg_frame_end;
                    }
                } else {
                    byte[] lastBytes = arrayList.get(posOfPreviousPacket);
                    String convertedHex = convertBytesToHex(new byte[]{lastBytes[(lastBytes.length) - 6], lastBytes[(lastBytes.length) - 5], lastBytes[(lastBytes.length) - 4], lastBytes[(lastBytes.length) - 3], lastBytes[(lastBytes.length) - 2], lastBytes[(lastBytes.length) - 1]});
                    if (convertedHex.contains("ffd9")) {
                        return endOfFrame;
                    } else {
                        return jpeg_frame_end;
                    }
                }
            } else {
                return jpeg_frame_end;
            }
        } else {
            return jpeg_frame_end;
        }
    }

    private void checkSequenceNumber(int sequenceNumber) {
        if ((lastRTPSequenceNumber + 1) != sequenceNumber) {
            rtpSkippedPacketCount++;
        }
        lastRTPSequenceNumber = sequenceNumber;
        updateUdpPacketsInfo();
    }

    boolean isReachedMax = false;

    private void handleMaxSequenceNumber(int rtpSequenceNumber) {
        if (!isReachedMax && rtpSequenceNumber > 65444) {
            isReachedMax = true;
        }
        if (!isReachedMaxSeqNum && (rtpSequenceNumber >= 0 && rtpSequenceNumber <= 10) && isReachedMax) {
            Log.e(TAG, "processForStream: Reached Max Sequence Number rtpSequenceNumber: " + rtpSequenceNumber + " previousSequenceNumber: " + previousSequenceNumber);
            isReachedMaxSeqNum = true;
            isReachedMax = false;
            previousSequenceNumber = -1;
        }
    }

    private void updateUdpPacketsInfo() {
        callback.updateGoodBadPackets(goodFrameCount, jpeg_last_fragment_offset, fps, receivedUdpPacketcount, rtpSkippedPacketCount, jpegBadStartPacketCount, jpegBadEndPacketCount, badOffset, fpsBasedOnGoodPackets);
    }

    public byte[] concatenate(List<byte[]> arrays) {
        int finalLength = 0;
        for (byte[] array : arrays) {
            finalLength += array.length;
        }
        byte[] dest = null;
        int destPos = 0;

        for (byte[] array : arrays) {
            if (dest == null) {
                dest = Arrays.copyOf(array, finalLength);
                destPos = array.length;
            } else {
                System.arraycopy(array, 0, dest, destPos, array.length);
                destPos += array.length;
            }
        }
        return dest;
    }

    private short headerToSend(short length, byte version) {
        int length1 = length & 0x0FFF;
        int version1 = (version << 12) & 0xF000;
        return (short) (length1 | version1);
    }


    /*OPSIN UDP Listerner*/
    private final AtomicInteger sotimeout_count = new AtomicInteger(0);
    private static MediaDecoder mediaDecoder;

    public void startListenOpsinUdp(MediaDecoder decoder) {
        if (executorServiceOpsinUDP == null || executorServiceOpsinUDP.isShutdown()) {
            Log.e(TAG, "startListenOpsinUdp: ");
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable);
                thread.setPriority(Thread.MAX_PRIORITY); // Set the desired priority
                return thread;
            };
            mediaDecoder = decoder;
            executorServiceOpsinUDP = Executors.newCachedThreadPool(threadFactory);
            executorServiceOpsinUDP.execute(() -> {
                startStopUdpReceiver(true);
            });
        }
    }


    public void stopListeningOpsinUdp() {
        Log.e(TAG, "stopListeningOpsinUdp: ");
        try {
            if (executorServiceOpsinUDP != null) {
                startStopUdpReceiver(false);
                executorServiceOpsinUDP.shutdownNow();
                executorServiceOpsinUDP = null;
            }
            sotimeout_count.set(0);
            mediaDecoder = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setState(int state) {
        switch (state) {
            case STATE_NONE:
                Log.e(TAG, "setState: STATE_NONE");
                mState = STATE_NONE;
                if (callback != null) {
                    callback.isTcpConnected(STATE_NONE);
                }
                break;
            case STATE_CONNECTED:
                Log.e(TAG, "setState: STATE_CONNECTED");
                mState = STATE_CONNECTED;
                if (callback != null) callback.isTcpConnected(STATE_CONNECTED);
                break;
            case STATE_FAILED:
                Log.e(TAG, "setState: STATE_FAILED");
                mState = STATE_FAILED;
                if (callback != null) {
                    callback.isTcpConnected(STATE_FAILED);
                    if (executorServiceConnectTcp != null) {
                        executorServiceConnectTcp.shutdown();
                        executorServiceConnectTcp = null;

                        shouldRestartSocketListen = false;
                    }
                    if (executorServiceTcpReceiver != null) {
                        executorServiceTcpReceiver.shutdown();
                        executorServiceTcpReceiver = null;
                    }

                    if (executorServiceNightwaveUDP != null) {
                        executorServiceNightwaveUDP.shutdownNow();
                        executorServiceNightwaveUDP = null;
                    }

                    if (scheduledFPSCalculation != null) {
                        scheduledFPSCalculation.shutdownNow();
                        scheduledFPSCalculation = null;
                    }
                    if (scheduledESMonitorSOTimeout != null) {
                        scheduledESMonitorSOTimeout.shutdownNow();
                        scheduledESMonitorSOTimeout = null;
                    }
                    if (scheduledExecutorServiceKeepAlive != null) {
                        scheduledExecutorServiceKeepAlive.shutdownNow();
                        scheduledExecutorServiceKeepAlive = null;
                    }
                }
                break;
            case STATE_DISCONNECTED:
                Log.e(TAG, "setState: STATE_DISCONNECTED");
                mState = Constants.STATE_DISCONNECTED;
                if (callback != null) {
                    callback.isTcpConnected(STATE_DISCONNECTED);
                    /*
                    the socket state set to be NONE once the socket disconnected
                     */
                    new Handler(Looper.getMainLooper()).postDelayed(() -> callback.isTcpConnected(STATE_NONE), 500);
                    shouldRestartSocketListen = false;
                }
                break;
            default:
                mState = STATE_NONE;
                break;
        }
    }

    private void retrySocketConnection() {
        Log.e(TAG, "retrySocketConnection: " + retryCount);
        if (retryCount < 3) {
            if (executorServiceConnectTcp != null) {
                executorServiceConnectTcp.shutdownNow();
                executorServiceConnectTcp = null;
            }
            if (executorServiceTcpReceiver != null) {
                executorServiceTcpReceiver.shutdown();
                executorServiceTcpReceiver = null;
            }
            if (scheduledFPSCalculation != null) {
                scheduledFPSCalculation.shutdownNow();
                scheduledFPSCalculation = null;
            }
            if (executorServiceNightwaveUDP != null) {
                executorServiceNightwaveUDP.shutdownNow();
                executorServiceNightwaveUDP = null;
            }
            if (executorServiceOpsinUDP != null) {
                executorServiceOpsinUDP.shutdownNow();
                executorServiceOpsinUDP = null;
            }

            if (scheduledESMonitorSOTimeout != null) {
                scheduledESMonitorSOTimeout.shutdownNow();
                scheduledESMonitorSOTimeout = null;
            }
            if (scheduledExecutorServiceKeepAlive != null) {
                scheduledExecutorServiceKeepAlive.shutdownNow();
                scheduledExecutorServiceKeepAlive = null;
            }
            retryCount = retryCount + 1;

            if (retry == null) {
                retry = Executors.newScheduledThreadPool(1);
                retry.schedule(() -> {
                    if (mWifiState == WIFI_STATE_CONNECTED) {
                        Log.e(TAG, "retrySocketConnection: ");
                        connectSocket();
                        retry.shutdownNow();
                    } else {
                        Log.e(TAG, "retrySocketConnection: Shutdown Dur to WIFI_DISCONNECTED");
                        retryCount = 0;
                        retry.shutdownNow();
                    }
                }, 5, TimeUnit.SECONDS);
            }
        } else {
            retryCount = 0;
            setState(STATE_FAILED);
        }
    }

    public void disconnectSocket() {
        Log.e(TAG, "disconnect() CALLED");
        try {
            shouldRestartSocketListen = false;
            noudpRetryCount = 0;
            lastCommandSentTime = -1;
            safeQueue.clear();
            arrayList.clear();

            if (mState == STATE_CONNECTED) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        startStopUdpReceiverNW(false);
                        break;
                    case OPSIN:
                        startStopUdpReceiver(false);
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"disconnectSocket : NW_Digital");
                        break;
                }

                setState(STATE_DISCONNECTED);
                disconnectTcpSocket();
            } else {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        startStopUdpReceiverNW(false);
                        break;
                    case OPSIN:
                        startStopUdpReceiver(false);
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"!state_connected : NW_Digital");
                        break;
                }
            }

            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    seconds = 0;
                    isAllZero = false;
                    if (executorServiceNightwaveUDP != null) {
                        executorServiceNightwaveUDP.shutdownNow();
                        executorServiceNightwaveUDP = null;
                    }
                    if (scheduleIsAllZero != null) {
                        scheduleIsAllZero.shutdownNow();
                        scheduleIsAllZero = null;
                    }
                    break;
                case OPSIN:
                    if (mediaDecoder != null) {
                        mediaDecoder.stopDecoder();
                    }
                    if (executorServiceOpsinUDP != null) {
                        executorServiceOpsinUDP.shutdownNow();
                        executorServiceOpsinUDP = null;
                    }
                    sotimeout_count.set(0);
                    mediaDecoder = null;
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"disconnectSocket : NW_Digital");
                    break;
            }

            if (executorServiceConnectTcp != null) {
                executorServiceConnectTcp.shutdownNow();
                executorServiceConnectTcp = null;
            }
            if (executorServiceTcpReceiver != null) {
                executorServiceTcpReceiver.shutdown();
                executorServiceTcpReceiver = null;
            }
            if (scheduledFPSCalculation != null) {
                scheduledFPSCalculation.shutdownNow();
                scheduledFPSCalculation = null;
            }
            if (scheduledESMonitorSOTimeout != null) {
                scheduledESMonitorSOTimeout.shutdownNow();
                scheduledESMonitorSOTimeout = null;
            }
            if (scheduledExecutorServiceKeepAlive != null) {
                scheduledExecutorServiceKeepAlive.shutdownNow();
                scheduledExecutorServiceKeepAlive = null;
            }
            if (retry != null) {
                retry.shutdownNow();
                retry = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetSocketState() {
        Log.e(TAG, "resetSocketState: ");
        setState(STATE_NONE);
        try {
            shouldRestartSocketListen = false;
            if (executorServiceConnectTcp != null) {
                executorServiceConnectTcp.shutdownNow();
                executorServiceConnectTcp = null;
            }
            if (executorServiceTcpReceiver != null) {
                executorServiceTcpReceiver.shutdown();
                executorServiceTcpReceiver = null;
            }
            if (executorServiceNightwaveUDP != null) {
                executorServiceNightwaveUDP.shutdownNow();
                executorServiceNightwaveUDP = null;
            }

            if (scheduledFPSCalculation != null) {
                scheduledFPSCalculation.shutdownNow();
                scheduledFPSCalculation = null;
            }
            if (scheduledESMonitorSOTimeout != null) {
                scheduledESMonitorSOTimeout.shutdownNow();
                scheduledESMonitorSOTimeout = null;
            }

            if (scheduledExecutorServiceKeepAlive != null) {
                scheduledExecutorServiceKeepAlive.shutdownNow();
                scheduledExecutorServiceKeepAlive = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] shortToByteArray(short sequence) {
        byte[] ret = new byte[2];
        ret[0] = (byte) (sequence & 0xff);
        ret[1] = (byte) ((sequence >> 8) & 0xff);
        return ret;
    }

    private static byte[] intToBytess(final int data) {
        return new byte[]{(byte) ((data) & 0xff), (byte) ((data >> 8) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 24) & 0xff)};
    }

    public static byte[] convertIntToByteArray2(int value) {
        return new byte[]{(byte) (value >> 24 & 0xff), (byte) (value >> 16 & 0xff), (byte) (value >> 8 & 0xff), (byte) (value & 0xff)};
    }

    public static String convertBytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte temp : bytes) {
            result.append(String.format("%02x", temp));
        }
        return result.toString();
    }

    private static byte[] intToBytes(final int data) {
        return new byte[]{(byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff), (byte) ((data) & 0xff),};
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        super.onTimeout(startId, fgsType);
        Log.e(TAG, "system onTimeout");
    }

    public void startForegroundService() {
        Log.e(TAG, "startForegroundService");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = createNotificationChannel(notificationManager);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder
                .setSmallIcon(R.drawable.ic_heading)
                .setContentTitle("SIONYX")
                .setOngoing(true)
                .setPriority(PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        if (isSDK13() || isSDK14() || isSDK15())
            notificationManager.notify(1, notification);

        if (isSDK14() ||  isSDK15() || isSDK16AndAbove()) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);//Android 14 above
        } else {
            startForeground(1, notification);
        }
    }

    private String createNotificationChannel(NotificationManager notificationManager) {
        String channelId = getString(R.string.app_name);
        String channelName = getString(R.string.app_name);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
}