import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;

public class ClientThread extends Thread {
    private final Server server;
    private final Socket clientSocket;
    private User user;
    // used to send data to client
    private ObjectOutputStream objectOutputStream;
    // used to acquire input from client
    private ObjectInputStream objectInputStream;

    // Text coloring for text
    final String ANSI_RESET = "\u001B[0m";
    final String ANSI_USER = "\u001B[35m" + "\u001B[1m";
    final String ANSI_BOLD = "\u001B[1m";
    final String ANSI_SERVER = "\u001B[34m" + ANSI_BOLD;

    ClientThread(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    /**
     * get the user who is using the current server
     * @return the user if a user is logged in else return null
     */
    public User getUser() {
        return user;
    }

    /**
     * Function that handles all enquires from the client
     */
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
                Packet packet = (Packet) objectInputStream.readObject();
                String type = packet.getType();
                String[] messageBody = packet.getMessage().split(" ");

                switch (type) {
                    case "login" -> {
                        // get username and password from client
                        String username = messageBody[0];
                        String password = messageBody[1];
                        String status = login(username, password);

                        Packet outputPacket = new Packet("SERVER", "login");
                        outputPacket.setMessage(username + " " + status);
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();

                        if (status.equals("SUCCESS")) {
                            loginSuccess();
                        }
                    }
                    case "register" -> {
                        String username = messageBody[0];
                        String password = messageBody[1];
                        String status = register(username, password);
                        Packet outputPacket = new Packet("SERVER", "register");
                        outputPacket.setMessage(username + " " + status);
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();

                        if (status.equals("SUCCESS")) {
                            loginSuccess();
                        }
                    }
                    case "message" -> {
                        Packet newPacket = new Packet(user.getUsername(), "broadcast");
                        newPacket.setReceiver(messageBody[0]);
                        String messageText = String.join(" ", Arrays.copyOfRange(messageBody, 1, messageBody.length));
                        newPacket.setMessage(messageText);
                        sendMessage(newPacket);
                    }
                    case "broadcast" -> {
                        String username = user.getUsername();
                        Packet broadcastMsg = new Packet(username, "broadcast");
                        broadcastMsg.setMessage(String.join(" ", messageBody));
                        server.broadcast("message", broadcastMsg);
                    }
                    case "whoelse" -> {
                        Packet outputPacket = server.listAllOnlineUsers(user.getUsername());
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();
                    }
                    case "whoelsesince" -> {
                        LocalDateTime dateTime = (LocalDateTime.now()).minusSeconds(Long.parseLong(messageBody[0]));
                        Packet outputPacket = server.getUsersSince(user.getUsername(), dateTime);
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();
                    }
                    case "block" -> {
                        String status = blockUser(messageBody[0]);
                        Packet outputPacket = new Packet("SERVER", "block");
                        outputPacket.setMessage(status);
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();
                    }
                    case "unblock" -> {
                        String status = unblockUser(messageBody[0]);
                        Packet outputPacket = new Packet("SERVER", "unblock");
                        outputPacket.setMessage(status);
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();
                    }
                    case "logout" -> {
                        user.setLoginStatus("OFFLINE");
                        server.updateUser(user);
                        sendPresenceBroadcast("offline");
                        user = null;
                        clientSocket.setSoTimeout(0);
                    }
                    case "startprivate" -> {
                        String target = messageBody[0];

                        // response to an invitation: <user> <response>
                        // request to start a private messaging: <user>
                        if (messageBody.length < 2) {
                            startPrivateMsg(target);
                        } else {
                            createConnection(target, messageBody[1]);
                        }
                    }
                    case "private" -> {
                        String target = messageBody[0];

                    }
                    case "exit" -> {
                        Packet outputPacket = new Packet("SERVER", "exit");
                        outputPacket.setMessage("N/A");
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();

                        System.out.println("===== the user disconnected, user - " + clientID);
                        clientAlive = false;
                    }
                }
            } catch (SocketTimeoutException timeout) {
                // timeout due to inactivity from the client
                Packet outputPacket = new Packet("SERVER", "timeout");
                outputPacket.setMessage("you have logged out due to inactivity.");

                try {
                    // logout
                    user.setLoginStatus("OFFLINE");
                    server.updateUser(user);
                    sendPresenceBroadcast("offline");
                    user = null;

                    clientSocket.setSoTimeout(0);
                    objectOutputStream.writeObject(outputPacket);
                    objectOutputStream.flush();

                } catch (Exception e) {
                    System.out.println("===== the user disconnected, user - " + clientID);
                    clientAlive = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("===== the user disconnected, user - " + clientID);
                clientAlive = false;
            }
        }
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        User Authentication                     │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        /**
         * Verify the login information
         * @param username username of the user who is trying to log in
         * @param password password of the user who is trying to log in
         * @return the response after the information is assessed
         */
        private String login(String username, String password) {
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

        /**
         * Verify entered information to register
         * @param username username of the user who is trying to register a new account
         * @param password password of the user who is trying to register a new account
         * @return the response after the information is assessed
         */
        private String register(String username, String password) {
            if (server.getUser(username) != null) {
                return "USERNAME";
            } else {
                server.addUser(username, password);
                user = server.getUser(username);
                user.setLoginStatus("ONLINE");
                user.setLastLogin(LocalDateTime.now());
                server.updateUser(user);
                return "SUCCESS";
            }
        }

        /**
         * Perform the following actions when a user logged in successfully
         * @throws SocketException error when the socket timeout
         * @throws IOException connection error with client streams
         */
        private void loginSuccess() throws SocketException, IOException {
            // set timeout to the client socket
            clientSocket.setSoTimeout((int) server.TIMEOUT * 1000);
            // send presence broadcast to other online users
            sendPresenceBroadcast("online");

            // read all unread messages
            Packet outputPacket = offlineMessages();
            objectOutputStream.writeObject(outputPacket);
            objectOutputStream.flush();
        }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                            Broadcasts                          │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        /**
         * receive broadcast that was sent by other users
         * @param packet packet contains information about the message
         * @throws IOException throw this exception when an error occurs with the input/output stream that connects
         * to the client
         */
        public void receiveBroadcast(Packet packet) throws IOException {
                objectOutputStream.writeObject(packet);
                objectOutputStream.flush();
            }

        /**
         * a helper function to send presence broadcast to other users
         * @param type "offline" if a user logged out, "online" if a user logged in
         * @throws IOException throw this exception when an error occurs with the input/output stream that connects
         *  to the client
         */
        private void sendPresenceBroadcast(String type) throws IOException {
                String username = user.getUsername();
                Packet broadcastMsg = new Packet(username, "broadcast");
                broadcastMsg.setMessage(username + " is " + type);
                server.broadcast("presence", broadcastMsg);
            }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                          Message Forwarding                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        /**
         * send direct message to a user
         * @param packet the packet that contains the information about the message
         * @throws IOException throw this exception when an error occurs with the input/output stream that connects
         * to the client
         */
        private void sendMessage(Packet packet) throws IOException {
            String sender = packet.getSender();
            String target = packet.getReceiver();
            User targetUser = server.getUser(target);
            Packet sendClient = new Packet(sender, "message");

            if (sender.equals(target)) {
                sendClient.setMessage("SELF");
            }else if (targetUser == null) {
                sendClient.setMessage("USERNAME");
            }else if (targetUser.isUserBlacklisted(sender)) {
                sendClient.setMessage("BLOCKED" + " " + target);
            } else if (packet.getMessage().equals("")) {
                sendClient.setMessage("EMPTY");
            } else if (!targetUser.getLoginStatus().equals("ONLINE")) {
                targetUser.addOfflineMessage(packet);
                sendClient.setMessage("OFFLINE" + " " + target);
            } else {
                ClientThread targetServer = server.getClientServer(target);
                targetServer.receiveBroadcast(packet);
                sendClient.setMessage("SUCCESS" + " " + target);
            }
            objectOutputStream.writeObject(sendClient);
            objectOutputStream.flush();
        }

        /**
         * Check user's eligibility to start a private messaging.
         * If eligible, send an invitation request to the target user
         * @param target the target user
         * @throws IOException throw an exception when an error occurs
         */
        private void startPrivateMsg(String target) throws IOException {
            User targetInfo = server.getUser(target);
            Packet outputPacket = new Packet(user.getUsername(), "startprivate");
            String msg = "";
            if (target.equals(user.getUsername())) {
                msg = "REQUEST SELF";
                outputPacket.setMessage(msg);
                objectOutputStream.writeObject(outputPacket);
                objectOutputStream.flush();
            } else if (targetInfo == null) {
                msg = " REQUEST USERNAME";
                outputPacket.setMessage(msg);
                objectOutputStream.writeObject(outputPacket);
                objectOutputStream.flush();
            } else if (targetInfo.isUserBlacklisted(user.getUsername())) {
                msg = "REQUEST BLOCKED";
                outputPacket.setMessage(msg);
                objectOutputStream.writeObject(outputPacket);
                objectOutputStream.flush();
            } else {
                msg = "REQUEST SENT";
                outputPacket.setMessage(msg);
                objectOutputStream.writeObject(outputPacket);
                objectOutputStream.flush();

                // send a request to target user to ask for permission
                ClientThread clientThread = server.getClientServer(target);
                Packet requestInvite = new Packet(user.getUsername(), "startprivate");
                requestInvite.setMessage("INVITE " + user.getUsername());
                clientThread.receiveBroadcast(requestInvite);
            }

        }

        /**
         * Attempt to create a private connection after target user
         * accepted the invitation to a private messaging
         * @param target the requester who initiated the private connection
         * @param response the response to the invitation
         */
        private void createConnection(String target, String response) throws Exception {
            // send the response of an invitation back to the requester
            Packet targetPacket = new Packet("SERVER", "startprivate");
            Packet outputPacket = new Packet("SERVER", "startprivate");
            String targetMsg = "";
            String outputMsg = "";
            if (response.equals("yes")){
                targetMsg = "REQUEST SUCCESS " + user.getUsername();
                targetPacket.setMessage(targetMsg);
                ClientThread requesterThread = server.getClientServer(target);
                requesterThread.receiveBroadcast(targetPacket);

                Thread.sleep(100);
                outputMsg = "RESPONSE YES " + target;
                outputPacket.setMessage(outputMsg);
                objectOutputStream.writeObject(outputPacket);
                objectOutputStream.flush();

            } else {
                targetMsg = "REQUEST FAIL " + user.getUsername();
                targetPacket.setMessage(targetMsg);
                ClientThread requesterThread = server.getClientServer(target);
                requesterThread.receiveBroadcast(targetPacket);
            }
        }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Offline Messaging                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        /**
         * get all the offline messages for a user that just logged in
         * @return the packet that contains the information of this request
                *  if the user has unread messages, then a list of unread messages is included in the message body
                *  else "NONE" will be in the message body indicates that no unread message
         */
        private Packet offlineMessages() {
            Packet outputPacket = new Packet("SERVER", "messages");
            Packet[] messages = user.getMessages().toArray(new Packet[0]);

            String messageBody = "";
            if (messages.length != 0) {
                messageBody += "you have " + String.valueOf(messages.length) + " unread messages.";

                for (Packet message: messages) {
                    String sender = message.getSender();
                    String senderMsg = message.getMessage();
                    messageBody += ("\n" + "   " + ANSI_USER + sender + ANSI_RESET + ": " + senderMsg);
                }
                user.resetMessages();
                server.updateUser(user);
            } else {
                messageBody  = "NONE";
            }
            outputPacket.setMessage(messageBody);
            return outputPacket;
        }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Blacklisting                         │ */
    /* └────────────────────────────────────────────────────────────────┘ */

        /**
         * block another user from getting the user's presence notification, sending broadcast messages, and direct
         * messages
         * @param username the username of the user to be blocked
         * @return the status of the attempted action
         */
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

        /**
         * unblock an user
         * @param username the username of the user that is being unblocked
         * @return the status of the attempted action
         */
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
