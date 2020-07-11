package com.gpstracking;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyService extends Service  {

    private static final String CHANNEL_ID = "Channel_Id";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.gpstracking.started_from_notification";

    androidx.fragment.app.DialogFragment dialogFragment;

    final String LOG_TAG = "myLogs";

    static OkHttpClient client;


    private final IBinder mBinder = new LocalBinder();
    private static final int NOTIF_ID = 1223;
    private boolean mChangingConfiguration = false;
    private NotificationManager notificationManager;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler serviceHandler;
    private Location location;
    boolean canGetLocation = false;


    public void requestLocationUpdates() {
        Common.setRequestingLocationUpdates(this, true);
        startService(new Intent (getApplicationContext(),MyService.class));
        try{
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        }catch (SecurityException ex){
            Log.e("ERROR","Could not request location  " + ex);
        }
    }

    public class LocalBinder extends Binder{
        MyService getService(){
            return MyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "onBind");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(!mChangingConfiguration && Common.requestingLocationUpdates(this))
            startForeground(NOTIF_ID, getNotification());
        return true;
    }

    @Override
    public void onCreate(){
//        super.onCreate();
        Log.i(LOG_TAG, "onCreate");
        dialogFragment = new DialogFragment();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread("Handler_Thread");
        handlerThread.start();
        serviceHandler = new Handler(handlerThread.getLooper());
        notificationManager  = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

         client = new OkHttpClient();
    }

    private void getLastLocation() {
        try{
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if(task.isSuccessful() && task.getResult() != null){
                                canGetLocation = true;
                                location = task.getResult();
                            }
                            else{
                                Log.e("ERROR", "Failed to get location");
                            }
                        }
                    });
        }catch(SecurityException ex){
            Log.e("ERROR", "Lost location permission  " + ex);
        }
    }
    public boolean canGetLocation(){
        return this.canGetLocation;
    }

    public void showSettingsAlert(Context context){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("GPS Налаштування");
        alertDialog.setMessage("GPS вимкнуто. Перейти до налаштувань?");
        alertDialog.setPositiveButton("Налаштування", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }
    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void onNewLocation(Location lastLocation) {
        location = lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(location));

        if(serviceIsRunningInForeground(this))
            notificationManager.notify(NOTIF_ID, getNotification());
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, MyService.class);
        String text = Common.getLocationText(location);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0 ,
                new Intent(this, MainActivity.class),0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch_black_24dp, "Launch" , activityPendingIntent)
                .addAction(R.drawable.ic_cancel_black_24dp, "Remove", servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            builder.setChannelId(CHANNEL_ID);
        }

        okhttp3.Request request = new Request.Builder()
                .url("https://webapiforandroid.azurewebsites.net/api/locations")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),text))
                .build();

        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(final Call call, IOException e) {

                        new MainActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("ERROR","ERROR");
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        String res = response.body().string();
                        Log.d("TAG", "response is: "+res);
                    }
                });

        return builder.build();
    }

    private boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE))
            if(getClass().getName().equals(service.service.getClassName()))
                if(service.foreground)
                    return true;
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        Log.i(LOG_TAG, "onStartCommand");

        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        if(startedFromNotification){
            removeLocationUpdates();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    public void removeLocationUpdates() {
        try{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequestingLocationUpdates(this,false);
            stopSelf();
        }catch (SecurityException ex){
            Common.setRequestingLocationUpdates(this,false);
            Log.e("ERROR", "Could not remove updates  " + ex);
        }
    }

    @Override
    public void onDestroy() {
        serviceHandler.removeCallbacks(null);
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
    }
}
