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
import java.util.ArrayList;
import java.util.List;

import jp.co.jri.internship.fintech_sample1.login.UserData;

public class CsvReader {

    private static final String TAG = "CsvReader";
    public List<FintechData> fintechObjects = new ArrayList<>();
    public List<UserData> userObjects = new ArrayList<>();

    // FintechDataを読み込む処理（fintechObjectsに格納される）
    // flagがtrueのとき（ローカルファイルが存在）：ローカルファイル(LocalFintechDateBase.txt)から読み込む
    // flagがfalseのとき（存在しない）：FintechDataBase.csvを読み込み、ローカルファイルに書き出してから読み込む
    public void readerFintechDataBase(Context context, Boolean flag) {

        // 環境情報をcontextから取得
        AssetManager assetManager = context.getResources().getAssets();

        // ローカルファイルのファイル名を定義
        String filename = "LocalFintechDateBase.txt";

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
}
