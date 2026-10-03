package com.example.chitfund;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChitFundLocalDB.db";
    private static final int DATABASE_VERSION = 4; 

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chits (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, frequency TEXT, installments INTEGER, amount_type TEXT, start_date TEXT, amounts TEXT)");
        db.execSQL("CREATE TABLE members (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id TEXT, name TEXT)");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id TEXT, installment_num INTEGER, date TEXT, member_name TEXT, amount REAL, timestamp INTEGER, notes TEXT)");
        db.execSQL("CREATE TABLE advances (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id TEXT, installment_num INTEGER, member_name TEXT, advance_amount REAL, new_amount REAL, date TEXT, notes TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS chits");
        db.execSQL("DROP TABLE IF EXISTS members");
        db.execSQL("DROP TABLE IF EXISTS payments");
        db.execSQL("DROP TABLE IF EXISTS advances");
        onCreate(db);
    }

    public long insertChit(String name, String frequency, int installments, String amountType, String date, ArrayList<Double> amountsArray) {
        SQLiteDatabase db = this.getWritableDatabase();
        StringBuilder amountsStr = new StringBuilder();
        for (int i = 0; i < amountsArray.size(); i++) {
            amountsStr.append(amountsArray.get(i));
            if (i < amountsArray.size() - 1) amountsStr.append(",");
        }

        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("frequency", frequency);
        v.put("installments", installments);
        v.put("amount_type", amountType);
        v.put("start_date", date);
        v.put("amounts", amountsStr.toString());
        return db.insert("chits", null, v);
    }

    public void insertMember(String chitId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("name", name);
        db.insert("members", null, v);
    }

    public void insertPayment(String chitId, int instNum, String memberName, double amount, String date, long timestamp, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("installment_num", instNum);
        v.put("member_name", memberName);
        v.put("amount", amount);
        v.put("date", date);
        v.put("timestamp", timestamp);
        v.put("notes", notes);
        db.insert("payments", null, v);
    }

    public void insertAdvance(String chitId, int instNum, String memberName, double advanceAmount, double newAmount, String date, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("installment_num", instNum);
        v.put("member_name", memberName);
        v.put("advance_amount", advanceAmount);
        v.put("new_amount", newAmount);
        v.put("date", date);
        v.put("notes", notes);
        db.insert("advances", null, v);
    }

    public void updateAdvance(String advanceId, int instNum, String memberName, double advanceAmount, double newAmount, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("installment_num", instNum);
        v.put("member_name", memberName);
        v.put("advance_amount", advanceAmount);
        v.put("new_amount", newAmount);
        v.put("notes", notes);
        db.update("advances", v, "id=?", new String[]{advanceId});
    }

    public void deleteChitFull(String chitId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("chits", "id=?", new String[]{chitId});
        db.delete("members", "chit_id=?", new String[]{chitId});
        db.delete("payments", "chit_id=?", new String[]{chitId});
        db.delete("advances", "chit_id=?", new String[]{chitId});
    }
}
