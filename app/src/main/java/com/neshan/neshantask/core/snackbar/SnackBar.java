package com.neshan.neshantask.core.snackbar;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.neshan.neshantask.R;

public class SnackBar extends BaseTransientBottomBar<SnackBar> {

    public SnackBar(ViewGroup parent, SnackBarView content) {
        super(parent, content, content);
        
        View sbView = getView();
        sbView.setBackgroundColor(
                ContextCompat.getColor(view.getContext(), android.R.color.transparent)
        );
        sbView.setPadding(0, 0, 0, 0);

        ViewGroup.LayoutParams params = sbView.getLayoutParams();
        if (params instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams coordinatorParams = (CoordinatorLayout.LayoutParams) params;
            coordinatorParams.gravity = Gravity.TOP;
            coordinatorParams.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.margin_30);
            coordinatorParams.leftMargin = 0;
            coordinatorParams.rightMargin = 0;
            sbView.setLayoutParams(coordinatorParams);
        } else {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
            frameParams.gravity = Gravity.TOP;
            frameParams.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.margin_30);
            frameParams.leftMargin = 0;
            frameParams.rightMargin = 0;
            sbView.setLayoutParams(frameParams);
        }

        setAnimationMode(ANIMATION_MODE_FADE);
    }

    public static SnackBar make(View anchorView, @StringRes int text, SnackBarType type, @DrawableRes Integer icon) {
        return make(anchorView, anchorView.getContext().getResources().getString(text), type, icon);
    }

    public static SnackBar make(View anchorView, String text, SnackBarType type, @DrawableRes Integer icon) {
        // First, we find a suitable parent for our custom view
        ViewGroup parent = findSuitableParent(anchorView);
        if (parent == null) {
            throw new IllegalArgumentException("No suitable parent found from the given view. Please provide a valid view.");
        }

        // We inflate our custom view
        SnackBarView snackBarView = (SnackBarView) LayoutInflater.from(anchorView.getContext()).inflate(
                R.layout.layout_snack_bar, parent, false
        );
        SnackBar snackBar = new SnackBar(parent, snackBarView);

        snackBarView.setSnackBarText(text);
        if (type != null) {
            snackBarView.setSnackBarType(type);
        }
        if (icon != null) {
            snackBarView.setSnackBarIconResource(icon);
        }
        snackBarView.onCloseClick(snackBar::dismiss);

        return snackBar;
    }

    private static ViewGroup findSuitableParent(View targetView) {
        View view = targetView;
        ViewGroup fallback = null;
        while (view != null) {
            if (view instanceof CoordinatorLayout) {
                // We've found a CoordinatorLayout, use it
                return (CoordinatorLayout) view;
            } else if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    // If we've hit the decor content view, then we didn't find a CoL in the hierarchy, so use it.
                    return (FrameLayout) view;
                } else {
                    // It's not the content view but we'll use it as our fallback
                    fallback = (FrameLayout) view;
                }
            }

            // Else, we will loop and crawl up the view hierarchy and try to find a parent
            View parent = (View) view.getParent();
            view = parent instanceof View ? parent : null;
        }

        // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
        return fallback;
    }
}
