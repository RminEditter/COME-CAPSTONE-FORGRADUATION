package com.example.capstone2026;

import android.Manifest;
import android.content.Intent;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PointF;
import android.content.res.ColorStateList;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
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

    private MapTagIcons tagIcons;
    private TextView markerLegend;
    private MapView mapView;
    private MapLibreMap mapLibreMap;

    private Spinner spinnerDistrict;
    private Button btnMyLocation;
    private Button btnTagFilter;
    private boolean matchAllTags = true;

    // 바텀시트 관련 UI
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvCafeName, tvCafeAddress, tvCafeReason;
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

        tagIcons = new MapTagIcons(this);
        markerLegend = findViewById(R.id.mapMarkerLegend);
        setupBackButton();
        initBottomSheet();

        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnTagFilter = findViewById(R.id.btnTagFilter);
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

            // 지도 이동/확대가 끝나면 현재 화면에 맞게 마커를 다시 계산합니다.
            mapLibreMap.addOnCameraIdleListener(() -> {
                if (!allCafeItems.isEmpty()) showFilteredCafeMarkers();
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
        findViewById(R.id.btnMapZoomIn).setOnClickListener(v -> {
            if (mapLibreMap != null) mapLibreMap.animateCamera(CameraUpdateFactory.zoomIn());
        });
        findViewById(R.id.btnMapZoomOut).setOnClickListener(v -> {
            if (mapLibreMap != null) mapLibreMap.animateCamera(CameraUpdateFactory.zoomOut());
        });
    }

    private void initBottomSheet() {
        View bottomSheet = findViewById(R.id.bottomSheetLayout);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            tvCafeName = findViewById(R.id.tvCafeName);
            tvCafeAddress = findViewById(R.id.tvCafeAddress);
            tvCafeReason = findViewById(R.id.tvCafeReason);
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
            MapTagIcons.styleChip(chip, MapTagStyle.forTag(tag));
            chip.setClickable(false);
            chip.setCheckable(false);
            chipGroupCafeTags.addView(chip);
        }

        String reason = buildMapRecommendationReason(cafe);
        tvCafeReason.setText(reason);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private String buildMapRecommendationReason(CafeMapItem cafe) {
        List<String> matched = new ArrayList<>();
        List<String> features = new ArrayList<>();
        if (cafe.rawTags != null) {
            for (Tag tag : cafe.rawTags) {
                if (tag == null) continue;
                String label = tag.getKoreanLabel();
                if (selectedTagFilters.contains(tag)) matched.add(label);
                else features.add(label);
            }
        }

        StringBuilder reason = new StringBuilder();
        if (!matched.isEmpty()) {
            int limit = Math.min(2, matched.size());
            reason.append("✓ 선택한 '")
                    .append(android.text.TextUtils.join("', '", matched.subList(0, limit)))
                    .append("' 조건과 일치해요.");
            if (matched.size() > limit) reason.append(" (외 ").append(matched.size() - limit).append("개 조건)");
        } else if (!selectedTagFilters.isEmpty()) {
            reason.append("선택한 태그와 직접 일치하는 특징은 없어요.");
        } else if (!features.isEmpty()) {
            reason.append("이 카페는 '").append(features.get(0)).append("' 태그가 등록되어 있어요.");
        } else {
            reason.append("등록된 카페 특징이 아직 없어요.");
        }

        if (lastKnownLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                    cafe.latitude, cafe.longitude, results);
            if (!Float.isNaN(results[0]) && !Float.isInfinite(results[0])) {
                int meters = Math.round(results[0]);
                String distance = meters >= 1000
                        ? String.format(java.util.Locale.KOREA, "%.1fkm", meters / 1000.0)
                        : meters + "m";
                reason.append("\n• 현재 위치에서 약 ").append(distance).append(" 거리에 있어요.");
            }
        }
        return reason.toString();
    }

    // 상세 필터: 기존 태그 enum 18개를 분류해 표시한다.
    private void setupTagFilter() {
        btnTagFilter.setOnClickListener(v -> showTagFilterDialog());
        updateMarkerLegend();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showTagFilterDialog() {
        final Tag[][] groups = {
                {Tag.BEAN_NUTTY, Tag.BEAN_ACIDIC},
                {Tag.INTERIOR_PRETTY, Tag.DRINK_TASTY, Tag.HIP, Tag.WORK_FRIENDLY},
                {Tag.DESSERT, Tag.SPECIALTY_DRIP, Tag.OUTLET_MANY, Tag.WIFI_FAST, Tag.LAPTOP_OK},
                {Tag.SMALL_CAFE, Tag.LARGE_CAFE},
                {Tag.SOLO, Tag.COUPLE, Tag.FRIEND, Tag.FAMILY, Tag.COLLEAGUE}
        };
        final String[] groupNames = {"원두 취향", "카페 분위기", "메뉴 및 편의시설", "카페 규모", "동반자"};
        final List<Tag> pending = new ArrayList<>(selectedTagFilters);
        final boolean[] pendingAnd = {matchAllTags};

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(12));
        scroll.addView(content);

        TextView modeTitle = new TextView(this);
        modeTitle.setText("여러 조건을 선택했을 때");
        modeTitle.setTextColor(Color.rgb(60, 43, 33));
        modeTitle.setTextSize(15);
        content.addView(modeTitle);

        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(LinearLayout.VERTICAL);
        RadioButton and = new RadioButton(this);
        and.setText("AND · 모든 조건 만족");
        RadioButton or = new RadioButton(this);
        or.setText("OR · 하나 이상 만족");
        modes.addView(and);
        modes.addView(or);
        (matchAllTags ? and : or).setChecked(true);
        modes.setOnCheckedChangeListener((group, checkedId) -> pendingAnd[0] = and.isChecked());
        content.addView(modes);

        for (int i = 0; i < groups.length; i++) {
            TextView title = new TextView(this);
            title.setText(groupNames[i]);
            title.setTextSize(16);
            title.setTextColor(Color.rgb(80, 53, 36));
            title.setPadding(0, dp(18), 0, dp(6));
            content.addView(title);
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setSingleSelection(false);
            chipGroup.setSingleLine(false);
            for (Tag tag : groups[i]) {
                Chip chip = new Chip(this);
                chip.setText(tag.getKoreanLabel());
                chip.setCheckable(true);
                chip.setEnsureMinTouchTargetSize(true);
                chip.setTextSize(13f);
                chip.setChipCornerRadius(dp(18));
                chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(217, 210, 204)));
                chip.setCheckedIconVisible(false);
                chip.setChecked(pending.contains(tag));
                styleFilterChip(chip, chip.isChecked());
                chip.setOnCheckedChangeListener((button, checked) -> {
                    if (checked && !pending.contains(tag)) pending.add(tag);
                    if (!checked) pending.remove(tag);
                    styleFilterChip(chip, checked);
                });
                chipGroup.addView(chip);
            }
            content.addView(chipGroup);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("카페 상세 필터")
                .setView(scroll)
                .setNeutralButton("초기화", null)
                .setNegativeButton("취소", (d, w) -> {})
                .setPositiveButton("적용", (d, w) -> {
                    selectedTagFilters.clear();
                    selectedTagFilters.addAll(pending);
                    matchAllTags = pendingAnd[0];
                    updateMarkerLegend();
                    showFilteredCafeMarkers();
                }).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v -> {
                    selectedTagFilters.clear();
                    matchAllTags = true;
                    updateMarkerLegend();
                    showFilteredCafeMarkers();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    // 선택한 태그는 진한 테두리와 브라운 배경으로 확실하게 강조
    private void styleFilterChip(Chip chip, boolean selected) {
        int border = selected ? Color.rgb(135, 82, 54) : Color.rgb(217, 210, 204);
        int background = selected ? Color.rgb(247, 228, 213) : Color.WHITE;
        int foreground = selected ? Color.rgb(117, 67, 41) : Color.rgb(65, 57, 52);
        chip.setChipBackgroundColor(ColorStateList.valueOf(background));
        chip.setChipStrokeColor(ColorStateList.valueOf(border));
        chip.setChipStrokeWidth(dp(selected ? 3 : 1));
        chip.setTextColor(foreground);
        chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        chip.setChipStartPadding(dp(10));
        chip.setChipEndPadding(dp(10));
    }

    private void updateMarkerLegend() {
        if (markerLegend == null) return;
        if (selectedTagFilters.isEmpty()) {
            markerLegend.setText("전체 카페 표시 · 상세 필터에서 취향을 선택하세요");
            btnTagFilter.setText("☰ 상세 필터");
        } else {
            markerLegend.setText((matchAllTags ? "모든 조건(AND)" : "하나 이상(OR)")
                    + " · " + selectedTagFilters.size() + "개 태그 선택됨");
            btnTagFilter.setText("☰ 필터 " + selectedTagFilters.size() + "개");
        }
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
        final List<Tag> markerTags = new ArrayList<>(selectedTagFilters);
        List<CafeMapItem> matches = MapCafeFilter.select(allCafeItems, selectedDistrict, selectedTagFilters, matchAllTags);
        // 숫자 클러스터 대신 확대 수준에 맞춰 작은 화면 격자당 카페 1개만 표시합니다.
        // 화면 밖 카페는 그리지 않으며, 확대할수록 더 많은 개별 마커가 나타납니다.
        List<CafeMapItem> visible = new ArrayList<>();
        int mapWidth = mapView.getWidth();
        int mapHeight = mapView.getHeight();
        if (mapWidth > 0 && mapHeight > 0) {
            double zoom = mapLibreMap.getCameraPosition().zoom;
            float density = getResources().getDisplayMetrics().density;
            float spacing = (zoom < 12 ? 42f : zoom < 14 ? 32f : zoom < 16 ? 22f : 0f) * density;
            java.util.HashSet<String> usedCells = new java.util.HashSet<>();
            for (CafeMapItem cafe : matches) {
                PointF p = mapLibreMap.getProjection().toScreenLocation(
                        new LatLng(cafe.latitude, cafe.longitude));
                if (p.x < 0 || p.y < 0 || p.x > mapWidth || p.y > mapHeight) continue;
                if (spacing > 0) {
                    String cell = ((int) Math.floor(p.x / spacing)) + ":" +
                            ((int) Math.floor(p.y / spacing));
                    if (!usedCells.add(cell)) continue;
                }
                visible.add(cafe);
            }
        } else {
            visible.addAll(matches);
        }
        final List<CafeMapItem> markersToDraw = visible;
        // Spread native marker operations across frames.
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
                    while (index < markersToDraw.size() && operations++ < 30) {
                        CafeMapItem cafe = markersToDraw.get(index++);
                        Marker marker = mapLibreMap.addMarker(new MarkerOptions()
                                .position(new LatLng(cafe.latitude, cafe.longitude)).title(cafe.name)
                                .icon(tagIcons.icon(cafe.rawTags, markerTags)));
                        cafeMarkers.add(marker);
                        markerCafeMap.put(marker.getId(), cafe);
                        if (android.os.SystemClock.uptimeMillis() >= deadline) break;
                    }
                }
                if (!removedOldMarkers || index < markersToDraw.size()) markerHandler.postDelayed(this, 16);
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
        intent.putExtra("cafe_reason", buildMapRecommendationReason(cafe));
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
