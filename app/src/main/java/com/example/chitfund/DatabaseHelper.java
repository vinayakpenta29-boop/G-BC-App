package com.example.chitfund;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "RealChitFund.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chits (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, frequency TEXT, installments INTEGER, amount_type TEXT, amount REAL, start_date TEXT)");
        db.execSQL("CREATE TABLE members (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, name TEXT)");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, date TEXT, member_name TEXT, amount REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS payments");
        db.execSQL("DROP TABLE IF EXISTS members");
        db.execSQL("DROP TABLE IF EXISTS chits");
        onCreate(db);
    }

    public long insertChit(String name, String frequency, int installments, String amountType, double amount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("frequency", frequency);
        v.put("installments", installments);
        v.put("amount_type", amountType);
        v.put("amount", amount);
        v.put("start_date", date);
        return db.insert("chits", null, v);
    }

    public void insertMember(long chitId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("name", name);
        db.insert("members", null, v);
    }

    public void insertPayment(long chitId, int instNum, String date, String memberName, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("installment_num", instNum);
        v.put("date", date);
        v.put("member_name", memberName);
        v.put("amount", amount);
        db.insert("payments", null, v);
    }

    public Cursor getAllChits() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM chits ORDER BY id DESC", null);
    }

    public ArrayList<String> getMembers(long chitId) {
        ArrayList<String> list = new ArrayList<>();
        Cursor c = this.getReadableDatabase().rawQuery("SELECT name FROM members WHERE chit_id = ?", new String[]{String.valueOf(chitId)});
        while (c.moveToNext()) {
            list.add(c.getString(0));
        }
        c.close();
        return list;
    }

    public Cursor getPayments(long chitId) {
        return this.getReadableDatabase().rawQuery("SELECT installment_num, date, member_name FROM payments WHERE chit_id = ?", new String[]{String.valueOf(chitId)});
    }
}
