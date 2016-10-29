package ch.ethz.inf.vs.a3.fabischn.udpclient;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.a3.fabischn.message.MessageOut;
import ch.ethz.inf.vs.a3.fabischn.message.MessageTypes;

/**
 * Created by fabian on 28.10.16.
 *
 * http://www.eng.nene.ac.uk/~espen/CSY2026/AndroidCSY2026-Week21.htm
 *
 */

public class UDPClient extends Thread {

    private static final String TAG = UDPClient.class.getSimpleName();

    private ExecutorService mThreadPool;
    private String mServerIP;
    private int mServerPORT;
    private String mClientUUID;
    private String mUsername;

    private DatagramSocket socket = null;

    public UDPClient(final String username, final String ip, int port, int threadPoolSize){
        mServerIP = ip;
        mServerPORT = port;
        mThreadPool = Executors.newFixedThreadPool(threadPoolSize);
        mClientUUID = UUID.randomUUID().toString();
        mUsername = username;
    }

    public void run(){
        Log.d(TAG, "Thread: " + Thread.currentThread().toString());
        // TODO test connection
        InetAddress serverIP = null;
        try {
            serverIP = InetAddress.getByName(mServerIP);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Hmmm... unknown host", e);
            return;
        }
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            Log.e(TAG, "Couldn't create UDP socket" ,e);
            return;
        }

        // Exclusively send and receive to and from server
        socket.connect(serverIP, mServerPORT);

        MessageOut msgOut = new MessageOut(MessageTypes.REGISTER, mUsername, mClientUUID, "Hallo Server!",serverIP, mServerPORT);

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
        // TODO if server down, is he undefinitely waiting?
        try {
            socket.receive(packetIn);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't receive", e);
            return;
        }
        Log.d(TAG, "Successfully received UDP packet: " + new String(packetIn.getData()));

    }

    @Override
    public void interrupt() {
        super.interrupt();
    }
}
