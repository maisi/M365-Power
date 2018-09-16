package maisi.M365.power.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.Snackbar;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import maisi.M365.power.main.Requests.AmpereRequest;
import maisi.M365.power.main.Requests.BatteryLifeRequest;
import maisi.M365.power.main.Requests.DistanceRequest;
import maisi.M365.power.main.Requests.SpeedRequest;
import maisi.M365.power.main.Requests.SuperBatteryRequest;
import maisi.M365.power.main.Requests.SuperMasterRequest;
import maisi.M365.power.main.Requests.SwitchRequests.Cruise.CheckCruise;
import maisi.M365.power.main.Requests.SwitchRequests.Cruise.CruiseOff;
import maisi.M365.power.main.Requests.SwitchRequests.Cruise.CruiseOn;
import maisi.M365.power.main.Requests.SwitchRequests.Light.CheckLight;
import maisi.M365.power.main.Requests.SwitchRequests.Light.LightOff;
import maisi.M365.power.main.Requests.SwitchRequests.Light.LightOn;
import maisi.M365.power.main.Requests.SwitchRequests.Locking.CheckLock;
import maisi.M365.power.main.Requests.SwitchRequests.Locking.LockOff;
import maisi.M365.power.main.Requests.SwitchRequests.Locking.LockOn;
import maisi.M365.power.main.Requests.SwitchRequests.Recovery.CheckRecovery;
import maisi.M365.power.main.Requests.SwitchRequests.Recovery.MediumMode;
import maisi.M365.power.main.Requests.SwitchRequests.Recovery.StrongMode;
import maisi.M365.power.main.Requests.SwitchRequests.Recovery.WeakMode;
import maisi.M365.power.main.Requests.VoltageRequest;
import maisi.M365.power.util.HexString;
import maisi.M365.power.util.LogWriter;

