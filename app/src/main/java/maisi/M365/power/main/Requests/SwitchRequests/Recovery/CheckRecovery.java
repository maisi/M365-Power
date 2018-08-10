package maisi.M365.power.main.Requests.SwitchRequests.Recovery;

import android.util.Log;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class CheckRecovery implements IRequest {
    private static int delay = 100;
    private final String requestBit = "7B";
    private final RequestType requestType = RequestType.RECOVERY;
    private long startTime;

    public CheckRecovery() {
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
                .setPosition(0x7B)
                .setPayload(0x02)
                .build();
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        if(request[6].equals("00")){
            //Log.d("REC","weak");
            Statistics.setRecoveryMode(0);
        }
        else if(request[6].equals("01")){
            //Log.d("REC","medium");
            Statistics.setRecoveryMode(1);
        }
        else if(request[6].equals("02")){
            //Log.d("REC","strong");
            Statistics.setRecoveryMode(2);
        }
        return request[6];
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
