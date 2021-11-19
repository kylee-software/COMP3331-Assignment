import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class ClientReceiveMessage extends Thread {
    private Client client;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Socket clientSocket;

    // Text coloring for text
    final String ANSI_RESET = "\u001B[0m";
    final String ANSI_BOLD = "\u001B[1m";
    final String ANSI_SERVER = "\u001B[34m" + ANSI_BOLD;
    final String ANSI_USER = "\u001B[35m" + ANSI_BOLD;
    final String ANSI_RED = "\u001B[31m";
    final String ANSI_USER_MENTION = ANSI_RED + "\u001B[4m";

    ClientReceiveMessage(Client client, Socket clientSocket) throws IOException {
        this.client = client;
        this.clientSocket = clientSocket;
        // define ObjectOutputStream instance which would be used to send message to the server
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
    }

    /**
     * Function that deals with responses from the server
     */
    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                Packet packet = (Packet) inputStream.readObject();
                String type = packet.getType();
                String sender = packet.getSender();
                String[] messageBody = packet.getMessage().split(" ");

                switch (type) {
                    case "login" -> {
                        String username = messageBody[0];
                        String loginStatus = messageBody[1];
                        login(loginStatus, username);
                    }
                    case "register" -> {
                        String username = messageBody[0];
                        String loginStatus = messageBody[1];
                        register(loginStatus, username);
                    }
                    case "broadcast" -> {
                        if (sender.equals("SERVER")) {
                            System.out.println(
                                    ANSI_SERVER + sender + ANSI_RESET + ": " + ANSI_USER_MENTION + messageBody[0] +
                                    ANSI_RESET + " " +
                                    String.join(" ", Arrays.copyOfRange(messageBody, 1, messageBody.length)));
                        } else if (sender.equals("server")) {
                            System.out.println(
                                    ANSI_SERVER + "SERVER" + ANSI_RESET + ": " + String.join(" ", messageBody));
                        } else {
                            System.out.println(
                                    ANSI_USER + sender + ANSI_RESET + ": " + String.join(" ", messageBody));
                        }
                    }
                    case "message" -> {
                        message(messageBody);
                    }
                    case "whoelse", "whoelsesince" -> {
                        System.out.println(ANSI_SERVER + sender + ANSI_RESET + ": " + String.join(" ", messageBody));
                    }
                    case "block" -> {
                        blockUser(messageBody);
                    }
                    case "unblock" -> {
                        unblockUser(messageBody);
                    }
                    case "exit" -> {
                        System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": Good Bye!");
                        inputStream.close();
                    }
                    case "messages" -> {
                        if (messageBody[0].equals("NONE")) {
                            System.out.println(ANSI_SERVER + sender + ANSI_RESET + ": you have no unread messages.");
                        } else {
                            System.out.println(
                                    ANSI_SERVER + sender + ANSI_RESET + ": " + String.join(" ", messageBody));
                        }
                    }
                    case "timeout" -> {
                        System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": " + String.join(" ", messageBody));
                        System.out.println(ANSI_BOLD +
                                           "----------------------------------------------------------------------" +
                                           ANSI_RESET);
                        client.setLoginStatus(false);
                        client.setUser(null);
                    }
                    case "startprivate" -> {
                        String responseType = messageBody[0];
                        String responseMsg = messageBody[1];
                        startPrivateMsg(responseType, responseMsg);
                    }
                    case "SERVER" -> {
                        System.out.println(String.join(" ", messageBody));
                    }
                }
            } catch (Exception e) {
                System.out.println("Something went wrong! Goodbye.");
                try {
                    e.printStackTrace();
//                    inputStream.close();
                    clientSocket.close();
                    break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Handles responses from the server regarding user's login status
     *
     * @param loginStatus login status from the response
     * @param username    username of the user who is trying to login
     * @throws Exception throw exception when an error occurs
     */
    private void login(String loginStatus, String username) throws Exception {
        switch (loginStatus) {
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": username is invalid! " +
                                   "Please" + " try again or register a new account!");
                System.out.println(
                        ANSI_BOLD + "----------------------------------------------------------------------" +
                        ANSI_RESET);
            }
            case "BLOCKED" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": your account is blocked die to multiple " +
                                   "failed login attempts! Please try again later!");
                System.out.println(
                        ANSI_BOLD + "----------------------------------------------------------------------" +
                        ANSI_RESET);
                client.setLoginStatus(false);
                client.setUser(null);
            }
            case "ONLINE" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET +
                        ": this account is already logged in somewhere else. Please" +
                        " " + "try another account!");
                System.out.println(
                        ANSI_BOLD + "----------------------------------------------------------------------" +
                        ANSI_RESET);
            }
            case "SUCCESS" -> {
                client.setLoginStatus(true);
                client.setUser(username);
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": welcome " + ANSI_USER_MENTION + username + ANSI_RESET +
                        "! You have logged in successfully!");
            }
            case "PASSWORD" -> {
                client.setUser(username);
                System.out.print(ANSI_SERVER + "SERVER" + ANSI_RESET + ": wrong password! Please re-enter: ");
            }
        }
    }

    /**
     * Handles responses from the server regarding user's login status when attempt to register a new account
     *
     * @param loginStatus the login status
     * @param username    username of the user who is trying to register a new account
     * @throws Exception throw exception when an error occurs
     */
    private void register(String loginStatus, String username) throws Exception {
        if (loginStatus.equals("USERNAME")) {
            System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": account with this username is already " +
                               "existed! Please try a different username.");
        } else {
            client.setLoginStatus(true);
            client.setUser(username);
            System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET +
                               ": you have successfully created an account! Welcome to the SQUAD " + ANSI_USER_MENTION +
                               username + ANSI_RESET + "!");
        }
    }

    /**
     * Handles responses from the server regarding the status of the message that the user requested to send out
     *
     * @param response response from the server
     */
    private void message(String[] response) {
        String status = response[0];

        switch (status) {
            case "SELF" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": you cannot send a message to yourself!");
            }
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": user does not exist!");
            }
            case "BLOCKED" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": you do not have the permission to send a " +
                                   "message to " + ANSI_USER_MENTION + response[1] + ANSI_RESET + "!");
            }
            case "EMPTY" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": message can not be empty!");
            }
            case "OFFLINE" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": message is sent but " + ANSI_USER_MENTION +
                        response[1] +
                        ANSI_RESET + " is offline right now.");
            }
            case "SUCCESS" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": " + ANSI_USER_MENTION + response[1] + ANSI_RESET +
                        " have received the message successfully!");
            }
        }
    }

    /**
     * Handles responses from the server for user's attempt to block another user
     *
     * @param response response from the server
     */
    private void blockUser(String[] response) {
        String status = response[0];
        switch (status) {
            case "SELF" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": you cannot block yourself!");
            }
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": user does not exist!");
            }
            case "SUCCESS" -> {
                String target = response[1];
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": you have successfully blocked " + ANSI_USER_MENTION +
                        target +
                        ANSI_RESET + ".");
            }
        }
    }

    /**
     * Handles responses from the server for user's attempt to unblock another user
     *
     * @param response response from the server
     */
    private void unblockUser(String[] response) {
        String status = response[0];
        switch (status) {
            case "SELF" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": you cannot unblock yourself!");
            }
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": user does not exist!");
            }
            case "UNBLOCKED" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": " + ANSI_USER_MENTION + response[1] + ANSI_RESET +
                        " was already unblocked.");
            }
            case "SUCCESS" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": you have successfully unblocked " + ANSI_USER_MENTION +
                        response[1] + ANSI_RESET + ".");
            }
        }
    }

    /**
     * Handles responds from the server regarding private messaging
     * @param type the type of response
     * @param response the message body of the response
     */
    private void startPrivateMsg(String type, String response) {
        switch (type) {
            case "REQUEST" -> {
                switch (response) {
                    case "SELF" -> {
                        System.out.println(
                                ANSI_SERVER + "SERVER" + ANSI_RESET + ": you can not start private messaging " +
                                "with yourself!");
                    }
                    case "USERNAME" -> {
                        System.out.println(
                                ANSI_SERVER + "SERVER" + ANSI_RESET + ": user does not exist!");
                    }
                    case "BLOCKED" -> {
                        System.out.println(
                                ANSI_SERVER + "SERVER" + ANSI_RESET + ": you do not have the permission to " +
                                "start private messaging with the user!");
                    }
                    case "SENT" -> {
                        System.out.println(
                                ANSI_SERVER + "SERVER" + ANSI_RESET + ": an invitation has sent to the user. " +
                                "Please wait for an response.");
                    }
                }
            }
            case "INVITE" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": " + ANSI_USER_MENTION + response + ANSI_RESET + " " +
                        "wants to send private messages to you. Type " + ANSI_RED + "startprivate <user> <yes/no>" +
                        ANSI_RESET + " to respond to this invitation.");

            }
            case "RESPONSE" -> {
                if (response.equals("SUCCESS")) {
                    System.out.println(
                            ANSI_SERVER + "SERVER" + ANSI_RESET + ": the user accepted your invitation!");
                } else {
                    System.out.println(
                            ANSI_SERVER + "SERVER" + ANSI_RESET + ": the user declined your invitation!");
                }
            }
        }
    }
}
