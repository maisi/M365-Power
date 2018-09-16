package maisi.M365.power.main;

import android.support.design.widget.TabLayout;
import android.util.Log;

import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;

public class Statistics {

    private static int useAverageAsDefault=0;


    private static boolean loggingEnabled=true;

    private static boolean scooterLocked=false;

    private static boolean cruiseActive = false;

    private static boolean lightActive = false;

    private static int recoveryMode =0; //0=weak,1=medium,2=strong

    private static double maxPower = 0;
    private static double minPower = -0;

    private static double recovered = 0.0; //Ampere hours
    private static double spent = 0.0; //Ampere hours

    private static double currentVoltage = 0.0;
    private static double currentAmpere = 0.0;

    private static long lastTimeStamp;
    private static double currDiff;

    private static int requestsSent = 1;
    private static int responseReceived = 1;

    private static int batteryLife = 0;
    private static int remainingCapacity = 7800; //Nennkapazität maH
    private static int batteryTemperature = 0;

    private static double motorTemperature =0;

    private static double distanceTravelled = 0.0001; //km
    private static double currentSpeed = 0.0; //km/h

    private static double mampHoursPerKilometer =0.0;
    private static int min_speed=4;


    private static int default_efficiency=600;
    private static double remainingRange=0.0;

    private static ConcurrentSkipListSet<Double> currentList = new ConcurrentSkipListSet<>();
    private static ConcurrentSkipListSet<Double> speedList = new ConcurrentSkipListSet<>();
    private static ConcurrentSkipListSet<Double> efficiencyList = new ConcurrentSkipListSet<>();
    private static ConcurrentSkipListSet<Double> averageSpeedList = new ConcurrentSkipListSet<>();
    public static int getDefault_efficiency() {
        return default_efficiency;
    }

    public static void setDefault_efficiency(int new_default_efficiency) {
        Log.d("stat","default:"+new_default_efficiency);
        if(new_default_efficiency==-1){
            new_default_efficiency=getAverageEfficiency();
            useAverageAsDefault = 1;
        }
        else if(new_default_efficiency==-2){
            new_default_efficiency=getLimitedAverageEfficiency();
            useAverageAsDefault = 2;
        }
        else{
            useAverageAsDefault = 0;
        }
        Statistics.default_efficiency = new_default_efficiency;
    }
    public static int getMin_speed() {
        return min_speed;
    }

