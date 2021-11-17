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
    private LocalDateTime lastLogout;
    private ArrayList<String> blacklist;
    private ArrayList<Message> messages;

    User(String username, String password) {
        this.username = username;
        this.password = password;
        this.loginStatus = "ONLINE";
        blacklist = new ArrayList<>();
        messages = new ArrayList<>();
    }

    public String getUsername() {
        return this.username;
    }
    public void setPassword(String newPass) {
        this.password = newPass;
    }

    public String getPassword() {
        return password;
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }

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

    public void addOfflineMessage(Message message) {
        messages.add(message);
    }

    public void resetMessages() {
        messages.clear();
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogout(LocalDateTime lastLogout) {
        this.lastLogout = lastLogout;
    }
}
