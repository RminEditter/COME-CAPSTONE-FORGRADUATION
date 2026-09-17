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
import android.widget.TextView;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
    private ChipGroup chipGroupFilter;

    // 바텀시트 관련 UI
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvCafeName, tvCafeAddress;
    private ChipGroup chipGroupCafeTags;
    private Button btnOpenDetail;
    private CafeMapItem selectedCafeItem;

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker currentLocationMarker;

    private final List<Marker> cafeMarkers = new ArrayList<>();
    private final List<CafeMapItem> allCafeItems = new ArrayList<>();
    private final Map<Long, CafeMapItem> markerCafeMap = new HashMap<>();

    private final android.os.Handler markerHandler = new android.os.Handler(Looper.getMainLooper());
    private int markerGeneration;
    private boolean updatingFilterChips;
    private boolean allTagsSelected = true;

    private String selectedDistrict = "전체";
    private final List<Tag> selectedTagFilters = new ArrayList<>(); // 선택된 태그 필터
    private Location lastKnownLocation;
    private boolean movedToCurrentLocation = false;

    private final String[] districtItems = {"전체", "유성구", "서구", "중구", "동구", "대덕구"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupBackButton();
        initBottomSheet();

        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        mapView = findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);

        setupDistrictSpinner();
        setupTagFilter();
        setupLocationCallback();

        mapView.getMapAsync(map -> {
            mapLibreMap = map;

            mapLibreMap.setCameraPosition(
                    new org.maplibre.android.camera.CameraPosition.Builder()
                            .target(new LatLng(36.3504, 127.3845))
                            .zoom(12.0)
                            .build()
            );

            mapLibreMap.setStyle("https://tiles.openfreemap.org/styles/liberty", style -> {
                loadCafeData();
                checkLocationPermission();
            });

            // 💡 [2번 기능] 마커 터치 시 바텀시트 오픈
            mapLibreMap.setOnMarkerClickListener(marker -> {
                if (marker.equals(currentLocationMarker)) {
                    return true;
                }

                CafeMapItem cafe = markerCafeMap.get(marker.getId());
                if (cafe != null) {
                    showCafeBottomSheet(cafe);
                }
                return true;
            });

            // 지도 바탕을 터치하면 바텀시트 내리기
            mapLibreMap.addOnMapClickListener(point -> {
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return false;
            });
        });

        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation());
    }

    private void initBottomSheet() {
        View bottomSheet = findViewById(R.id.bottomSheetLayout);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            tvCafeName = findViewById(R.id.tvCafeName);
            tvCafeAddress = findViewById(R.id.tvCafeAddress);
            chipGroupCafeTags = findViewById(R.id.chipGroupCafeTags);
            btnOpenDetail = findViewById(R.id.btnOpenDetail);

            btnOpenDetail.setOnClickListener(v -> {
                if (selectedCafeItem != null) {
                    openCafeDetail(selectedCafeItem);
                }
            });
        }
    }

    // 마커 클릭 시 바텀시트에 데이터 채우고 올리기
    private void showCafeBottomSheet(CafeMapItem cafe) {
        this.selectedCafeItem = cafe;
        tvCafeName.setText(cafe.name);

        // 내 위치와의 거리 계산
        String distanceStr = "";
        if (lastKnownLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                    cafe.latitude, cafe.longitude, results);
            int meters = Math.round(results[0]);
            distanceStr = meters >= 1000 ? String.format(" • %.1fkm", meters / 1000.0) : " • " + meters + "m";
        }

        tvCafeAddress.setText(cafe.address + distanceStr);

        // 태그 칩 세팅
        chipGroupCafeTags.removeAllViews();
        for (Tag tag : cafe.rawTags) {
            Chip chip = new Chip(this);
            chip.setText("#" + tag.getKoreanLabel());
            chip.setClickable(false);
            chip.setCheckable(false);
            chipGroupCafeTags.addView(chip);
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    // 💡 [1번 기능] 상단 태그 필터 칩 연동
    private void setupTagFilter() {
        if (chipGroupFilter == null) return;

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updatingFilterChips) return;
            updatingFilterChips = true;
            boolean allChecked = checkedIds.contains(R.id.chipAll);
            if (allChecked && !allTagsSelected) {
                group.clearCheck();
                group.check(R.id.chipAll);
            } else if (allChecked && checkedIds.size() > 1) {
                ((Chip) findViewById(R.id.chipAll)).setChecked(false);
            } else if (checkedIds.isEmpty()) {
                group.check(R.id.chipAll);
            }
            allTagsSelected = ((Chip) findViewById(R.id.chipAll)).isChecked();
            updatingFilterChips = false;
            selectedTagFilters.clear();

            for (int id : group.getCheckedChipIds()) {
                if (id == R.id.chipWork) selectedTagFilters.add(Tag.WORK_FRIENDLY);
                else if (id == R.id.chipOutlet) selectedTagFilters.add(Tag.OUTLET_MANY);
                else if (id == R.id.chipLaptop) selectedTagFilters.add(Tag.LAPTOP_OK);
                else if (id == R.id.chipDessert) selectedTagFilters.add(Tag.DESSERT);
                else if (id == R.id.chipNutty) selectedTagFilters.add(Tag.BEAN_NUTTY);
            }

            // 전체 칩(chipAll)이 선택되었거나 다른 선택 칩이 없다면 전체 보기 처리
            showFilteredCafeMarkers();
        });
    }

    private void setupBackButton() {
        AppCompatButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupDistrictSpinner() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, districtItems
        );
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);
        spinnerDistrict.setSelection(0);

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDistrict = districtItems[position];
                showFilteredCafeMarkers();
                moveCameraToDistrict(selectedDistrict);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCafeData() {
        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) return;
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, "카페 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            allCafeItems.clear();

            for (QueryDocumentSnapshot document : task.getResult()) {
                if (!CafeDiscoveryPolicy.isDiscoverable(document.getData())) continue;
                String cafeId = document.getString("id");
                String cafeName = document.getString("name");
                String address = document.getString("address");
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");

                if (latitude == null || longitude == null) continue;

                List<Tag> tagEnums = new ArrayList<>();
                StringBuilder tagText = new StringBuilder();
                Object tagsObject = document.get("tags");

                if (tagsObject instanceof List<?>) {
                    List<?> tagList = (List<?>) tagsObject;
                    for (Object tagObject : tagList) {
                        if (tagObject instanceof String) {
                            try {
                                Tag tag = Tag.valueOf(((String) tagObject).trim().toUpperCase());
                                tagEnums.add(tag);
                                tagText.append("#").append(tag.getKoreanLabel()).append(" ");
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }

                CafeMapItem cafe = new CafeMapItem(
                        cafeId != null ? cafeId : document.getId(),
                        cafeName != null ? cafeName : "카페",
                        address != null ? address : "",
                        tagText.toString(),
                        tagEnums,
                        latitude,
                        longitude
                );

                allCafeItems.add(cafe);
            }

            showFilteredCafeMarkers();
        });
    }

    // 💡 [1번 기능] 마커 렌더링 필터링 및 대용량 최적화
    private void showFilteredCafeMarkers() {
        if (mapLibreMap == null) return;

        final int generation = ++markerGeneration;
        markerHandler.removeCallbacksAndMessages(null);
        if (bottomSheetBehavior != null) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        selectedCafeItem = null;
        List<CafeMapItem> matches = MapCafeFilter.select(allCafeItems, selectedDistrict, selectedTagFilters);
        // Spread native marker operations across frames; never truncate the matching cafes.
        markerHandler.post(new Runnable() {
            int index;
            boolean removedOldMarkers;
            @Override public void run() {
                if (generation != markerGeneration || isFinishing() || isDestroyed()) return;
                long deadline = android.os.SystemClock.uptimeMillis() + 6;
                int operations = 0;
                while (!removedOldMarkers && !cafeMarkers.isEmpty() && operations++ < 30) {
                    Marker marker = cafeMarkers.remove(cafeMarkers.size() - 1);
                    markerCafeMap.remove(marker.getId());
                    mapLibreMap.removeMarker(marker);
                    if (android.os.SystemClock.uptimeMillis() >= deadline) break;
                }
                if (cafeMarkers.isEmpty()) removedOldMarkers = true;
                if (removedOldMarkers) {
                    while (index < matches.size() && operations++ < 30) {
                        CafeMapItem cafe = matches.get(index++);
                        Marker marker = mapLibreMap.addMarker(new MarkerOptions()
                                .position(new LatLng(cafe.latitude, cafe.longitude)).title(cafe.name));
                        cafeMarkers.add(marker);
                        markerCafeMap.put(marker.getId(), cafe);
                        if (android.os.SystemClock.uptimeMillis() >= deadline) break;
                    }
                }
                if (!removedOldMarkers || index < matches.size()) markerHandler.postDelayed(this, 16);
            }
        });
    }
    private void moveCameraToDistrict(String district) {
        if (mapLibreMap == null) return;
        double latitude = 36.3504, longitude = 127.3845, zoom = 10.8;

        switch (district) {
            case "유성구": latitude = 36.3622; longitude = 127.3568; zoom = 12.0; break;
            case "서구": latitude = 36.3555; longitude = 127.3837; zoom = 12.0; break;
            case "중구": latitude = 36.3258; longitude = 127.4214; zoom = 12.0; break;
            case "동구": latitude = 36.3503; longitude = 127.4548; zoom = 12.0; break;
            case "대덕구": latitude = 36.3467; longitude = 127.4156; zoom = 12.0; break;
        }

        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom));
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) return;

                lastKnownLocation = location;
                updateCurrentLocationMarker(location);

                if (!movedToCurrentLocation) {
                    movedToCurrentLocation = true;
                    moveToCurrentLocation();
                }
            }
        };
    }

    private void checkLocationPermission() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void updateCurrentLocationMarker(Location location) {
        if (mapLibreMap == null) return;
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentLocationMarker == null) {
            currentLocationMarker = mapLibreMap.addMarker(
                    new MarkerOptions().position(currentPosition).title("현재 위치")
            );
        } else {
            currentLocationMarker.setPosition(currentPosition);
        }
    }

    private void moveToCurrentLocation() {
        if (mapLibreMap == null) return;
        if (lastKnownLocation == null) {
            Toast.makeText(this, "현재 위치를 확인하고 있습니다.", Toast.LENGTH_SHORT).show();
            checkLocationPermission();
            return;
        }

        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 15.0));
    }

    private void openCafeDetail(CafeMapItem cafe) {
        Intent intent = new Intent(MapActivity.this, CafeDetailActivity.class);
        intent.putExtra("cafe_id", cafe.id);
        intent.putExtra("cafe_name", cafe.name);
        intent.putExtra("cafe_address", cafe.address);
        intent.putExtra("cafe_tags", cafe.tags);
        intent.putExtra("cafe_reason", "지도에서 선택한 카페입니다.");
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) startLocationUpdates();
            else Toast.makeText(this, "위치 권한이 없어 현재 위치를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); if (mapLibreMap != null) checkLocationPermission(); }
    @Override protected void onPause() { super.onPause(); stopLocationUpdates(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onDestroy() { markerGeneration++; markerHandler.removeCallbacksAndMessages(null); super.onDestroy(); stopLocationUpdates(); mapView.onDestroy(); }

}
