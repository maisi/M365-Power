package maisi.M365.power.main;

import java.util.concurrent.ConcurrentSkipListSet;

public class Statistics {

    private static double maxPower = 0;
    private static double minPower = -0;

    private static double recovered = 0.0; //watt hours
    private static double spent = 0.0; //watt hours

    private static double currentVoltage = 0.0;
    private static double currentAmpere = 0.0;

    private static long lastTimeStamp;
    private static double currDiff;

    private static int requestsSent = 1;
    private static int responseReceived = 1;

    private static int batteryLife = 0;
    private static int remainingCapacity = 7800; //Nennkapazität
    private static int batteryTemperature = 0;

    private static double distanceTravelled = 0.0; //km
    private static double currentSpeed = 0.0; //km/h

    private static ConcurrentSkipListSet<Double> currentList = new ConcurrentSkipListSet<>();
    private static ConcurrentSkipListSet<Double> speedList = new ConcurrentSkipListSet<>();

    private static void calculateEnergy() {
        Long now = System.nanoTime();
        double diff = now - lastTimeStamp;
        diff /= 1000000;
        if (diff > 10000) {
            diff = 500;
        }
        currDiff = diff;
        diff /= 1000;
        double power = getAveragedPower();
        //Log.d("Stat","seconds:"+diff+" "+testTime+" power:"+power);
        if (power < 0) {
            recovered += ((power / 60 / 60) * diff);
        } else if (power > 0) {
            spent += ((power / 60 / 60) * diff);
        }
        lastTimeStamp = now;
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
        Statistics.currentAmpere = currentAmpere;
        currentList.add(currentAmpere);
        //calculateEnergy();
        setMaxPower(getPower());
        setMinPower(getPower());
    }
}
