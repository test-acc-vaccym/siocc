package name.xunicorn.iocipherbrowserext.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import org.json.JSONException;
import org.json.JSONObject;

public class CrashActivity extends AppCompatActivity {
    static final String TAG = "CrashActivity";

    private final String SINGLE_LINE_SEP = "\n";
    private final String DOUBLE_LINE_SEP = "\n\n";

    public android.support.v7.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        toolbar  = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        Log.i(TAG, "[onCreate]");

        //Exception ex = (Exception)getIntent().getSerializableExtra("exception");

        showInfo(null);

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.txtAppCrashedTitle)
                .setMessage(R.string.txtAppCrashedMessage)
                .setNeutralButton(R.string.btnOk, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        System.exit(0);
                    }
                })
                .show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_crash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showInfo(Throwable paramThrowable) {
        final StringBuffer report = new StringBuffer();
        final String lineSeperator = "-------------------------------\n\n";

        if(paramThrowable != null) {
            StackTraceElement[] arr = paramThrowable.getStackTrace();
            report.append(paramThrowable.toString());

            report.append(DOUBLE_LINE_SEP);
            report.append("--------- Stack trace ---------\n\n");
            for (int i = 0; i < arr.length; i++) {
                report.append("    ");
                report.append(arr[i].toString());
                report.append(SINGLE_LINE_SEP);
            }
            report.append(lineSeperator);
            // If the exception was thrown in a background thread inside
            // AsyncTask, then the actual exception can be found with getCause
            report.append("--------- Cause ---------\n\n");
            Throwable cause = paramThrowable.getCause();
            if (cause != null) {
                report.append(cause.toString());
                report.append(DOUBLE_LINE_SEP);
                arr = cause.getStackTrace();
                for (int i = 0; i < arr.length; i++) {
                    report.append("    ");
                    report.append(arr[i].toString());
                    report.append(SINGLE_LINE_SEP);
                }
            }
        }
        // Getting the Device brand,model and sdk verion details.
        report.append(lineSeperator);
        report.append("--------- Device ---------\n\n");
        report.append("Brand: ");
        report.append(Build.BRAND);
        report.append(SINGLE_LINE_SEP);
        report.append("Device: ");
        report.append(Build.DEVICE);
        report.append(SINGLE_LINE_SEP);
        report.append("Model: ");
        report.append(Build.MODEL);
        report.append(SINGLE_LINE_SEP);
        report.append("Id: ");
        report.append(Build.ID);
        report.append(SINGLE_LINE_SEP);
        report.append("Product: ");
        report.append(Build.PRODUCT);
        report.append(SINGLE_LINE_SEP);
        report.append(lineSeperator);
        report.append("--------- Firmware ---------\n\n");
        report.append("SDK: ");
        report.append(Build.VERSION.SDK);
        report.append(SINGLE_LINE_SEP);
        report.append("Release: ");
        report.append(Build.VERSION.RELEASE);
        report.append(SINGLE_LINE_SEP);
        report.append("Incremental: ");
        report.append(Build.VERSION.INCREMENTAL);
        report.append(SINGLE_LINE_SEP);
        report.append(lineSeperator);

        Log.e("Report ::", report.toString());
    }

    protected void sendCrashTicket(final FeedbackActivity.Ticket ticket) {
        String url = Configs.URL_CREATE_TICKET;

        Log.i(TAG, "[sendCrashTicket] url: " + url + ", ticket: " + ticket);

        RequestQueue queue = Volley.newRequestQueue(getBaseContext());

        JSONObject json = new JSONObject();

        try {
            json.put(FeedbackActivity.TAG_FEEDBACK, ticket.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "[sendCrashTicket] json: " + json.toString());

        JsonObjectRequest request = new JsonObjectRequest(url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "[setDefaultUncaughtExceptionHandler][sendCrashTicket][onResponse] response: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "[setDefaultUncaughtExceptionHandler][sendCrashTicket][onErrorResponse] error: " + error.getMessage(), error);

                    }
                });

        Log.d(TAG, "[sendCrashTicket] request method: " + request.getMethod());

        queue.add(request);

        Log.i(TAG, "[sendCrashTicket] crash report was sent");
    }
}