public class DeviceActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final static String TAG = DeviceActivity.class.getSimpleName();
    private static long lastTimeStamp;
    private static double currDiff = 0L;
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
    private SpecialTextView speedMeter;
    private TextView powerMeter;
    private TextView minPowerView;
    private TextView maxPowerView;
    private TextView efficiencyMeter;
    private TextView rangeMeter;
    private TextView recoveredPower;
    private TextView spentPower;
    private TextView time;
    private TextView battTemp;
    private TextView distance;
    private TextView capacity;
    private TextView averageSpeed;
    private TextView averageEfficiency;
    private TextView motorTemp;

    private Button startHandlerButton;
    private Deque<IRequest> requestQueue = new LinkedBlockingDeque<>();
    private Map<RequestType, IRequest> requestTypes = new HashMap<>();
    private List<SpecialTextView> textViews = new ArrayList<>();
    private ConcurrentSkipListSet<RequestType> checkFirst = new ConcurrentSkipListSet<>();
    private String[] lastResponse;
    private HandlerThread handlerThread;
    private HandlerThread handlerThread1;
    private Handler handler;
    private Handler handler1;
    private LogWriter logWriter = new LogWriter(this);
    private int lastDepth = 0;
    private boolean storagePermission = false;
    private boolean handlerStarted=false;
    private boolean runOnce = false;
    private static final int PERMISSION_EXTERNAL_STORAGE = 0;
    private ConstraintLayout mRootView;


    private Runnable updateSuperRunnable = new Runnable() {
        @Override
        public void run() {
            requestQueue.add(new SuperMasterRequest());
            handler.postDelayed(this, Constants.getSpeedDelay());
        }
    };
    private Runnable updateSuperBatteryRunnable = new Runnable() {
        @Override
        public void run() {
            requestQueue.add(new SuperBatteryRequest());
            handler.postDelayed(this, Constants.getAmpereDelay());
        }
    };

    private Runnable getLogsRunnable = new Runnable() {
        @Override
        public void run() {
            logWriter.writeLog(false);
            handler.postDelayed(this, Constants.getLogDelay());
        }
    };
    private Runnable runnableMeta = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Queue Size:" + requestQueue.size() + " QueueDelay:" + Constants.QUEUE_DELAY + " BaseDelay:" + Constants.BASE_DELAY);
            Log.d(TAG, "Sent:" + Statistics.getRequestsSent() + " Received:" + Statistics.getResponseReceived() + " Ratio:" + (double) Statistics.getRequestsSent() / Statistics.getResponseReceived());
            adjustTiming();


            if (isConnected() && !runOnce) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(updateSuperRunnable, Constants.getSpeedDelay());
                handler.postDelayed(updateSuperBatteryRunnable, Constants.getAmpereDelay());
                if(storagePermission){
                    handler.postDelayed(getLogsRunnable, 2000);
                }
                handler.postDelayed(this, 30000);
                runOnce=true;
            }

            if(Statistics.getCurrentSpeed()<3){
                fillCheckFirstList();
            }
        }
    };
    private Runnable process = new Runnable() {
        @Override
        public void run() {
            if(!checkFirst.isEmpty()){
                checkFirst();
            }
            setupNotificationAndSend();
            try {
                IRequest toSend= requestQueue.remove();
                String command = toSend.getRequestString();
                //Log.d(TAG,"command:"+command);
                if (isConnected()) {
                    connection.writeCharacteristic(UUID.fromString(Constants.CHAR_WRITE), HexString.hexToBytes(command)).subscribe();
                    //Log.d(TAG, "Req sent: " + command);
                    if(toSend.getRequestType()!=RequestType.NOCOUNT){
                        Statistics.countRequest();
                    }

                }
            } catch (NoSuchElementException e) {
            } finally {
                handler1.postDelayed(this, Constants.QUEUE_DELAY);
            }

        }
    };
    private Menu menu;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThread.quit();
        handlerThread1.quit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.MyAppTheme);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device);


        voltageMeter = this.findViewById(R.id.voltageMeter);
        voltageMeter.setType(RequestType.VOLTAGE);
        textViews.add(voltageMeter);

        ampMeter = this.findViewById(R.id.ampMeter);
        ampMeter.setType(RequestType.AMEPERE);
        textViews.add(ampMeter);

        speedMeter = this.findViewById(R.id.speedMeter);
        speedMeter.setType(RequestType.SPEED);
        textViews.add(speedMeter);

        powerMeter = this.findViewById(R.id.powerMeter);

        minPowerView = this.findViewById(R.id.minPowerView);

        maxPowerView = this.findViewById(R.id.maxPowerView);

        efficiencyMeter = this.findViewById(R.id.efficiencyMeter);

        rangeMeter = this.findViewById(R.id.rangeMeter);

        recoveredPower = this.findViewById(R.id.recoveredPower);

        startHandlerButton = this.findViewById(R.id.start_handler_button);

        spentPower = this.findViewById(R.id.spentPower);

        battTemp = this.findViewById(R.id.battTemp);

        distance = this.findViewById(R.id.distanceMeter);

        capacity = this.findViewById(R.id.remainingAmps);

        averageEfficiency = this.findViewById(R.id.AverageEfficiencyMeter);

        averageSpeed = this.findViewById(R.id.averageSpeedMeter);

        motorTemp = this.findViewById(R.id.motorTemp);

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

        requestTypes.put(RequestType.VOLTAGE, new VoltageRequest());
        requestTypes.put(RequestType.AMEPERE, new AmpereRequest());
        requestTypes.put(RequestType.BATTERYLIFE, new BatteryLifeRequest());
        requestTypes.put(RequestType.SPEED, new SpeedRequest());
        requestTypes.put(RequestType.DISTANCE, new DistanceRequest());
        requestTypes.put(RequestType.SUPERMASTER, new SuperMasterRequest());
        requestTypes.put(RequestType.SUPERBATTERY, new SuperBatteryRequest());

        requestTypes.put(RequestType.LOCK,new CheckLock());
        requestTypes.put(RequestType.CRUISE,new CheckCruise());
        requestTypes.put(RequestType.LIGHT,new CheckLight());
        requestTypes.put(RequestType.RECOVERY,new CheckRecovery());

        fillCheckFirstList();

        lastTimeStamp = System.nanoTime();
        mRootView= findViewById(R.id.root);

        handlerThread=new HandlerThread("RequestThread");
        handlerThread.start();
        handler=new Handler(handlerThread.getLooper());
        handlerThread1=new HandlerThread("LoggingThread");
        handlerThread1.start();
        handler1=new Handler(handlerThread1.getLooper());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
        else{
            storagePermission=true;
        }
    }

    private void fillCheckFirstList() {
        checkFirst.clear();
        checkFirst.add(RequestType.CRUISE);
        checkFirst.add(RequestType.LOCK);
        checkFirst.add(RequestType.LIGHT);
        checkFirst.add(RequestType.RECOVERY);
    }

    @SuppressLint("CheckResult")
    private void setupNotificationAndSend() {

        connection.setupNotification(UUID.fromString(Constants.CHAR_READ))
                .doOnNext(notificationObservable -> {
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .timeout(200, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.empty())
                .subscribe(
                        bytes -> updateUI(bytes)

                );
    }

    private void updateUI(byte[] bytes) {
        if (bytes.length == 0) { //super request returns a third empty message
            return;
        }
        //Log.d(TAG, "Resp rec. length:" + bytes.length);
        //handler1.post(process);

        String[] hexString = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte[] temp = new byte[1];
            temp[0] = bytes[i];
            hexString[i] = HexString.bytesToHex(temp);
        }

        String requestBit = hexString[5];
        //Log.d(TAG, "requestBit: "+requestBit+" " + Arrays.toString(hexString));

        if (bytes.length > 10) { //Super handling
            if (requestBit.equals(requestTypes.get(RequestType.SUPERMASTER).getRequestBit())) {
                lastResponse = hexString;
                Statistics.countRespnse();
                return;
            } else if (requestBit.equals(requestTypes.get(RequestType.SUPERBATTERY).getRequestBit())) {
                Statistics.countRespnse();
                Long now = System.nanoTime();
                double diff = now - lastTimeStamp;
                diff /= 1000000;
                currDiff = diff;

                lastTimeStamp = now;
                Log.d(TAG, "super battery time in ms:" + diff);
                requestTypes.get(RequestType.SUPERBATTERY).handleResponse(hexString);
            } else {
                String[] combinedRespose = new String[lastResponse.length + hexString.length];
                System.arraycopy(lastResponse, 0, combinedRespose, 0, lastResponse.length);
                System.arraycopy(hexString, 0, combinedRespose, lastResponse.length, hexString.length);
                String speed = requestTypes.get(RequestType.SUPERMASTER).handleResponse(combinedRespose);
                for (SpecialTextView f : textViews) {
                    if (f.getType() == RequestType.SPEED) {
                        runOnUiThread(() -> f.setText(speed));
                    }
                }
            }
        } else {
            Log.d(TAG, "Other stuff received");
            //Statistics.countRespnse();
            for (IRequest e : requestTypes.values()) {
                if (e.getRequestBit().equals(requestBit)) {
                    String temp = e.handleResponse(hexString);
                    if(e.getRequestType()==RequestType.LOCK){
                        MenuItem lock = menu.findItem(R.id.lock);
                        runOnUiThread(() -> lock.setChecked(Statistics.isScooterLocked()));
                        if(Statistics.isScooterLocked()){
                            Constants.BASE_DELAY=10000;
                        }
                        else{
                            Constants.BASE_DELAY=300;
                        }
                        if(checkFirst.remove(RequestType.LOCK) && !handlerStarted){
                            requestQueue.clear(); //remove unnecessary requests
                            if(checkFirst.isEmpty()&& !handlerStarted){
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    }
                    else if(e.getRequestType()==RequestType.CRUISE){
                        MenuItem cruise = menu.findItem(R.id.cruise);
                        runOnUiThread(() -> cruise.setChecked(Statistics.isCruiseActive()));
                        if(checkFirst.remove(RequestType.CRUISE)&& !handlerStarted){
                            requestQueue.clear();
                            if(checkFirst.isEmpty()&& !handlerStarted){
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    }
                    else if(e.getRequestType()==RequestType.LIGHT){
                        MenuItem light = menu.findItem(R.id.light);
                        runOnUiThread(() -> light.setChecked(Statistics.isLightActive()));
                        if(checkFirst.remove(RequestType.LIGHT)&& !handlerStarted){
                            requestQueue.clear();
                            if(checkFirst.isEmpty()&& !handlerStarted){
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    }
                    else if(e.getRequestType()==RequestType.RECOVERY){



                        if(temp.equals("00")){
                            //Log.d(TAG,"weak setting");
                            MenuItem weak = menu.findItem(R.id.weak);
                            runOnUiThread(() -> weak.setChecked(true));
                            //runOnUiThread(() -> medium.setChecked(false));
                            //runOnUiThread(() -> strong.setChecked(false));
                        }
                        else if(temp.equals("01")){
                            //Log.d(TAG,"medium setting");
                            //runOnUiThread(() -> weak.setChecked(false));
                            MenuItem medium = menu.findItem(R.id.medium);
                            runOnUiThread(() -> medium.setChecked(true));
                            //runOnUiThread(() -> strong.setChecked(false));
                        }
                        else if(temp.equals("02")){
                            //Log.d(TAG,"strong setting");
                            //runOnUiThread(() -> weak.setChecked(false));
                            //runOnUiThread(() -> medium.setChecked(false));
                            MenuItem strong = menu.findItem(R.id.strong);
                            runOnUiThread(() -> strong.setChecked(true));
                        }
                        if(checkFirst.remove(RequestType.RECOVERY)&& !handlerStarted){
                            requestQueue.clear();
                            if(checkFirst.isEmpty() && !handlerStarted){
                               handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    }
                    for (SpecialTextView f : textViews) {
                        if (f.getType() == e.getRequestType()) {
                            runOnUiThread(() -> f.setText(temp));
                        }
                    }
                }
            }

        }

        //update on each response
        Thread t = new Thread() {
            public void run() {
                runOnUiThread(() -> {
                    powerMeter.setText((int) Statistics.getPower() + "W");
                    DecimalFormat df = new DecimalFormat("#.####");
                    df.setRoundingMode(RoundingMode.CEILING);
                    DecimalFormat df1 = new DecimalFormat("##.#");
                    df.setRoundingMode(RoundingMode.CEILING);
                    minPowerView.setText("min Power: " + (int)Statistics.getMinPower() + "W");
                    maxPowerView.setText("max Power: " + (int)Statistics.getMaxPower() + "W");
                    //minPowerView.setText("QueueD: " + Constants.QUEUE_DELAY + "ms");
                    //maxPowerView.setText("Req/Res: " + Statistics.getRequestsSent() + " " + Statistics.getResponseReceived());
                    efficiencyMeter.setText(Statistics.getMampHoursPerKilometer()+" mAh/Km");
                    rangeMeter.setText(Statistics.getRemainingRange()+" km ");
                    spentPower.setText("spent: " + df.format(Statistics.getSpent()) + " Ah");
                    recoveredPower.setText("recovered: " + df.format(Statistics.getRecovered()) + " Ah");
                    time.setText(Statistics.getCurrDiff() + " ms");
                    life.setText(Statistics.getBatteryLife() + " %");
                    ampMeter.setText(Statistics.getCurrentAmpere() + " A");
                    voltageMeter.setText(Statistics.getCurrentVoltage() + " V");
                    battTemp.setText(Statistics.getBatteryTemperature()+ " °C");
                    motorTemp.setText(Statistics.getMotorTemperature()+ " °C");
                    capacity.setText(Statistics.getRemainingCapacity()+ "");
                    distance.setText(df1.format(Statistics.getDistanceTravelled())+ " km");
                    averageEfficiency.setText(df1.format(Statistics.getAverageEfficiency())+ " mAh/km");
                    averageSpeed.setText(df1.format(Statistics.getAverageSpeed())+ " km/h");
                });
            }
        };
        t.start();
    }

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

    public void startHandler(View view) {
        if(!handlerStarted) {
            if (!isConnected()) {
                doConnect();
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    handler1.post(process);
                    handler.post(runnableMeta);
                }, 5000);
            } else {
                handler1.post(process);
                handler.post(runnableMeta);
            }
            startHandlerButton.setText("Stop Handler");
            handlerStarted=true;
        }
        else{
            stopHandler();
            handlerStarted=false;
        }
    }

    public void reset(View view) {
        Statistics.resetPowerStats();
    }

    public void stopHandler() {
        Log.d(TAG,"Stop Handler called");
        handler.removeCallbacksAndMessages(null);
        handler1.removeCallbacksAndMessages(null);
        requestQueue.clear();
        logWriter.writeLog(true);
        Toast.makeText(this, "Logs in:"+logWriter.getPath(), Toast.LENGTH_LONG).show();
        startHandlerButton.setText("Start Handler");
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
                        Toast.makeText(DeviceActivity.this, "Could not connect to scooter,please retry", Toast.LENGTH_LONG).show();
                        //handler.removeCallbacksAndMessages(null);
                        //handler1.removeCallbacksAndMessages(null);
                        dispose();
                        time.setText("disconnected");
                    })
                    .subscribe(this::onConnectionReceived, this::onConnectionFailure);
        }
    }

    private void triggerDisconnect() {

        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
        time.setText("disconnected");
        stopHandler();
    }

    private void dispose() {
        connectionDisposable = null;
    }

    private void onConnectionFailure(Throwable throwable) {
        Log.d(TAG,"connection fail: "+throwable.getMessage());
        Toast.makeText(DeviceActivity.this, "Could not connect to scooter,please retry", Toast.LENGTH_LONG).show();
    }

    private void onConnectionReceived(RxBleConnection connection) {
        fillCheckFirstList();
        Toast.makeText(DeviceActivity.this, "Starting preliminary activities", Toast.LENGTH_LONG).show();
        this.connection = connection;
        this.time.setText("connected");
        handler1.post(process);
        checkFirst();
    }

    private void checkFirst() {
        for(RequestType e:checkFirst){
            requestQueue.addFirst(requestTypes.get(e));
        }
    }

    //Change request and queue timings
    private void adjustTiming() {
        double requests = Statistics.getRequestsSent();
        double response = Statistics.getResponseReceived();
        if ((requests / response) > 1.3) {
            Constants.QUEUE_DELAY *= 1.1;
        } else if (requests / response == 1) {
            Constants.QUEUE_DELAY *= 0.9;
        }
        int size = requestQueue.size();
        if ((requestQueue.size() > 50) && (lastDepth <= size)) {
            Constants.BASE_DELAY *= 1.1;
        } else if ((requestQueue.size() < 50) && (lastDepth >= size)) {
            Constants.BASE_DELAY *= 0.9;
        }
        if(requestQueue.size()>100){
            requestQueue.clear();
        }
        lastDepth = size;
        Statistics.resetRequestStats();

    }

    private void requestStoragePermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mRootView, R.string.permission_request,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(DeviceActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_EXTERNAL_STORAGE);
                }
            }).show();

        } else {
            Snackbar.make(mRootView, R.string.permission_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mRootView, R.string.permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
                storagePermission=true;
            } else {
                // Permission request was denied.
                Snackbar.make(mRootView, R.string.permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }


    //------MENU------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu=menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            Log.d(TAG,"settings clicked");
            startActivity(new Intent(DeviceActivity.this, SettingsActivity.class));
            return true;
        }
        else if(id == R.id.resetStat){
            Statistics.resetPowerStats();
            return true;
        }
        else if(id == R.id.connect){
            doConnect();
            return true;
        }
        else if(id == R.id.lock){
            if(Statistics.isScooterLocked()){
                lockOff();
                item.setChecked(false);
            }
            else {
                lockOn();
                item.setChecked(true);
            }
            return true;
        }
        else if(id == R.id.cruise){
            if(Statistics.isCruiseActive()){
                cruiseOff();
                item.setChecked(false);
            }
            else {
                cruiseOn();
                item.setChecked(true);
            }
            return true;
        }
        else if(id == R.id.light){
            if(Statistics.isLightActive()){
                lightOff();
                item.setChecked(false);
            }
            else {
                lightOn();
                item.setChecked(true);
            }
            return true;
        }
        else if(id == R.id.weak){
            if(Statistics.getRecoveryMode()!=0){
                setWeakMode();
            }
            return true;
        }
        else if(id == R.id.medium){
            if(Statistics.getRecoveryMode()!=1){
                setMediumMode();
            }
            return true;
        }
        else if(id == R.id.strong){
            if(Statistics.getRecoveryMode()!=2){
                setStrongMode();
            }
            return true;
        }
        fillCheckFirstList();
        return super.onOptionsItemSelected(item);
    }

    private void checkRecovery(){
        requestQueue.add(new CheckRecovery());
    }

    private void setStrongMode() {
        requestQueue.addFirst(new StrongMode());
    }

    private void setMediumMode() {
        requestQueue.addFirst(new MediumMode());
    }

    private void setWeakMode() {
        requestQueue.addFirst(new WeakMode());
    }

    private void lightOn() {
        requestQueue.addFirst(new LightOn());
    }

    private void lightOff() {
        requestQueue.addFirst(new LightOff());
    }

    private void cruiseOn() {
        requestQueue.addFirst(new CruiseOn());
    }

    private void cruiseOff() {
        requestQueue.addFirst(new CruiseOff());
    }

    private void lockOn() {
        requestQueue.addFirst(new LockOn());
    }

    private void lockOff() {
        requestQueue.addFirst(new LockOff());
    }



}
