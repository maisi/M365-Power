package maisi.M365.power.main;

import java.util.ArrayList;
import java.util.List;

public class LogDTO {
    private double averageCurrent;
    private double voltage;
    private double averagePower;
    private double averageSpeed;
    private double distanceTravelled;
    private int batteryLife;
    private double spentPower;
    private double recoveredPower;
    private int remainingCapacity;
    private int battTemp;
    private double mampHoursPerKilometer;
    private double remainingRange;
    private long timestamp;

    public double getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }



    public double getAverageCurrent() {
        return averageCurrent;
    }

    public void setAverageCurrent(double averageCurrent) {
        this.averageCurrent = averageCurrent;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public double getAveragePower() {
        return averagePower;
    }

    public void setAveragePower(double averagePower) {
        this.averagePower = averagePower;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(double distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public double getBatteryLife() {
        return batteryLife;
    }

    public void setBatteryLife(int batteryLife) {
        this.batteryLife = batteryLife;
    }

    public double getSpentPower() {
        return spentPower;
    }

    public void setSpentPower(double spentPower) {
        this.spentPower = spentPower;
    }

    public double getRecoveredPower() {
        return recoveredPower;
    }

    public void setRecoveredPower(double recoveredPower) {
        this.recoveredPower = recoveredPower;
    }

    public List<String> toList(){
        List<String> result =new ArrayList<>();
        result.add(String.valueOf(this.averageCurrent));
        result.add(String.valueOf(this.voltage));
        result.add(String.valueOf(this.averagePower));
        result.add(String.valueOf(this.averageSpeed));
        result.add(String.valueOf(this.distanceTravelled));
        result.add(String.valueOf(this.batteryLife));
        result.add(String.valueOf(this.remainingCapacity));
        result.add(String.valueOf(this.battTemp));
        result.add(String.valueOf(this.recoveredPower));
        result.add(String.valueOf(this.spentPower));
        result.add(String.valueOf(this.mampHoursPerKilometer));
        result.add(String.valueOf(this.remainingRange));
        result.add(String.valueOf(this.timestamp));
        return result;
    }

    public List<String> getHeader(){
        List<String> result =new ArrayList<>();
        result.add("averageCurrent");
        result.add("voltage");
        result.add("averagePower");
        result.add("averageSpeed");
        result.add("distanceTravelled");
        result.add("batteryLife");
        result.add("remainingCapacity");
        result.add("batteryTemperature");
        result.add("recoveredPower");
        result.add("spentPower");
        result.add("amphperkilometer");
        result.add("remainingRange");
        result.add("timestamp");
        return result;
    }

    public int getBattTemp() {
        return battTemp;
    }

    public void setBattTemp(int battTemp) {
        this.battTemp = battTemp;
    }

    public double getMampHoursPerKilometer() {
        return mampHoursPerKilometer;
    }

    public void setMampHoursPerKilometer(double mampHoursPerKilometer) {
        this.mampHoursPerKilometer = mampHoursPerKilometer;
    }

    public double getRemainingRange() {
        return remainingRange;
    }

    public void setRemainingRange(double remainingRange) {
        this.remainingRange = remainingRange;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
