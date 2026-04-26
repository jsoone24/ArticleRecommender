package com.example.project;

import java.io.Serializable;

import io.realm.RealmObject;

/**
 * A bookmarked article. Persisted in the local Realm; never sent to the
 * server. Carries a snapshot of the scraped article body so bookmarks
 * remain readable even if the source page later changes or 404s.
 */
public class BookmarkVO extends RealmObject implements Serializable {
    public String title;
    public String summary;
    public String link;
    public String date;
    public String readdate;
    public String publisher;
    public String article;
    public String imageUri;
}