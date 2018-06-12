package maisi.M365.power.main;

import android.util.Log;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Statistics {

    private static double maxPower = 0;
    private static double minPower = -0;

    private static double recoverd = 0.0; //watt hours
    private static double spent = 0.0; //watt hours

    private static double currentVoltage = 0.0;
    private static double currentAmpere = 0.0;

    private static long lastTimeStamp;
    private static double currDiff;

    private static int requestsSent=1;
    private static int responseReceived=1;

    private static int batteryLife=0;

    private static double distanceTravelled = 0.0; //km
    private static double currentSpeed = 0.0; //km/h

    private static List<Double> currentList = new ArrayList<>();
    private static List<Double> speedList = new ArrayList<>();

    private static void calculateEnergy(){
        Long now=System.currentTimeMillis();
        double diff = now - lastTimeStamp;
        diff /= 1000;
        if (diff > 2) {
            diff = 0.5;
        }
        currDiff=diff;
        if (getPower() < 0) {
            recoverd += (getPower() / 60 / 60 * diff);
        } else if (getPower() > 0) {
            spent += (getPower() / 60 / 60 * diff);
        }
        lastTimeStamp= now;
    }

    public static void resetPowerStats(){
        maxPower=0;
        minPower=0;
        recoverd=0;
        spent=0;
    }

    public static void resetRequestStats(){
        requestsSent=1;
        responseReceived=1;
    }

    public static double getCurrDiff() {
        return currDiff;
    }

    public static double getPower(){
        return currentAmpere*currentVoltage;
    }

    public static double getCurrentVoltage() {
        return currentVoltage;
    }

    public static void setCurrentVoltage(double currentVoltage) {
        Statistics.currentVoltage = currentVoltage;
        calculateEnergy();
    }

    public static double getCurrentAmpere() {
        return currentAmpere;
    }

    public static void setCurrentAmpere(double currentAmpere) {
        Statistics.currentAmpere = currentAmpere;
        currentList.add(currentAmpere);
        calculateEnergy();
        setMaxPower(getPower());
        setMinPower(getPower());
    }

    public static double getMaxPower() {
        return maxPower;
    }

    public static void setMaxPower(double maxPower) {
        if(Statistics.maxPower < maxPower){
             Statistics.maxPower = maxPower;
        }
    }

    public static double getMinPower() {
        return minPower;
    }

    public static void setMinPower(double minPower) {
        if(Statistics.minPower > minPower){
            Statistics.minPower = minPower;
        }
    }

    public static double getRecoverd() {
        return recoverd;
    }

    public static double getSpent() {
        return spent;
    }

    public static void countRespnse() {
        responseReceived+=1;
    }

    public static void countRequest() {
        requestsSent+=1;
    }

    public static int getRequestsSent() {
        return requestsSent;
    }

    public static int getResponseReceived() {
        return responseReceived;
    }

    public static void setBatteryLife(int batteryLife) {
        Statistics.batteryLife = batteryLife;
    }

    public static void setDistanceTravelled(double distanceTravelled) {
        Statistics.distanceTravelled = distanceTravelled;
    }

    public static void setSpeed(double speed) {
        Statistics.speedList.add(speed);
        Statistics.currentSpeed=speed;
    }

    public static LogDTO getLogStats (){
        LogDTO logDTO=new LogDTO();
        double currentSum =0.0;
        for(double d:currentList){
            currentSum+=d;
        }
        double speedSum = speedList.stream().mapToDouble(Double::doubleValue).sum();
        double averageCurrent = (currentSum/currentList.size());
        double averageSpeed = speedSum/speedList.size();
        if(Double.isNaN(averageCurrent)){
            averageCurrent=0.0;
        }
        if(Double.isNaN(averageSpeed)){
            averageSpeed=0.0;
        }

        logDTO.setAverageCurrent(round(averageCurrent,2));
        logDTO.setAveragePower(round(averageCurrent*currentVoltage,2));
        logDTO.setAverageSpeed(averageSpeed);
        logDTO.setBatteryLife(batteryLife);
        logDTO.setRecoveredPower(round(recoverd,4));
        logDTO.setSpentPower(round(spent,4));
        logDTO.setVoltage(currentVoltage);

        currentList.clear();
        speedList.clear();
        return logDTO;
    }

    public static double round(double toRound,int decimals){
        int temp = (int)(toRound*Math.pow(10,decimals));
        double temp2= (double)temp;
        return temp2/Math.pow(10,decimals);
    }
}
