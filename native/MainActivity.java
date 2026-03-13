package com.mariano.speedguard;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final int REQ_LOCATION   = 99;
    private static final int REQ_BACKGROUND = 100;
    private BroadcastReceiver locationReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(this::checkLocationPermission, 2000);
        registerLocationReceiver();
    }

    private void registerLocationReceiver() {
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float kmh = intent.getFloatExtra(LocationService.EXTRA_SPEED, 0f);
                double lat = intent.getDoubleExtra(LocationService.EXTRA_LAT, 0);
                double lon = intent.getDoubleExtra(LocationService.EXTRA_LON, 0);
                float acc = intent.getFloatExtra(LocationService.EXTRA_ACC, 0f);

                // Pasar datos al WebView via JavaScript
                String js = String.format(
                    "javascript:window.nativeLocation({speed:%f,lat:%f,lon:%f,acc:%f})",
                    kmh / 3.6f, lat, lon, acc  // volvemos a m/s porque el JS ya multiplica por 3.6
                );
                WebView wv = getBridge().getWebView();
                runOnUiThread(() -> wv.evaluateJavascript(js, null));
            }
        };
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                }, REQ_LOCATION);
        } else {
            askBackgroundAndStartService();
        }
    }

    private void askBackgroundAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                    "Seleccioná 'Permitir todo el tiempo' para funcionar con pantalla apagada",
                    Toast.LENGTH_LONG).show();
                new Handler().postDelayed(() ->
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQ_BACKGROUND), 1500);
                return;
            }
        }
        startLocationService();
    }

    private void startLocationService() {
        Intent intent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            askBackgroundAndStartService();
        } else {
            Toast.makeText(this,
                "Sin permiso de ubicación SpeedGuard no puede funcionar.",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationReceiver != null) unregisterReceiver(locationReceiver);
    }
}
