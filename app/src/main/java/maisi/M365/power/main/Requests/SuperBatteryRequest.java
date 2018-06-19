package maisi.M365.power.main.Requests;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class SuperBatteryRequest implements IRequest {

    private final String requestBit = "31";
    private final RequestType requestType = RequestType.SUPERBATTERY;

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public String getRequestString() {
        //55 aa 03 22 01 31 0a 9e ff
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x31)
                .setPayload(0x0a)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String temp = request[7] + request[6];
        int remainingCapacity = (short) Integer.parseInt(temp, 16);
        Statistics.setRemainingCapacity(remainingCapacity);

        temp = request[9] + request[8];
        int batteryLife = (short) Integer.parseInt(temp, 16);
        Statistics.setBatteryLife(batteryLife);

        temp = request[11] + request[10];
        int amps = (short) Integer.parseInt(temp, 16);
        double c = amps;
        c = c / 100;
        Statistics.setCurrentAmpere(c);

        temp = request[13] + request[12];
        int voltage = (short) Integer.parseInt(temp, 16);
        double v = voltage;
        v = v / 100;
        Statistics.setCurrentVoltage(v);

        temp = request[14];
        int battTemp1 = (short) Integer.parseInt(temp, 16);
        temp = request[15];
        int battTemp2 = (short) Integer.parseInt(temp, 16);


        int maxBattTemp = Math.max(battTemp1, battTemp2) - 20;
        Statistics.setBatteryTemperature(maxBattTemp);

        return c + " A";
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
