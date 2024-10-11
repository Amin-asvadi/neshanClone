package com.neshan.neshantask.core.snackbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import com.google.android.material.snackbar.ContentViewCallback;
import com.neshan.neshantask.R;

public class SnackBarView extends FrameLayout implements ContentViewCallback {

    private final View container;
    private final ImageView snackBarIcon;
    private final TextView snackBarText;

    public SnackBarView(Context context) {
        this(context, null);
    }

    public SnackBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SnackBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.view_snack_bar, this);
        setClipToPadding(false);
        this.container = findViewById(R.id.container);
        this.snackBarIcon = findViewById(R.id.snackBarIcon);
        this.snackBarText = findViewById(R.id.snackBarText);
    }

    @Override
    public void animateContentIn(int delay, int duration) {
        // Animation logic can be implemented here if needed.
        /*
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(snackBarIcon, View.SCALE_X, 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(snackBarIcon, View.SCALE_Y, 0f, 1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.setDuration(500);
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();
        */
    }

    @Override
    public void animateContentOut(int delay, int duration) {
        // Implement animation out if needed.
    }

    public void setSnackBarText(String text) {
        this.snackBarText.setText(text);
    }

    public void setSnackBarType(SnackBarType snackBarType) {
        switch (snackBarType) {
            case NORMAL:
                this.snackBarIcon.setImageResource(R.drawable.ic_circle_notification);
                break;
            case ERROR:
                this.snackBarIcon.setImageResource(R.drawable.ic_warning);
                break;
        }
    }

    public void setSnackBarIconResource(@DrawableRes int iconResource) {
        this.snackBarIcon.setImageResource(iconResource);
    }

    public void onCloseClick(final Runnable onClose) {
        this.container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClose.run();
            }
        });
    }
}
