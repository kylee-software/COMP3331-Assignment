import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

public class P2PThread extends Thread {

    Socket socket;
    String peer;
    DataOutputStream outputStream;
    DataInputStream inputStream;

    // Styling texts
    final String ANSI_RESET = "\u001B[0m";
    final String ANSI_SENDER = "\u001B[36m" + "\u001B[1m";

    P2PThread(Socket socket, String peer, DataOutputStream outputStream, DataInputStream inputStream) {
        this.socket = socket;
        this.peer = peer;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
    }

    /**
     * function for receiving data from the peer
     */
    @Override
    public void run() {
        while(true) {
            try {
                String info = (String) inputStream.readUTF();
                if (info.equals("stopprivate")) {
                    System.out.println("Ending a private messaging " + peer + ".");
                    break;
                } else {
                    System.out.println(ANSI_SENDER + peer + ANSI_RESET + ": " + info);
                }
            } catch (Exception e) {
                break;
            }

            try {
                outputStream.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * get the username of the other peer that is in the private message
     * @return the username of the peer
     */
    public String getPeer() {
        return peer;
    }

    /**
     * send private message to the other peer
     * @param message the message to be sent
     */
    public void sendMessage(String message) {
        try {
            outputStream.writeUTF(message);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
