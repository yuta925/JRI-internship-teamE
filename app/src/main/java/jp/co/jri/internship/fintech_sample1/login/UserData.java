package jp.co.jri.internship.fintech_sample1.login;

/**
 * CSV読み込み・認証用のユーザーデータクラス
 */
public class UserData {

    private final String userId;
    private final String password;
    private final String displayName;
    private final int permissionLevel; // 1: 全て許可, 2: 資金移動のみ不可, 3: 閲覧のみ可能
    private final String parentUserId; // 追加した親ユーザーのID

    public UserData(String userId, String password, String displayName, int permissionLevel, String parentUserId) {
        this.userId = userId;
        this.password = password;
        this.displayName = displayName;
        this.permissionLevel = permissionLevel;
        this.parentUserId = parentUserId;
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

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public String getParentUserId() {
        return parentUserId;
    }
}