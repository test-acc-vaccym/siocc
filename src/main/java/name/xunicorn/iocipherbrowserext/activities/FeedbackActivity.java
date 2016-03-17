package name.xunicorn.iocipherbrowserext.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.NewTicketDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FeedbackActivity extends AppCompatActivity implements ListView.OnItemClickListener, View.OnClickListener, NewTicketDialog.OnNewTicketListener {
    public static final String TAG = "FeedbackActivity";

    public static final String TAG_ID        = "id";
    public static final String TAG_PARENT_ID = "parent_id";
    public static final String TAG_IS_ADMIN  = "is_admin";
    public static final String TAG_IS_CLOSED = "is_closed";
    public static final String TAG_DATE      = "date";
    public static final String TAG_USERNAME  = "username";
    public static final String TAG_TOPIC     = "topic";
    public static final String TAG_MESSAGE   = "message";

    public static final String TAG_FEEDBACK  = "Feedback";
    public static final String TAG_SUCCESS   = "success";

    private ListView mListView;
    private List<Ticket> tickets;

    Boolean isExpandable = false;

    ProgressBar feedbackIsLoading;

    Button createTicketUp;
    Button createTicketBt;

    Handler mHandler;

    Integer parent_id = 0;
    String url = Configs.URL_GET_TOP_TICKETS;

    RequestQueue queue;

    public android.support.v7.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Set the adapter
        mListView = (ListView) findViewById(android.R.id.list);

        feedbackIsLoading = (ProgressBar) findViewById(R.id.feedbackIsLoading);

        createTicketBt = (Button) findViewById(R.id.feedbackCreteTicketBottom);
        createTicketUp = (Button) findViewById(R.id.feedbackCreteTicketUpper);

        toolbar        = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);



        createTicketUp.setOnClickListener(this);
        createTicketBt.setOnClickListener(this);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                feedbackIsLoading.setVisibility(View.GONE);
                refreshAdapter();
                return true;
            }
        });

        queue = Volley.newRequestQueue(getBaseContext());

        requestGetTickets(Configs.URL_GET_TOP_TICKETS);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "[onItemClick] position: " + position);
        //Log.d(TAG, "[onItemClick] ticket: " + tickets.get(position));

        if(!isExpandable) {
            parent_id  = tickets.get(position).id;

            //Log.d(TAG, "[onItemClick] new url: " + url);

            isExpandable = true;

            changeToolbarTopic(tickets.get(position).topic.toUpperCase());

            requestGetTickets(Configs.URL_GET_TICKETS.replace("#", parent_id.toString()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "[onCreateOptionsMenu]");

        getMenuInflater().inflate(R.menu.menu_feedback, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                Log.i(TAG, "[onOptionsItemSelected] android.R.id.home");

                toolbar.setTitle(getResources().getString(R.string.title_activity_feedback));
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);

                isExpandable = false;
                createTicketUp.setVisibility(View.VISIBLE);
                createTicketUp.setText(getResources().getText(R.string.btnTicketCreate));
                createTicketBt.setText(getResources().getText(R.string.btnTicketCreate));

                requestGetTickets(Configs.URL_GET_TOP_TICKETS);
                break;

            case R.id.menuBackToProgram:
                Log.i(TAG, "[onOptionsItemSelected] R.id.menuBackToProgram");
                startActivity(new Intent(getBaseContext(), MainActivity.class));
                finish();
                break;

            case R.id.menuRefresh:
                Log.i(TAG, "[onOptionsItemSelected] R.id.menuRefresh");
                requestGetTickets(this.url);
                break;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "[onClick]");
        NewTicketDialog newTicket;

        if(isExpandable) {
            newTicket = NewTicketDialog.newInstance(tickets.get(0).id, tickets.get(0).topic);
        } else {
            newTicket = NewTicketDialog.newInstance();
        }

        newTicket.show(getFragmentManager(), "NewTicketDialog");
    }

    @Override
    public void onCreateNewTicket(Ticket ticket) {
        Log.i(TAG, "[onCreateNewTicket] ticket: " + ticket);

        requestCreateTicket(Configs.URL_CREATE_TICKET, ticket);
    }

    protected void changeToolbarTopic(String topic) {
        createTicketUp.setText(getResources().getText(R.string.btnTicketResponse));
        createTicketBt.setText(getResources().getText(R.string.btnTicketResponse));
        createTicketUp.setVisibility(View.GONE);

        toolbar.setTitle(topic);

        // Enable the Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void refreshAdapter() {
        Log.i(TAG, "[refreshAdapter]");

        //Log.d(TAG, "[refreshAdapter] tickets: " + tickets);

        mListView.setAdapter(new TicketsAdapter(tickets));
    }

    protected void requestGetTickets(String url) {
        Log.i(TAG, "[requestGetTickets] url: " + url);

        this.url = url;

        feedbackIsLoading.setVisibility(View.VISIBLE);

        tickets = new ArrayList<Ticket>();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d(TAG, "[requestGetTickets][StringRequest][onResponse] response: " + response);

                        try {
                            JSONArray entries = new JSONArray(response);

                            for (int i = 0; i < entries.length(); i++) {
                                JSONObject obj = entries.getJSONObject(i);

                                Integer id        = obj.getInt(TAG_ID);
                                Integer parent_id = obj.getInt(TAG_PARENT_ID);
                                Boolean is_admin  = obj.getInt(TAG_IS_ADMIN) == 1;
                                Boolean is_closed = obj.getInt(TAG_IS_CLOSED) == 1;
                                Date date         = new Date(obj.getLong(TAG_DATE) * 1000);
                                String username   = obj.getString(TAG_USERNAME);
                                String topic      = obj.getString(TAG_TOPIC);
                                String message    = obj.getString(TAG_MESSAGE);

                                tickets.add(new Ticket(
                                        date,
                                        id,
                                        parent_id,
                                        is_admin,
                                        is_closed,
                                        username,
                                        topic,
                                        message
                                ));
                            }

                        } catch (Exception ex) {
                            Log.e(TAG, "[requestGetTickets][StringRequest][onResponse] error, invalid JSON: " + response, ex);

                            Toast.makeText(getBaseContext(), "Invalid JSON from server", Toast.LENGTH_SHORT).show();
                        }

                        mHandler.sendEmptyMessage(0);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "[requestGetTickets][StringRequest][onErrorResponse] error");

                mHandler.sendEmptyMessage(0);
            }
        }
        );

        queue.add(stringRequest);
    }

    protected void requestCreateTicket(String url, final Ticket ticket) {
        Log.i(TAG, "[requestCreateTicket] url: " + url + ", ticket: " + ticket);

        JSONObject json = new JSONObject();

        try {
            json.put(TAG_FEEDBACK, ticket.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "[requestCreateTicket] json: " + json.toString());

        JsonObjectRequest request = new JsonObjectRequest(url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "[requestCreateTicket][JsonObjectRequest][onResponse] response: " + response.toString());

                        try {
                            if (!response.getBoolean(TAG_SUCCESS)) {
                                return;
                            }

                            String url = Configs.URL_GET_TICKETS;

                            if (ticket.parent_id == 0) {
                                Integer id = response.getInt(TAG_ID);
                                url = url.replace("#", id.toString());
                            } else {
                                url = url.replace("#", ticket.parent_id.toString());
                            }

                            changeToolbarTopic(ticket.topic);
                            requestGetTickets(url);
                        } catch (JSONException ex) {
                            Log.e(TAG, "[requestCreateTicket][JsonObjectRequest][onResponse] error: " + ex.getMessage(), ex);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "[requestCreateTicket][JsonObjectRequest][onErrorResponse] error: " + error.getMessage(), error);
                    }
                });

        Log.d(TAG, "[requestCreateTicket] request method: " + request.getMethod());

        queue.add(request);
    }

    class TicketsAdapter extends ArrayAdapter<Ticket> {

        SimpleDateFormat format;

        public TicketsAdapter(List<Ticket> tickets) {
            super(getBaseContext(), R.layout.activity_feedback_row, tickets);

            format = new SimpleDateFormat("y-MM-dd HH:mm:ss");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null) {
                convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.activity_feedback_row, null);
            }

            TextView topic    = (TextView) convertView.findViewById(R.id.feedbackTopic);
            TextView username = (TextView) convertView.findViewById(R.id.feedbackUsername);
            TextView date     = (TextView) convertView.findViewById(R.id.feedbackDate);
            TextView message  = (TextView) convertView.findViewById(R.id.feedbackMessage);

            final Ticket ticket     = this.getItem(position);

            //Log.d("TicketsAdapter", "[getView] position: " + position + " ticket: " + ticket);

            String usernameStr = ticket.parent_id == 0 ? "TS: " : "";

            usernameStr += ticket.username;

            topic.setText(ticket.topic);
            username.setText(usernameStr);
            date.setText(format.format(ticket.date));

            //if(ticket.is_admin) {
                topic.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.cyan));
                username.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.cyan));
            //}

            if(isExpandable) {
                topic.setVisibility(View.GONE);
                message.setVisibility(View.VISIBLE);
                message.setText(ticket.message);
            }

            return convertView;
        }
    }

    public static class Ticket {
        public final Integer id;
        public final Integer parent_id;
        public final Boolean is_admin;
        public final Boolean is_closed;
        public final Date date;
        public final String username;
        public final String topic;
        public final String message;

        public Ticket(Date date, Integer id, Integer parent_id, Boolean is_admin, Boolean is_closed, String username, String topic, String message) {
            this.date = date;
            this.id = id;
            this.parent_id = parent_id;
            this.is_admin = is_admin;
            this.is_closed = is_closed;
            this.username = username;
            this.topic = topic;
            this.message = message;
        }

        public Ticket(String message, Integer parent_id, String topic, String username) {
            this.message   = message;
            this.parent_id = parent_id;
            this.topic     = topic;
            this.username  = username;

            id        = null;
            is_admin  = false;
            is_closed = false;
            date      = new Date();
        }

        public JSONObject toJSON() {
            JSONObject obj = new JSONObject();

            try{

                obj.put(TAG_ID, id);
                obj.put(TAG_PARENT_ID, parent_id);
                obj.put(TAG_IS_ADMIN, is_admin);
                obj.put(TAG_IS_CLOSED, is_closed);
                obj.put(TAG_DATE, date.getTime() / 1000);
                obj.put(TAG_USERNAME, username);
                obj.put(TAG_TOPIC, topic);
                obj.put(TAG_MESSAGE, message);

                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString() {
            return "Ticket{" +
                    "date=" + date +
                    ", id=" + id +
                    ", parent_id=" + parent_id +
                    ", is_admin=" + is_admin +
                    ", is_closed=" + is_closed +
                    ", username='" + username + '\'' +
                    ", title='" + topic + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
