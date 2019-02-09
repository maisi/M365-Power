package maisi.M365.power.main.srclk;

import android.util.Log;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScrLkRequest {
    private static final String SCR_LK_URL = "http://10.12.10.79/panel";
    private static final String TOKEN = "81b2998d8b99e16b46b131850b04cf5c13b8dee7";

    private static boolean WAS_ALREADY_EXECUTED = false;

    private final double dist;

    public ScrLkRequest(double dist) {
        this.dist = dist;
    }

    public void execute() {
        if (!WAS_ALREADY_EXECUTED) {
            WAS_ALREADY_EXECUTED = true;
            String response = this.postData();
            Log.i("ScrLk response: ", response);
        }
    }

    private String postData() {
        RequestBody formBody = new FormBody.Builder()
                .add("mileage", String.valueOf(this.dist))
                .add("token", TOKEN)
                .add("submit", "mileage")
                .build();
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(SCR_LK_URL)
                .post(formBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
