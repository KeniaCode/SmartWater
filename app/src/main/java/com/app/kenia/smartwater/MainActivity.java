package com.app.kenia.smartwater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import at.grabner.circleprogress.CircleProgressView;
import me.itangqi.waveloadingview.WaveLoadingView;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener,
        com.google.android.gms.location.LocationListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private static final int PETICION_PERMISO_LOCALIZACION = 101;
    private static final int PETICION_CONFIG_UBICACION = 201;
    public static double latitude = 0.0;
    public static double longitude = 0.0;
    public static double marker_latitude = 0.0;
    public static double marker_longitude = 0.0;
    private final String USER_AGENT = "Mozilla/5.0";
    final private int REQUEST_CODE_ASK_LOCATION = 1;
    final private String ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    WaveLoadingView mWaveLoadingView;
    CircleProgressView circleProgressViewTemp;
    CircleProgressView circleProgressViewTurbidez;
    int consumoAgua = 5;
    float temperatura = (float) 17.5;
    float turbidez = (float) 25.8;
    SupportMapFragment sMapFragment;
    private GoogleApiClient apiClient;
    private String LOGTAG = "android-localizacion";
    private LocationRequest locRequest;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sMapFragment = SupportMapFragment.newInstance();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);

        mWaveLoadingView = (WaveLoadingView) findViewById(R.id.waveLoadingViewAgua);
        mWaveLoadingView.setShapeType(WaveLoadingView.ShapeType.CIRCLE);
        mWaveLoadingView.setTopTitle("10 ml");
        mWaveLoadingView.setProgressValue(1);
       /* mWaveLoadingView.setCenterTitleColor(Color.GRAY);
        mWaveLoadingView.setBottomTitleSize(18);
        mWaveLoadingView.setBorderWidth(10);
        mWaveLoadingView.setAmplitudeRatio(60);
        mWaveLoadingView.setWaveColor(Color.GRAY);
        mWaveLoadingView.setBorderColor(Color.GRAY);
        mWaveLoadingView.setTopTitleStrokeColor(Color.BLUE);
        mWaveLoadingView.setTopTitleStrokeWidth(3);
        mWaveLoadingView.setAnimDuration(3000);
        mWaveLoadingView.pauseAnimation();
        mWaveLoadingView.resumeAnimation();
        mWaveLoadingView.cancelAnimation();
        mWaveLoadingView.startAnimation();*/

         circleProgressViewTemp = (CircleProgressView) findViewById(R.id.circleViewTemp);
         circleProgressViewTurbidez = (CircleProgressView) findViewById(R.id.circleViewTurbidez);

        circleProgressViewTemp.setValue((float) 17.5);
        circleProgressViewTurbidez.setValue((float) 30.5);

        //Construcción cliente API Google
        apiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();


        final Handler handler = new Handler();
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            //Ejecuta tu AsyncTask!
                           actualizarDatos();
                        } catch (Exception e) {
                            Log.e("error", e.getMessage());
                        }
                    }
                });
            }
        };

        timer.schedule(task, 0, 100000); //actualiza datos cada 10 segundos
    }

    /**
     * Called when a swipe gesture triggers a refresh.
     */
    public void actualizarDatos(){

        new sendPostConsumo().execute("", "");
        new sendPostTemperatura().execute("", "");
        new sendPostTurbidez().execute("", "");
        new sendPostUbicacion().execute("", "");

        mWaveLoadingView.setTopTitle(Integer.toString(consumoAgua)+" ml");
        mWaveLoadingView.setProgressValue(consumoAgua);
        circleProgressViewTemp.setValue(temperatura);
        circleProgressViewTurbidez.setValue(turbidez);
        Toast.makeText(getApplication(), "Datos Actualizados", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRefresh() {
        actualizarDatos();
    }

    public void getPermission(String PERMISSION, int REQUEST_CODE) {
        if (ContextCompat.checkSelfPermission(this, PERMISSION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PETICION_PERMISO_LOCALIZACION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permiso concedido
                @SuppressWarnings("MissingPermission")
                Location lastLocation =
                        LocationServices.FusedLocationApi.getLastLocation(apiClient);
                updateUI(lastLocation);
            } else {
                //Permiso denegado:
                //Deberíamos deshabilitar toda la funcionalidad relativa a la localización.
                Log.e("", "Permiso denegado");
            }
        }

        switch (requestCode) {
            case REQUEST_CODE_ASK_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    getPermission(ACCESS_FINE_LOCATION, 1);
                }
                return;
            }
        }
    }

    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle result of pick image chooser

        switch (requestCode) {
            case PETICION_CONFIG_UBICACION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("", "El usuario no ha realizado los cambios de configuración necesarios");
                        break;
                }
                break;
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(0, 200, 0, 0);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // mMap.setOnMyLocationButtonClickListener();
        enableLocationUpdates();
    }

    private void enableLocationUpdates() {
        locRequest = new LocationRequest();
        locRequest.setInterval(20000);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest locSettingsRequest =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locRequest)
                        .build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        apiClient, locSettingsRequest);

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(LOGTAG, "Configuración correcta");
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            Log.i(LOGTAG, "Se requiere actuación del usuario");
                            status.startResolutionForResult(MainActivity.this, PETICION_CONFIG_UBICACION);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(LOGTAG, "Error al intentar solucionar configuración de ubicación");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(LOGTAG, "No se puede cumplir la configuración de ubicación necesaria");
                        break;
                }
            }
        });
    }

    private void startLocationUpdates() {


        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Ojo: estamos suponiendo que ya tenemos concedido el permiso.
            //Sería recomendable implementar la posible petición en caso de no tenerlo.
            Log.i("", "Inicio de recepción de ubicaciones");
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locRequest, MainActivity.this);
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            updateUI(lastLocation);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16));
            moverMarcador(latitude, longitude);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker arg0) {
                marker_longitude = arg0.getPosition().longitude;
                marker_latitude = arg0.getPosition().latitude;
                return false;
            }

        });
    }

    public void moverMarcador(double lat, double lon) {
        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> list;
        try {
            list = geocoder.getFromLocation(lat, lon, 1);
            // Handle case where no address was found.
            if (list.size() > 0) {
                Address address = list.get(0);
                mMap.clear();
                MarkerOptions options = new MarkerOptions()
                        .draggable(true)
                        //  .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1))
                        .title(address.getAddressLine(0))
                        .anchor(0.5f, 0.5f)
                        .position(new LatLng(lat, lon));
                mMap.addMarker(options);
                marker_latitude = lat;
                marker_longitude = lon;
                //  mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 19));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
            }

        } catch (IOException e) {
            e.printStackTrace();
            mMap.clear();
            MarkerOptions options = new MarkerOptions()
                    .draggable(true)
                    //   .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1))
                    .position(new LatLng(lat, lon));
            mMap.addMarker(options);
            marker_latitude = lat;
            marker_longitude = lon;
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 19));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
        }

        moverMarcador2(lat + 0.001, lon + 0.001);

    }

    public void moverMarcador2(double lat, double lon) {
        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> list;
        try {
            list = geocoder.getFromLocation(lat, lon, 1);
            // Handle case where no address was found.
            if (list.size() > 0) {
                Address address = list.get(0);
                MarkerOptions options = new MarkerOptions()
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.water))
                        .title(address.getAddressLine(0))
                        .anchor(0.5f, 0.5f)
                        .position(new LatLng(lat, lon));
                mMap.addMarker(options);
                marker_latitude = lat;
                marker_longitude = lon;
                //  mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 19));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
            }

        } catch (IOException e) {
            e.printStackTrace();
            MarkerOptions options = new MarkerOptions()
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.water))
                    .position(new LatLng(lat, lon));
            mMap.addMarker(options);
            marker_latitude = lat;
            marker_longitude = lon;
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 19));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
        }

    }

    private void updateUI(Location loc) {
        if (loc != null) {
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            //    moverMarcador(latitude, longitude);

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("", "Recibida nueva ubicación!");
        //Mostramos la nueva ubicación recibida
        updateUI(location);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        //Se ha producido un error que no se puede resolver automáticamente
        //y la conexión con los Google Play Services no se ha establecido.
        Log.e("", "Error grave al conectar con Google Play Services");
        Toast.makeText(this, "Se ha interrumpido la conexión", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Conectado correctamente a Google Play Services
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PETICION_PERMISO_LOCALIZACION);
        } else {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            updateUI(lastLocation);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Se ha interrumpido la conexión con Google Play Services
        Log.e("", "Se ha interrumpido la conexión con Google Play Services");
        Toast.makeText(this, "Se ha interrumpido la conexión", Toast.LENGTH_LONG).show();
    }

    private class sendPostConsumo extends AsyncTask<String, Integer, String> {
        protected void onPostExecute(String result) {
            //	pb.setVisibility(View.GONE);
            super.onPostExecute(result);
        }

        protected void onProgressUpdate(Integer... progress) {
            //		pb.setProgress(progress[0]);
        }

        @Override
        protected String doInBackground(String... strings) {
            String urlParameters = "usuario=1";

            //URL url;
            HttpURLConnection connection = null;
            try {
                //Create connection
                String url = "http://35.202.207.233/arqui2/get_consumo.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.out.println(response.toString());
                JSONObject JSON = null;
                JSON = new JSONObject(response.toString());
                Boolean result = JSON.getBoolean("result");
                String consumo = JSON.getString("consumo");

                if (result) {
                    consumoAgua = Integer.valueOf(consumo);
                } else {

                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }
    }

    private class sendPostTurbidez extends AsyncTask<String, Integer, String> {
        protected void onPostExecute(String result) {
            //	pb.setVisibility(View.GONE);
            super.onPostExecute(result);
        }

        protected void onProgressUpdate(Integer... progress) {
            //		pb.setProgress(progress[0]);
        }

        @Override
        protected String doInBackground(String... strings) {
            String urlParameters = "usuario=1";

            //URL url;
            HttpURLConnection connection = null;
            try {
                //Create connection
                String url = "http://35.202.207.233/arqui2/get_turbidez.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.out.println(response.toString());
                JSONObject JSON = null;
                JSON = new JSONObject(response.toString());
                Boolean result = JSON.getBoolean("result");
                String turb = JSON.getString("trubidez");

                if (result) {
                    turbidez = Float.valueOf(turb);
                } else {

                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }
    }

    private class sendPostTemperatura extends AsyncTask<String, Integer, String> {
        protected void onPostExecute(String result) {
            //	pb.setVisibility(View.GONE);
            super.onPostExecute(result);
        }

        protected void onProgressUpdate(Integer... progress) {
            //		pb.setProgress(progress[0]);
        }

        @Override
        protected String doInBackground(String... strings) {
            String urlParameters = "usuario=1";

            //URL url;
            HttpURLConnection connection = null;
            try {
                //Create connection
                String url = "http://35.202.207.233/arqui2/get_temperatura.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.out.println(response.toString());
                JSONObject JSON = null;
                JSON = new JSONObject(response.toString());
                Boolean result = JSON.getBoolean("result");
                String temp = JSON.getString("temp");

                if (result) {
                    temperatura = Integer.valueOf(temp);
                } else {

                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }
    }

    private class sendPostUbicacion extends AsyncTask<String, Integer, String> {
        protected void onPostExecute(String result) {
            //	pb.setVisibility(View.GONE);
            super.onPostExecute(result);
        }

        protected void onProgressUpdate(Integer... progress) {
            //		pb.setProgress(progress[0]);
        }

        @Override
        protected String doInBackground(String... strings) {
            String urlParameters = "usuario=1";

            //URL url;
            HttpURLConnection connection = null;
            try {
                //Create connection
                String url = "http://35.202.207.233/arqui2/get_ubicacion.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.out.println(response.toString());
                JSONObject JSON = null;
                JSON = new JSONObject(response.toString());
                Boolean result = JSON.getBoolean("result");
                String lat = JSON.getString("lat");
                String lon = JSON.getString("lat");

                if (result) {
                    latitude = Integer.valueOf(lat);
                    longitude = Integer.valueOf(lon);
                } else {

                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }
    }



}
