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

public class BookmarkAdapter extends ArrayAdapter<ArticleVO> { //북마크 목록을 보여주는 어댑터 설정
    Context context;
    int resId;
    ArrayList<ArticleVO> datas;

    public BookmarkAdapter(@NonNull Context context, int resId, ArrayList<ArticleVO> datas) {
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
            BookmarkHolder holder = new BookmarkHolder(convertView);
            convertView.setTag(holder);
        }
        BookmarkHolder holder = (BookmarkHolder) convertView.getTag();

        ImageView articleImage = holder.articleImage;
        TextView articleTitle = holder.articleTitle;
        TextView articleSummary = holder.articleSummary;
        TextView articleDate = holder.articleDate;
        TextView articlePublisher = holder.articlePublisher;

        final ArticleVO vo = datas.get(position); //북마크에 저장된 기사들을 불러와 북마크 목록의 사진, 제목 등을 설정한다.

        articleTitle.setText(vo.title);
        articleDate.setText(vo.date);
        articleSummary.setText(vo.summary);
        articlePublisher.setText(vo.publisher);

        articleImage.setImageResource(R.drawable.no_photo);
        if (!vo.imageUri.equals(""))
            Glide.with(context).load(vo.imageUri).into(articleImage);

        return convertView;
    }
}
