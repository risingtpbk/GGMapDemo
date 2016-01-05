package com.example.framgianguyenkeninh.mapdemo.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.framgianguyenkeninh.mapdemo.Item.DirectionPointGson;
import com.example.framgianguyenkeninh.mapdemo.Item.LocationItem;
import com.example.framgianguyenkeninh.mapdemo.Item.ReverseGeoCodeGson;
import com.example.framgianguyenkeninh.mapdemo.R;
import com.example.framgianguyenkeninh.mapdemo.StaticMethod;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Marker mMarker;
    private Marker destinationMarker;
    private LocationRequest mLocationRequest;
    private OkHttpClient okHttpClient;
    private LatLng origin;
    private LatLng destination;
    private boolean isDragging;
    private Polyline polyline;
    private String originName = "My Location";
    private String destinationName = "Destination Location";
    private boolean isSetMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(onMapReadyCallBack);

        if (!StaticMethod.canGetLocation(this)) {
            showSettingsAlert();
        }
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();
        okHttpClient = new OkHttpClient();
    }

    private ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {

            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mMarker != null) mMarker.remove();
            try {
                mMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                        .title(originName));
                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                mMap.moveCamera(center);
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                Log.i("CURRENTLOC", mLastLocation.getLatitude() + ";" + mLastLocation.getLongitude());
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, locationListener);

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if (destinationMarker != null && isDragging == false) {
                        destinationMarker.remove();
                    }
                    destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                                    .draggable(true)
                                    .title(destinationName)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    );
                    if (polyline != null) polyline.remove();
                    getDirection(mMarker.getPosition(), destinationMarker.getPosition());
                }
            });

            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    isDragging = true;
                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    isDragging = false;
                    if (polyline != null) polyline.remove();
                    getDirection(mMarker.getPosition(), marker.getPosition());
                }
            });

        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    private void getDirection(LatLng origin, LatLng destination) {
        getLatLngGeoCode(origin, 1);
        getLatLngGeoCode(destination, 2);
    }

    private void getLatLngGeoCode(LatLng position, int type) {
        try {
            String queryResGeocode = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                    position.latitude + "," + position.longitude +
                    "&key=" + getString(R.string.google_api_browser_key);
            new GetLatLng(queryResGeocode, type).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (mMarker != null) mMarker.remove();
            mMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .title(originName));
            if (!isSetMarker) {
                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
                mMap.moveCamera(center);
                isSetMarker = true;
            }
            Log.i("CURRENTLOC", location.getLatitude() + ";" + location.getLongitude());
        }
    };

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private OnConnectionFailedListener connectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Toast.makeText(MainActivity.this, connectionResult.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    };

    private OnMapReadyCallback onMapReadyCallBack = new OnMapReadyCallback() {
        @Override
        public void onMapReady(final GoogleMap googleMap) {
            mMap = googleMap;
            mMap.setMyLocationEnabled(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(21.0162568, 105.7845594)));    // default location focused
            mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private class GetLatLng extends AsyncTask<String, Void, String> {
        private String url;
        private int type;

        public GetLatLng(String url, int type) {
            this.url = url;
            this.type = type;
        }

        @Override
        protected String doInBackground(String... ulr) {
            Response response = null;
            Request request = new Request.Builder()
                    .url(this.url)
                    .build();

            try {
                response = okHttpClient.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(String result) {
            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            ReverseGeoCodeGson reverseGeoCodeGson = gson.fromJson(result, ReverseGeoCodeGson.class);
            ReverseGeoCodeGson.Results resultsFirst = reverseGeoCodeGson.getResults().get(0);
            LocationItem location = resultsFirst.getGeometry().getLocation();
            if (this.type == 1) {
                origin = new LatLng(location.getLat(), location.getLng());
                originName = resultsFirst.getFormattedAddress();
                mMarker.setTitle(originName);
            } else {
                destination = new LatLng(location.getLat(), location.getLng());
                destinationName = resultsFirst.getFormattedAddress();
                destinationMarker.setTitle(destinationName);
                startGetDirection();
            }
            Log.i("ResGeocode :", result);
        }

    }

    private void startGetDirection() {
        String queryDirection =
                "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                        origin.latitude + "," + origin.longitude +
//                        "21.017324,105.784054" +
                        "&destination=" +
                        destination.latitude + "," + destination.longitude +
//                        "21.004344,105.8426233" +
                        "&key=" + getString(R.string.google_api_browser_key);
        new GetDirection().execute(queryDirection);
    }

    private class GetDirection extends AsyncTask<String, Void, String> {
        private static final String TAG = "BackgroundTask";

        @Override
        protected String doInBackground(String... ulr) {
            Response response;
            Request request = new Request.Builder()
                    .url(ulr[0])
                    .build();

            try {
                response = okHttpClient.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("Direction :", result);
            PolylineOptions rectLine = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            DirectionPointGson directionPointGson = gson.fromJson(result, DirectionPointGson.class);

            // Not exactly if draw by this way
//            DirectionPointGson.Leg legFirst = directionPointGson.getRoutes().get(0).getLegs().get(0);
//            ArrayList<DirectionPointGson.Step> steps = legFirst.getSteps();
//            int size = steps.size();
//            for (int i = 0; i < size; i++) {
//                LocationItem locationItem = steps.get(i).getStartLocation();
//                rectLine.add(new LatLng(locationItem.getLat(), locationItem.getLng()));
//                if (i == size - 1) {
//                    rectLine.add(new LatLng(steps.get(i).getEndLocation().getLat(), steps.get(i).getEndLocation().getLng()));
//                }
//            }
            DirectionPointGson.Polyline polylineOverview = directionPointGson.getRoutes().get(0).getOverviewPolyline();
            rectLine.addAll(StaticMethod.decodePoly(polylineOverview.getPoints()));
            polyline = mMap.addPolyline(rectLine);
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle(getString(R.string.action_settings));
        alertDialog.setMessage("123");

        alertDialog.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }
}
