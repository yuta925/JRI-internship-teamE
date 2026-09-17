package jp.co.jri.internship.fintech_sample1;

import java.io.Serializable;

/*　フィンテックアプリで扱うデータの形式　*/
public class FintechData implements Serializable {

    //  データの名称と型
    private final int id;              // データ番号
    private final String transDate;    // 取引日付
    private final String transTime;    // 取引時間
    private final String service;      // 連携先
    private final String category;     // 取引分類
    private final String supplier;     // 取引先
    private final String content;       // 内容
    private final String use;          // 用途分類
    private final int amount;          // 取引金額
    private final int balance;         // 残高

    // クラスにデータをセットする際の形式
    public FintechData(int id,String transDate,String transTime,String service,String category,String supplier,String content,String use,int amount,int balance){
        this.id = id;
        this.transDate = transDate;
        this.transTime = transTime;
        this.service = service;
        this.category = category;
        this.supplier = supplier;
        this.content = content;
        this.use = use;
        this.amount = amount;
        this.balance = balance;
    }

    //　それぞれのデータを参照する際に利用するメソッド
    public int getId() {
        return id;
    }

    public String getTransDate() {
        return transDate;
    }

    public String getTransTime() {
        return transTime;
    }

    public String getService() {
        return service;
    }

    public String getCategory() {
        return category;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getContent() {
        return content;
    }

    public String getUse() {
        return use;
    }

    public int getAmount() {
        return amount;
    }

    public int getBalance() {
        return balance;
    }
}


