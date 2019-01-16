package com.codetoarttech.demo.mapboxlayers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.LocationRequest;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

@SuppressLint({"CheckResult", "LogNotTimber", "MissingPermission"})
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnCameraIdleListener, MapboxMap.OnCameraMoveListener {

    private static final String TAG = "MainActivity";
    private static final String DISCOVER_CIRCLE_SOURCE_ID = "discover_circle_source";
    private static final String DISCOVER_CIRCLE_LAYER_ID = "discover_circle_layer";
    private static final String DISCOVER_SYMBOL_SOURCE_ID = "discover_symbol_source";
    private static final String DISCOVER_SYMBOL_LAYER_ID = "discover_symbol_layer";
    private MapView mapView;
    private MapboxMap mapboxMap;
    private ImageButton btnShowLocalView;
    private ImageButton btnShowMapView;
    private LinearLayout llMapviewToggleParent;
    private ReactiveLocationProvider locationProvider;
    private Observable<Location> latestOrLastLocationObservable;
    private LocationRequest locationRequest;
    private LatLng currentLocation;
    private RxPermissions rxPermissions;
    private CompositeDisposable compositeDisposable;
    private ObjectMapper objectMapper;
    private double currentZoom;
    private double discoverCircleRadius = 100;
    private double heatmapCircleRadius = 5000;
    private boolean isCameraMoving = false;
    private boolean circleVisibilty = true;
    private HashMap<String, Bitmap> markerIdBitmapSet;
    private Set<FutureTarget<Bitmap>> futureTargetSet;
    private Set<MarkerData> markerDataSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        btnShowLocalView = findViewById(R.id.btn_show_local_view);
        btnShowLocalView.setOnClickListener(v -> {
            landNearSurface();
            btnShowLocalView.setImageResource(R.drawable.ic_my_location_blue);
            btnShowMapView.setImageResource(R.drawable.ic_public_gray);
        });
        btnShowMapView = findViewById(R.id.btn_show_map_view);
        btnShowMapView.setOnClickListener(v -> {
            floatAboveSurface();
            btnShowLocalView.setImageResource(R.drawable.ic_my_location_gray);
            btnShowMapView.setImageResource(R.drawable.ic_public_blue);
        });
        llMapviewToggleParent = findViewById(R.id.ll_mapview_toggle_parent);
        compositeDisposable = new CompositeDisposable();
        markerIdBitmapSet = new HashMap<>();
        futureTargetSet = new HashSet<>();
        markerDataSet = new HashSet<>();
        setupObjectMapper();
        setupLocationProvider();
        setupPermissionHelper();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    private void setupObjectMapper() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void landNearSurface() {
        if (mapboxMap == null || currentLocation == null) return;
        SymbolLayer symbolLayer = mapboxMap.getLayerAs(DISCOVER_SYMBOL_LAYER_ID);
        if (symbolLayer != null) {
            symbolLayer.withProperties(PropertyFactory.visibility(Property.VISIBLE));
        }
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,
                getZoomByRadius(discoverCircleRadius)));
    }

    public int getZoomByRadius(double radius) {
        int zoomLevel = 13;
        if (radius != 0) {
            radius = radius + radius / 2;
            double scale = radius / 500;
            zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel;
    }

    private void floatAboveSurface() {
        if (mapboxMap == null || currentLocation == null) return;
        SymbolLayer symbolLayer = mapboxMap.getLayerAs(DISCOVER_SYMBOL_LAYER_ID);
        if (symbolLayer != null) {
            symbolLayer.withProperties(PropertyFactory.visibility(Property.NONE));
        }
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,
                getZoomByRadius(heatmapCircleRadius)));
    }

    private void setupLocationProvider() {
        locationProvider = new ReactiveLocationProvider(this);
    }

    private void setupPermissionHelper() {
        rxPermissions = new RxPermissions(this);
        rxPermissions.setLogging(true);
    }

    private void useNewLocationValue(Location location) {
        if (mapboxMap == null || location == null) return;
        Log.d(TAG, "useNewLocationValue: " + location.toString());
        LatLng latLng = new LatLng(location);
        this.currentLocation = latLng;
        createCircleWithPolygon(discoverCircleRadius, latLng);
        if (!isCameraMoving) {
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, currentZoom));
        }
        setupResponsesFromAssets();
    }

    private void createCircleWithPolygon(double meterRadius, LatLng latLng) {
        compositeDisposable.add(Observable.just(latLng)
                .subscribeOn(Schedulers.computation())
                .switchMap((Function<LatLng, Observable<ArrayList<Point>>>) input ->
                        getObservablePointsAroundPoint(input, meterRadius))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::changeCircleWidth));
    }

    private Observable<ArrayList<Point>> getObservablePointsAroundPoint(LatLng latLng, double withMeterRadius) {
        ObservableOnSubscribe<ArrayList<Point>> handler = emitter -> {
            Future<ArrayList<Point>> future = Executors.newSingleThreadExecutor().submit(() -> {
//                Log.d(TAG, "getObservablePointsAroundPoint: " + Thread.currentThread().getName());
                double degreesBetweenPoints = 2.0;
                //45 sides
                double numberOfPoints = Math.floor(360.0 / degreesBetweenPoints);
                double distRadians = withMeterRadius / 6371000.0;
                // earth radius in meters
                double centerLatRadians = latLng.getLatitude() * Math.PI / 180;
                double centerLonRadians = latLng.getLongitude() * Math.PI / 180;
                ArrayList<Point> points = new ArrayList<>();
                for (int i = 0; i < (int) numberOfPoints; i++) {
                    double degrees = (double) i * degreesBetweenPoints;
                    double degreeRadians = degrees * Math.PI / 180;
                    double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians)
                            + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
                    double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians)
                            * Math.sin(distRadians) * Math.cos(centerLatRadians), Math.cos(distRadians)
                            - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
                    double pointLat = pointLatRadians * 180 / Math.PI;
                    double pointLon = pointLonRadians * 180 / Math.PI;
                    points.add(Point.fromLngLat(pointLon, pointLat));
                }
                emitter.onNext(points);
                emitter.onComplete();
                return null;
            });
            emitter.setCancellable(() -> future.cancel(true));
        };
        return Observable.create(handler);
    }

    private void setupResponsesFromAssets() {
        Log.i(TAG, "init: " + SystemClock.elapsedRealtime());
//        mapboxMap.clear();
        futureTargetSet.clear();
        compositeDisposable.add(Observable.just("discover_response.json")
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(input -> getAssets().open(input))
                .map(this::getJsonStringFromStream)
                .map(input -> objectMapper.readValue(input, DiscoverApiResponse.class))
                .map(DiscoverApiResponse::getDiscoveredUserList)
                .flatMapIterable(list -> list)
                .switchMap((Function<UserData, Observable<MarkerData>>) this::getMarkerDataObservable)
                .map(this::buildFeatureWithData)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateDiscoverLayerSource,
                        throwable -> Log.e(TAG, throwable.getLocalizedMessage())));
    }

    private void updateDiscoverLayerSource(List<Feature> featureSet) {
        GeoJsonSource geoJsonSource = mapboxMap.getSourceAs(DISCOVER_SYMBOL_SOURCE_ID);
        if (geoJsonSource != null) {
            geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(featureSet));
        }
        mapboxMap.addImages(markerIdBitmapSet);
