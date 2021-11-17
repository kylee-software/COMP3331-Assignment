import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientReceiveMessage extends Thread {
    private Client client;
    private Socket clientSocket;
    private ObjectInputStream inputStream;

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
                        System.out.println(String.join(" ", messageBody));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void login(String loginStatus, String username) throws Exception {
        switch (loginStatus) {
            case "USERNAME" -> System.out.println("Username is invalid! Please try again or register a new account!");
            case "BLOCKED" -> {
                System.out.println(
                        "Your account is blocked die to multiple failed login attempts! Please try again " +
                        "later!");
                client.setLoginStatus(false);
                client.setUser(null);
            }
            case "ONLINE" -> System.out.println(
                    "This account is already logged in somewhere else. Please try another account!");
            case "SUCCESS" -> {
                client.setLoginStatus(true);
                client.setUser(username);
                System.out.println("Welcome back " + username + "! You have logged in successfully!");
            }
            case "PASSWORD" -> {
                client.setUser(username);
                System.out.println("Wrong password! Please re-enter:");
            }
        }
    }

    private void register(String loginStatus, String username) throws Exception {
        if (loginStatus.equals("USERNAME")) {
            System.out.println("Account with this username is already existed! Please try a different username");
        } else {
            client.setLoginStatus(true);
            client.setUser(username);
            System.out.println("You have successfully created an account! Welcome to the SQUAD " + username + "!");
        }
    }

}
