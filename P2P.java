import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.ArrayList;

public class P2P extends Thread {
    private ServerSocket p2pSocket;
    private ArrayList<P2PThread> connections;

    // text styling
    final String ANSI_RESET = "\u001B[0m";
    final String ANSI_RED = "\u001B[31m";

    P2P(ServerSocket p2pSocket) {
        this.p2pSocket = p2pSocket;
        this.connections = new ArrayList<>();
    }

    @Override
    public void run() {

        Socket clientSocket = null;
        while (true) {
            try {
                clientSocket = p2pSocket.accept();
//                DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
//                DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
//                // the peer that initiated the connection
//                String peer = inputStream.readUTF();
//                // p2p connection for the user that is being invited
//                P2PThread p2PThread = new P2PThread(clientSocket, peer, outputStream, inputStream);
//                connections.add(p2PThread);
//                p2PThread.start();
            } catch (Exception e) {
                break;
            }
        }
    }

    public boolean createConnection(String sender, String target, int port) {
        try {
            Socket connectionSocket = new Socket(InetAddress.getLocalHost(), port);
            DataOutputStream outputStream = new DataOutputStream(connectionSocket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(connectionSocket.getInputStream());
            // p2p thread for the user requested the connection
            P2PThread senderThread = new P2PThread(connectionSocket, sender, outputStream, inputStream);
            connections.add(senderThread);
            // p2p thread for the user that is being invited to the connection
            P2PThread targetThread = new P2PThread(connectionSocket, target, outputStream, inputStream);
            connections.add(targetThread);

            senderThread.start();
            targetThread.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * send a private message to another user
     * @param target the targeted user
     * @param message the message to be sent
     */
    public void sendMessage(String target, String message) {
        for (P2PThread p2PThread : connections) {
            if (p2PThread.getPeer().equals(target) && p2PThread.isAlive()) {
                p2PThread.sendMessage(message);
                return;
            }
        }
        System.out.println(ANSI_RED + "ERROR" + ANSI_RESET + ": private messaging to " + target + " has not been " +
                           "established.");
    }

    /**
     * close all private connections
     */
    public void closeConnections() {
        for (P2PThread p2PThread : connections) {
            if (p2PThread.isAlive()) {
                p2PThread.sendMessage("stopprivate");
            }
        }
    }

}
