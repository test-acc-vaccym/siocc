package name.xunicorn.iocipherbrowserext.components;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.CrashActivity;
import name.xunicorn.iocipherbrowserext.activities.FeedbackActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class AppClass extends Application {
    private static final String TAG = "AppClass";

    private static AppClass singleton;

    RequestQueue queue;

    private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e(TAG, "[UncaughtExceptionHandler][uncaughtException] APP CRASH: " + ex.getMessage(), ex);

            //sendCrashTicket(ex);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if(singleton == null) {
            singleton = this;
        }

        queue = Volley.newRequestQueue(this);

        Thread.setDefaultUncaughtExceptionHandler(handler);
/*
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                Log.e(TAG, "[setDefaultUncaughtExceptionHandler][uncaughtException] APP CRASH: " + paramThrowable.getMessage(), paramThrowable);

                //Bundle extras = new Bundle();
                //extras.putSerializable("exception", paramThrowable);

                Intent intent = new Intent(singleton, CrashActivity.class);
                //intent.putExtras(extras);
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                //System.exit(0);

                //crush(paramThrowable);
            }

            protected void crush(Throwable exception) {
                FeedbackActivity.Ticket ticket = FeedbackActivity.Ticket.createCrashTicket(exception);

                //sendCrashTicket(ticket);

                Intent intent = new Intent(getApplicationContext(), CrashActivity.class);
                intent.putExtra("exception", exception);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                System.exit(0);
            }

            protected void sendCrashTicket(final FeedbackActivity.Ticket ticket) {
                String url = Configs.URL_CREATE_TICKET;

                Log.i(TAG, "[sendCrashTicket] url: " + url + ", ticket: " + ticket);

                //RequestQueue queue = Volley.newRequestQueue(getBaseContext());

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
        });
        */
    }

    public static AppClass instance() {
        return singleton;
    }

    protected void sendCrashTicket(Throwable exception) {
        FeedbackActivity.Ticket ticket = FeedbackActivity.Ticket.createCrashTicket(exception);

        String url = Configs.URL_CREATE_TICKET;

        Log.i(TAG, "[sendCrashTicket] url: " + url + ", ticket: " + ticket);

        //RequestQueue queue = Volley.newRequestQueue(getBaseContext());

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
                        Log.i(TAG, "[sendCrashTicket][onResponse] response: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "[sendCrashTicket][onErrorResponse] error: " + error.getMessage(), error);

                    }
                });

        Log.d(TAG, "[sendCrashTicket] request method: " + request.getMethod());

        queue.add(request);

        Log.i(TAG, "[sendCrashTicket] crash report was sent");
    }

}
