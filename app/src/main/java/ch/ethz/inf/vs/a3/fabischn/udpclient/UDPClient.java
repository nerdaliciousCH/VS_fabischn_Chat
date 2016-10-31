package ch.ethz.inf.vs.a3.fabischn.udpclient;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.a3.fabischn.message.MessageIn;
import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;

/**
 * Created by fabian on 28.10.16.
 *
 * http://www.eng.nene.ac.uk/~espen/CSY2026/AndroidCSY2026-Week21.htm
 *
 */

// TODO make this
public class UDPClient extends Thread {

    private static final String TAG = UDPClient.class.getSimpleName();

    private String mServerIP;
    private int mServerPORT;
    private String mClientUUID;
    private String mUsername;

    private Handler mHandler;

    private volatile boolean mRunning;

    private DatagramSocket socket = null;

    // TODO make this an AsyncTask to get Chatlog
    // run until timeout on receive -> got all messages

    public UDPClient(final String username, final String ip, int port, Handler handler){
        mServerIP = ip;
        mServerPORT = port;
        mClientUUID = UUID.randomUUID().toString();
        mUsername = username;
        mHandler = handler;
    }

    public void run(){
        InetAddress serverIP = null;
        mRunning = true;
        try {
            serverIP = InetAddress.getByName(mServerIP);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Hmmm... unknown host", e);
            return;
        }
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(NetworkConsts.SOCKET_TIMEOUT);
        } catch (SocketException e) {
            Log.e(TAG, "Couldn't create UDP socket" ,e);
            return;
        }

        // TODO put all below into a tight loop and handle interrupt to stop thread
        // Exclusively send and receive to and from server
        socket.connect(serverIP, mServerPORT);

        MessageOut msgOut = new MessageOut(MessageTypes.RETRIEVE_CHAT_LOG, mUsername, mClientUUID, null ,serverIP, mServerPORT);
        DatagramPacket packetOut = msgOut.getDatagramPacket();
        try {
            socket.send(packetOut);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't send", e);
            return;
        }
        Log.d(TAG, "Successfully send UDP packet");

        byte[] bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];
        DatagramPacket packetIn = new DatagramPacket(bufIn, bufIn.length);
        try {
            socket.receive(packetIn);
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException){
                Log.e(TAG, "Socket timed out trying to receive", e);
         } else {
                Log.e(TAG, "Couldn't receive", e);
            }
            // stop thread
            return;
        }

        Log.d(TAG, "Successfully received UDP packet");
        MessageIn msgIn = new MessageIn(packetIn);
//        mHandler.sendMessage(Message.obtain(mHandler, 99, "blubb"));
        Log.d(TAG, "Send a android.os.Message to the MainActivity");
    }

    public void close(){
        mRunning = false;
        socket.close();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        // TODO stop thread
    }
}
