package com.example.project;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Owns the local {@code recorddb} SQLite that captures every article the
 * user opens. Schema:
 * <pre>
 *   CREATE TABLE tb_record (
 *       _id          INTEGER PRIMARY KEY AUTOINCREMENT,
 *       readdate     TEXT,   -- yyyyMMdd HHmmss
 *       articledate  TEXT,   -- the publish date scraped from the page
 *       link         TEXT    -- article URL
 *   );
 * </pre>
 * On app close the file is uploaded to the server by {@link RecordSender}
 * so the recommender can recompute the user's keyword profile.
 */
public class RecordDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;

    public RecordDBHelper(Context context){
        super(context,"recorddb",null,DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String recordSQL = "create table tb_record "+"(_id integer primary key autoincrement,"+"readdate,"+ "articledate,"+ "link)";
        sqLiteDatabase.execSQL(recordSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        if(i1 == DATABASE_VERSION){
            sqLiteDatabase.execSQL("drop table tb_record");
            onCreate(sqLiteDatabase);
        }
    }
}
