package name.xunicorn.iocipherbrowserext.fragments.dialogs;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import name.xunicorn.iocipherbrowserext.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class NewNameDialog extends DialogFragment {
    final String TAG = "NewNameDialog";

    static final String BUNDLE_TYPE   = "type";
    static final String BUNDLE_ACTION = "action";
    static final String BUNDLE_OLD_NAME = "old_name";

    public enum TYPE { CONTAINER, DIR, FILE }
    public enum ACTION { NEW, RENAME }

    public interface onSetNewNameListener {
        void setNewName(String oldName, String newName, TYPE type, ACTION action);
    }

    onSetNewNameListener listener;

    TYPE type;
    ACTION action;
    String oldName;

    EditText edit;

    Button btnOk;

    public NewNameDialog() {
        // Required empty public constructor
    }


    public static NewNameDialog newInstance(TYPE type, ACTION action, String oldName) {
        Bundle bundle = new Bundle();

        bundle.putString(BUNDLE_TYPE, type.toString());
        bundle.putString(BUNDLE_ACTION, action.toString());
        bundle.putString(BUNDLE_OLD_NAME, oldName);


        NewNameDialog dialog = new NewNameDialog();

        dialog.setArguments(bundle);

        return dialog;
    }

    public static NewNameDialog newInstance(TYPE type, ACTION action) {
        Bundle bundle = new Bundle();

        bundle.putString(BUNDLE_TYPE, type.toString());
        bundle.putString(BUNDLE_ACTION, action.toString());

        NewNameDialog dialog = new NewNameDialog();

        dialog.setArguments(bundle);

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "[onCreateView]");

        if(getArguments() == null) {
            Log.e(TAG, "[onCreateView] unknown dialog type");
            dismiss();
        }

        type    = TYPE.valueOf(getArguments().getString(BUNDLE_TYPE));
        action  = ACTION.valueOf(getArguments().getString(BUNDLE_ACTION));
        oldName = getArguments().getString(BUNDLE_OLD_NAME, "undef");

        if(!TextUtils.isEmpty(oldName)) {
            if(oldName.contains("/")) {
                String[] segments = oldName.split("/");

                oldName = segments[segments.length - 1];
            }
        }

        getDialog().setTitle("New " + type.toString().toLowerCase() + " name");

        View v = inflater.inflate(R.layout.dialog_new_name, null);

        edit  = (EditText) v.findViewById(R.id.newDirName);
        btnOk = (Button) v.findViewById(R.id.btnCreate);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "[onCreateView][setOnClickListener]");

                String new_name = edit.getText().toString();

                listener.setNewName(oldName, new_name, type, action);

                //hideKeyboard();

                dismiss();
            }
        });

        btnOk.setText(action == ACTION.RENAME ? "Rename" : "Create");

        v.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hideKeyboard();
                dismiss();
            }
        });

        if(action == ACTION.RENAME) {
            edit.setText(oldName);
            edit.setSelection(0, oldName.lastIndexOf(".") != -1 ? (oldName.lastIndexOf(".")) : oldName.length());
/*
            if(edit.requestFocus()) {
                showKeyboard();
            }
*/

        }

        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                edit.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
        edit.requestFocus();

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (onSetNewNameListener)activity;
    }

    @Override
    public void onStart() {
        super.onStart();
/*
        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
*/
    }

    protected void showKeyboard() {
        Log.i(TAG, "[showKeyboard]");
        InputMethodManager imm = (InputMethodManager) ((Activity)listener).getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
    }

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)((Activity)listener).getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }

}
