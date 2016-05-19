package name.xunicorn.iocipherbrowserext.components.activities.main;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.util.Log;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.FeedbackActivity;
import name.xunicorn.iocipherbrowserext.activities.MainActivity;
import name.xunicorn.iocipherbrowserext.activities.UpdateActivity;
import name.xunicorn.iocipherbrowserext.components.exceptions.NotInitializedException;


public class PlugComponent {
    public final static String TAG = "PlugComponent";

    private static PlugComponent instance;

    final MainActivity activity;

    private PlugComponent(MainActivity activity) {
        this.activity = activity;
    }

    public static PlugComponent getComponent(MainActivity activity) {
        if(instance == null) {
            instance = new PlugComponent(activity);
        }

        return instance;
    }

    public static PlugComponent getComponent() throws NotInitializedException{
        if(instance == null) {
            throw  new NotInitializedException(PlugComponent.class);
        }

        return instance;
    }

    protected void plugSettingsContainer() {
        Log.i(TAG, "[plugSettingsContainer]");

        changeMainWindowMenuVisibility(true, false);

        FragmentTransaction fTrans = activity.getFragmentManager().beginTransaction();

        fTrans.replace(R.id.frgmContent, settingsFragment);

        fTrans.addToBackStack(null);

        fTrans.commit();

        hideItemsMenuGroup();
    }

    protected void plugContainerFragment() {
        Log.i(TAG, "[plugContainerFragment]");

        changeMainWindowMenuVisibility(false, true);

        FragmentTransaction fTrans = activity.getFragmentManager().beginTransaction();

        fTrans.replace(R.id.frgmContent, containerFragment);

        fTrans.addToBackStack(null);

        fTrans.commit();

        showItemsMenuGroup();
    }

    protected void plugDefaultFragment() {
        Log.i(TAG, "[plugDefaultFragment]");

        changeMainWindowMenuVisibility(false, true);

        vfsUnMount();

        FragmentTransaction fTrans = activity.getFragmentManager().beginTransaction();

        fTrans.replace(R.id.frgmContent, defaultFragment);

        fTrans.addToBackStack(null);

        fTrans.commit();

        hideItemsMenuGroup();
    }

    protected void plugFeedbackFragment() {
        Log.i(TAG, "[plugFeedbackFragment]");

        activity.startActivity(new Intent(activity, FeedbackActivity.class));
        //finish();
    }

    protected void plugUpdateFragment() {
        Log.i(TAG, "[plugUpdateFragment]");

        activity.startActivity(new Intent(activity, UpdateActivity.class));

    }
}
