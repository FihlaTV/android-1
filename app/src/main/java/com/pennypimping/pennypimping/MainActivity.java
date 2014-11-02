package com.pennypimping.pennypimping;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;


public class MainActivity extends Activity implements LocationListener {

    private LocationManager locationManager;
    private String provider;

    public double latitude, longitude;


    private LatLng CUR_LOCATION;
    private GoogleMap map;

    JSONObject mapJSON;

    private ArrayList<Integer> intDirections;
    private ArrayList<LatLng> stepByStep;
    int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

// Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        // Define the criteria how to select the location provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);



        // Initialize the location fields
        if (location != null) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
        } else {
        }

        CUR_LOCATION = new LatLng(location.getLatitude(), location.getLongitude());


        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
        Marker aMarker = map.addMarker(new MarkerOptions().position(CUR_LOCATION)
                .title("Your Location"));


        // Move the camera instantly to hamburg with a zoom of 15.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(CUR_LOCATION, 30));

        // Zoom in, animating the camera.
        map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                // TODO Auto-generated method stub
                map.addMarker(new MarkerOptions().position(point));

                getDirectionObject(CUR_LOCATION.latitude, CUR_LOCATION.longitude, point.latitude, point.longitude);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void getDirectionObject(double beginLat, double beginLon, double endLat, double endLon)
    {
        String query = "http://dev.virtualearth.net/REST/V1/Routes/Walking?&key=AouOnxQCcOABTsNKp834rSOrFt_tZdpHVlfxAQTxBGMcmX8s62XMiJjG09Qaau_J";
        query += "&wp.0=" + beginLat + "," + beginLon;
        query += "&wp.1=" + endLat + "," + endLon;
        //mapJSON = getJSONData();
        getJSONData(query);


    }

    public void parseJSONObject()
    {
        try {
            // Parse Directions
            intDirections = new ArrayList<Integer>();
            JSONArray compassDirs = mapJSON.getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources").getJSONObject(0).getJSONArray("routeLegs").getJSONObject(0).getJSONArray("itineraryItems");
            for (int i = 0; i < compassDirs.length(); i++)
            {
                intDirections.add(compassDirs.getJSONObject(i).getJSONArray("details").getJSONObject(0).getInt("compassDegrees"));
            }


            // Parse LatLng
            stepByStep = new ArrayList<LatLng>();
            JSONArray coordinates = mapJSON.getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources").getJSONObject(0).getJSONArray("routeLegs").getJSONObject(0).getJSONArray("itineraryItems").getJSONObject(currentStep).getJSONObject("maneuverPoint").getJSONArray("coordinates");

            for (int i = 0; i < coordinates.length(); i++) {
                stepByStep.add(new LatLng(coordinates.getDouble(0), coordinates.getDouble(1)));
            }
        } catch (JSONException e) {
            return;
        }
        loopForever();
    }

    public void loopForever()
    {
        Thread thread = new Thread() {
            public void run() {
                URL left = null;
                URL forward = null;
                URL right = null;
                try
                {
                    left = new URL("http://172.26.2.145:1337/1");
                    right = new URL("http://172.26.2.145:1337/2");
                    forward = new URL("http://172.26.2.145:1337/0");
                } catch (MalformedURLException e)
                {
                }

                while (true)

                {
                    // Break for end of destination
                    if (currentStep == stepByStep.size())
                    {
                        break;
                    }

                    Location location = locationManager.getLastKnownLocation(provider);
                    LatLng step = stepByStep.get(currentStep);
                    if (Math.sqrt(Math.abs(location.getLatitude() - step.latitude)) +
                            Math.sqrt(Math.abs(location.getLongitude() - step.longitude)) < 0.07) { // if it's close enough
                        if (Math.sqrt(Math.abs(location.getLatitude() - step.latitude)) +
                                Math.sqrt(Math.abs(location.getLongitude() - step.longitude)) < 0.001)
                            currentStep++;

                        //code left
                        if (intDirections.get(currentStep + 1) - intDirections.get(currentStep) > 20) {
                            System.out.println("l");
                            sendRequest(left);

                        }
                        //code right
                        else if (intDirections.get(currentStep + 1) - intDirections.get(currentStep) < 20) {
                            System.out.println("r");
                            sendRequest(right);
                        }
                        //code forward
                        if (Math.abs(intDirections.get(currentStep + 1) - intDirections.get(currentStep)) <= 20) {
                            System.out.println("f");
                            sendRequest(forward);
                        }
                    } else {
                        System.out.println("f");
                        sendRequest(forward);
                    }
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {}
                }
            }
        };
        thread.start();
    }

    public void sendRequest(URL url) {
        try {
            URLConnection urlc = url.openConnection();
            urlc.getInputStream();
        } catch (Exception e) {
        }
    }


    ///////////////////////////////////////////////////////////////////
    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    /////////////////////////////////////////////////
    public String GET(String url)
    {
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            result = "error";
        }
        return result;
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();

            try {
                mapJSON = new JSONObject(result);
                parseJSONObject();
            } catch (JSONException e) {

            }
        }
    }

    public void getJSONData(String query)
    {
        new HttpAsyncTask().execute(query);

    }

}
