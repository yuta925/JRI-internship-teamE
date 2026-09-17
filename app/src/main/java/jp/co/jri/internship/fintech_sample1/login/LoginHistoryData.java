package jp.co.jri.internship.fintech_sample1.login;

/**
 * CSV読み込み用のログイン履歴データクラス（LoginHistory.csvの1行分）
 */
public class LoginHistoryData {

    private final String userId;
    private final String displayName;
    private final String loginDateTime; // "yyyy/MM/dd HH:mm:ss"

    public LoginHistoryData(String userId, String displayName, String loginDateTime) {
        this.userId = userId;
        this.displayName = displayName;
        this.loginDateTime = loginDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLoginDateTime() {
        return loginDateTime;
    }
}
