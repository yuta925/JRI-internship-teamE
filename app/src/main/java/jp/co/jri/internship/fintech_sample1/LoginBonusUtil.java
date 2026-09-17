package jp.co.jri.internship.fintech_sample1;

import android.content.Context;
import android.content.SharedPreferences;

// ログインボーナスのポイント計算・累計ポイントの保存/取得をまとめたユーティリティ
public final class LoginBonusUtil {

    private static final String PREFS_NAME = "login_bonus_prefs";
    private static final String KEY_TOTAL_POINTS = "total_points";

    private LoginBonusUtil() {
    }

    // 連続ログイン日数に応じたボーナスポイントをざっくり算出する
    public static int calcPoints(int consecutiveDays) {
        if (consecutiveDays >= 30) {
            return 100;
        } else if (consecutiveDays >= 14) {
            return 50;
        } else if (consecutiveDays >= 7) {
            return 30;
        } else if (consecutiveDays >= 3) {
            return 20;
        } else {
            return 10;
        }
    }

    // 獲得ポイントを総ポイント数へ加算する（この機能導入前の総ポイント数は0として扱う）
    public static void addPoints(Context context, int points) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int total = prefs.getInt(KEY_TOTAL_POINTS, 0) + points;
        prefs.edit().putInt(KEY_TOTAL_POINTS, total).apply();
    }

    // 現在の総ポイント数を取得する
    public static int getTotalPoints(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_TOTAL_POINTS, 0);
    }
}
