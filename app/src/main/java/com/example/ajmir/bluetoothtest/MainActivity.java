package com.example.ajmir.bluetoothtest;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public static final UUID DEFAULT_UUID = UUID.fromString("0000112f-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_PERMISSION_COARSE_LOCATION = 10;

    public static final int REQUEST_ENABLE_BT = 102;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.mac_text_view)
    TextView mTextView;

    @BindView(R.id.scan_button)
    TextView mScanButton;

    RxBleClient mRxBleClient;

    CompositeDisposable mRxDisposable;

    BluetoothDeviceAdapter mAdapter;

    Disposable connectionStateChangesSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRxDisposable = new CompositeDisposable();
        mRxBleClient = RxBleClient.create(this);

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
        Disposable disposable = mRxBleClient.scanBleDevices(new ScanSettings.Builder().build())
                .subscribeOn(Schedulers.io())
                .map(scanResult -> {
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
        mRxDisposable.add(disposable);
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
        connectionStateChangesSubscription = bleDevice.observeConnectionStateChanges()
                .subscribeOn(Schedulers.io())
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
                        Throwable::printStackTrace
                );

        // Etablish connection with the device
        Disposable disposable = bleDevice.establishConnection(true)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        rxBleConnection -> Log.d("test", "connectToBluetoothDevice: " +rxBleConnection.toString()),
                        Throwable::printStackTrace,
                        () -> Log.d("test", "connectToBluetoothDevice: completed")
                );
        mRxDisposable.add(disposable);
    }
}
