package com.example.ajmir.bluetoothtest;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.example.ajmir.bluetoothtest.sensors.AccelerometerService;
import com.example.ajmir.bluetoothtest.sensors.DoppyService;
import com.example.ajmir.bluetoothtest.sensors.LocationService;
import com.facebook.stetho.Stetho;

import timber.log.Timber;


public class BaseApplication extends Application implements ServiceConnection {

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        Timber.plant(new Timber.DebugTree());

        //Intent service = new Intent(this, LocationService.class);
        //bindService(service, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Timber.i("onServiceConnected: %s %s", name, service.getClass().getName());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Timber.i("onServiceDisconnected: %s", name);
    }
}
