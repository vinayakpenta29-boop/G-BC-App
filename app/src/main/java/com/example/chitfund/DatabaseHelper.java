package com.example.chitfund;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChitFund.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_GROUPS = "chit_groups";
    public static final String COL_ID = "id";
    public static final String COL_GROUP_NAME = "group_name";
    public static final String COL_TOTAL_VALUE = "total_value";
    public static final String COL_MONTHS = "duration_months";
    public static final String COL_MIN_CONTRIBUTION = "monthly_contribution";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_GROUPS_TABLE = "CREATE TABLE " + TABLE_GROUPS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_GROUP_NAME + " TEXT,"
                + COL_TOTAL_VALUE + " REAL,"
                + COL_MONTHS + " INTEGER,"
                + COL_MIN_CONTRIBUTION + " REAL" + ")";
        db.execSQL(CREATE_GROUPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        onCreate(db);
    }

    public long createGroup(String name, double totalValue, int months) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_GROUP_NAME, name);
        values.put(COL_TOTAL_VALUE, totalValue);
        values.put(COL_MONTHS, months);
        values.put(COL_MIN_CONTRIBUTION, totalValue / months); 

        return db.insert(TABLE_GROUPS, null, values);
    }

    // Fetches all saved records sorted by newest entry first
    public Cursor getAllGroups() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_GROUPS + " ORDER BY " + COL_ID + " DESC", null);
    }
}
