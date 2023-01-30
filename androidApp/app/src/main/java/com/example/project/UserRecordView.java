package com.example.project;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class UserRecordView extends AppCompatActivity {
    private String dirPath = "/data/data/com.example.project/databases"; //데이터베이스의 경로
    String filePath = dirPath + "/" + "recorddb";
    ArrayList<RecordVO> datas  = new ArrayList<>();

    ListView listView;
    RecordAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_record_view);

        try {
            File file = new File(filePath);
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
            Cursor cursor = db.rawQuery("select link, readdate from tb_record order by readdate desc", null);

            while (cursor.moveToNext()) {
                String link = cursor.getString(0);
                String date = cursor.getString(1);
                RecordVO vo = new RecordVO();
                vo.link = "링크 : " + link;
                vo.readDate = "읽은 시간 : " + date;

                datas.add(vo);
            }
            cursor.close();

            listView = findViewById(R.id.list_record_item);
            adapter = new RecordAdapter(UserRecordView.this, R.layout.record_layout, datas);
            listView.setAdapter(adapter);
        }
        catch (Exception e)
        {}
    }
}