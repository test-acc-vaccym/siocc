package name.xunicorn.iocipherbrowserext.fragments.dialogs;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.util.Log;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import name.xunicorn.iocipherbrowserext.models.Containers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SelectContainerDialog extends DialogFragment {
    final String TAG = "SelectContainerDialog";

    final static String BUNDLE_ACTION = "";

    public interface OnSelectContainerPathListener {
        void onSelectContainerPath(SelectedContainer container);
    }

    public enum CONTAINER_PATH { INTERNAL, EXTERNAL, CUSTOM }
    public enum ACTION { CREATE, OPEN, DELETE }

    OnSelectContainerPathListener listener;

    RadioGroup radioGroup;
    Button btnCancel;
    Button btnOk;

    TextView hintCustomPath;
    EditText editCustomPath;

    LinearLayout customGroup;
    LinearLayout savedGroup;

    ListView lvSaved;

    List<String> names;

    ACTION action;

    private String containerFileName = Configs.containerFileName;

    //boolean isCreateNew = false;

    public SelectContainerDialog() {
        // Required empty public constructor
    }


    public static SelectContainerDialog newInstance(ACTION action) {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_ACTION, action.toString());

        SelectContainerDialog dialog = new SelectContainerDialog();
        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String path_external = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getString(R.string.app_name) + "/" + containerFileName;

        View v = inflater.inflate(R.layout.dialog_select_container, null);

        if(getArguments() == null) {
            Log.e(TAG, "[onCreateView] initialization error");
            dismiss();
        }

        action = ACTION.valueOf(getArguments().getString(BUNDLE_ACTION));

        //isCreateNew = action == ACTION.CREATE;

        switch (action) {
            case CREATE:
                getDialog().setTitle(R.string.txtSelectContainerCreate);
                break;

            case OPEN:
            case DELETE:
                getDialog().setTitle(R.string.txtSelectContainerSelect);
                break;
        }

        hintCustomPath = (TextView) v.findViewById(R.id.hintCustomPath);
        editCustomPath = (EditText) v.findViewById(R.id.editCustomPath);
        radioGroup     = (RadioGroup) v.findViewById(R.id.radioGroup);
        customGroup    = (LinearLayout) v.findViewById(R.id.customContainerGroup);
        savedGroup     = (LinearLayout) v.findViewById(R.id.savedContainersGroup);
        lvSaved        = (ListView) v.findViewById(R.id.lvSavedContainers);
        btnCancel      = (Button) v.findViewById(R.id.btnCancel);
        btnOk          = (Button) v.findViewById(R.id.btnOk);

        hintCustomPath.setText(path_external);

        lvSaved.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.radioBtnPathCustom) {
                    customGroup.setVisibility(View.VISIBLE);
                } else {
                    customGroup.setVisibility(View.GONE);
                }


                //customGroup.setVisibility(View.GONE);

                //if (action != ACTION.CREATE) {
                    updateSavedItems(getSelectedPath());
                //}
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "[onCreateView][btnOk][setOnClickListener]");

                CONTAINER_PATH containerPath = getSelectedPath();

                String filename = null;
                String custom   = null;

                int index = lvSaved.getCheckedItemPosition();
                if(containerPath != CONTAINER_PATH.CUSTOM) {
                    if (index > -1) {
                        filename = names.get(index);
                    }
                } else {
                    if(index > 0) {
                        custom = names.get(index);
                    } else {
                        custom = editCustomPath.getText().toString();
                    }
                }



                //SelectedContainer container = new SelectedContainer(custom, selected, isCreateNew, containerPath);
                SelectedContainer container = new SelectedContainer(action, custom, filename, containerPath);

                listener.onSelectContainerPath(container);
                dismiss();
            }
        });

        if(action == ACTION.CREATE) {
            v.findViewById(R.id.radioBtnPathCustom).setVisibility(View.GONE);
            btnOk.setText(R.string.btnCreate);
        }

        v.findViewById(R.id.radioBtnPathInternal).setVisibility(View.GONE);

        //if (action != ACTION.CREATE) {
            updateSavedItems(getSelectedPath());
        //}

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (OnSelectContainerPathListener)activity;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

    }

    void updateSavedItems(CONTAINER_PATH path) {
        List<String> items = Containers.model((Context)listener).getContainers(path);

        names = new ArrayList<String>();

        for (String item : items) {
            File _file = new File(item);

            if(!_file.exists()) {
                Containers.model((Context)listener).deleteContainer(item);
            } else {

                //names.add(_file.getName());
                names.add((path != CONTAINER_PATH.CUSTOM ? _file.getName() : item));
            }
        }


        if(path == CONTAINER_PATH.CUSTOM) {
            names.add(0, "[Custom]");
        }


        if(names.isEmpty()) {
            savedGroup.setVisibility(View.GONE);
            lvSaved.setAdapter(null);
        } else {
            //names.add(0, "[new]");
            if(action != ACTION.CREATE) {
                lvSaved.setAdapter(new ArrayAdapter<String>((Context) listener, android.R.layout.simple_list_item_single_choice, names));
            } else {
                lvSaved.setAdapter(new ArrayAdapter<String>((Context)listener, android.R.layout.simple_list_item_1, names));
            }

            lvSaved.setItemChecked(0, true);
            savedGroup.setVisibility(View.VISIBLE);
        }


    }

    CONTAINER_PATH getSelectedPath() {
        CONTAINER_PATH containerPath = CONTAINER_PATH.INTERNAL;

        switch (radioGroup.getCheckedRadioButtonId()) {

            case R.id.radioBtnPathInternal:
                containerPath = CONTAINER_PATH.INTERNAL;
                break;

            case R.id.radioBtnPathExternal:
                containerPath = CONTAINER_PATH.EXTERNAL;
                break;

            case R.id.radioBtnPathCustom:
                containerPath = CONTAINER_PATH.CUSTOM;
                break;

        }

        return containerPath;
    }

    public class SelectedContainer {
        public CONTAINER_PATH path;
        public String custom;
        public String filename;
        public ACTION action;

        public SelectedContainer(ACTION action, String custom, String filename, CONTAINER_PATH path) {
            this.action = action;
            this.custom = custom;
            this.filename = filename;
            this.path   = path;
        }

        @Override
        public String toString() {
            return "SelectedContainer{" +
                    "action=" + action +
                    ", path=" + path +
                    ", custom='" + custom + '\'' +
                    ", filename='" + filename + '\'' +
                    '}';
        }

        public boolean isCreate() {
            return action == ACTION.CREATE;
        }

        public boolean isOpen() {
            return action == ACTION.OPEN;
        }

        public boolean isDelete() {
            return action == ACTION.DELETE;
        }
    }
}
