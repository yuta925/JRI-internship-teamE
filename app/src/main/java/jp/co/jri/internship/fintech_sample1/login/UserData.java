package jp.co.jri.internship.fintech_sample1.login;

/**
 * CSV読み込み・認証用のユーザーデータクラス
 */
public class UserData {

    private final String userId;
    private final String password;
    private final String displayName;

    public UserData(String userId, String password, String displayName) {
        this.userId = userId;
        this.password = password;
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getDisplayName() {
        return displayName;
    }
}