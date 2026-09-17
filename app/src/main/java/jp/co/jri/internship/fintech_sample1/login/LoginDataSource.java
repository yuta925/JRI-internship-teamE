package jp.co.jri.internship.fintech_sample1.login;

import android.content.Context;

import java.io.IOException;

import jp.co.jri.internship.fintech_sample1.CsvReader;

/**
 * ユーザー認証を行うデータソースクラス
 */
public class LoginDataSource {

    /**
     * ユーザー認証を行う
     * @param userId ユーザーID
     * @param password パスワード
     * @param context コンテキスト（CSV読み込み用）
     * @return 認証結果（成功時はLoggedInUser、失敗時はError）
     */
    public Result<LoggedInUser> login(String userId, String password, Context context) {

        try {
            // CsvReaderでユーザーデータを読み込む
            CsvReader csvReader = new CsvReader();
            csvReader.readerUserDataBase(context);

            // ユーザーIDとパスワードが一致するユーザーを検索
            for (UserData user : csvReader.userObjects) {
                if (user.getUserId().equals(userId) && user.getPassword().equals(password)) {
                    // 認証成功：ログイン日時を記録し、連続ログイン日数を計算する
                    csvReader.appendLoginHistory(context, user.getUserId(), user.getDisplayName());
                    csvReader.readerLoginHistory(context);
                    int consecutiveDays = csvReader.calcConsecutiveLoginDays(user.getUserId());
                    String lastLoginDateTime = csvReader.loginHistoryObjects.isEmpty()
                            ? null
                            : csvReader.loginHistoryObjects.get(csvReader.loginHistoryObjects.size() - 1).getLoginDateTime();

                    LoggedInUser loggedInUser = new LoggedInUser(
                            user.getUserId(), user.getDisplayName(), consecutiveDays, lastLoginDateTime);
                    return new Result.Success<>(loggedInUser);
                }
            }

            // 認証失敗（該当ユーザーなし、またはパスワード不一致）
            return new Result.Error(new Exception("ユーザーIDまたはパスワードが正しくありません"));

        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }
}