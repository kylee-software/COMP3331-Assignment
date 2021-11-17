import java.io.Serializable;

/**
 * A packet that carry information to send message to the server and from server to client
 */
public class Packet implements Serializable {
    private String sender;
    private String receiver;
    private String type;
    private String message;

    Packet(String sender, String type) {
        this.sender = sender;
        this.type = type;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return this.sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
