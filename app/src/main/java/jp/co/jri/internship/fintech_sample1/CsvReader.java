package jp.co.jri.internship.fintech_sample1;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import jp.co.jri.internship.fintech_sample1.login.LoginHistoryData;
import jp.co.jri.internship.fintech_sample1.login.UserData;

public class CsvReader {

    private static final String TAG = "CsvReader";
    public List<FintechData> fintechObjects = new ArrayList<>();
    public List<UserData> userObjects = new ArrayList<>();
    public List<LoginHistoryData> loginHistoryObjects = new ArrayList<>();

    // FintechDataを読み込む処理（fintechObjectsに格納される）
    // flagがtrueのとき（ローカルファイルが存在）：ローカルファイル(LocalFintechDateBase.txt)から読み込む
    // flagがfalseのとき（存在しない）：FintechDataBase.csvを読み込み、ローカルファイルに書き出してから読み込む
    public void readerFintechDataBase(Context context, Boolean flag) {

        // 環境情報をcontextから取得
        AssetManager assetManager = context.getResources().getAssets();

        // ローカルファイルのファイル名を定義
        String filename = "LocalFintechDateBase_v4.txt";

        try {
            InputStreamReader inputStreamReader;
            if (flag) {
                // ローカルファイルが存在するのでローカルファイルを読み込む
                FileInputStream fis = context.openFileInput(filename);
                inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            } else {
                // ローカルファイルが存在しないのでcsvから読み込み、ローカルファイルへ書き出す
                InputStream inputStream = assetManager.open("FintechDataBase.csv");
                InputStreamReader ir = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(ir);
                String filedata;
                while ((filedata = br.readLine()) != null) {
                    try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE | Context.MODE_APPEND)) {
                        fos.write((filedata + "\n").getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        Log.e(TAG, "ローカルファイル書き込みエラー", e);
                    }
                }
                br.close();
                // 書き出したローカルファイルを再度読み込む
                FileInputStream fis = context.openFileInput(filename);
                inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            }

            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferReader.readLine()) != null) {
                // カンマ区切りで１つづつ配列に入れる
                String[] RowData = line.split(",");
                FintechData fintechData = new FintechData(
                        Integer.parseInt(RowData[0]),
                        RowData[1],
                        RowData[2],
                        RowData[3],
                        RowData[4],
                        RowData[5],
                        RowData[6],
                        RowData[7],
                        Integer.parseInt(RowData[8]),
                        Integer.parseInt(RowData[9])
                );
                fintechObjects.add(fintechData);
            }
            bufferReader.close();
        } catch (IOException e) {
            Log.e(TAG, "CSV読み込みエラー", e);
        }
    }

    // UserDataを読み込む処理（userObjectsに格納される）
    public void readerUserDataBase(Context context) {
        AssetManager assetManager = context.getResources().getAssets();
        try {
            // CSVファイルの読み込み
            InputStream inputStream = assetManager.open("UserDataBase.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferReader.readLine()) != null) {
                // カンマ区切りで１つづつ配列に入れる
                String[] RowData = line.split(",");
                UserData userData = new UserData(
                        RowData[0],
                        RowData[1],
                        RowData[2]
                );
                userObjects.add(userData);
            }
            bufferReader.close();
        } catch (IOException e) {
            Log.e(TAG, "CSV読み込みエラー", e);
        }
    }

    // ログイン日時をCSV形式でローカルファイル(LoginHistory.csv)に追記する
    // 1行あたり "ユーザーID,表示名,ログイン日時" の形式
    public void appendLoginHistory(Context context, String userId, String displayName) {
        String filename = "LoginHistory.csv";
        String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).format(new Date());
        String line = userId + "," + displayName + "," + timestamp + "\n";
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE | Context.MODE_APPEND)) {
            fos.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "ログイン履歴書き込みエラー", e);
        }
    }

    // ログイン履歴(LoginHistory.csv)を読み込む処理（loginHistoryObjectsに格納される）
    // ファイルがまだ存在しない（初回ログイン前）の場合は何もしない
    public void readerLoginHistory(Context context) {
        String filename = "LoginHistory.csv";
        if (!context.getFileStreamPath(filename).exists()) {
            return;
        }
        try (FileInputStream fis = context.openFileInput(filename)) {
            InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferReader.readLine()) != null) {
                String[] RowData = line.split(",");
                loginHistoryObjects.add(new LoginHistoryData(RowData[0], RowData[1], RowData[2]));
            }
            bufferReader.close();
        } catch (IOException e) {
            Log.e(TAG, "ログイン履歴読み込みエラー", e);
        }
    }

    // "yyyy/MM/dd"形式の日付をエポック日数（1970/01/01からの経過日数）に変換する
    private long toEpochDay(String yyyyMMdd) {
        String[] parts = yyyyMMdd.split("/");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        return cal.getTimeInMillis() / (24L * 60 * 60 * 1000);
    }

    // 指定ユーザーの「本日を含む連続ログイン日数」をloginHistoryObjectsから計算する
    // （事前にreaderLoginHistoryを呼び出しておくこと）
    public int calcConsecutiveLoginDays(String userId) {
        TreeSet<Long> uniqueDays = new TreeSet<>(Collections.reverseOrder());
        for (LoginHistoryData history : loginHistoryObjects) {
            if (history.getUserId().equals(userId)) {
                uniqueDays.add(toEpochDay(history.getLoginDateTime().substring(0, 10)));
            }
        }
        if (uniqueDays.isEmpty()) {
            return 0;
        }

        Iterator<Long> it = uniqueDays.iterator();
        long expectedDay = it.next(); // 最新の日付から開始
        int streak = 1;
        while (it.hasNext()) {
            long day = it.next();
            if (day == expectedDay - 1) {
                streak++;
                expectedDay = day;
            } else {
                break; // 連続が途切れたので終了
            }
        }
        return streak;
    }
}
