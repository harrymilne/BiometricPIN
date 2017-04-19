package com.harrymilne.biometricpin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Harry on 27/03/2017.
 */

public class DBManager extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "timings.db";

    public static final String COLUMN_ID = "_id";

    public static final String TABLE_USER  = "user";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PIN = "pin";
    public static final String COLUMN_TCOUNT = "timing_count";

    public static final String TABLE_TIMING = "timing";
    public static final String COLUMN_USERID = "user_id";
    public static final String COLUMN_SETID = "set_id";
    public static final String COLUMN_IDX = "idx";
    public static final String COLUMN_INTERVAL = "interval";

    public DBManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String user_table = "CREATE TABLE " + TABLE_USER + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PIN + " TEXT, " +
                COLUMN_TCOUNT + " INTEGER " +
                ");";
        db.execSQL(user_table);

        String timing_table = "CREATE TABLE " + TABLE_TIMING + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERID + " INTEGER, " +
                COLUMN_SETID + " INTEGER, " +
                COLUMN_IDX + " INTEGER, " +
                COLUMN_INTERVAL + " INTEGER " +
                ");";

        db.execSQL(timing_table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS ";
        db.execSQL(query + TABLE_USER);
        db.execSQL(query + TABLE_TIMING);
        this.onCreate(db);
    }

    public void addUser(User user) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, user.get_name());
        values.put(COLUMN_PIN, user.get_pin());
        values.put(COLUMN_TCOUNT, user.get_timing_count());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_USER, null, values);
        db.close();
    }

    public User getUser(int _id) {
        User user = null;
        SQLiteDatabase db = getWritableDatabase();
        String query = COLUMN_ID + " = ?";
        String[] id_arg = {Integer.toString(_id)};
        Cursor c = db.query(TABLE_USER, null, query, id_arg, null, null, null);
        if (c.getCount() == 1) {
            c.moveToFirst();
            String name = c.getString(c.getColumnIndex(COLUMN_NAME));
            String pin = c.getString(c.getColumnIndex(COLUMN_PIN));
            int t_count = c.getInt(c.getColumnIndex(COLUMN_TCOUNT));

            user = new User(_id, name, pin, t_count);
        }
        c.close();
        return user;
    }

    public void updateUser(User user) {
        String where = COLUMN_ID + " = ?";
        String[] whereArgs = {Integer.toString(user.get_id())};
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, user.get_name());
        values.put(COLUMN_PIN, user.get_pin());
        values.put(COLUMN_TCOUNT, user.get_timing_count());

        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_USER, values, where, whereArgs);
        db.close();
    }

    public void addTiming(Timing timing) {
        int user_id = timing.get_user_id();
        User user = this.getUser(user_id);
        user.set_timing_count(user.get_timing_count() + 1);

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERID, user_id);
        values.put(COLUMN_SETID, timing.get_timing_set_id());
        values.put(COLUMN_IDX, timing.get_idx());
        values.put(COLUMN_INTERVAL, timing.get_interval());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_TIMING, null, values);
        db.close();
        this.updateUser(user);
        Log.i("biometric_pin", String.format("%d %d %d %f", user_id, timing.get_timing_set_id(), timing.get_idx(), timing.get_interval()));
    }

    public void addTimingVector(TimingVector vector) {
        Timing[] timings = vector.get_timings();
        for (int i = 0; i < timings.length; i++) {
            this.addTiming(timings[i]);
        }
    }

    public Timing[] getAllTimings(int user_id) {
        SQLiteDatabase db = getReadableDatabase();

        String query = COLUMN_USERID + " = ?";
        String[] id_arg = {Integer.toString(user_id)};
        String order_by = COLUMN_SETID + " ASC, " + COLUMN_IDX + " ASC";
        Cursor c = db.query(TABLE_TIMING, null, query, id_arg, null, null, order_by);

        Timing[] timings = new Timing[c.getCount()];
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            int set_id = c.getInt(c.getColumnIndex(COLUMN_SETID));
            int idx = c.getInt(c.getColumnIndex(COLUMN_IDX));
            long interval = c.getLong(c.getColumnIndex(COLUMN_INTERVAL));

            timings[i] = new Timing(user_id, set_id, idx, interval);
            c.moveToNext();
        }
        c.close();
        Log.i("biometric_pin", "all timings: " + Arrays.toString(timings));
        return timings;
    }

    public TimingVector[] getAllTimingVectors(int user_id) {
        Timing[] timings = this.getAllTimings(user_id);
        User user = this.getUser(user_id);
        int setSize = user.get_pin().length()*2-1;
        Timing[] tmp = new Timing[setSize];
        TimingVector[] vectors = new TimingVector[user.get_timing_count()/setSize];

        Log.i("biometric_pin", Arrays.toString(timings));

        for (int i = 0; i < timings.length; i++) {
            tmp[i % setSize] = timings[i];
            if ((i + 1) % setSize == 0) {
                vectors[i/setSize] = new TimingVector(tmp);
                tmp = new Timing[setSize];
            }
        }

        Log.i("biometric_pin", "all vectors: " + Arrays.toString(vectors));

        return vectors;
    }
    
    public double[] compareAllVectors(TimingVector vector) {
        TimingVector[] stored = this.getAllTimingVectors(vector.get_user_id());
        double[] diffs = new double[stored.length];
        for (int i = 0; i < stored.length; i++) {
            diffs[i] = vector.diff(stored[i]);
        }
        return diffs;
    }

    public User[] getAllUsers() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USER + ";";
        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();

        User[] users = new User[c.getCount()];
        int idx = 0;
        while (!c.isAfterLast()) {
            int user_id = c.getInt(c.getColumnIndex(COLUMN_ID));
            String name = c.getString(c.getColumnIndex(COLUMN_NAME));
            String pin = c.getString(c.getColumnIndex(COLUMN_PIN));
            int timing_count = c.getInt(c.getColumnIndex(COLUMN_TCOUNT));
            users[idx] = new User(user_id, name, pin, timing_count);
            c.moveToNext();
            idx++;
        }
        db.close();
        c.close();
        return users;
    }
}