    public static void setMin_speed(int min_speed) {
        Log.d("stat","min_speed:"+min_speed);
        Statistics.min_speed = min_speed;
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static void setLoggingEnabled(boolean loggingEnabled) {
        loggingEnabled = loggingEnabled;
    }


    public static double getMotorTemperature() {
        return motorTemperature;
    }

    public static void setMotorTemperature(double motorTemperature) {
        Statistics.motorTemperature = motorTemperature;
    }
    public static int isUseAverageAsDefault() {
        return useAverageAsDefault;
    }

    public static void setUseAverageAsDefault(int useAverageAsDefault) {
        Statistics.useAverageAsDefault = useAverageAsDefault;
    }

    private static void calculateEnergy() {
        Long now = System.currentTimeMillis();
        double diff = now - lastTimeStamp;
        if (diff > 10000) {
            diff = 500;
        }
        //currDiff = diff;
        diff /= 1000;
        double power = getAveragedCurrent();
        if (Double.isNaN(power)) {
            power = getCurrentAmpere();
        }

        //Log.d("Stat","seconds:"+diff+" "+testTime+" power:"+power);
        if (power < 0) {
            recovered += ((power / 60 / 60) * diff);
        } else if (power > 0) {
            spent += ((power / 60 / 60) * diff);
        }
        //lastTimeStamp = now;

        if (power==0.0) {
            power = getCurrentAmpere();
        }
        //Log.d("Stat", "calculateEnergy: "+power+ " speed: "+currentSpeed);
        if(getCurrentSpeed()>=min_speed) {
            mampHoursPerKilometer = ((power * 1000) / currentSpeed);
            if (mampHoursPerKilometer < 0.01) {
                mampHoursPerKilometer = 0.01;
            }
            remainingRange = remainingCapacity / mampHoursPerKilometer;
            if (remainingRange < 0.01) {
                remainingRange = 0.0;
            }
            efficiencyList.add(getMampHoursPerKilometer());
        }
        else{
            if(useAverageAsDefault==1){
                default_efficiency=getAverageEfficiency();
            }
            else if(useAverageAsDefault==2)
            {
                default_efficiency=getLimitedAverageEfficiency();
            }
            mampHoursPerKilometer=default_efficiency;
            remainingRange = remainingCapacity / mampHoursPerKilometer;
        }
    }

    public static void resetPowerStats() {
        maxPower = 0;
        minPower = 0;
        recovered = 0;
        spent = 0;
    }

    public static void resetRequestStats() {
        requestsSent = 1;
        responseReceived = 1;
    }

    public static double getCurrDiff() {
        return currDiff;
    }

    public static double getPower() {
        return currentAmpere * currentVoltage;
    }

    public static double getAveragedPower() {
        double currentSum = 0.0;
        for (double d : currentList) {
            currentSum += d;
        }
        double averageCurrent = (currentSum / currentList.size());
        if (Double.isNaN(averageCurrent)) {
            averageCurrent = 0.0;
        }
        return averageCurrent * currentVoltage;
    }

    public static double getAveragedCurrent() {
        double currentSum = 0.0;
        for (double d : currentList) {
            currentSum += d;
        }
        double averageCurrent = (currentSum / currentList.size());
        if (Double.isNaN(averageCurrent)) {
            averageCurrent = 0.0;
        }
        return averageCurrent;
    }

    public static int getAverageEfficiency() {
        if(efficiencyList.size()==0){
            return 600; //just started app, no values yet
        }
        double sum =efficiencyList.stream().mapToDouble(Double::doubleValue).sum();
        double avgEff = (sum / efficiencyList.size());
        return (int)avgEff;
    }

    public static int getLimitedAverageEfficiency() {
        if(efficiencyList.size()==0){
            return 600;
        }

        double currentSum = 0.0;
        int i=100;
        Iterator iterator = efficiencyList.descendingIterator();
        while(!iterator.hasNext() || i==0){
            currentSum+=(Double)iterator.next();
            i--;
        }
        double averageCurrent = (currentSum / (100-i));

        //double sum =efficiencyList.stream().mapToDouble(Double::doubleValue).sum();
        //double avgEff = (sum / efficiencyList.size());
        return (int)averageCurrent;
    }

    public static double getAverageSpeed() {
        double sum =averageSpeedList.stream().mapToDouble(Double::doubleValue).sum();
        double avgEff = (sum / averageSpeedList.size());
        return avgEff;
    }

    public static double getMaxPower() {
        return maxPower;
    }

    public static void setMaxPower(double maxPower) {
        if (Statistics.maxPower < maxPower) {
            Statistics.maxPower = maxPower;
        }
    }

    public static double getMinPower() {
        return minPower;
    }

    public static void setMinPower(double minPower) {
        if (Statistics.minPower > minPower) {
            Statistics.minPower = minPower;
        }
    }

    public static double getSpent() {
        return spent;
    }

    public static void countRespnse() {
        responseReceived += 1;
    }

    public static void countRequest() {
        requestsSent += 1;
    }

    public static int getRequestsSent() {
        return requestsSent;
    }

    public static int getResponseReceived() {
        return responseReceived;
    }

    public static void setSpeed(double speed) {
        Statistics.speedList.add(speed);
        Statistics.currentSpeed = speed;
    }

    public static int getRemainingCapacity() {
        return remainingCapacity;
    }

    public static void setRemainingCapacity(int remainingCapacity) {
        Statistics.remainingCapacity = remainingCapacity;
    }

    public static double getDistanceTravelled() {
        return distanceTravelled;
    }

    public static void setDistanceTravelled(double distanceTravelled) {
        Statistics.distanceTravelled = distanceTravelled;
    }

    public static double getCurrentSpeed() {
        return currentSpeed;
    }

    public static double getRecovered() {
        return recovered;
    }

    public static LogDTO getLogStats() {
        calculateEnergy();
        LogDTO logDTO = new LogDTO();
        double currentSum = 0.0;
        for (double d : currentList) {
            currentSum += d;
        }
        double speedSum = speedList.stream().mapToDouble(Double::doubleValue).sum();
        double averageCurrent = (currentSum / currentList.size());
        double averageSpeed = speedSum / speedList.size();
        if (Double.isNaN(averageCurrent)) {
            averageCurrent = getCurrentAmpere();
        }
        if (Double.isNaN(averageSpeed)) {
            averageSpeed = getCurrentSpeed();
        }
        averageSpeedList.add(averageSpeed);
        logDTO.setAverageCurrent(round(averageCurrent, 2));
        logDTO.setAveragePower(round(averageCurrent * currentVoltage, 2));
        logDTO.setAverageSpeed(averageSpeed);
        logDTO.setBatteryLife(getBatteryLife());
        logDTO.setRecoveredPower(round(getRecovered(), 4));
        logDTO.setSpentPower(round(getSpent(), 4));
        logDTO.setVoltage(getCurrentVoltage());
        logDTO.setRemainingCapacity(getRemainingCapacity());
        logDTO.setDistanceTravelled(getDistanceTravelled());
        logDTO.setBattTemp(getBatteryTemperature());
        logDTO.setMampHoursPerKilometer(getMampHoursPerKilometer());
        logDTO.setRemainingRange(getRemainingRange());
        logDTO.setTimestamp(lastTimeStamp);

        currentList.clear();
        speedList.clear();
        return logDTO;
    }

    public static double round(double toRound, int decimals) {
        int temp = (int) (toRound * Math.pow(10, decimals));
        double temp2 = (double) temp;
        return temp2 / Math.pow(10, decimals);
    }

    public static int getBatteryLife() {
        return batteryLife;
    }

    public static void setBatteryLife(int batteryLife) {
        Statistics.batteryLife = batteryLife;
    }

    public static int getBatteryTemperature() {
        return batteryTemperature;
    }

    public static void setBatteryTemperature(int batteryTemperature) {
        Statistics.batteryTemperature = batteryTemperature;
    }

    public static double getCurrentVoltage() {
        return currentVoltage;
    }

    public static void setCurrentVoltage(double currentVoltage) {
        Statistics.currentVoltage = currentVoltage;
        //calculateEnergy();
    }

    public static double getCurrentAmpere() {
        return currentAmpere;
    }

    public static void setCurrentAmpere(double currentAmpere) {
        //Log.d("Stat","Current: "+currentAmpere);
        Statistics.currentAmpere = currentAmpere;
        currentList.add(currentAmpere);
        //calculateEnergy();
        setMaxPower(getPower());
        setMinPower(getPower());
        Long now = System.currentTimeMillis();
        double diff = now - lastTimeStamp;
        if (diff > 10000) {
            diff = 500;
        }
        if(diff >50){
            currDiff = diff;
        }
        lastTimeStamp = now;
    }


    public static double getMampHoursPerKilometer() {
        return round(mampHoursPerKilometer,2);
    }

    public static double getRemainingRange() {
        return round(remainingRange,1);
    }

    public static void activateLogging(boolean enable) {
        setLoggingEnabled(enable);
    }

    public static boolean isScooterLocked() {
        return scooterLocked;
    }

    public static void setScooterLocked(boolean scooterLocked) {
        Statistics.scooterLocked = scooterLocked;
    }

    public static boolean isCruiseActive() {
        return cruiseActive;
    }

    public static void setCruiseActive(boolean cruiseActive) {
        Statistics.cruiseActive = cruiseActive;
    }

    public static boolean isLightActive() {
        return lightActive;
    }

    public static void setLightActive(boolean lightActive) {
        Statistics.lightActive = lightActive;
    }

    public static int getRecoveryMode() {
        return recoveryMode;
    }

    public static void setRecoveryMode(int recoveryMode) {
        Statistics.recoveryMode = recoveryMode;
    }
}
