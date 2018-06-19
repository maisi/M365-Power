package maisi.M365.power.main.Requests;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class VoltageRequest implements IRequest {

    private static int delay = 500;
    private final String requestBit = "34";
    private final RequestType requestType = RequestType.VOLTAGE;
    private long startTime;

    public VoltageRequest() {
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
                .setPosition(0x34)
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
        int voltage = (short) Integer.parseInt(temp, 16);
        double v = voltage;
        v = v / 100;

        Statistics.setCurrentVoltage(v);
        return v + " V";
        //return textViews;
    }


    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
