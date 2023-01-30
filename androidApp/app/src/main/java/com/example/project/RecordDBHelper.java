package com.example.project;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RecordDBHelper extends SQLiteOpenHelper { //사용자 이용 기록 데이터베이스 관리
    public static final int DATABASE_VERSION =1;

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
