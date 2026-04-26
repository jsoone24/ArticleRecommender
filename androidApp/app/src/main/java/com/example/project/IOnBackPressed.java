package com.example.project;

/**
 * Lets a fragment opt into intercepting the host activity's back press.
 * {@link MainActivity#onBackPressed()} consults the visible fragment via
 * this interface before falling through to the default behavior.
 */
public interface IOnBackPressed {
    /** Return {@code true} to consume the press; {@code false} to let the activity handle it. */
    boolean onBackPressed();
}
