package maisi.M365.power.main.Requests;

import android.util.Log;

import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.SpecialTextView;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class AmpereRequest implements IRequest {
    private static int delay = 100;
    private long startTime;
    private final String requestBit = "33";
    private final RequestType requestType = RequestType.AMEPERE;

    public AmpereRequest(){
        this.startTime = System.currentTimeMillis() + delay;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public String getRequestString() {
        String ctrlVersion = new NbMessage()
                .setDirection(NbCommands.MASTER_TO_BATTERY)
                .setRW(NbCommands.READ)
                .setPosition(0x33)
                .setPayload(0x02)
                .build();
        return ctrlVersion;
    }

    @Override
    public String getRequestBit() {
        return requestBit;
    }

    @Override
    public String handleResponse(String[] request) {
        String temp = request[7] + request[6];
        int amps = (short) Integer.parseInt(temp, 16);
        double c = amps;
        c = c / 100;
        Statistics.setCurrentAmpere(c);
        return ""+c+" A";
    }

    @Override
    public long getDelay(TimeUnit timeUnit) {
        long diff = startTime - System.currentTimeMillis();
        return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed delayed) {
        return (int) (this.startTime - ((AmpereRequest) delayed).startTime);
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
