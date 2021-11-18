import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class User {
    private String username;
    private String password;
    private String loginStatus;
    private int loginAttempts;
    private LocalDateTime blockedTime;
    private LocalDateTime lastLogin;
    private ArrayList<String> blacklist;
    private ArrayList<Packet> packets;

    User(String username, String password) {
        this.username = username;
        this.password = password;
        this.loginStatus = "OFFLINE";
        blacklist = new ArrayList<>();
        packets = new ArrayList<>();
    }

    public String getUsername() {
        return this.username;
    }


    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }

    /**
     * determines whether a user is still being blocked by the server from logging in
     * @param blockDuration the block duration of the server for three failed-login attempts
     * @return return true if user is still being blocked else false;
     */
    public boolean isBlocked(long blockDuration) {
        if (loginStatus.equals("BLOCKED")){
            long diff = blockedTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
            if (diff >= blockDuration) {
                this.loginStatus = "OFFLINE";
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public void setBlockedTime(LocalDateTime blockedTime) {
        this.blockedTime = blockedTime;
    }

    public void addBlacklistUser(String username) {
        blacklist.add(username);
    }

    public boolean isCorrectPassword (String password) {
        loginAttempts += 1;
        return this.password.equals(password);
    }

    public void resetAttempts() {
        loginAttempts = 0;
    }

    public int getLoginAttempts() {
        return loginAttempts;
    }

    public void removeBlacklistUser(String username) {
        blacklist.remove(username);
    }

    public boolean isUserBlacklisted(String username) {
        return blacklist.contains(username);
    }

    /**
     * add a message to user's to-read list when another user tried to send the user a message but the user is offline
     * @param packet the message to be read by the user later
     */
    public void addOfflineMessage(Packet packet) {
        packets.add(packet);
    }

    /**
     * reset messages after user is logged in and read them
     */
    public void resetMessages() {
        packets.clear();
    }

    public ArrayList<Packet> getMessages() {
        return packets;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
}
