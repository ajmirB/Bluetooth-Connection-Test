package com.example.ajmir.bluetoothtest.sensors;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.ajmir.bluetoothtest.DoppyManager;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;


public class DoppyService extends Service {

    // region companion object

    public static final String TAG = DoppyService.class.getName();

    private static final String DOPPY_MAC_ADRESS = "FE:77:48:3B:7F:2D";

    private static final String CONNECTED_DATE_PREFERENCE_KEY = "connected_date";

    private static final String DISCONNECTED_DATE_PREFERENCE_KEY = "diconnected_date";

    // endregion

    private DoppyManager mDoppyManager;

    private CompositeDisposable mRxDisposable;

    private RxSharedPreferences mRxPreferences;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        mRxDisposable = new CompositeDisposable();
        mDoppyManager = new DoppyManager(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRxPreferences = RxSharedPreferences.create(preferences);

        // Connection
        Disposable disposable = mDoppyManager.establishConnection(DOPPY_MAC_ADRESS)
                .subscribe(
                        connection -> {},
                        Throwable::printStackTrace,
                        () -> {}
                );
        mRxDisposable.add(disposable);

        // Connection state
        disposable = mDoppyManager.observeConnectionStateChanged()
                .subscribe(
                        connectionState -> {
                            Date currentDate = Calendar.getInstance().getTime();
                            Log.d(TAG, "Connection state: " + connectionState + " at " + currentDate.toString());

                            // Initialize in function of the state
                            Preference<String> datePreference = null;
                            switch (connectionState) {
                                case CONNECTING:
                                    break;
                                case CONNECTED:
                                    datePreference = mRxPreferences.getString(CONNECTED_DATE_PREFERENCE_KEY);
                                    break;
                                case DISCONNECTED:
                                    datePreference = mRxPreferences.getString(DISCONNECTED_DATE_PREFERENCE_KEY);
                                    stopSelf();
                                    break;
                                case DISCONNECTING:
                                    break;
                            }

                            // Save in shared pref the date
                            if (datePreference != null) {
                                if (datePreference.isSet()) {
                                    datePreference.set(datePreference.get() + " ; " + currentDate.toString());
                                } else {
                                    datePreference.set(currentDate.toString());
                                }
                            }
                        },
                        Throwable::printStackTrace,
                        () -> {}
                );
        mRxDisposable.add(disposable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mRxDisposable.dispose();
    }
}
