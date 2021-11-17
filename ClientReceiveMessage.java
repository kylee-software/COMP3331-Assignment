import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientReceiveMessage extends Thread {
    private Client client;
    private Socket clientSocket;
    private ObjectInputStream inputStream;

    // Text coloring for text
    final String ANSI_RESET = "\u001B[0m";
    final String ANSI_SERVER = "\u001B[34m";
    final String ANSI_USER = "\u001B[45m" + "\u001B[1m";

    ClientReceiveMessage(Client client, Socket clientSocket) throws IOException {
        this.client = client;
        this.clientSocket = clientSocket;
        // define ObjectOutputStream instance which would be used to send message to the server
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                Message inputMessage = (Message) inputStream.readObject();
                String type = inputMessage.getType();
                String sender = inputMessage.getSender();
                String[] messageBody = inputMessage.getMessage().split(" ");

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
                            System.out.println(ANSI_SERVER + sender + ANSI_RESET + " " + String.join(" ", messageBody));
                        } else {
                            System.out.println(
                                    ANSI_USER + sender + ANSI_RESET +
                                    " " + String.join(" ", messageBody));
                        }
                    }
                    case "message" -> {
                        message(messageBody);
                    }
                    case "whoelse", "whoelsesince" -> {
                        System.out.println(ANSI_SERVER + sender + ANSI_RESET + " " + String.join(" ", messageBody));
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
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void login(String loginStatus, String username) throws Exception {
        switch (loginStatus) {
            case "USERNAME" -> System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " username is invalid! Please" +
                                                  " try again or register a new account!");
            case "BLOCKED" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " your account is blocked die to multiple " +
                                   "failed login attempts! Please try again later!");
                client.setLoginStatus(false);
                client.setUser(null);
            }
            case "ONLINE" -> System.out.println(
                    ANSI_SERVER + "SERVER" + ANSI_RESET + " this account is already logged in somewhere else. Please " +
                    "try another account!");
            case "SUCCESS" -> {
                client.setLoginStatus(true);
                client.setUser(username);
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " welcome back " + username + "! You have " +
                                   "logged in successfully!");
            }
            case "PASSWORD" -> {
                client.setUser(username);
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " wrong password! Please re-enter:");
            }
        }
    }

    private void register(String loginStatus, String username) throws Exception {
        if (loginStatus.equals("USERNAME")) {
            System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " account with this username is already existed!" +
                               " Please try a different username");
        } else {
            client.setLoginStatus(true);
            client.setUser(username);
            System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET +
                               " you have successfully created an account! Welcome to the SQUAD " + username + "!");
        }
    }

    private void message(String[] response) {
        String status = response[0];

        switch (status) {
            case "SELF" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " you cannot send a message to yourself!");
            }
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " user does not exist!");
            }
            case "BLOCKED" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " You do not have the permission to send a " +
                                   "message to " + ANSI_USER + response[1] + ANSI_RESET + " " +
                                   "!");
            }
            case "OFFLINE" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + " Message is sent but " + ANSI_USER + response[1] +
                        ANSI_RESET + " is offline right now.");
            }
            case "SUCCESS" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + " " + ANSI_USER + response[1] + ANSI_RESET +
                        " have received the message successfully!");
            }
        }
    }

    private void blockUser(String[] response) {
        String status = response[0];
        switch (status) {
            case "SELF" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " you cannot block yourself!");
            }
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " user does not exist!");
            }
            case "SUCCESS" -> {
                String target = response[1];
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + " you have successfully blocked " + ANSI_USER + target +
                        ANSI_RESET + " .");
            }
        }
    }

    private void unblockUser(String[] response) {
        String status = response[0];
        switch (status) {
            case "SELF" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " you cannot unblock yourself!");
            }
            case "USERNAME" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + " user does not exist!");
            }
            case "UNBLOCKED" -> {
                System.out.println(ANSI_SERVER + "SERVER" + ANSI_RESET + ": " + ANSI_USER + response[1] + ANSI_RESET +
                                   " was already unblocked.");
            }
            case "SUCCESS" -> {
                System.out.println(
                        ANSI_SERVER + "SERVER" + ANSI_RESET + ": you have successfully unbblocked " + ANSI_USER + response[1] +
                        ANSI_RESET + " .");
            }
        }
    }
}
