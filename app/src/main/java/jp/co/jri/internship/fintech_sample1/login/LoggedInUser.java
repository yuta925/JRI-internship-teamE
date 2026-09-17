package jp.co.jri.internship.fintech_sample1.login;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String displayName;
    private int consecutiveLoginDays;
    private String lastLoginDateTime;

    public LoggedInUser(String userId, String displayName, int consecutiveLoginDays, String lastLoginDateTime) {
        this.userId = userId;
        this.displayName = displayName;
        this.consecutiveLoginDays = consecutiveLoginDays;
        this.lastLoginDateTime = lastLoginDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getConsecutiveLoginDays() {
        return consecutiveLoginDays;
    }

    public String getLastLoginDateTime() {
        return lastLoginDateTime;
    }
}
