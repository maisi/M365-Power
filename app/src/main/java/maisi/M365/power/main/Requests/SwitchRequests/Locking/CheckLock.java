package maisi.M365.power.main.Requests.SwitchRequests.Locking;

import android.util.Log;

import java.util.Arrays;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class CheckLock implements IRequest {
    private static int delay = 100;
    private final String requestBit = "B2";
    private final RequestType requestType = RequestType.LOCK;
    private long startTime;

    public CheckLock() {
        this.startTime = System.currentTimeMillis() + delay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public String getRequestString() {
        return new NbMessage()
                .setDirection(NbCommands.MASTER_TO_M365)
                .setRW(NbCommands.READ)
                .setPosition(0xB2)
                .setPayload(0x02)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        if(request[6].equals("02")){
            Statistics.setScooterLocked(true);
        }
        else{
            Statistics.setScooterLocked(false);
        }
        return "";
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
