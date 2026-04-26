package com.example.project;

import java.io.Serializable;

import io.realm.RealmObject;

/**
 * One row from the recommended-article list. Populated in
 * {@link StartScreen} by combining the {@code (link, similarity)} pair
 * from the server's {@code recommenddb} with metadata scraped from the
 * article page itself (title, publisher, image, etc.).
 *
 * Implements {@link Serializable} so an {@code ArrayList<ArticleVO>}
 * can be passed between activities through {@code Intent.putExtra}.
 */
public class ArticleVO extends RealmObject implements Serializable {
    public String link;
    public String similarity;
    public String title;
    public String date;
    public String summary;
    public String publisher;
    public String imageUri;
}
