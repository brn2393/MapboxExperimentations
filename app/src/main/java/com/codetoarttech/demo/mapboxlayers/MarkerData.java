package com.codetoarttech.demo.mapboxlayers;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class MarkerData {
    private Bitmap bitmap;
    private LatLng latLng;

    public MarkerData(Bitmap bitmap, LatLng latLng) {
        this.bitmap = bitmap;
        this.latLng = latLng;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public LatLng getLatLng() {
        return latLng;
    }
}
