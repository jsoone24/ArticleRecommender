package com.example.project;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

/** Lists every article the user has read, newest first. */
public class UserRecordView extends AppCompatActivity {
    ArrayList<RecordVO> datas = new ArrayList<>();

    ListView listView;
    RecordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_record_view);

        String filePath = Config.databasesDir(this) + "/recorddb";
        File file = new File(filePath);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(file, null);
            cursor = db.rawQuery("select link, readdate from tb_record order by readdate desc", null);

            while (cursor.moveToNext()) {
                String link = cursor.getString(0);
                String date = cursor.getString(1);
                RecordVO vo = new RecordVO();
                vo.link = "링크 : " + link;
                vo.readDate = "읽은 시간 : " + date;
                datas.add(vo);
            }

            listView = findViewById(R.id.list_record_item);
            adapter = new RecordAdapter(UserRecordView.this, R.layout.record_layout, datas);
            listView.setAdapter(adapter);
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }
}
