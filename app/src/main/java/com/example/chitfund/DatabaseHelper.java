package com.example.chitfund;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ChitFund.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_GROUPS = "chit_groups";
    public static final String TABLE_AUCTIONS = "auctions";

    // Common Column
    public static final String COL_ID = "id";

    // Groups Table Columns
    public static final String COL_GROUP_NAME = "group_name";
    public static final String COL_TOTAL_VALUE = "total_value";
    public static final String COL_MONTHS = "duration_months";
    public static final String COL_MIN_CONTRIBUTION = "monthly_contribution";

    // Auctions Table Columns
    public static final String COL_GROUP_ID = "group_id";
    public static final String COL_MONTH_NUMBER = "month_number";
    public static final String COL_WINNER_NAME = "winner_name";
    public static final String COL_WINNING_BID = "winning_bid"; // The discount amount offered
    public static final String COL_DIVIDEND_PER_MEMBER = "dividend_per_member";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Groups Table
        String CREATE_GROUPS_TABLE = "CREATE TABLE " + TABLE_GROUPS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_GROUP_NAME + " TEXT,"
                + COL_TOTAL_VALUE + " REAL,"
                + COL_MONTHS + " INTEGER,"
                + COL_MIN_CONTRIBUTION + " REAL" + ")";
        db.execSQL(CREATE_GROUPS_TABLE);

        // Create Auctions Table
        String CREATE_AUCTIONS_TABLE = "CREATE TABLE " + TABLE_AUCTIONS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_GROUP_ID + " INTEGER,"
                + COL_MONTH_NUMBER + " INTEGER,"
                + COL_WINNER_NAME + " TEXT,"
                + COL_WINNING_BID + " REAL,"
                + COL_DIVIDEND_PER_MEMBER + " REAL,"
                + "FOREIGN KEY(" + COL_GROUP_ID + ") REFERENCES " + TABLE_GROUPS + "(" + COL_ID + "))";
        db.execSQL(CREATE_AUCTIONS_TABLE);
    }

    @Override
    public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUCTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        onCreate(db);
    }

    // Method to insert a new Chit Group
    public long createGroup(String name, double totalValue, int months) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_GROUP_NAME, name);
        values.put(COL_TOTAL_VALUE, totalValue);
        values.put(COL_MONTHS, months);
        values.put(COL_MIN_CONTRIBUTION, totalValue / months); // Base contribution before dividend

        return db.insert(TABLE_GROUPS, null, values);
    }
}
