package com.app.kenia.smartwater;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Timer;
import java.util.TimerTask;

import at.grabner.circleprogress.CircleProgressView;
import me.itangqi.waveloadingview.WaveLoadingView;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener {
    WaveLoadingView mWaveLoadingView;
    CircleProgressView circleProgressViewTemp;
    CircleProgressView circleProgressViewTurbidez;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
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

        timer.schedule(task, 0, 10000); //actualiza datos cada 10 segundos
    }

    /**
     * Called when a swipe gesture triggers a refresh.
     */

    public void actualizarDatos(){

        int consumoAgua = 5;
        float temperatura = (float) 17.5;
        float turbidez = (float) 25.8;
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
