package com.example.project;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BookmarkHolder {
    public ImageView articleImage;
    public TextView articleTitle;
    public TextView articleSummary;
    public TextView articleDate;
    public TextView articlePublisher;

    public BookmarkHolder(View root) {
        articleImage = root.findViewById(R.id.article_image);
        articleTitle = root.findViewById(R.id.article_title);
        articleSummary = root.findViewById(R.id.article_summary);
        articleDate = root.findViewById(R.id.article_date);
        articlePublisher = root.findViewById(R.id.article_publisher);
    }
}