package ch.ethz.inf.vs.a3.fabischn.udpclient;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

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

    private DatagramSocket socket = null;

    public UDPClient(String ip, int port, int threadPoolSize){
        mServerIP = ip;
        mServerPORT = port;
        mThreadPool = Executors.newFixedThreadPool(threadPoolSize);
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

        byte[] bufOut = new byte[NetworkConsts.PAYLOAD_SIZE];
        byte[] bufIn = new byte[NetworkConsts.PAYLOAD_SIZE];

        String testMsg = "Lorem Ipsum Dolor";
        bufOut = testMsg.getBytes();

        DatagramPacket packetOut = new DatagramPacket(bufOut, bufOut.length, serverIP, mServerPORT);
        DatagramPacket packetIn = new DatagramPacket(bufIn, bufIn.length);
        try {
            socket.send(packetOut);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't send", e);
            return;
        }

        Log.d(TAG, "Successfully send UDP packet");

        // TODO if server down, is he undefinitely waiting?
        try {
            socket.receive(packetIn);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't receive", e);
            return;
        }

        Log.d(TAG, "Successfully received UDP packet: " + new String(packetIn.getData()));
//        socket.getChannel()

    }

    @Override
    public void interrupt() {
        super.interrupt();
    }
}
