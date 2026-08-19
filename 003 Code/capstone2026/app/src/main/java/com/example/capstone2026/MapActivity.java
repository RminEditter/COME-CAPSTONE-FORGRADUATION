package com.example.capstone2026;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private MapLibreMap mapLibreMap;

    private Spinner spinnerDistrict;
    private Button btnMyLocation;

    private FirebaseFirestore db;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Marker currentLocationMarker;

    private final List<Marker> cafeMarkers =
            new ArrayList<>();

    private final List<CafeMapItem> allCafeItems =
            new ArrayList<>();

    private final Map<Long, CafeMapItem> markerCafeMap =
            new HashMap<>();

    private String selectedDistrict = "전체";

    private Location lastKnownLocation;

    private boolean movedToCurrentLocation = false;

    private final String[] districtItems = {
            "전체",
            "유성구",
            "서구",
            "중구",
            "동구",
            "대덕구"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(
                        this
                );

        setupBackButton();

        spinnerDistrict =
                findViewById(R.id.spinnerDistrict);

        btnMyLocation =
                findViewById(R.id.btnMyLocation);

        mapView =
                findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);

        setupDistrictSpinner();

        setupLocationCallback();

        mapView.getMapAsync(map -> {

            mapLibreMap = map;

            // 대전 기본 위치
            mapLibreMap.setCameraPosition(
                    new org.maplibre.android.camera.CameraPosition.Builder()
                            .target(
                                    new LatLng(
                                            36.3504,
                                            127.3845
                                    )
                            )
                            .zoom(11.0)
                            .build()
            );

            // API 키가 필요 없는 OpenFreeMap 스타일 사용
            mapLibreMap.setStyle(
                    "https://tiles.openfreemap.org/styles/liberty",
                    style -> {

                        loadCafeData();

                        checkLocationPermission();
                    }
            );

            // 카페 마커 정보창 클릭 시 상세 화면 이동
            mapLibreMap.setOnInfoWindowClickListener(
                    marker -> {

                        CafeMapItem cafe =
                                markerCafeMap.get(
                                        marker.getId()
                                );

                        if (cafe == null) {
                            return false;
                        }

                        openCafeDetail(cafe);

                        return true;
                    }
            );
        });

        btnMyLocation.setOnClickListener(v ->
                moveToCurrentLocation()
        );
    }

    private void setupBackButton() {

        AppCompatButton btnBack =
                findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(
                    v -> finish()
            );
        }
    }

    private void setupDistrictSpinner() {

        ArrayAdapter<String> districtAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        districtItems
                );

        districtAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDistrict.setAdapter(
                districtAdapter
        );

        spinnerDistrict.setSelection(0);

        spinnerDistrict.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedDistrict =
                                districtItems[position];

                        showFilteredCafeMarkers();

                        moveCameraToDistrict(
                                selectedDistrict
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    private void loadCafeData() {

        db.collection("cafes")
                .get()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful() ||
                            task.getResult() == null) {

                        Toast.makeText(
                                this,
                                "카페 정보를 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    allCafeItems.clear();

                    for (QueryDocumentSnapshot document :
                            task.getResult()) {

                        String cafeId =
                                document.getString("id");

                        String cafeName =
                                document.getString("name");

                        String address =
                                document.getString("address");

                        Double latitude =
                                document.getDouble("latitude");

                        Double longitude =
                                document.getDouble("longitude");

                        if (latitude == null ||
                                longitude == null) {

                            continue;
                        }

                        StringBuilder tagText =
                                new StringBuilder();

                        Object tagsObject =
                                document.get("tags");

                        if (tagsObject instanceof List<?>) {

                            List<?> tagList =
                                    (List<?>) tagsObject;

                            for (Object tagObject :
                                    tagList) {

                                if (!(tagObject instanceof String)) {
                                    continue;
                                }

                                String tagString =
                                        (String) tagObject;

                                try {

                                    Tag tag =
                                            Tag.valueOf(
                                                    tagString
                                                            .trim()
                                                            .toUpperCase()
                                            );

                                    tagText.append("#")
                                            .append(
                                                    tag.getKoreanLabel()
                                            )
                                            .append(" ");

                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }

                        CafeMapItem cafe =
                                new CafeMapItem(
                                        cafeId != null
                                                ? cafeId
                                                : document.getId(),
                                        cafeName != null
                                                ? cafeName
                                                : "카페",
                                        address != null
                                                ? address
                                                : "",
                                        tagText.toString(),
                                        latitude,
                                        longitude
                                );

                        allCafeItems.add(
                                cafe
                        );
                    }

                    showFilteredCafeMarkers();
                });
    }

    private void showFilteredCafeMarkers() {

        if (mapLibreMap == null) {
            return;
        }

        clearCafeMarkers();

        for (CafeMapItem cafe :
                allCafeItems) {

            if (!isCafeInSelectedDistrict(cafe)) {
                continue;
            }

            Marker marker =
                    mapLibreMap.addMarker(
                            new MarkerOptions()
                                    .position(
                                            new LatLng(
                                                    cafe.latitude,
                                                    cafe.longitude
                                            )
                                    )
                                    .title(
                                            cafe.name
                                    )
                                    .snippet(
                                            cafe.address
                                                    + "\n상세보기를 누르려면 정보창을 터치하세요."
                                    )
                    );

            cafeMarkers.add(
                    marker
            );

            markerCafeMap.put(
                    marker.getId(),
                    cafe
            );
        }
    }

    private boolean isCafeInSelectedDistrict(
            CafeMapItem cafe
    ) {

        if ("전체".equals(
                selectedDistrict
        )) {
            return true;
        }

        if (cafe.address == null) {
            return false;
        }

        return cafe.address.contains(
                selectedDistrict
        );
    }

    private void clearCafeMarkers() {

        if (mapLibreMap == null) {
            return;
        }

        for (Marker marker :
                cafeMarkers) {

            mapLibreMap.removeMarker(
                    marker
            );
        }

        cafeMarkers.clear();
        markerCafeMap.clear();
    }

    private void moveCameraToDistrict(
            String district
    ) {

        if (mapLibreMap == null) {
            return;
        }

        double latitude;
        double longitude;
        double zoom;

        switch (district) {

            case "유성구":

                latitude = 36.3622;
                longitude = 127.3568;
                zoom = 12.0;

                break;

            case "서구":

                latitude = 36.3555;
                longitude = 127.3837;
                zoom = 12.0;

                break;

            case "중구":

                latitude = 36.3258;
                longitude = 127.4214;
                zoom = 12.0;

                break;

            case "동구":

                latitude = 36.3503;
                longitude = 127.4548;
                zoom = 12.0;

                break;

            case "대덕구":

                latitude = 36.3467;
                longitude = 127.4156;
                zoom = 12.0;

                break;

            default:

                latitude = 36.3504;
                longitude = 127.3845;
                zoom = 10.8;

                break;
        }

        mapLibreMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(
                                latitude,
                                longitude
                        ),
                        zoom
                )
        );
    }

    private void setupLocationCallback() {

        locationCallback =
                new LocationCallback() {

                    @Override
                    public void onLocationResult(
                            @NonNull LocationResult locationResult
                    ) {

                        Location location =
                                locationResult.getLastLocation();

                        if (location == null) {
                            return;
                        }

                        lastKnownLocation =
                                location;

                        updateCurrentLocationMarker(
                                location
                        );

                        if (!movedToCurrentLocation) {

                            movedToCurrentLocation =
                                    true;

                            moveToCurrentLocation();
                        }
                    }
                };
    }

    private void checkLocationPermission() {

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {

            startLocationUpdates();

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void startLocationUpdates() {

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            return;
        }

        LocationRequest locationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        5000
                )
                        .setMinUpdateIntervalMillis(
                                2000
                        )
                        .build();

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void stopLocationUpdates() {

        if (fusedLocationClient != null &&
                locationCallback != null) {

            fusedLocationClient.removeLocationUpdates(
                    locationCallback
            );
        }
    }

    private void updateCurrentLocationMarker(
            Location location
    ) {

        if (mapLibreMap == null) {
            return;
        }

        LatLng currentPosition =
                new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );

        if (currentLocationMarker == null) {

            currentLocationMarker =
                    mapLibreMap.addMarker(
                            new MarkerOptions()
                                    .position(
                                            currentPosition
                                    )
                                    .title(
                                            "현재 위치"
                                    )
                    );

        } else {

            currentLocationMarker.setPosition(
                    currentPosition
            );
        }
    }

    private void moveToCurrentLocation() {

        if (mapLibreMap == null) {
            return;
        }

        if (lastKnownLocation == null) {

            Toast.makeText(
                    this,
                    "현재 위치를 확인하고 있습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            checkLocationPermission();

            return;
        }

        mapLibreMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(
                                lastKnownLocation.getLatitude(),
                                lastKnownLocation.getLongitude()
                        ),
                        15.0
                )
        );
    }

    private void openCafeDetail(
            CafeMapItem cafe
    ) {

        Intent intent =
                new Intent(
                        MapActivity.this,
                        CafeDetailActivity.class
                );

        intent.putExtra(
                "cafe_id",
                cafe.id
        );

        intent.putExtra(
                "cafe_name",
                cafe.name
        );

        intent.putExtra(
                "cafe_address",
                cafe.address
        );

        intent.putExtra(
                "cafe_tags",
                cafe.tags
        );

        intent.putExtra(
                "cafe_reason",
                "지도에서 선택한 카페입니다."
        );

        startActivity(
                intent
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST_CODE) {

            boolean granted = false;

            for (int result :
                    grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {

                startLocationUpdates();

            } else {

                Toast.makeText(
                        this,
                        "위치 권한이 없어 현재 위치를 표시할 수 없습니다.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mapView.onResume();

        if (mapLibreMap != null) {
            checkLocationPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();

        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        super.onSaveInstanceState(outState);

        mapView.onSaveInstanceState(
                outState
        );
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopLocationUpdates();

        mapView.onDestroy();
    }

    private static class CafeMapItem {

        String id;
        String name;
        String address;
        String tags;

        double latitude;
        double longitude;

        CafeMapItem(
                String id,
                String name,
                String address,
                String tags,
                double latitude,
                double longitude
        ) {

            this.id = id;
            this.name = name;
            this.address = address;
            this.tags = tags;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}