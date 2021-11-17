import java.io.Serializable;

public class Message implements Serializable {
    private String sender;
    private String receiver;
    private String type;
    private String message;

    Message(String sender, String type) {
        this.sender = sender;
        this.type = type;
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
