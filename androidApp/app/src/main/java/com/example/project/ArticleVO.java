package com.example.project;

import java.io.Serializable;

import io.realm.RealmObject;

public class ArticleVO extends RealmObject implements Serializable { //각 기사 정보를 효과적으로 관리하기 위한 클래스
    public String link;
    public String similarity;
    public String title;
    public String date;
    public String summary;
    public String publisher;
    public String imageUri;
}
