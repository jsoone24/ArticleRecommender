package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class RecordAdapter extends ArrayAdapter<RecordVO> {
    Context context; //어댑터를 실행한 액티비티의 context를 저장하기 위한 변수
    int resId;
    ArrayList<RecordVO> datas;

    public RecordAdapter(@NonNull Context context, int resId, ArrayList<RecordVO> datas) {
        super(context, resId);
        this.context = context;
        this.resId = resId;
        this.datas = datas;
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(resId, null);
            RecordHolder holder = new RecordHolder(convertView);
            convertView.setTag(holder);
        }
        RecordHolder holder = (RecordHolder) convertView.getTag();

        TextView recordDate = holder.recordDate;
        TextView recordLink = holder.recordLink;

        recordDate.setText(datas.get(position).readDate);
        recordLink.setText(datas.get(position).link);

        return convertView;
    }
}
