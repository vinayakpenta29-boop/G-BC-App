package com.example.chitfund;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChitFundMatrix.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chits (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, frequency TEXT, installments INTEGER, amount_type TEXT, start_date TEXT)");
        db.execSQL("CREATE TABLE members (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, name TEXT)");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, date TEXT, member_name TEXT, amount REAL)");
        db.execSQL("CREATE TABLE installment_structures (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, amount REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS installment_structures");
        db.execSQL("DROP TABLE IF EXISTS payments");
        db.execSQL("DROP TABLE IF EXISTS members");
        db.execSQL("DROP TABLE IF EXISTS chits");
        onCreate(db);
    }

    public long insertChit(String name, String frequency, int installments, String amountType, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("frequency", frequency);
        v.put("installments", installments);
        v.put("amount_type", amountType);
        v.put("start_date", date);
        return db.insert("chits", null, v);
    }

    public void insertInstallmentAmount(long chitId, int instNum, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("installment_num", instNum);
        v.put("amount", amount);
        db.insert("installment_structures", null, v);
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
        return this.getReadableDatabase().rawQuery("SELECT id AS _id, name, installments, amount_type FROM chits ORDER BY id DESC", null);
    }

    public double getInstallmentAmount(long chitId, int installmentNum) {
        double amt = 0.0;
        Cursor c = this.getReadableDatabase().rawQuery("SELECT amount FROM installment_structures WHERE chit_id = ? AND installment_num = ?", 
                new String[]{String.valueOf(chitId), String.valueOf(installmentNum)});
        if (c.moveToFirst()) {
            amt = c.getDouble(0);
        }
        c.close();
        return amt;
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

    public boolean isPaymentMade(long chitId, String memberName, int installmentNum) {
        Cursor c = this.getReadableDatabase().rawQuery("SELECT id FROM payments WHERE chit_id = ? AND member_name = ? AND installment_num = ?", 
                new String[]{String.valueOf(chitId), memberName, String.valueOf(installmentNum)});
        boolean paid = c.getCount() > 0;
        c.close();
        return paid;
    }
}
