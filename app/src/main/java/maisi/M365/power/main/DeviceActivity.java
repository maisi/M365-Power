package maisi.M365.power.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import maisi.M365.power.util.HexString;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class DeviceActivity extends Activity {


    public static final String CHAR_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    //private final static String TAG = DeviceActivity.class.getSimpleName();
    public static String CHAR_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"; //WRITE
    public static String CHAR_READ = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"; //READ
    public static String GET_NAME_SERVICE = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GET_NAME_CHAR = "00002a00-0000-1000-8000-00805f9b34fb";

    RxBleClient rxBleClient;
    private String mDeviceName;
    private String mDeviceAddress;
    private RxBleDevice bleDevice;
    private Observable<RxBleConnection> connectionObservable;
    private Disposable connectionDisposable;
    private RxBleConnection connection;
    private TextView voltageMeter;
    private TextView ampMeter;
    private TextView powerMeter;
    private TextView minPowerView;
    private TextView maxPowerView;
    private TextView recoveredPower;
    private TextView spentPower;
    private TextView time;
    private TextView life;

    private double currentAmp = -1d;
    private double currentVolt = -1d;

    private int maxPower = 0;
    private int minPower = -0;

    private double recoverd = 0.0; //watt hours
    private double spent = 0.0; //watt hours

    private long lastTime;


    private Handler handler = new Handler();
    private Handler handler1 = new Handler();


    private Runnable runnableMeta = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(updateAmpsRunnable, 100);
                handler.postDelayed(updateVoltageBatterylife, 100);
                handler.postDelayed(updateVoltageRunnable, 1000);
                handler1.postDelayed(this, 10000);
            }
        }
    };

    private Runnable updateAmpsRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                setupNotificationAndSend();
                updateAmps();
                handler.postDelayed(updateVoltageRunnable, 100);
                handler.postDelayed(this, 200);
            }
        }
    };

    private Runnable updateVoltageRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                setupNotificationAndSend();
                updateVoltage();
                //handler.postDelayed(this, 200);
            }
        }
    };

    private Runnable updateVoltageBatterylife = new Runnable() {
        @Override
        public void run() {
            setupNotificationAndSend();
            updateBatteryLife();
            handler.postDelayed(this, 5150);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device);

        voltageMeter = findViewById(R.id.voltageMeter);
        ampMeter = findViewById(R.id.ampMeter);
        powerMeter = findViewById(R.id.powerMeter);
        minPowerView = findViewById(R.id.minPowerView);
        ;
        maxPowerView = findViewById(R.id.maxPowerView);
        ;
        recoveredPower = findViewById(R.id.recoveredPower);
        ;
        spentPower = findViewById(R.id.spentPower);
        time = findViewById(R.id.time);
        life = findViewById(R.id.life);
        ;

        final Intent intent = getIntent();
        //mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        //mDeviceAddress = intent.getStringExtra(Constants.EXTRAS_DEVICE_ADDRESS);

        rxBleClient = RxBleClient.create(this);
        bleDevice = rxBleClient.getBleDevice(mDeviceAddress);
        connectionObservable = prepareConnectionObservable();
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false);
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }


    public void readName(View view) {
        if (!isConnected()) {
            doConnect();
        }
        //Start
        handler1.post(runnableMeta);
    }

    private void setupNotificationAndSend() {

        connection.setupNotification(UUID.fromString(CHAR_READ))
                .doOnNext(notificationObservable -> {
                    //Log.d(TAG, "notification has been setup");
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .timeout(200, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.empty())
                .subscribe(
                        bytes -> {
                            //Log.d(TAG, "response: " + HexString.bytesToHex(bytes));
                            updateUI(bytes);
                        }

                );
    }

    private void writeField(String command) {
        //Log.d(TAG,"sending: "+command);
        if(isConnected()) {
            connection.writeCharacteristic(UUID.fromString(CHAR_WRITE), HexString.hexToBytes(command)).subscribe();
        }

    }

    private void updateUI(byte[] bytes) {
        String[] test = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte[] temp = new byte[1];
            temp[0] = bytes[i];
            test[i] = HexString.bytesToHex(temp);
        }

        if (test[5].equals("33")) {
            String temp = test[7] + test[6];
            int amps = (short) Integer.parseInt(temp, 16);
            double c = amps;
            c = c / 100;

            currentAmp = c;
            double finalC = c;
            Thread t = new Thread() {
                public void run() {
                    runOnUiThread(() -> ampMeter.setText(finalC + " A"));
                }
            };
            t.start();

            updateVoltage();

        } else if (test[5].equals("32")) {
            String temp = test[7] + test[6];
            int lifet = (short) Integer.parseInt(temp, 16);
            Thread t = new Thread() {
                public void run() {
                    runOnUiThread(() -> life.setText(lifet + " %"));
                }
            };
            t.start();

        } else if (test[5].equals("34")) {
            String temp = test[7] + test[6];
            int voltage = (short) Integer.parseInt(temp, 16);
            double v = voltage;
            v = v / 100;

            currentVolt = v;
            double finalV = v;
            Thread t = new Thread() {
                public void run() {
                    runOnUiThread(() -> {
                        Long tsLong = System.currentTimeMillis();
                        double diff = tsLong - lastTime;
                        diff /= 1000;
                        if (diff > 2) {
                            diff = 0.5;
                        }
                        time.setText(tsLong - lastTime + " ms");
                        lastTime = tsLong;

                        voltageMeter.setText(finalV + " V");
                        int p = (int) (currentAmp * currentVolt);
                        powerMeter.setText(p + "W");

                        if (p < 0 && p < minPower) {
                            minPower = p;
                        }
                        if (p > 0 && p > maxPower) {
                            maxPower = p;
                        }
                        if (p < 0) {
                            Double d = currentAmp * currentVolt;
                            recoverd += (d / 60 / 60 * diff);
                        } else if (p > 0) {
                            Double d = currentAmp * currentVolt;
                            spent += (d / 60 / 60 * diff);
                        }
                        DecimalFormat df = new DecimalFormat("#.####");
                        df.setRoundingMode(RoundingMode.CEILING);

                        minPowerView.setText("min Power: " + minPower + "W");
                        maxPowerView.setText("max Power: " + maxPower + "W");
                        spentPower.setText("spent: " + df.format(spent) + " Wh");
                        recoveredPower.setText("recoverd: " + df.format(recoverd) + " Wh");

                        //Log.d(TAG,"spent: "+spent+" recovered: "+recoverd);

                    });
                }
            };
            t.start();
            updateBatteryLife();
        }
    }

    private void updateVoltage() {
        String ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x34)
                .setPayload(0x02)
                .build();
        //setupNotificationAndSend(ctrlVersion);
        writeField(ctrlVersion);
    }

    private void updateAmps() {
        String ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x33)
                .setPayload(0x02)
                .build();
        //setupNotificationAndSend(ctrlVersion);
        writeField(ctrlVersion);
    }

    private void updateBatteryLife() {
        String ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x32)
                .setPayload(0x02)
                .build();
        //setupNotificationAndSend(ctrlVersion);
        writeField(ctrlVersion);
    }

    private void onReadFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Log.d(TAG,"READ FAIL:"+throwable.getMessage());
    }

    public void connect(View view) {
        doConnect();
    }

    private void doConnect() {
        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionDisposable = bleDevice.establishConnection(false)
                    //.compose(bindUntilEvent(PAUSE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(this::dispose)
                    .doOnError(throwable -> {
                        System.out.println("ERROR,disconnect");
                        Toast.makeText(this, "Scooter disconnected", Toast.LENGTH_LONG).show();
                        handler.removeCallbacksAndMessages(null);
                        handler1.removeCallbacksAndMessages(null);
                        dispose();
                        time.setText("disconnectd");
                    })
                    .subscribe(this::onConnectionReceived, this::onConnectionFailure);
        }
    }

    private void triggerDisconnect() {

        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

    private void dispose() {
        connectionDisposable = null;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Log.d(TAG,"connection fail: "+throwable.getMessage());
    }

    private void onConnectionReceived(RxBleConnection connection) {
        this.connection = connection;
        this.time.setText("connected");
    }


    public void stopHandler(View view) {
        handler.removeCallbacksAndMessages(null);
        handler1.removeCallbacksAndMessages(null);
    }

    public void reset(View view) {
        maxPower = 0;
        minPower = -0;

        recoverd = 0.0; //watt hours
        spent = 0.0; //watt hours
    }
}
