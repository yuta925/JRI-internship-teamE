package jp.co.jri.internship.fintech_sample1.login;

import android.content.Context;

// ログインユーザ情報の保持用クラス
public class LoginRepository {

    private static volatile LoginRepository instance;

    private LoginDataSource dataSource;
    private LoggedInUser user = null;

    // private constructor : singleton access
    private LoginRepository(LoginDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static LoginRepository getInstance(LoginDataSource dataSource) {
        if (instance == null) {
            instance = new LoginRepository(dataSource);
        }
        return instance;
    }

    private void setLoggedInUser(LoggedInUser user) {
        this.user = user;
    }

    public Result<LoggedInUser> login(String userId, String password, Context context) {
        // handle login
        Result<LoggedInUser> result = dataSource.login(userId, password, context);
        if (result instanceof Result.Success) {
            setLoggedInUser(((Result.Success<LoggedInUser>) result).getData()); //ログイン中ユーザ情報をセット
        }
        return result;
    }
}
