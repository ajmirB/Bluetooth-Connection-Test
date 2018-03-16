package com.example.ajmir.bluetoothtest;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleAdapterStateObservable;
import com.polidea.rxandroidble2.RxBleDevice;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.polidea.rxandroidble2.RxBleAdapterStateObservable.BleAdapterState.STATE_OFF;
import static com.polidea.rxandroidble2.RxBleAdapterStateObservable.BleAdapterState.STATE_ON;
import static com.polidea.rxandroidble2.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF;
import static com.polidea.rxandroidble2.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_PERMISSION_COARSE_LOCATION = 10;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.mac_text_view)
    TextView mTextView;

    @BindView(R.id.scan_button)
    TextView mScanButton;

    CompositeDisposable mRxDisposable;

    BluetoothDeviceAdapter mAdapter;

    Disposable connectionStateChangesSubscription;

    DoppyManager mDoppyManager;

    Disposable mScanDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRxDisposable = new CompositeDisposable();

        // Verify conditions are ok to use bluetooth
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask permission
            Log.d("test", "onCreate: bluetooth permissions asked");
            allowToScan(false);
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_COARSE_LOCATION);
        }

        // Configure recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        mAdapter = new BluetoothDeviceAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mDoppyManager = new DoppyManager(this);


        RxBleAdapterStateObservable rxBleAdapterStateObservable = new RxBleAdapterStateObservable(getApplicationContext());
        Disposable disposable = rxBleAdapterStateObservable
                .subscribeOn(Schedulers.io())
                .subscribe(
                        bleAdapterState -> {
                            if (bleAdapterState == STATE_TURNING_ON) {
                                Log.d("test"," manageBluetooth: turning on");
                            } else if (bleAdapterState ==  STATE_ON) {
                                Log.d("test"," manageBluetooth: state on");
                            } else if (bleAdapterState == STATE_TURNING_OFF) {
                                Log.d("test"," manageBluetooth: turning off");
                            } else if (bleAdapterState == STATE_OFF){
                                Log.d("test"," manageBluetooth: state off");
                            } else {
                                Log.d("test"," manageBluetooth: state unknown");
                            }
                        },
                        Throwable::printStackTrace,
                        () -> {}
                );
        mRxDisposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRxDisposable.clear();
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_COARSE_LOCATION) {
            for (String permission : permissions) {
                if (android.Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
                    allowToScan(true);
                }
            }
        }
    }

    @OnClick(R.id.scan_button)
    public void onScanClicked() {
        mAdapter.clear();
        mScanDisposable = mDoppyManager.scan()
                .subscribeOn(Schedulers.io())
                .map(scanResult -> {
                    Log.d("test", "onScanClicked: " + scanResult.getBleDevice().getName() + " -> " + scanResult.toString());
                    BluetoothDeviceData bluetoothDeviceData = new BluetoothDeviceData();
                    bluetoothDeviceData.bleDevice = scanResult.getBleDevice();
                    bluetoothDeviceData.onClickListener = v -> connectToBluetoothDevice(scanResult.getBleDevice());
                    return bluetoothDeviceData;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        bluetoothDeviceData -> mAdapter.addItem(bluetoothDeviceData),
                        Throwable::printStackTrace,
                        () -> Log.d("test", "scanBleDevices: completed")
                );
        mRxDisposable.add(mScanDisposable);
    }

    private void allowToScan(boolean allowToScan) {
        mScanButton.setEnabled(allowToScan);
        mScanButton.setClickable(allowToScan);
    }

    private void connectToBluetoothDevice(RxBleDevice bleDevice) {
        // Listen on connection change in the device
        if (connectionStateChangesSubscription != null && !connectionStateChangesSubscription.isDisposed()) {
            Log.d("test", "connectToBluetoothDevice: dispose current changes subscription");
            connectionStateChangesSubscription.dispose();
        }

        // Establish connection with the device
        Disposable disposable = mDoppyManager.establishConnection(bleDevice)
                .subscribeOn(Schedulers.io())
                .doOnNext(connection -> {
                    mScanDisposable.dispose();
                    Log.d("test", "connection on thread" + Thread.currentThread().getName() + " - " + Thread.currentThread().getId());
                    mDoppyManager.observeShake()
                            .subscribeOn(Schedulers.newThread())
                            .doOnNext(shake -> Log.d("test", "connectToBluetoothDevice: shake" + shake + " on thread" + Thread.currentThread().getName() + " - " + Thread.currentThread().getId()))
                            .subscribe(o -> {}, Throwable::printStackTrace, () -> {});
                })
                /*
                .flatMapCompletable(connection ->
                        mDoppyManager.setLedEnabled(true)
                                .andThen(mDoppyManager.getBatteryLevel().doOnSuccess(batteryLevel -> Log.d("test", "connectToBluetoothDevice: " + batteryLevel)).toCompletable())
                                .andThen(mDoppyManager.sleep()))
                 */
                .subscribe(o -> {}, Throwable::printStackTrace, () -> {Log.e("test", "completed");});
        mRxDisposable.add(disposable);


        connectionStateChangesSubscription = mDoppyManager.observeConnectionStateChanged()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(subscriber -> Log.d("test", "connectToBluetoothDevice: subscribe"))
                .doOnDispose(() -> Log.d("test", "connectToBluetoothDevice: dispose"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connectionState -> {
                            Log.d("test", "connectToBluetoothDevice: " + connectionState);
                            switch (connectionState) {
                                case CONNECTING:
                                    break;
                                case CONNECTED:
                                    mTextView.setText(bleDevice.getName() + " " + bleDevice.getMacAddress() + " -> connected");
                                    break;
                                case DISCONNECTED:
                                    mTextView.setText(null);
                                    break;
                                case DISCONNECTING:
                                    break;
                            }
                        },
                        Throwable::printStackTrace,
                        () -> Log.d("test", "connectToBluetoothDevice: completed")
                );
    }
}
