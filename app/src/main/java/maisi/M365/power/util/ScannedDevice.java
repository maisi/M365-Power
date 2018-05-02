package maisi.M365.power.util;

import android.bluetooth.BluetoothDevice;

/**
 * LeScanned Bluetooth Device
 */
public class ScannedDevice {
    private static final String UNKNOWN = "Unknown";
    /**
     * BluetoothDevice
     */
    private BluetoothDevice mDevice;
    /**
     * RSSI
     */
    private int mRssi;
    /**
     * Display Name
     */
    private String mDisplayName;

    public ScannedDevice(BluetoothDevice device, int rssi) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
            mDisplayName = UNKNOWN;
        }
        mRssi = rssi;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }
}
