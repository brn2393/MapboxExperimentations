package com.codetoarttech.demo.mapboxlayers;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class MarkerData {
    private final int id;
    private final Bitmap bitmap;
    private final LatLng latLng;

    public MarkerData(int id, Bitmap bitmap, LatLng latLng) {
        this.id = id;
        this.bitmap = bitmap;
        this.latLng = latLng;
    }

    public int getId() {
        return id;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public double getLat() {
        return latLng.getLatitude();
    }

    public double getLng() {
        return latLng.getLongitude();
    }
}
