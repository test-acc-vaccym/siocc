package name.xunicorn.iocipherbrowserext.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected final String TAG = "SettingsFragment";

    protected SharedPreferences sp;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "[onCreate]");

        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "[onResume]");
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String selectedContainerPath = sp.getString(Configs.PREF_CONTAINER_PATH, "not set");
        //String importAction          = sp.getString(Configs.PREF_IMPORT_ACTION, "COPY");

        findPreference(Configs.PREF_CONTAINER_PATH).setSummary(selectedContainerPath);
        //findPreference(Configs.PREF_IMPORT_ACTION).setSummary(importAction);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "[onPause]");
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "[OnSharedPreferenceChangeListener] key: " + key);

        String defaultVal = "";

        if(key.equals(Configs.PREF_IMPORT_ACTION)) {
            defaultVal = "COPY";
        }

        Preference pref = findPreference(key);
        pref.setSummary(sharedPreferences.getString(key, defaultVal));
    }
}
