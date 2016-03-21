package name.xunicorn.iocipherbrowserext.fragments.dialogs;


import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import name.xunicorn.iocipherbrowserext.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class PasswordDialog extends DialogFragment {
    public final String TAG = "PasswordDialog";

    public interface OnSetContainerPasswordListener {
        void onSetContainerPassword(char[] password);
    }

    EditText editPassword;
    CheckBox chbShowPassword;
    Button   btnOk;

    OnSetContainerPasswordListener listener;

    public PasswordDialog() {
        // Required empty public constructor
    }

    public static PasswordDialog newInstance() {
        return new PasswordDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getDialog().setTitle(R.string.txtPasswordDialogTitle);

        View v = inflater.inflate(R.layout.dialog_password, null);

        editPassword    = (EditText) v.findViewById(R.id.editPassword);
        chbShowPassword = (CheckBox) v.findViewById(R.id.checkPasswordShow);
        btnOk           = (Button) v.findViewById(R.id.btnOk);

        editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        chbShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "[setOnCheckedChangeListener][onCheckedChanged] isChecked: " + isChecked);

                if(isChecked) {
                    editPassword.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSetContainerPassword(editPassword.getText().toString().toCharArray());
                dismiss();
            }
        });

        return v;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (OnSetContainerPasswordListener)activity;
    }
}
