package jp.co.jri.internship.fintech_sample1.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private final String displayName;
    private final int consecutiveLoginDays;
    private final String lastLoginDateTime;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String displayName, int consecutiveLoginDays, String lastLoginDateTime) {
        this.displayName = displayName;
        this.consecutiveLoginDays = consecutiveLoginDays;
        this.lastLoginDateTime = lastLoginDateTime;
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
}
