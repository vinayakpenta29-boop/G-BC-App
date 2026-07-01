package com.example.chitfund;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChitFundMatrix.db";
    private static final int DATABASE_VERSION = 3; // Bumped to 3 to introduce full historical tracking columns

    public static class ChitItem {
        public long id;
        public String name;
        public ChitItem(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chits (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, frequency TEXT, installments INTEGER, amount_type TEXT, start_date TEXT)");
        db.execSQL("CREATE TABLE members (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, name TEXT)");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, date TEXT, member_name TEXT, amount REAL)");
        db.execSQL("CREATE TABLE installment_structures (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, amount REAL)");
        
        // UPDATE: Created complete architecture structure schema tracking variables natively
        db.execSQL("CREATE TABLE advances (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, member_name TEXT, advance_amount REAL, new_amount REAL, date TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS advances");
            db.execSQL("CREATE TABLE advances (id INTEGER PRIMARY KEY AUTOINCREMENT, chit_id INTEGER, installment_num INTEGER, member_name TEXT, advance_amount REAL, new_amount REAL, date TEXT)");
        }
    }

    public void insertAdvance(long chitId, int installmentNum, String memberName, double advanceAmount, double newAmount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("chit_id", chitId);
        v.put("installment_num", installmentNum);
        v.put("member_name", memberName);
        v.put("advance_amount", advanceAmount);
        v.put("new_amount", newAmount);
        v.put("date", date);
        db.insert("advances", null, v);
    }

    // ADDED: Returns full historical advances logs tracking records 
    public Cursor getAllAdvancesRecordsCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT a.date, c.name AS chit_name, a.member_name, a.installment_num, a.advance_amount, a.new_amount " +
                "FROM advances a " +
                "JOIN chits c ON a.chit_id = c.id " +
                "ORDER BY a.id DESC", null);
    }

    public double getMemberInstallmentAmount(long chitId, String memberName, int installmentNum) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT new_amount FROM advances " +
                "WHERE chit_id = ? AND member_name = ? AND installment_num < ? " +
                "ORDER BY installment_num DESC LIMIT 1",
                new String[]{String.valueOf(chitId), memberName, String.valueOf(installmentNum)});
        
        double customizedAmount = -1;
        if (c.moveToFirst()) {
            customizedAmount = c.getDouble(0);
        }
        c.close();
        
        if (customizedAmount != -1) return customizedAmount;
        return getInstallmentAmount(chitId, installmentNum);
    }

    public Cursor getTransactionHistoryCursor(long filterChitId) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (filterChitId == -1) {
            return db.rawQuery(
                    "SELECT p.date, c.name AS chit_name, p.member_name, p.installment_num, p.amount " +
                    "FROM payments p " +
                    "JOIN chits c ON p.chit_id = c.id " +
                    "ORDER BY p.id DESC", null);
        } else {
            return db.rawQuery(
                    "SELECT p.date, c.name AS chit_name, p.member_name, p.installment_num, p.amount " +
                    "FROM payments p " +
                    "JOIN chits c ON p.chit_id = c.id " +
                    "WHERE p.chit_id = ? " +
                    "ORDER BY p.id DESC", new String[]{String.valueOf(filterChitId)});
        }
    }

    public ArrayList<ChitItem> getChitList() {
        ArrayList<ChitItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name FROM chits ORDER BY id DESC", null);
        while (c.moveToNext()) {
            list.add(new ChitItem(c.getLong(0), c.getString(1)));
        }
        c.close();
        return list;
    }

    public long getLatestChitId() {
        long id = -1;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM chits ORDER BY id DESC LIMIT 1", null);
        if (c.moveToFirst()) {
            id = c.getLong(0);
        }
        c.close();
        return id;
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
