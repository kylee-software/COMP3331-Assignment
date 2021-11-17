import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientThread extends Thread {
    private final Server server;
    private final Socket clientSocket;
    private User user;
    // used to send data to client
    private ObjectOutputStream objectOutputStream;
    // used to acquire input from client
    private ObjectInputStream objectInputStream;

    ClientThread(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public User getUser() {
        return user;
    }

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

                switch (type) {
                    case "login" -> {
                        // get username and password from client
                        String username = messageBody[0];
                        String password = messageBody[1];
                        String status = login(username, password);

                        Message outputMessage = new Message("SERVER", "login");
                        outputMessage.setMessage(username + " " + status);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();

                        if (status.equals("SUCCESS")) {
                            sendPresenceBroadcast("ONLINE");
                        }
                    }
                    case "register" -> {
                        String username = messageBody[0];
                        String password = messageBody[1];
                        String status = register(username, password);
                        Message outputMessage = new Message("SERVER", "login");
                        outputMessage.setMessage(username + " " + status);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();

                        if (status.equals("SUCCESS")) {
                            sendPresenceBroadcast("ONLINE");
                        }

                    }
                    case "message" -> {
                        Message newMessage = new Message(user.getUsername(), "broadcast");
                        newMessage.setReceiver(messageBody[0]);
                        newMessage.setMessage(messageBody[1]);
                        sendMessage(newMessage);
                    }
                    case "broadcast" -> {
                        String username = user.getUsername();
                        Message broadcastMsg = new Message(username, "broadcast");
                        broadcastMsg.setMessage(String.join( " ", messageBody));
                        server.broadcast("message", broadcastMsg);
                    }
                    case "whoelse" -> {
                        Message outputMessage = server.listAllOnlineUsers(user.getUsername());
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();
                    }
                    case "whoelsesince" -> {
                        LocalDateTime dateTime = (LocalDateTime.now()).minusSeconds(Long.parseLong(messageBody[0]));
                        Message outputMessage = server.getUsersSince(user.getUsername(), dateTime);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();
                    }
                    case "block" -> {
                        String status = blockUser(messageBody[0]);
                        Message outputMessage = new Message("SERVER", "block");
                        outputMessage.setMessage(status);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();
                    }
                    case "unblock" -> {
                        String status = unblockUser(messageBody[0]);
                        Message outputMessage = new Message("SERVER", "unblock");
                        outputMessage.setMessage(status);
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();
                    }
                    case "logout" -> {
                        user.setLoginStatus("OFFLINE");
                        server.updateUser(user);
                        sendPresenceBroadcast("OFFLINE");
                        user = null;
                    }
                    case "exit" -> {
                        Message outputMessage = new Message("SERVER", "exit");
                        outputMessage.setMessage("N/A");
                        objectOutputStream.writeObject(outputMessage);
                        objectOutputStream.flush();

                        System.out.println("===== the user disconnected, user - " + clientID);
                        clientAlive = false;
                    }
                }
            } catch (EOFException e) {
                System.out.println("===== the user disconnected, user - " + clientID);
                clientAlive = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        User Authentication                     │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        private String login(String username, String password) throws Exception {
            User loginUser = server.getUser(username);
            if (loginUser == null){
                // check valid username
                return "USERNAME";
            } else if (loginUser.isBlocked(server.BLOCK_DURATION)) {
                // check if the system blocked the user or not
                return "BLOCKED";
            } else if (loginUser.getLoginStatus().equals("ONLINE")) {
                return "ONLINE";
            } else if (loginUser.isCorrectPassword(password)) {
                loginUser.resetAttempts();
                loginUser.setLoginStatus("ONLINE");
                loginUser.setLastLogin(LocalDateTime.now());
                server.updateUser(loginUser);
                user = loginUser;
                return "SUCCESS";
            } else {
                if (loginUser.getLoginAttempts() == 3) {
                    loginUser.setLoginStatus("BLOCKED");
                    loginUser.setBlockedTime(LocalDateTime.now());
                    server.updateUser(loginUser);
                    return "BLOCKED";
                }
                return "PASSWORD";
            }
        }

        private String register(String username, String password) throws Exception {
            if (server.getUser(username) != null) {
                return "USERNAME";
            } else {
                server.addUser(username, password);
                user = server.getUser(username);
                user.setLastLogin(LocalDateTime.now());
                server.updateUser(user);
                return "SUCCESS";
            }
        }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                              Timeout                           │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                            Broadcasts                          │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        public void receiveBroadcast(Message message) throws Exception {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        }

        private void sendPresenceBroadcast(String type) throws Exception {
            String username = user.getUsername();
            Message broadcastMsg = new Message(username, "broadcast");
            broadcastMsg.setMessage(username + " is " + type);
            server.broadcast("presence", broadcastMsg);
        }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                          Message Forwarding                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        private void sendMessage(Message message) throws Exception {
            String sender = message.getSender();
            String target = message.getReceiver();
            User targetUser = server.getUser(target);
            Message sendClient = new Message(sender, "message");

            if (sender.equals(target)) {
                sendClient.setMessage("SELF");
            }else if (targetUser == null) {
                sendClient.setMessage("USERNAME");
            }else if (targetUser.isUserBlacklisted(sender)) {
                sendClient.setMessage("BLOCKED" + " " + target);
            } else if (!targetUser.getLoginStatus().equals("ONLINE")) {
                targetUser.addOfflineMessage(message);
                sendClient.setMessage("OFFLINE" + " " + target);
            } else {
                ClientThread targetServer = server.getClientServer(target);
                targetServer.receiveBroadcast(message);
                sendClient.setMessage("SUCCESS" + " " + target);
            }
            objectOutputStream.writeObject(sendClient);
            objectOutputStream.flush();
        }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Offline Messaging                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */


    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Blacklisting                         │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        private String blockUser(String username) {
            User target = server.getUser(username);

            if (user.getUsername().equals(username)) {
                return "SELF";
            } else if (target == null) {
                return "USERNAME";
            } else {
                user.addBlacklistUser(username);
                server.updateUser(user);
                return "SUCCESS" + " " + username;
            }
        }

        private String unblockUser(String username) {
            User target = server.getUser(username);

            if (user.getUsername().equals(username)) {
                return "SELF";
            } else if (target == null) {
                return "USERNAME";
            } else if (!user.isUserBlacklisted(username)) {
                return "UNBLOCKED" + " " + username;
            } else {
                user.removeBlacklistUser(username);
                server.updateUser(user);
                return "SUCCESS" + " " + username;
            }
        }
}
