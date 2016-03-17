package name.xunicorn.iocipherbrowserext.activities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class UpdateActivity
        extends AppCompatActivity
        implements
            View.OnClickListener {

    public final static String TAG = "UpdateActivity";

    private final String TAG_LATEST_VERSION = "latest";

    private final int HANDLER_GET_VERSION = 1;
    private final int HANDLER_DOWNLOAD    = 2;
    private final int HANDLER_ERROR       = 3;

    protected String currentVersion;
    protected String latestVersion;

    String error;

    private long enqueue;
    private DownloadManager dm;

    ProgressBar pbProgress;
    ProgressBar pbDownload;

    TextView tvCurrentVersion;
    TextView tvLatestVersion;
    TextView tvUpdatingHint;

    Button btnUpdate;

    Handler mHandler;

    BroadcastReceiver receiverComplete;

    android.support.v7.widget.Toolbar toolbar;

    int totalPercents = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersion = pInfo.versionName;
        } catch(Exception e) {
            Log.e(TAG, "[onCreate] error: " + e.getMessage(), e);
        }

        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        pbProgress = (ProgressBar)findViewById(R.id.progressBar);
        pbDownload = (ProgressBar)findViewById(R.id.progressBarDownload);


        tvCurrentVersion = (TextView)findViewById(R.id.tvCurrentVersion);
        tvLatestVersion  = (TextView)findViewById(R.id.tvLatestVersion);
        tvUpdatingHint   = (TextView)findViewById(R.id.tvUpdateHint);

        btnUpdate        = (Button)findViewById(R.id.btnUpdate);

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch(message.what) {
                    case HANDLER_GET_VERSION:
                        pbProgress.setVisibility(View.GONE);
                        tvLatestVersion.setText(latestVersion);

                        if(latestVersion.equals(currentVersion)) {
                            tvUpdatingHint.setText(R.string.txtUpdatingHintOk);
                            tvLatestVersion.setTextColor(getResources().getColor(android.R.color.holo_green_light));

                            btnUpdate.setVisibility(View.GONE);
                        } else {
                            tvUpdatingHint.setText(R.string.txtUpdatingHintFail);
                            tvLatestVersion.setTextColor(getResources().getColor(android.R.color.holo_red_light));

                            btnUpdate.setVisibility(View.VISIBLE);
                        }

                        break;

                    case HANDLER_DOWNLOAD:
                        pbDownload.setProgress(totalPercents);

                        if(totalPercents == 100) {
                            Log.i(TAG, "[onCreate][handleMessage][HANDLER_DOWNLOAD] download is over");

                            pbDownload.setVisibility(View.GONE);
                            pbProgress.setVisibility(View.GONE);
                        }
                        break;

                    case HANDLER_ERROR:
                        pbProgress.setVisibility(View.GONE);
                        pbDownload.setVisibility(View.GONE);
                        tvUpdatingHint.setText(error);
                        tvUpdatingHint.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                        break;

                }

                return true;
            }
        });

        tvCurrentVersion.setText(currentVersion);

        btnUpdate.setOnClickListener(this);

        receiverComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    Cursor c = dm.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c
                                .getInt(columnIndex)) {

                            Toast.makeText(getBaseContext(), "Download file was successfull", Toast.LENGTH_LONG).show();
                            /*
                            ImageView view = (ImageView) findViewById(R.id.imageView1);
                            String uriString = c
                                    .getString(c
                                            .getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            view.setImageURI(Uri.parse(uriString));
                            */

                            //pbDownload.setVisibility(View.GONE);


                            installApk();
                        }
                    }
                }
            }
        };

        registerReceiver(receiverComplete, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        getLatestVersion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiverComplete);
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "[onClick]");

        pbProgress.setVisibility(View.VISIBLE);

        //pbDownload.setVisibility(View.VISIBLE);
        pbDownload.setMax(100);
        pbDownload.setProgress(0);

        if(!isApkLatestVersion()) {
            Toast.makeText(getBaseContext(), R.string.txtUpdateFileStartDownload, Toast.LENGTH_LONG).show();
            downloadLatestVersion();
        } else {
            Toast.makeText(getBaseContext(), R.string.txtUpdateFileAlreadyDownloaded, Toast.LENGTH_LONG).show();
            installApk();
        }

    }

    //region Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_update, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.menuBackToProgram:
                finish();
                break;

            case R.id.menuRefresh:
                getLatestVersion();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region APK functions
    protected File getSavePath() {
        Log.i(TAG, "[getSavePath]");

        File path =  new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name) + "/latest_version");

        if(!path.exists()) {
            path.mkdirs();
        }

        return path;
    }

    protected File getSaveFile() {
        Log.i(TAG, "[getSaveFile]");

        return new File(getSavePath(), getApkName());
    }

    protected String getApkName() {
        Log.i(TAG, "[getApkName]");

        return getBaseContext().getString(R.string.app_name) + ".apk";
    }

    protected void installApk() {
        Log.i(TAG, "[installApk]");

        pbProgress.setVisibility(View.GONE);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(getSaveFile()), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected boolean isApkLatestVersion() {
        Log.i(TAG, "[isApkLatestVersion]");

        File file = new File(getSavePath(), getApkName());

        if(file.exists()) {
            PackageInfo infoFile = getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);

            Log.i(TAG, "[isApkLatestVersion] apk version: " + infoFile.versionCode + " | version name: " + infoFile.versionName);

            return infoFile.versionName.equals(latestVersion);
        }

        return false;
    }
    //endregion

    //region HTTP requests
    protected void getLatestVersion() {
        Log.i(TAG, "[getLatestVersion]");

        /*
        t = new Thread(new ShadowRun(false));
        t.start();
        */

        Log.i(TAG, "[getLatestVersion]");

        pbProgress.setVisibility(View.VISIBLE);

        RequestQueue queue = Volley.newRequestQueue(getBaseContext());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, Configs.URL_GET_LATEST_VERSION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "[getLatestVersion][StringRequest][onResponse] response: " + response);

                        try {
                            JSONObject obj = new JSONObject(response);

                            latestVersion = obj.getString(TAG_LATEST_VERSION);

                            mHandler.sendEmptyMessage(HANDLER_GET_VERSION);

                        } catch (JSONException e) {
                            Log.e(TAG, "[getLatestVersion][StringRequest][onResponse] error: " + e.getMessage(), e);

                            error = e.getMessage();
                            mHandler.sendEmptyMessage(HANDLER_ERROR);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "[getLatestVersion][StringRequest][onErrorResponse] error", error);

                        UpdateActivity.this.error = error.getMessage();
                        mHandler.sendEmptyMessage(HANDLER_ERROR);
                    }
                }
        );

        queue.add(stringRequest);
    }

    protected void downloadLatestVersion() {
        Log.i(TAG, "[downloadLatestVersion]");
        File saveFile = getSaveFile();

        if(saveFile.exists()) {
            saveFile.delete();
        }

        Log.d(TAG, "[downloadLatestVersion] save uri: " + Uri.fromFile(saveFile));

        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(Configs.URL_DOWNLOAD_LATEST_VERSION));

        request.setDescription(getString(R.string.dmUpdateDescription))
                .setTitle(getString(R.string.dmUpdateTitle))
                .setDestinationUri(Uri.fromFile(saveFile));

        enqueue = dm.enqueue(request);

        //t = new Thread(new ShadowRun(true));
        //t.start();
    }
    //endregion

}
