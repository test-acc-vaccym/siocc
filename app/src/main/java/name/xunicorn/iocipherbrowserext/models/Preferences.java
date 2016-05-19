package name.xunicorn.iocipherbrowserext.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import name.xunicorn.iocipherbrowserext.components.Configs;

public class Preferences {
    public final String TAG = "Preferences";

    private Context context;

    public Preferences(Context context) {
        this.context = context;
    }

    public static Preferences model(Context context) {
        return new Preferences(context);
    }

    public String getImportContainer() {
        Log.i(TAG, "[getImportContainer]");

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        return sp.getString(Configs.PREF_CONTAINER_PATH, null);
    }

    public void setImportContainer(String containerPath) {
        Log.i(TAG, "[setImportContainer] path: " + containerPath);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sp.edit();

        editor.putString(Configs.PREF_CONTAINER_PATH, containerPath);
        editor.apply();
    }
}
