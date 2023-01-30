package com.example.project;

import java.io.Serializable;

import io.realm.RealmObject;

public class BookmarkVO extends RealmObject implements Serializable { //북마크 객체 데이터를 처리하기 위한 클래스이다.
    public String title;
    public String summary;
    public String link;
    public String date;
    public String readdate;
    public String publisher;
    public String article;
    public String imageUri;
}