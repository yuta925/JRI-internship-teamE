package jp.co.jri.internship.fintech_sample1.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private final String userId;
    private final String displayName;
    private final int consecutiveLoginDays;
    private final String lastLoginDateTime;
    private final int permissionLevel;

    LoggedInUserView(String userId, String displayName, int consecutiveLoginDays, String lastLoginDateTime, int permissionLevel) {
        this.userId = userId;
        this.displayName = displayName;
        this.consecutiveLoginDays = consecutiveLoginDays;
        this.lastLoginDateTime = lastLoginDateTime;
        this.permissionLevel = permissionLevel;
    }

    String getUserId() {
        return userId;
    }

    String getDisplayName() {
        return displayName;
    }

    int getConsecutiveLoginDays() {
        return consecutiveLoginDays;
    }

    String getLastLoginDateTime() {
        return lastLoginDateTime;
    }

    int getPermissionLevel() {
        return permissionLevel;
    }
}
