package com.codetoarttech.demo.mapboxlayers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import java.security.MessageDigest;

public class CircleCropBorder extends BitmapTransformation {
    // The version of this transformation, incremented to correct an error in a previous version.
    // See #455.
    private static final int VERSION = 1;
    private static final String ID = "com.bumptech.glide.load.resource.bitmap.CircleCrop." + VERSION;
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    public CircleCropBorder() {
        super();
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CircleCropBorder;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
                               int outWidth, int outHeight) {
        Bitmap circle = TransformationUtils.circleCrop(pool, toTransform, outWidth, outHeight);
        return switchBetweenPingStatuses(circle);
    }

    private Bitmap switchBetweenPingStatuses(Bitmap circle) {
        Bitmap mBitmap;
        int color;
        int colorEnd;
        color = Color.YELLOW;
        colorEnd = Color.RED;
        mBitmap = addBorderToCircularBitmap(circle, 10, color, colorEnd, false);
        return mBitmap;
    }

    // Custom method to add a border around circular bitmap
    private Bitmap addBorderToCircularBitmap(Bitmap srcBitmap, int borderWidth, int borderColor,
                                             int endColor, boolean isDotted) {
        // Calculate the circular bitmap width with border
        int dstBitmapWidth = srcBitmap.getWidth() + borderWidth * 2;

        Bitmap dstBitmap = Bitmap.createBitmap(dstBitmapWidth, dstBitmapWidth, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dstBitmap);
        canvas.drawBitmap(srcBitmap, borderWidth, borderWidth, null);

        Paint paint = new Paint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(new RadialGradient(canvas.getWidth(), canvas.getWidth(), canvas.getWidth(),
                endColor, borderColor, Shader.TileMode.MIRROR));
        paint.setStrokeWidth(borderWidth);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        paint.setAntiAlias(true);

        if (isDotted) {
            paint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
        }

        // Draw the circular border around circular bitmap
        canvas.drawCircle(
                canvas.getWidth() / 2, // cx
                canvas.getWidth() / 2, // cy
                canvas.getWidth() / 2 - borderWidth / 2, // Radius
                paint // Paint
        );

        // Free the native object associated with this bitmap.
        srcBitmap.recycle();

        // Return the bordered circular bitmap
        return dstBitmap;
    }
}