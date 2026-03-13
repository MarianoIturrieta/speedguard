package com.mariano.speedguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String CHANNEL_ID = "SpeedGuardChannel";
    public static final String ACTION_LOCATION = "com.mariano.speedguard.LOCATION_UPDATE";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_LAT   = "lat";
    public static final String EXTRA_LON   = "lon";
    public static final String EXTRA_ACC   = "acc";

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location loc : result.getLocations()) {
                    float kmh = loc.getSpeed() * 3.6f;
                    // Broadcast para que MainActivity lo pase al WebView
                    Intent intent = new Intent(ACTION_LOCATION);
                    intent.putExtra(EXTRA_SPEED, kmh);
                    intent.putExtra(EXTRA_LAT,   loc.getLatitude());
                    intent.putExtra(EXTRA_LON,   loc.getLongitude());
                    intent.putExtra(EXTRA_ACC,   loc.getAccuracy());
                    sendBroadcast(intent);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SpeedGuard activo")
                .setContentText("Monitoreando velocidad en segundo plano")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
        requestLocationUpdates();
        return START_STICKY;
    }

    private void requestLocationUpdates() {
        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1500)
                .setMinUpdateIntervalMillis(1000)
                .build();
        try {
            fusedLocationClient.requestLocationUpdates(
                req, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "SpeedGuard", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
