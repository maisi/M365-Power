package maisi.M365.power.main;

import android.widget.TextView;

import java.util.List;
import java.util.concurrent.Delayed;

public interface IRequest extends Delayed {
    
    public int getDelay();
    
    public String getRequestString();

    //get RequestBit to identify
    public String getRequestBit();

    //expected to update the textviews and the statistic class
    public String handleResponse(String[] request);

    public RequestType getRequestType();

}
