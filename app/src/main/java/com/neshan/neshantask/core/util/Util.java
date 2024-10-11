package com.neshan.neshantask.core.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.view.View;
import com.neshan.neshantask.R;
import com.neshan.neshantask.core.snackbar.SnackBar;
import com.neshan.neshantask.core.snackbar.SnackBarType;
import com.neshan.neshantask.data.model.error.GeneralError;
import com.neshan.neshantask.data.model.error.NetworkError;
import com.neshan.neshantask.data.model.error.ServerError;
import com.neshan.neshantask.data.model.error.SimpleError;
import com.neshan.neshantask.data.model.error.TimeoutError;
import com.neshan.neshantask.data.model.error.UnknownError;

import org.neshan.common.model.LatLng;

import retrofit2.HttpException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Util {

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Get error detail from Throwable object
     */
    public static GeneralError getError(Throwable throwable) {
        throwable.printStackTrace();

        if (throwable instanceof UnknownHostException || throwable instanceof SocketException) {
            return NetworkError.instance();
        } else if (throwable instanceof SocketTimeoutException) {
            return TimeoutError.instance();
        } else if (throwable instanceof HttpException) {
            // TODO: improve parsing server errors
            return ServerError.fromCode(((HttpException) throwable).response() != null ? ((HttpException) throwable).response().code() : 0);
        } else {
            return UnknownError.instance();
        }
    }

    /**
     * Show error as snack bar
     */
    public static void showError(View rootView, GeneralError error) {
        if (error instanceof NetworkError) {
            SnackBar.make(rootView, R.string.network_connection_error, SnackBarType.ERROR, null).show();

        } else if (error instanceof ServerError) {
            SnackBar.make(rootView, R.string.unknown_server_error, SnackBarType.ERROR, null).show();
        } else if (error instanceof SimpleError) {
            SnackBar.make(rootView, ((SimpleError) error).getErrorMessage(), SnackBarType.ERROR, null).show();
        } else if (error instanceof UnknownError) {
            SnackBar.make(rootView, R.string.unknown_error, SnackBarType.ERROR, null).show();
        }
    }

    /**
     * Calculates distance to target point
     */
    public static float[] distanceFrom(LatLng thisLatLng, LatLng latLng) {
        float[] distanceResult = new float[3];
        Location.distanceBetween(thisLatLng.getLatitude(), thisLatLng.getLongitude(), latLng.getLatitude(), latLng.getLongitude(), distanceResult);
        return distanceResult;
    }

    /**
     * Checks points are the same
     */
    public static boolean equalsTo(LatLng thisLatLng, LatLng latLng) {
        return (thisLatLng.getLatitude() == latLng.getLatitude() && thisLatLng.getLongitude() == latLng.getLongitude());
    }

    /**
     * Calculate angle between two points (LatLng) with north axis
     */
    public static double angleWithNorthAxis(LatLng p1, LatLng p2) {
        double longDiff = p2.getLongitude() - p1.getLongitude();

        double a = Math.atan2(
                Math.sin(longDiff) * Math.cos(p2.getLatitude()),
                Math.cos(p1.getLatitude()) * Math.sin(p2.getLatitude())
                        - Math.sin(p1.getLatitude())
                        * Math.cos(p2.getLatitude())
                        * Math.cos(longDiff)
        ) * 180 / Math.PI;

        return (a + 360) % 360;
    }
}
