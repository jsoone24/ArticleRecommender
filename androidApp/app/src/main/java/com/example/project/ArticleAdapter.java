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

public class ArticleAdapter extends ArrayAdapter<ArticleVO> { //기사 목록을 보여주는 어댑터 설정
    Context context; //어댑터를 실행한 액티비티의 context를 저장하기 위한 변수
    int resId;
    ArrayList<ArticleVO> datas;

    public ArticleAdapter(@NonNull Context context, int resId, ArrayList<ArticleVO> datas) {
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

        ImageView articleImage = holder.articleImage; //각 뷰에 이미지 뷰 설정
        TextView articleTitle = holder.articleTitle;
        TextView articleSummary = holder.articleSummary;
        TextView articleDate = holder.articleDate;
        TextView articlePublisher = holder.articlePublisher;

        final ArticleVO vo = datas.get(position);

        articleTitle.setText(vo.title); //어댑터 뷰에서 보여질 내용 설정
        articleDate.setText(vo.date);
        articleSummary.setText(vo.summary);
        articlePublisher.setText(vo.publisher);

        articleImage.setImageResource(R.drawable.no_photo); //사진을 실시간으로 가져와 어댑터 뷰에서 보여주는 부분
        if (!vo.imageUri.equals(""))
            Glide.with(context).load(vo.imageUri).into(articleImage);

        return convertView;
    }
}
