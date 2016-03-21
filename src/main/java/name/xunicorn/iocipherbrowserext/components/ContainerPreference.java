package name.xunicorn.iocipherbrowserext.components;

import android.content.Context;
import android.os.Environment;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.*;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.SelectContainerDialog;
import name.xunicorn.iocipherbrowserext.models.Containers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContainerPreference extends DialogPreference {
    final String TAG = "ContainerPreference";

    protected RadioGroup radioGroup;
    protected Button btnCancel;
    protected Button btnOk;

    protected TextView hintCustomPath;
    protected EditText editCustomPath;

    protected LinearLayout customGroup;
    protected LinearLayout savedGroup;

    protected ListView lvSaved;

    protected List<String> names;
    protected List<String> fullPaths;

    public ContainerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_select_container);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        View v = super.onCreateDialogView();

        String path_external = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getContext().getString(R.string.app_name) + "/" + Configs.containerFileName;

        v.findViewById(R.id.actionButtonsGroup).setVisibility(View.GONE);

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

                updateSavedItems(getSelectedPath());
            }
        });

        updateSavedItems(getSelectedPath());

        Log.d(TAG, "[onCreateDialogView] key: " + getKey() + " | value: " + getPersistedString("not set"));

        return v;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

        Log.i(TAG, "[onDialogClosed]");

        if(positiveResult) {
            String path = "";

            SelectContainerDialog.CONTAINER_PATH containerPath = getSelectedPath();

            int index = lvSaved.getCheckedItemPosition();

            if(index > -1) {

                if(index == 0 && containerPath == SelectContainerDialog.CONTAINER_PATH.CUSTOM) {
                    path = editCustomPath.getText().toString();

                    File file = new File(path);

                    if(!file.exists()) {
                        path = null;
                    }
                } else {
                    path = fullPaths.get(containerPath == SelectContainerDialog.CONTAINER_PATH.CUSTOM ? (index - 1) : index);
                }
            }

            if(!TextUtils.isEmpty(path)) {
                Log.i(TAG, "[onDialogClosed] path: " + path);

                persistString(path);
            } else {
                Toast.makeText(getContext(), "No one path selected", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "[onDialogClosed] no one path selected");
            }
        }
    }

    protected SelectContainerDialog.CONTAINER_PATH getSelectedPath() {
        SelectContainerDialog.CONTAINER_PATH containerPath = SelectContainerDialog.CONTAINER_PATH.INTERNAL;

        switch (radioGroup.getCheckedRadioButtonId()) {

            case R.id.radioBtnPathInternal:
                containerPath = SelectContainerDialog.CONTAINER_PATH.INTERNAL;
                break;

            case R.id.radioBtnPathExternal:
                containerPath = SelectContainerDialog.CONTAINER_PATH.EXTERNAL;
                break;
/*
            case R.id.radioBtnPathCustom:
                containerPath = SelectContainerDialog.CONTAINER_PATH.CUSTOM;
                break;
*/
        }

        return containerPath;
    }

    protected void updateSavedItems(SelectContainerDialog.CONTAINER_PATH path) {
        List<String> items = Containers.model(getContext()).getContainers(path);

        names     = new ArrayList<String>();
        fullPaths = new ArrayList<String>();

        for (String item : items) {
            File _file = new File(item);

            if (!_file.exists()) {
                Containers.model(getContext()).deleteContainer(item);
            } else {

                fullPaths.add(item);
                names.add((path != SelectContainerDialog.CONTAINER_PATH.CUSTOM ? _file.getName() : item));
            }
        }


        if (path == SelectContainerDialog.CONTAINER_PATH.CUSTOM) {
            names.add(0, "[Custom]");
        }


        if (names.isEmpty()) {
            savedGroup.setVisibility(View.GONE);
            lvSaved.setAdapter(null);
        } else {
            lvSaved.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_single_choice, names));
            lvSaved.setItemChecked(0, true);
            savedGroup.setVisibility(View.VISIBLE);
        }
    }

}
