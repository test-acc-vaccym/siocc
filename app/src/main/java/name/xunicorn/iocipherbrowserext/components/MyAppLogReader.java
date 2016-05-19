package name.xunicorn.iocipherbrowserext.components;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MyAppLogReader {
    private static final String TAG = MyAppLogReader.class.getCanonicalName();
    private static final String processId = Integer.toString(android.os.Process
            .myPid());

    public static StringBuilder getLog() {
        Log.i(TAG, "[getLog] process ID: " + processId);

        StringBuilder builder = new StringBuilder();

        try {
            String[] command = new String[] { "logcat", "-d","-v", "threadtime" };

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(processId)) {
                    //Log.i(TAG, "[getLog] line: " + line);
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                    //Code here
                }
            }

            bufferedReader.close();

        } catch (IOException ex) {
            Log.e(TAG, "getLog failed", ex);
        }

        Log.i(TAG, "[getLog] logs count: " + builder.capacity());

        return builder;
    }

    public static void saveLogFile() {
        Log.i(TAG, "[saveLogFile]");

        try {
            String[] command = new String[] { "logcat", "-d","-f", Environment.getExternalStorageDirectory().getAbsolutePath() + "/logcat.log"};

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                Log.i(TAG, "[saveLogFile] line: " + line);
            }

        } catch (IOException ex) {
            Log.e(TAG, "getLog failed", ex);
        }
    }
}
