package com.example.foodappsever.common;

import android.graphics.Rect;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacesItemDecoraion extends RecyclerView.ItemDecoration {
    int space;

    public SpacesItemDecoraion(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.top=outRect.bottom=outRect.left=outRect.right=space;
    }
}
