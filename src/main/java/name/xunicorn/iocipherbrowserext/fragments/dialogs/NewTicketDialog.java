package name.xunicorn.iocipherbrowserext.fragments.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.EditText;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.FeedbackActivity;


public class NewTicketDialog extends DialogFragment implements View.OnClickListener {
    public static final String TAG = "NewTicketDialog";

    public static final String BUNDLE_PARENT_ID = "parent_id";
    public static final String BUNDLE_PARENT_TOPIC = "parent_topic";

    private OnNewTicketListener mListener;


    EditText title;
    EditText user;
    EditText message;

    Button btnCreate;
    Button btnCancel;

    Integer parent_id = 0;
    String parent_topic;

    Boolean is_response = false;

    public NewTicketDialog() {
        // Required empty public constructor
    }

    public static NewTicketDialog newInstance(Integer parent_id, String parent_topic) {
        Bundle bundle = new Bundle();

        bundle.putString(BUNDLE_PARENT_TOPIC, parent_topic);
        bundle.putInt(BUNDLE_PARENT_ID, parent_id);

        NewTicketDialog ticketFragment = new NewTicketDialog();

        ticketFragment.setArguments(bundle);

        return ticketFragment;
    }

    public static NewTicketDialog newInstance() {
        return new NewTicketDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_new_ticket, container, false);

        title = (EditText) v.findViewById(R.id.newTicketTitle);
        user  = (EditText) v.findViewById(R.id.newTicketUsername);
        message = (EditText) v.findViewById(R.id.newTicketMessage);

        btnCreate = (Button) v.findViewById(R.id.btnCreate);
        btnCancel = (Button) v.findViewById(R.id.btnCancel);

        btnCreate.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        if(getArguments() != null) {
            parent_topic = getArguments().getString(BUNDLE_PARENT_TOPIC);
            parent_id    = getArguments().getInt(BUNDLE_PARENT_ID);

            title.setText(parent_topic);
            title.setEnabled(false);

            is_response = true;
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnNewTicketListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnNewTicketListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnCancel) {
            dismiss();
        }

        String title    = this.title.getText().toString();
        String username = this.user.getText().toString();
        String message  = this.message.getText().toString();

        FeedbackActivity.Ticket ticket = new FeedbackActivity.Ticket(message, parent_id, title, username);

        mListener.onCreateNewTicket(ticket);

        dismiss();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnNewTicketListener {
        void onCreateNewTicket(FeedbackActivity.Ticket ticket);
    }

}
