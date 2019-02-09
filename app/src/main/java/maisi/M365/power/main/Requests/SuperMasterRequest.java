package maisi.M365.power.main.Requests;

import android.util.Log;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class SuperMasterRequest implements IRequest {

    private final String requestBit = "B0";
    private final RequestType requestType = RequestType.SUPERMASTER;

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public String getRequestString() {
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.READ)
                .setPosition(0xb0)
                .setPayload(0x20)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }


    @Override
    public String handleResponse(String[] request) {

        String temp = request[17] + request[16];
        int speed = (short) Integer.parseInt(temp, 16);
        double v = speed;
        v = v / 1000;
        //Log.d("Speed","speed:"+v);
        Statistics.setSpeed(v);
        v = Statistics.round(v, 1);


        double dist = getDistTotal(request);
        Log.i("TOtal distance is: ", "v: " + dist);
        Statistics.setDistanceTravelled(dist);

        temp = request[29] + request[28];
        int temperature = (short) Integer.parseInt(temp, 16);
        double temperature1 = temperature;
        temperature1 = temperature1 / 10;
        Statistics.setMotorTemperature(temperature1);
        //Log.d("SuperDistance","Distance:"+dist);

        return v + "";

    }

    private double getDistTotal(String[] request) {
        String temp;
        temp = request[23] + request[22] +  request[21] + request[20];
        int distance = Integer.parseInt(temp, 16);

        double dist = distance;
        dist = dist / 1000;

        return dist;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
