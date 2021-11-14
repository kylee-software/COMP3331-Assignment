import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

public class ClientThread extends Thread {
    private final Server server;
    private final Socket clientSocket;
    private HashMap<String, User> data;
    private User user;
    // used to acquire input from client
    private DataInputStream dataInputStream;
    // used to send data to client
    private DataOutputStream dataOutputStream;

    ClientThread(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;

        // get login credential data from txt file and parse it into hashmaps;
        data = server.getData();
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        User Authentication                     │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    private String login(String username, String password) throws IOException {
        User user = data.get(username);
        if (user == null){
            // check valid username
            return "USERNAME";
        } else if (user.isBlocked(server.blockDuration)) {
            // check if the system blocked the user or not
            return "BLOCKED";
        } else if (user.getLoginStatus().equals("ONLINE")) {
            return "ONLINE";
        } else if (user.isCorrectPassword(password)) {
            user.resetAttempts();
            user.setLoginStatus("ONLINE");
            return "SUCCESS";
        } else {
            if (user.getLoginAttempts() == 3) {
                user.setLoginStatus("BLOCKED");
                return "BLOCKED";
            }
            return "PASSWORD";
        }
    }

    private String register(String username, String password) throws IOException {
        if (data.get(username) != null) {
            return "USERNAME";
        } else {
            server.saveData(username, password);
            data = server.getData();
            return "SUCCESS";
        }
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                              Timeout                           │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        Presence Broadcasts                     │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                      List of Online User                       │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                         Online History                         │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                          Message Forwarding                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Offline Messaging                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                         Message Broadcast                      │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Blacklisting                         │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    @Override
    public void run() {
        super.run();
        // get client Internet Address and port number
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();
        String clientID = "(" + clientAddress + ", " + clientPort + ")";

        System.out.println("===== New connection created for user - " + clientID);
        boolean clientAlive = true;

        try {
            dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (clientAlive) {
            try {
                String[] message = ((String) dataInputStream.readUTF()).split(" ");
                String toSend = null;

                switch (message[0]) {
                    case "login" -> {
                        // get username and password from client
                        String username = message[1];
                        String password = message[2];
                        toSend = login(username, password);
                        dataOutputStream.writeUTF(toSend);
                        dataOutputStream.flush();
                    }
                    case "register" -> {
                        String username = message[1];
                        String password = message[2];
                        toSend = register(username, password);
                        dataOutputStream.writeUTF(toSend);
                        dataOutputStream.flush();
                    }
                    case "message" -> {
                        break;
                    }
                    case "broadcast" -> {
                        break;
                    }
                    case "whoelse" -> {
                        break;
                    }
                    case "whoelsesince" -> {
                        break;
                    }
                    case "block" -> {
                        break;
                    }
                    case "unblock" -> {
                        break;
                    }
                    case "logout" -> {
                        User user = data.get(message[1]);
                        user.setLoginStatus("OFFLINE");
                    }
                }

            } catch (EOFException e) {
                System.out.println("===== the user disconnected, user - " + clientID);
                clientAlive = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
