package maisi.M365.power.main.Requests;

import maisi.M365.power.main.IRequest;
import maisi.M365.power.main.RequestType;
import maisi.M365.power.main.Statistics;
import maisi.M365.power.util.NbCommands;
import maisi.M365.power.util.NbMessage;

public class AmpereRequest implements IRequest {
    private static int delay = 100;
    private final String requestBit = "33";
    private final RequestType requestType = RequestType.AMEPERE;
    private long startTime;

    public AmpereRequest() {
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
                .setPosition(0x33)
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
        int amps = (short) Integer.parseInt(temp, 16);
        double c = amps;
        c = c / 100;
        Statistics.setCurrentAmpere(c);
        return "" + c + " A";
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }
}
