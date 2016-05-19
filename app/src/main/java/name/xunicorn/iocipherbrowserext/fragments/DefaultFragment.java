package name.xunicorn.iocipherbrowserext.fragments;


import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public class DefaultFragment extends Fragment {
    final String TAG = "DefaultFragment";

    MainActivity activity;

    Button btnCreate;
    Button btnPlug;

    public DefaultFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "[onCreateView]");

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_actions, container, false);

        btnCreate = (Button) v.findViewById(R.id.btnCreate);
        btnPlug   = (Button) v.findViewById(R.id.btnPlug);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "[setOnClickListener][onClick] commandCreateContainer");

                ((MainActivity)getActivity()).commandCreateContainer();
            }
        });

        btnPlug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "[setOnClickListener][onClick] commandPlugContainer");

                ((MainActivity)getActivity()).commandPlugContainer();
            }
        });

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.i(TAG, "[onAttach]");

        if(context instanceof MainActivity) {
            activity = (MainActivity)context;
        }
    }

}
