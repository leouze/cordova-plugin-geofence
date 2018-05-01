package com.cowbell.cordova.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.AsyncTask;
import javax.net.ssl.*;
import java.net.*;
import java.util.*;
import java.io.*;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import org.json.JSONObject;

public class TransitionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.setLogger(new Logger(GeofencePlugin.TAG, context, false));
        Logger logger = Logger.getLogger();

        String error = intent.getStringExtra("error");

        if (error != null) {
            //handle error
            logger.log(Log.DEBUG, error);
        } else {
            String geofencesJson = intent.getStringExtra("transitionData");
            PostLocationTask task = new TransitionReceiver.PostLocationTask(context);
            task.geofencesJson = geofencesJson;
            task.execute();
        }
    }

    public class PostLocationTask extends AsyncTask<Void, Void, Void> implements LocationListener {
        private Context ContextAsync;
        public String geofencesJson;

        private String providerAsync;
        private LocationManager locationManagerAsync;
        double latAsync = 0.0;
        double lonAsync = 0.0;

        Location location;

        public MyAsyncTask(Context context) {
            this.ContextAsync = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {

                Log.println(Log.DEBUG, GeofencePlugin.TAG, "Executing PostLocationTask#doInBackground");
                this.getLocation();

                GeoNotification[] geoNotifications = Gson.get().fromJson(geofencesJson, GeoNotification[].class);

                for (int i = 0; i < geoNotifications.length; i++) {
                    GeoNotification geoNotification = geoNotifications[i];

                    URL url = new URL(geoNotification.url);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");

                    JSONObject params = new JSONObject(geoNotification.toJson());
                    params.put("current_latitude", latAsync);
                    params.put("current_longitude", lonAsync);
                    params.put("current_time", System.currentTimeMillis() / 1000);

                    String data = params.toString();
                    Log.v(GeofencePlugin.TAG, data);

                    DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
                    writer.writeBytes(data);
                    writer.flush();
                    writer.close();

                    //Get Response
                    InputStream is = conn.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();

                    Log.println(Log.DEBUG, GeofencePlugin.TAG, "Response received: " + response.toString());
                }
            } catch (Throwable e) {
                Log.println(Log.ERROR, GeofencePlugin.TAG, "Exception posting geofence: " + e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            onLocationChanged(location);
            Log.v("latAsync_lonAsync", latAsync + "_" + lonAsync);
        }


        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            try {
                locationManagerAsync.requestLocationUpdates(providerAsync, 0, 0, this);
            } catch(SecurityException exception) {
                Log.v("Security exception: {}", exception.getMessage());
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

        protected void getLocation() {
            // TODO Auto-generated method stub
            locationManagerAsync = (LocationManager) ContextAsync.getSystemService(ContextAsync.LOCATION_SERVICE);

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setCostAllowed(false);
            criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
            providerAsync = locationManagerAsync.getBestProvider(criteria, false);


            if (locationManagerAsync.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                providerAsync = LocationManager.GPS_PROVIDER;
            } else if (locationManagerAsync.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                providerAsync = LocationManager.NETWORK_PROVIDER;
            } else if (locationManagerAsync.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                providerAsync = LocationManager.PASSIVE_PROVIDER;
            }

            try {
                location = locationManagerAsync.getLastKnownLocation(providerAsync);
                // Initialize the location fields
                if (location != null) {
                    latAsync = location.getLatitude();
                    lonAsync = location.getLongitude();
                    Log.v(GeofencePlugin.TAG, latAsync + "_" + lonAsync);
                }
            } catch(SecurityException exception) {
                Log.v("Security exception: {}", exception.getMessage());
            }

        }
    }
}