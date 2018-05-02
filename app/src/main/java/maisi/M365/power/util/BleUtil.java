package maisi.M365.power.util;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Util for Bluetooth Low Energy
 */
public class BleUtil {

    private BleUtil() {
        // Util
    }

    /**
     * check if BLE Supported device
     */
    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * get BluetoothManager
     */
    public static BluetoothManager getManager(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }
}
