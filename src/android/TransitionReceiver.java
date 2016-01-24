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
            PostLocationTask task = new TransitionReceiver.PostLocationTask();           
            task.execute(geofencesJson);                       
        }       
    }
    
    private class PostLocationTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... geofencesJson) {
            try {
                
                Log.println(Log.DEBUG, GeofencePlugin.TAG, "Executing PostLocationTask#doInBackground");

                GeoNotification[] geoNotifications = Gson.get().fromJson(geofencesJson[0], GeoNotification[].class);

                for (int i=0; i < geoNotifications.length; i++){
                    GeoNotification geoNotification = geoNotifications[i];

                    URL url = new URL(geoNotification.url);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                 
                    for (Map.Entry<String, String> entry : geoNotification.headers.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                                        
                    DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
                    writer.writeBytes(geoNotification.toJson());
                    writer.flush();
                    writer.close();
                    
                    //Get Response	
                    InputStream is = conn.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer(); 
                    while((line = rd.readLine()) != null) {
                      response.append(line);
                      response.append('\r');
                    }
                    rd.close();
                    
                    Log.println(Log.DEBUG, GeofencePlugin.TAG,  "Response received: "+ response.toString());                   
                }
            } catch (Throwable e) {
                Log.println(Log.ERROR, GeofencePlugin.TAG, "Exception posting geofence: " + e);    
            }
            
            return "Executed";
        }
        
        @Override
        protected void onPostExecute(String result) {
          
        }
    }
}