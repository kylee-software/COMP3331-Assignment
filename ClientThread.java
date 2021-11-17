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
                            sendPresenceBroadcast("online");
                        }
                    }
                    case "register" -> {
                        String username = messageBody[0];
                        String password = messageBody[1];
                        String status = register(username, password);
                        Packet outputPacket = new Packet("SERVER", "login");
                        outputPacket.setMessage(username + " " + status);
                        objectOutputStream.writeObject(outputPacket);
                        objectOutputStream.flush();

                        if (status.equals("SUCCESS")) {
                            sendPresenceBroadcast("online");
                        }

                    }
                    case "message" -> {
                        Packet newPacket = new Packet(user.getUsername(), "broadcast");
                        newPacket.setReceiver(messageBody[0]);
                        newPacket.setMessage(messageBody[1]);
                        sendMessage(newPacket);
                    }
                    case "broadcast" -> {
                        String username = user.getUsername();
                        Packet broadcastMsg = new Packet(username, "broadcast");
                        broadcastMsg.setMessage(String.join( " ", messageBody));
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

        /**
         * Verify the login information
         * @param username username of the user who is trying to log in
         * @param password password of the user who is trying to log in
         * @return the response after the information is assessed
         * @throws Exception throw an exception if an error occurs
         */
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

        /**
         * Verify entered information to register
         * @param username username of the user who is trying to register a new account
         * @param password password of the user who is trying to register a new account
         * @return the response after the information is assessed
         * @throws Exception throw an exception if an error occurs
         */
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

        /**
         * receive broadcast that was sent by other users
         * @param packet packet contains information about the message
         * @throws Exception throw an exception when an error occurs
         */
        public void receiveBroadcast(Packet packet) throws Exception {
                objectOutputStream.writeObject(packet);
                objectOutputStream.flush();
            }

        /**
         * a helper function to send presence broadcast to other users
         * @param type "offline" if a user logged out, "online" if a user logged in
         * @throws Exception throw an exception when an error occurs
         */
        private void sendPresenceBroadcast(String type) throws Exception {
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
         * @throws Exception throw an exception when an error occurs
         */
        private void sendMessage(Packet packet) throws Exception {
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

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Offline Messaging                    │ */
    /* └────────────────────────────────────────────────────────────────┘ */


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
