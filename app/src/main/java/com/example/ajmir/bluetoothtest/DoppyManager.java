package com.example.ajmir.bluetoothtest;

import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

import static com.polidea.rxandroidble2.scan.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static com.polidea.rxandroidble2.scan.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;

public class DoppyManager {

    // Key values

    private static final String BATTERY_CHARACTERISTIC_UUID     = "00002A19-0000-1000-8000-00805F9B34FB";

    private static final String DOPPY_SERVICE_UUID              = "0000DC1A-0000-1000-8000-00805F9B34FB";

    private static final String DOPPY_SHAKE_CHARACTERISTIC_UUID = "00000003-793C-07BC-11E1-A3F04B301ADC";

    private static final String DOPPY_SLEEP_CHARACTERISTIC_UUID = "00000023-793C-07BC-11E1-A3F04B301ADC";

    private static final String DOPPY_LED_CHARACTERISTIC_UUID   = "00000008-793C-07BC-11E1-A3F04B301ADC";

    // Local values

    private RxBleClient mBleClient;

    private RxBleDevice mDevice;

    private RxBleConnection mConnection;

    public DoppyManager(Context context) {
        mBleClient = RxBleClient.create(context);
    }

    /**
     * Scan all doppy around the user to search the specific one with the mac adress
     * and configure the manager with it
     * @return an observable emitting the doppy with the mac address
     */
    public Observable<ScanResult> scan() {
        return mBleClient.scanBleDevices(
                        new ScanSettings.Builder().build(),
                        new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(DOPPY_SERVICE_UUID))).build()
                );
    }

    /**
     * When the manager is configured to a doppy, it will try to connect to it
     * @return an observable emitting the ble connection to the doppy
     */
    public Observable<RxBleConnection> establishConnection() {
        return mDevice.establishConnection(true)
                .doOnNext(connection -> {
                    mConnection = connection;
                    Log.d("test", "establishConnection: " + mConnection);
                })
                .doOnComplete(() -> Log.d("test", "establishConnection: completed"));
    }


    /**
     * When the manager is NOT configured to a doppy, try to connect to a doppy by the device input.
     * It will also configure the manager to the doppy with the given mac address
     * @param device the doppy targeted
     * @return an observable emitting the ble connection to the doppy
     */
    public Observable<RxBleConnection> establishConnection(RxBleDevice device) {
        mDevice = device;
        return establishConnection();
    }

    /**
     * When the manager is NOT configured to a doppy, try to connect to a doppy by its mac address.
     * It will also configure the manager to the doppy with the given mac address
     * @param macAddress attached to the doppy targeted
     * @return an observable emitting the ble connection to the doppy
     */
    public Observable<RxBleConnection> establishConnection(String macAddress) {
        return establishConnection(mBleClient.getBleDevice(macAddress));
    }

    /**
     * Observe on connection state change in the doppy attached to the manager
     * @return an observable emitting the connection state changed
     */
    public Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanged() {
        return mDevice.observeConnectionStateChanges();
    }

    // region Characteristics

    /**
     * Observe on shake in the doppy attached to the manager
     * @return an observable emittting the different shake received from the doppy
     */
    public Observable<Integer> observeShake() {
        return mConnection.setupNotification(UUID.fromString(DOPPY_SHAKE_CHARACTERISTIC_UUID), NotificationSetupMode.DEFAULT)
                .flatMap(obs -> obs)
                .map(ConversionUtils::byteArrayToInteger);
    }

    /**
     * Enable or not the led in the doppy
     * @param enabled new state of the led
     * @return an observable which complete immediately when the state has changed
     */
    public Completable setLedEnabled(boolean enabled) {
        byte[] enabledValue = ConversionUtils.integerToByteArray(enabled ? 1 : 0);
        return mConnection.writeCharacteristic(UUID.fromString(DOPPY_LED_CHARACTERISTIC_UUID), enabledValue)
                .toCompletable();
    }

    /**
     * Get the battery level in the doppy
     * @return the battery level in the doppy
     */
    public Single<Integer> getBatteryLevel() {
        return mConnection.readCharacteristic(UUID.fromString(BATTERY_CHARACTERISTIC_UUID))
                .map(bytes -> (int) bytes[0]);
    }

    /**
     * Put the doppy in a sleep mode
     * @return an observable which complete immediately when the doppy is sleeping
     */
    public Completable sleep() {
        byte[] sleepValue = ConversionUtils.integerToByteArray(1);
        return mConnection.writeCharacteristic(UUID.fromString(DOPPY_SLEEP_CHARACTERISTIC_UUID), sleepValue)
                .toCompletable();
    }

    // endregion
}