//        Log.i(TAG, "getAnnotations " + mapboxMap.getAnnotations().size());
//        Log.i(TAG, "getLayers " + mapboxMap.getLayers().size());
//        Log.i(TAG, "getSources " + mapboxMap.getSources().size());
        Log.i(TAG, "complete: " + SystemClock.elapsedRealtime());
    }

    @NonNull
    private String getJsonStringFromStream(InputStream input) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        StringBuilder total = new StringBuilder();
        for (String line; (line = r.readLine()) != null; ) {
            total.append(line).append('\n');
        }
        return total.toString();
    }

    @NonNull
    private Feature buildFeatureWithData(MarkerData input) {
        Feature feature = Feature.fromGeometry(Point.fromLngLat(input.getLng(), input.getLat()));
        feature.addStringProperty("icon_id", String.valueOf(input.getId()));
        return feature;
    }

    private Observable<MarkerData> getMarkerDataObservable(UserData userData) throws java.util.concurrent.ExecutionException, InterruptedException {
        FutureTarget<Bitmap> futureTarget;
        ImageData imageData = userData.getSelectedProfileImage();
        if (imageData != null && imageData.getThumb() != null) {
            futureTarget = Glide.with(MainActivity.this)
                    .asBitmap()
                    .load(imageData.getThumb())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_phone)
                            .transform(new CircleCropBorder()))
                    .submit();
            this.futureTargetSet.add(futureTarget);
            MarkerData markerData = new MarkerData(userData.getId(), futureTarget.get(), userData.getLatLng());
            this.markerIdBitmapSet.put(String.valueOf(markerData.getId()), markerData.getBitmap());
            this.markerDataSet.add(markerData);
            return Observable.just(markerData);
        } else {
            return Observable.error(new Throwable("ImageData cannot be null!"));
        }
    }

    private void changeCircleWidth(ArrayList<Point> routeCoordinates) {
        if (mapboxMap == null) return;
//        Log.d(TAG, "changeCircleWidth: " + Thread.currentThread().getName());
        // Create the LineString from the list of coordinates and then make a GeoJSON
        // FeatureCollection so we can add the line to our map as a layer.
        LineString lineString = LineString.fromLngLats(routeCoordinates);
        FeatureCollection featureCollection =
                FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(lineString)});
        GeoJsonSource source = mapboxMap.getSourceAs(DISCOVER_CIRCLE_SOURCE_ID);
        LineLayer layer = (LineLayer) mapboxMap.getLayer(DISCOVER_CIRCLE_LAYER_ID);
        if (source != null) {
            source.setGeoJson(featureCollection);
        } else {
            GeoJsonSource geoJsonSource = new GeoJsonSource(DISCOVER_CIRCLE_SOURCE_ID, featureCollection);
            mapboxMap.addSource(geoJsonSource);
        }
        if (layer == null) {
            LineLayer lineLayer = new LineLayer(DISCOVER_CIRCLE_LAYER_ID, DISCOVER_CIRCLE_SOURCE_ID);
            // The layer properties for our line. This is where we make the line dotted, set the
            // color, etc.
            lineLayer.setProperties(
                    PropertyFactory.visibility(circleVisibilty ? Property.VISIBLE : Property.NONE),
                    PropertyFactory.lineDasharray(new Float[]{0.5f, 3f}),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineColor(Color.DKGRAY)
            );
            mapboxMap.addLayer(lineLayer);
        } else {
            layer.withProperties(
                    PropertyFactory.visibility(circleVisibilty ? Property.VISIBLE : Property.NONE)
            );
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        setupMapActiveComponent();
        this.mapboxMap.setMinZoomPreference(8);
        this.mapboxMap.getUiSettings().setCompassEnabled(false);
        this.mapboxMap.getUiSettings().setRotateGesturesEnabled(false);
        this.mapboxMap.addOnCameraMoveListener(this);
        this.mapboxMap.addOnCameraIdleListener(this);
        mapView.addOnDidFinishLoadingMapListener(() -> {
            Log.i(TAG, "onDidFinishLoadingMap");
            requestLocationPermission();
        });
        setupDiscoverSymbolSource();
        setupDiscoverSymbolLayer();
    }

    private void requestLocationPermission() {
        // Must be done during an initialization phase like onCreate
        compositeDisposable.add(rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(granted -> {
                            if (granted) {
                                setupLocationRequestObservable();
                                requestNewLocation();
                            }
                            Log.e(TAG, "requestLocationPermission: granted:" + granted);
                        },
                        throwable -> Log.e(TAG, "requestLocationPermission: ", throwable)));
    }

    private void setupLocationRequestObservable() {
        latestOrLastLocationObservable =
                locationProvider.getUpdatedLocation(getLocationRequest())
                        .subscribeOn(Schedulers.io())
                        .filter(location -> location.getAccuracy() < 100)
                        .debounce(6, TimeUnit.SECONDS)
//                        .distinctUntilChanged()
                        .timeout(15, TimeUnit.SECONDS,
                                AndroidSchedulers.mainThread(), locationProvider.getLastKnownLocation())
//                        .firstElement()
                        .observeOn(AndroidSchedulers.mainThread());
    }

    private LocationRequest getLocationRequest() {
        if (locationRequest == null) {
            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                    .setExpirationDuration(TimeUnit.SECONDS.toMillis(40))
                    .setFastestInterval(9000)
                    .setInterval(10000);
        }
        return locationRequest;
    }

    private void requestNewLocation() {
        compositeDisposable.add(latestOrLastLocationObservable
//        locationProvider.getLastKnownLocation()
                .subscribe(this::useNewLocationValue));
    }

    private void setupDiscoverSymbolSource() {
        Source geoJsonSource = new GeoJsonSource(DISCOVER_SYMBOL_SOURCE_ID);
        mapboxMap.addSource(geoJsonSource);
    }

    private void setupDiscoverSymbolLayer() {
        SymbolLayer symbolLayer = new SymbolLayer(DISCOVER_SYMBOL_LAYER_ID, DISCOVER_SYMBOL_SOURCE_ID);
        symbolLayer.withProperties(
                PropertyFactory.iconImage("{icon_id}"),
                PropertyFactory.iconAllowOverlap(true)
        );
        mapboxMap.addLayer(symbolLayer);
    }

    private void setupMapActiveComponent() {
        if (rxPermissions.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        for (FutureTarget<Bitmap> request : futureTargetSet) {
            if (request != null && request.getRequest() != null) {
                Log.i(TAG, "onPause: " + request.cancel(true));
                request.getRequest().clear();
            }
        }
        compositeDisposable.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
        if (locationProvider != null && latestOrLastLocationObservable != null) {
            requestNewLocation();
        } else if (!rxPermissions.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestLocationPermission();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_overflow, menu);
        MenuItem toggleItem = menu.findItem(R.id.overflow_toggle_map_list);
        ToggleButton toggleButton = (ToggleButton) toggleItem.getActionView();
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> processToggleChecked(isChecked));
        return true;
    }

    private void processToggleChecked(boolean isChecked) {
        if (isChecked) {
            mapView.setVisibility(View.GONE);
            llMapviewToggleParent.setVisibility(View.GONE);
        } else {
            mapView.setVisibility(View.VISIBLE);
            llMapviewToggleParent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCameraIdle() {
        this.isCameraMoving = false;
        if (mapboxMap != null) this.currentZoom = mapboxMap.getCameraPosition().zoom;
    }

    @Override
    public void onCameraMove() {
        this.isCameraMoving = true;
    }
}
