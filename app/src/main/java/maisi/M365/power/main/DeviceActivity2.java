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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;


import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import maisi.M365.power.main.Requests.AmpereRequest;
import maisi.M365.power.main.Requests.BatteryLifeRequest;
import maisi.M365.power.main.Requests.VoltageRequest;
import maisi.M365.power.util.HexString;

public class DeviceActivity2 extends Activity{

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final static String TAG = DeviceActivity2.class.getSimpleName();

    private RxBleClient rxBleClient;
    private Observable<RxBleConnection> connectionObservable;
    private Disposable connectionDisposable;
    private RxBleConnection connection;

    private String mDeviceName;
    private String mDeviceAddress;
    private RxBleDevice bleDevice;

    private SpecialTextView voltageMeter;
    private SpecialTextView ampMeter;
    private SpecialTextView life;
    private TextView powerMeter;
    private TextView minPowerView;
    private TextView maxPowerView;
    private TextView recoveredPower;
    private TextView spentPower;
    private TextView time;


    //DelayQueue<IRequest> requestQueue = new DelayQueue();
    private Queue<IRequest> requestQueue= new LinkedBlockingQueue();
    private List<IRequest> requestTypes = new ArrayList<>();
    private List<SpecialTextView> textViews = new ArrayList<>();

    private byte[] lastResponse;

    private Handler handler = new Handler();
    private Handler handler1 = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device);

        voltageMeter = this.findViewById(R.id.voltageMeter);
        voltageMeter.setType(RequestType.VOLTAGE);
        textViews.add(voltageMeter);

        ampMeter = this.findViewById(R.id.ampMeter);
        ampMeter.setType(RequestType.AMEPERE);
        textViews.add(ampMeter);

        powerMeter = this.findViewById(R.id.powerMeter);

        minPowerView = this.findViewById(R.id.minPowerView);

        maxPowerView = this.findViewById(R.id.maxPowerView);

        recoveredPower = this.findViewById(R.id.recoveredPower);

        spentPower = this.findViewById(R.id.spentPower);
        time = this.findViewById(R.id.time);
        life = this.findViewById(R.id.life);
        life.setType(RequestType.BATTERYLIFE);
        textViews.add(life);


        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        rxBleClient = RxBleClient.create(this);
        bleDevice = rxBleClient.getBleDevice(mDeviceAddress);
        connectionObservable = prepareConnectionObservable();

        requestTypes.add(new VoltageRequest());
        requestTypes.add(new AmpereRequest());
        requestTypes.add(new BatteryLifeRequest());
    }
    
    private void setupNotificationAndSend() {

        connection.setupNotification(UUID.fromString(Constants.CHAR_READ))
                .doOnNext(notificationObservable -> {
                    //Log.d(TAG, "notification has been setup");
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .timeout(200, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.empty())
                .subscribe(
                        bytes -> {
                            updateUI(bytes);
                        }

                );
    }

    private void updateUI(byte[] bytes) {
        //if(!Arrays.equals(lastResponse,bytes)){
            lastResponse=bytes;
            Statistics.countRespnse();
            //Log.d(TAG,"updateUI");
            String[] hexString = new String[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                byte[] temp = new byte[1];
                temp[0] = bytes[i];
                hexString[i] = HexString.bytesToHex(temp);
            }
            String requestBit = hexString[5];


            for(IRequest e:requestTypes){
                //Log.d(TAG,"Type:"+e.getRequestBit()+" "+requestBit);
                if(e.getRequestBit().equals(requestBit)){
                    //Log.d(TAG,"match");
                    String temp = e.handleResponse(hexString);
                    for(SpecialTextView f:textViews){
                        if(f.getType()==e.getRequestType()){
                            runOnUiThread(() -> f.setText(temp));
                        }
                    }
                }
            }
            //update on each response
            Thread t = new Thread() {
                public void run() {
                    runOnUiThread(() -> {
                        powerMeter.setText((int)Statistics.getPower()+"W");
                        DecimalFormat df = new DecimalFormat("#.####");
                        df.setRoundingMode(RoundingMode.CEILING);

                        minPowerView.setText("min Power: " + (int)Statistics.getMinPower() + "W");
                        maxPowerView.setText("max Power: " + (int)Statistics.getMaxPower() + "W");
                        spentPower.setText("spent: " + df.format(Statistics.getSpent()) + " Wh");
                        recoveredPower.setText("recoverd: " + df.format(Statistics.getRecoverd()) + " Wh");
                        time.setText(Statistics.getCurrDiff()+" ms");
                    });
                }
            };
            t.start();


        }


    //}

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false);
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    public void connect(View view) {
        doConnect();
    }

    public void readName(View view) {
        if (!isConnected()) {
            doConnect();
        }
        //Start
        handler1.post(process);
        handler.post(runnableMeta);

    }

    public void reset(View view) {
        Statistics.resetPowerStats();
    }

    public void stopHandler(View view) {
        handler.removeCallbacksAndMessages(null);
        handler1.removeCallbacksAndMessages(null);
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
                        //handler.removeCallbacksAndMessages(null);
                        //handler1.removeCallbacksAndMessages(null);
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

    private Runnable runnableMeta = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"Queue Size:"+requestQueue.size());
            Log.d(TAG,"Sent:"+Statistics.getRequestsSent()+" Received:"+Statistics.getResponseReceived()+" Ratio:"+Statistics.getRequestsSent()/Statistics.getResponseReceived());
            adjustTiming();
            if(isConnected()) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(updateAmpsRunnable, Constants.getAmpereDelay());
                handler.postDelayed(updateBatterylife, Constants.getBatterylifeDelay());
                handler.postDelayed(updateVoltageRunnable, Constants.getVoltageDelay());
                handler.postDelayed(this, 10000);
            }
        }
    };

    //Change request timings according to stats
    private void adjustTiming() {
       /* int requests= Statistics.getRequestsSent();
        int response= Statistics.getResponseReceived();

        if((requests/response)>2){
            Constants.BASE_DELAY*=1.1;
        }
        else if(requests/response==1){
            Constants.BASE_DELAY*=0.9;
        }
        Statistics.resetRequestStats();*/

       if(requestQueue.size()>100){
           Constants.BASE_DELAY*=1.1;
       }
       else if(requestQueue.size()<100){
           Constants.BASE_DELAY*=0.9;
       }
        Statistics.resetRequestStats();

    }

    private Runnable updateAmpsRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                requestQueue.add(new AmpereRequest());
                handler.postDelayed(this, Constants.getAmpereDelay());
            }
        }
    };

    private Runnable updateVoltageRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected()) {
                requestQueue.add(new VoltageRequest());
                handler.postDelayed(this, Constants.getVoltageDelay());
            }
        }
    };

    private Runnable updateBatterylife = new Runnable() {
        @Override
        public void run() {
            requestQueue.add(new BatteryLifeRequest());
            handler.postDelayed(this, Constants.getBatterylifeDelay());
        }
    };

    private Runnable process = new Runnable() {
        @Override
        public void run() {
            setupNotificationAndSend();
            try{
                String command = requestQueue.remove().getRequestString();
                if(isConnected()) {
                    connection.writeCharacteristic(UUID.fromString(Constants.CHAR_WRITE), HexString.hexToBytes(command)).subscribe();
                    Statistics.countRequest();
                }
            }
            catch (NoSuchElementException e){
            }finally {
                handler1.postDelayed(this, Constants.QUEUE_DELAY);
            }


        }
    };



}
