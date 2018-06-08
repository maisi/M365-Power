package maisi.M365.power.main;

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
}
