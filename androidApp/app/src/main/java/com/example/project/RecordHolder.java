package com.example.project;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/** View-holder for one row of {@code R.layout.record_layout} (reading history). */
public class RecordHolder {
    public TextView recordDate;
    public TextView recordLink;

    public RecordHolder(View root) {
        recordDate = root.findViewById(R.id.recordDate);
        recordLink = root.findViewById(R.id.recordLink);
    }
}
