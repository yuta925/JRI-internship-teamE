package jp.co.jri.internship.fintech_sample1.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.util.Patterns;

import jp.co.jri.internship.fintech_sample1.R;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final LoginRepository loginRepository;

    LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String userId, String password, Context context) {
        // can be launched in a separate asynchronous job
        Result<LoggedInUser> result = loginRepository.login(userId, password, context); //ログインボタンがクリックされたときの処理

        if (result instanceof Result.Success) {
            LoggedInUser data = ((Result.Success<LoggedInUser>) result).getData();
            loginResult.setValue(new LoginResult(new LoggedInUserView(data.getDisplayName())));
        } else {
            loginResult.setValue(new LoginResult(R.string.login_failed));
        }
    }

    public void loginDataChanged(String userId, String password) {
        if (!isUserIdValid(userId)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_userId, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder userId validation check
    private boolean isUserIdValid(String userId) {
        if (userId == null) {
            return false;
        }
        if (userId.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(userId).matches();
        } else {
            return !userId.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}
