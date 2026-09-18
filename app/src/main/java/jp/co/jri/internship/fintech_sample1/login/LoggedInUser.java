package jp.co.jri.internship.fintech_sample1.login;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private final String userId;
    private final String displayName;
    private final int consecutiveLoginDays;
    private final String lastLoginDateTime;
    private final int permissionLevel;

    public LoggedInUser(String userId, String displayName, int consecutiveLoginDays, String lastLoginDateTime, int permissionLevel) {
        this.userId = userId;
        this.displayName = displayName;
        this.consecutiveLoginDays = consecutiveLoginDays;
        this.lastLoginDateTime = lastLoginDateTime;
        this.permissionLevel = permissionLevel;
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

    public int getPermissionLevel() {
        return permissionLevel;
    }
}
