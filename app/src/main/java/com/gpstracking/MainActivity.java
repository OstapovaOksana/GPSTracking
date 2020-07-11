package com.gpstracking;


import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {

    OkHttpClient client;
    CardView requestLocation;
    CardView removeLocation;
    CardView btnBrowser;
    MyService service = null;
    boolean mBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            MyService.LocalBinder binder = (MyService.LocalBinder) iBinder;
            service = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            mBound = false;
        }
    };

    private Context mContext;

    public Context getContext() {
        return this.mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if(mBound){
            unbindService(serviceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setContext(this);
        client = new OkHttpClient();


        Dexter.withActivity(this)
                .withPermissions(Arrays.asList(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ))
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        requestLocation = (CardView) findViewById(R.id.btnStart);
                        removeLocation = (CardView) findViewById(R.id.btnStop);
                        btnBrowser = (CardView) findViewById(R.id.btnBrowser);

                        requestLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(service.canGetLocation()){
                                    service.requestLocationUpdates();
                                }
                                else {
                                    service.showSettingsAlert(getContext());
                                }
                            }
                        });
                        removeLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                service.removeLocationUpdates();
                            }
                        });
                        btnBrowser.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://webapiforandroid.azurewebsites.net/"));
                                startActivity(browserIntent);
                            }
                        });

                        setButtonState(Common.requestingLocationUpdates(MainActivity.this));
                        bindService(new Intent(MainActivity.this, MyService.class), serviceConnection,Context.BIND_AUTO_CREATE);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals(Common.KEY_REQUESTING_LOCATION_UPDATES)){
            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATES, false));
        }

    }

    private void setButtonState(boolean isRequestEnable) {
        if(isRequestEnable){
            requestLocation.setEnabled(false);
            removeLocation.setEnabled(true);
        }
        else{
            requestLocation.setEnabled(true);
            removeLocation.setEnabled(false);
        }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onListenLocation(SendLocationToActivity event){
//        if(event != null){
//            String text = Common.getLocationText(event.getLocation());
//            okhttp3.Request request = new Request.Builder()
//                    .url("https://webapiforandroid.azurewebsites.net/api/locations")
//                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),text))
//                    .build();
//
//            client.newCall(request)
//                    .enqueue(new Callback() {
//                        @Override
//                        public void onFailure(final Call call, IOException e) {
//
//                            new MainActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Log.e("ERROR","ERROR");
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onResponse(Call call, final Response response) throws IOException {
//                            String res = response.body().string();
//                            Log.d("TAG", "response is: "+res);
//                        }
//                    });
//        }

    }
}
