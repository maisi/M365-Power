package maisi.M365.power.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import maisi.M365.power.main.LogDTO;
import maisi.M365.power.main.Statistics;

public class LogWriter {

    private final char DEFAULT_SEPARATOR = ',';
    private Context context;
    private List<LogDTO> dtoList;
    private StringBuilder allBuilder;
    private String path;

    public LogWriter(Context context) {
        this.context = context;
        this.dtoList = new ArrayList<>();
        allBuilder = new StringBuilder();
    }

    public void writeLog(boolean all) {
        dtoList.add(Statistics.getLogStats());
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");

        Date resultdate = new Date();
        sdf.format(resultdate);
        StringBuilder sb = new StringBuilder();
        sb.append(writeLine(dtoList.get(0).getHeader()));
        if (allBuilder.length() == 0) {
            allBuilder.append(writeLine(dtoList.get(0).getHeader()));
        }
        List<LogDTO> sublist;
        if (!all && dtoList.size() >= 30) {
            sublist = dtoList;
            for (LogDTO e : sublist) {
                if (e.getAverageCurrent() != 0.0) {
                    String temp = writeLine(e.toList());
                    sb.append(temp);
                    allBuilder.append(temp);
                }
            }
            writeFileOnInternalStorage("LOG " + sdf.format(resultdate) + ".csv", sb.toString());
            dtoList.clear();
        } else if(all){
            Log.d("Log", "write ALL");
            writeFileOnInternalStorage("LOG " + sdf.format(resultdate) + "ALL.csv", allBuilder.toString());
        }


    }


    private void writeFileOnInternalStorage(String sFileName, String sBody) {
        //Log.d("CSV","Write to file");
        File file = new File(Environment.getExternalStorageDirectory(), "M365Log");
        this.path = file.getAbsolutePath();
        if (!file.exists()) {
            file.mkdir();
        }

        try {
            File gpxfile = new File(file, sFileName);
            //Log.d("CSV","Path: "+file.getAbsolutePath());
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    private String writeLine(List<String> values) {
        return writeLine(values, DEFAULT_SEPARATOR, ' ');
    }

    //https://tools.ietf.org/html/rfc4180
    private String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    private String writeLine(List<String> values, char separators, char customQuote) {

        boolean first = true;

        //default customQuote is empty

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
        sb.append("\n");

        return sb.toString();

    }


    public String getPath() {
        return path;
    }
}
