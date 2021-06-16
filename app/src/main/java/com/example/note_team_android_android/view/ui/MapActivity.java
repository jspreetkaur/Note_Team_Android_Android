package com.example.note_team_android_android.view.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.note_team_android_android.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import java.io.IOException;
import java.security.Permissions;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Button btn;
    private final static int PLACE_PICKER_REQUEST = 999;
    private final static int LOCATION_REQUEST_CODE = 23;
    private Activity context = MapActivity.this;

    Button selectLocationBtn;

    double selectedLatitude = 0.0;
    double selectedLongitude = 0.0;
    String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        selectLocationBtn = findViewById(R.id.selectLocation);
        selectLocationBtn.setVisibility(View.GONE);
        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String[] permissions1 = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        Permissions.check(context, permissions1, null, null, new PermissionHandler() {
            @Override
            public void onGranted() {
                updateMap();

            }
        });

        selectLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("latitude", selectedLatitude);
                intent.putExtra("longitude",selectedLongitude);
                intent.putExtra("address",selectedAddress);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

    }

    private void updateMap() {
        if (mMap != null) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationChangeListener(location -> {
                /*LatLng ltlng = new LatLng(location.getLatitude(), location.getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                        ltlng, 16f);
                mMap.animateCamera(cameraUpdate);*/
            });
            Location location = mMap.getMyLocation();

            if(getIntent()!=null){
                    double latIntent = getIntent().getDoubleExtra("latitude",0.0);
                    double longIntent = getIntent().getDoubleExtra("longitude",0.0);

                if(latIntent != 0.0 && longIntent != 0.0) {
                    LatLng latLngIntent = new LatLng(latIntent,longIntent);

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLngIntent);
                    markerOptions.title(getAddress(latLngIntent));
                    mMap.clear();
                    CameraUpdate loctionIntent = CameraUpdateFactory.newLatLngZoom(latLngIntent, 16);
                    mMap.animateCamera(loctionIntent);
                    mMap.addMarker(markerOptions);
                    selectedLatitude = latIntent;
                    selectedLongitude = longIntent;
                }

            }

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);

                    markerOptions.title(getAddress(latLng));
                    mMap.clear();
                    CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                            latLng, 16);
                    mMap.animateCamera(location);
                    mMap.addMarker(markerOptions);
                    selectedLatitude = latLng.latitude;
                    selectedLongitude = latLng.longitude;
                    selectLocationBtn.setVisibility(View.VISIBLE);
                }
            });
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateMap();
    }


    private String getAddress(LatLng latLng){

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();
            selectedAddress= address;

            /*FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {

                ft.remove(prev);
            }
            ft.addToBackStack(null);
            DialogFragment dialogFragment = new ConfirmAddress();

            Bundle args = new Bundle();
            args.putDouble("lat", latLng.latitude);
            args.putDouble("long", latLng.longitude);
            args.putString("address", address);
            dialogFragment.setArguments(args);
            dialogFragment.show(getSupportFragmentManager(), "dialog");*/
            return address;
        } catch (IOException e) {
            e.printStackTrace();
            return "No Address Found";

        }


    }
}