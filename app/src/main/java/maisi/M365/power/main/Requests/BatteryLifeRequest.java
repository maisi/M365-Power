package maisi.M365.power.main.Requests;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class BatteryLifeRequest implements IRequest {
    private static int delay = 1000;
    private final String requestBit = "32";
    private final RequestType requestType = RequestType.BATTERYLIFE;
    private long startTime;

    public BatteryLifeRequest() {
        this.startTime = System.currentTimeMillis() + delay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public String getRequestString() {
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x32)
                .setPayload(0x02)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String temp = request[7] + request[6];
        int batteryLife = (short) Integer.parseInt(temp, 16);
        Statistics.setBatteryLife(batteryLife);
        return batteryLife + " %";
        //return textViews;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }

}
