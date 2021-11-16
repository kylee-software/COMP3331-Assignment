import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.util.HashMap;

public class ClientThread extends Thread {
    private final Server server;
    private final Socket clientSocket;
    private HashMap<String, User> data;
    private User user;
    // used to send data to client
    private ObjectOutputStream objectOutputStream;
    // used to acquire input from client
    private ObjectInputStream objectInputStream;

    ClientThread(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;

        // get login credential data from txt file and parse it into hashmaps;
        data = server.getData();
    }

    public User getUser() {
        return user;
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        User Authentication                     │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    private String login(String username, String password) throws Exception {
        User loginUser = data.get(username);
        if (loginUser == null){
            // check valid username
            return "USERNAME";
        } else if (loginUser.isBlocked(server.blockDuration)) {
            // check if the system blocked the user or not
            return "BLOCKED";
        } else if (loginUser.getLoginStatus().equals("ONLINE")) {
            return "ONLINE";
        } else if (loginUser.isCorrectPassword(password)) {
            loginUser.resetAttempts();
            loginUser.setLoginStatus("ONLINE");
            user = loginUser;
            server.broadcast(username, "ONLINE");
            return "SUCCESS";
        } else {
            if (loginUser.getLoginAttempts() == 3) {
                loginUser.setLoginStatus("BLOCKED");
                return "BLOCKED";
            }
            return "PASSWORD";
        }
    }

    private String register(String username, String password) throws Exception {
        if (data.get(username) != null) {
            return "USERNAME";
        } else {
            server.saveData(username, password);
            data = server.getData();
            user = data.get(username);
            return "SUCCESS";
        }
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                              Timeout                           │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        Presence Broadcasts                     │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        public void broadcast(String messageBody) throws Exception {
            Message message = new Message("broadcast");
            message.setMessage(messageBody);
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        }
    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                      List of Online User                       │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                         Online History                         │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                          Message Forwarding                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */

//        public void sendMessage(Message messageBody) {
//
//        }

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
            objectOutputStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(this.clientSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (clientAlive) {
            try {
                Message inputMessage = (Message) objectInputStream.readObject();
                String type = inputMessage.getType();
                String[] messageBody = inputMessage.getMessage().split(" ");
                server.synLock.lock();
                String toSend = null;

                switch (type) {
                    case "login" -> {
                        // get username and password from client
                        String username = messageBody[0];
                        String password = messageBody[1];
                        toSend = login(username, password);

                        Message outputMessage = new Message("login");
                        outputMessage.setMessage(username + " " + toSend);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();

                        if (toSend.equals("SUCCESS")) {
                            server.broadcast(username, "broadcast");
                        }
                    }
                    case "register" -> {
                        String username = messageBody[0];
                        String password = messageBody[1];
                        toSend = register(username, password);
                        Message outputMessage = new Message("login");
                        outputMessage.setMessage(username + " " + toSend);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();

                        if (toSend.equals("SUCCESS")) {
                            server.broadcast(username, "broadcast");
                        }
                    }
                    case "messageBody" -> {
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
                        user.setLoginStatus("OFFLINE");
                        server.broadcast(user.getUsername(), "OFFLINE");
                        user = null;
                    }
                }
                server.synLock.unlock();
            } catch (EOFException e) {
                System.out.println("===== the user disconnected, user - " + clientID);
                clientAlive = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
