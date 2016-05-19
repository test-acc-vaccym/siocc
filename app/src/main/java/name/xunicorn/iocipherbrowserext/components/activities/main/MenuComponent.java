package name.xunicorn.iocipherbrowserext.components.activities.main;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.MainActivity;
import name.xunicorn.iocipherbrowserext.components.exceptions.NotInitializedException;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.CopyDialog;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.NewNameDialog;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.SelectContainerDialog;
import name.xunicorn.iocipherbrowserext.providers.IOCipherContentProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MenuComponent {
    public final static String TAG = "MenuComponent";

    private static MenuComponent instance;

    final MainActivity activity;

    private MenuComponent(MainActivity activity) {
        this.activity = activity;
    }

    public static MenuComponent getComponent(MainActivity activity) {
        if(instance == null) {
            instance = new MenuComponent(activity);
        }

        return instance;
    }

    public static MenuComponent getComponent() throws NotInitializedException {
        if(instance == null) {
            throw new NotInitializedException(MenuComponent.class);
        }

        return instance;
    }

    public void commandSettings() {
        Log.i(TAG, "[commandSettings]");
        plugSettingsContainer();
    }

    public void commandMainWindow() {
        Log.i(TAG, "[commandMainWindow]");

        if(vfs.isMounted()) {
            plugContainerFragment();
        } else {
            plugDefaultFragment();
        }
    }

    public void commandDebug() {
        Log.d(TAG, "[commandDebug] currentPath: " + containerFragment.getCurrentPath());
    }

    public void commandCreateContainer() {
        Log.i(TAG, "[commandCreateContainer]");
        SelectContainerDialog
                .newInstance(SelectContainerDialog.ACTION.CREATE)
                .show(getFragmentManager(), "selectContainerDialog");
    }

    public void commandPlugContainer() {
        Log.i(TAG, "[commandPlugContainer]");
        SelectContainerDialog
                .newInstance(SelectContainerDialog.ACTION.OPEN)
                .show(getFragmentManager(), "selectContainerDialog");
    }

    public void commandUnPlugContainer() {
        Log.i(TAG, "[commandPlugContainer]");

        vfsUnMount();
        plugDefaultFragment();
    }

    public void commandDeleteContainer() {
        Log.i(TAG, "[commandDeleteContainer]");
        /*
        SelectContainerDialog
                .newInstance(SelectContainerDialog.ACTION.DELETE)
                .show(getFragmentManager(), "selectContainerDialog");
        */

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.containerDeleteDialogTitle)
                .setMessage(R.string.containerDeleteDialogMessage)
                .setPositiveButton(R.string.btnYes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "[commandDeleteContainer][AlertDialog][positiveOnClick]");
                        vfsDelete();
                        plugDefaultFragment();
                    }
                })
                .setNegativeButton(R.string.btnNo, null)
                .show();
    }

    public void commandCreateDir() {
        Log.i(TAG, "[commandCreateDir]");

        NewNameDialog
                .newInstance(NewNameDialog.TYPE.DIR, NewNameDialog.ACTION.NEW)
                .show(getFragmentManager(), "newNameDialogDirectory");
    }

    public void commandCopyItems(boolean is_move) {
        Log.i(TAG, "[commandCopyItems] isMove command: " + is_move);

        actionItems = containerFragment.getSelectedItems();
        menuAction  = is_move ? CopyDialog.ACTION.MOVE : CopyDialog.ACTION.COPY;
    }

    public void commandPasteItems() {
        Log.i(TAG, "[commandPasteItems]");

        if(actionItems.isEmpty()) {
            return;
        }

        CopyDialog
                .newInstance(
                        containerFragment.getCurrentPath(),
                        menuAction,
                        actionItems
                )
                .show(getFragmentManager(), "copyDialog");

        containerFragment.getFileListForCurrent();
    }

    public void commandDeleteItems() {
        Log.i(TAG, "[commandDeleteItems]");

        CopyDialog
                .newInstance(
                        containerFragment.getCurrentPath(),
                        CopyDialog.ACTION.DELETE,
                        containerFragment.getSelectedItems()
                )
                .show(getFragmentManager(), "copyDialog");

        containerFragment.getFileListForCurrent();
    }

    public void commandExport() {
        Log.i(TAG, "[commandExport]");

        CopyDialog
                .newInstance(
                        containerFragment.getCurrentPath(),
                        CopyDialog.ACTION.EXPORT,
                        containerFragment.getSelectedItems()
                )
                .show(getFragmentManager(), "copyDialog");

        containerFragment.getFileListForCurrent();
    }

    public void commandShare() {
        Log.i(TAG, "[commandShare]");

        Intent intent = new Intent();

        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.");
        intent.setType("image/jpeg");

        ArrayList<Uri> files = new ArrayList<Uri>();
        List<String> checkedFiles = containerFragment.getSelectedItems();

        Log.d(TAG, "[commandShare] files for share: " + checkedFiles);

        for(String path: checkedFiles) {
            Uri uri = Uri.parse(IOCipherContentProvider.FILES_URI + path);
            files.add(uri);
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        startActivity(intent);
    }

    public void commandFeedback() {
        Log.i(TAG, "[commandFeedback]");

        //vfsUnMount();
        plugFeedbackFragment();
    }

    public void commandRename() {
        Log.i(TAG, "[commandFeedback]");

        List<String> items = containerFragment.getSelectedItems();

        if(!items.isEmpty()) {
            for(String item: items) {
                if(containerFragment.isDirectory(item)) {
                    NewNameDialog
                            .newInstance(NewNameDialog.TYPE.DIR, NewNameDialog.ACTION.RENAME, item)
                            .show(getFragmentManager(), "renameDialogDir");
                } else {
                    NewNameDialog
                            .newInstance(NewNameDialog.TYPE.FILE, NewNameDialog.ACTION.RENAME, item)
                            .show(getFragmentManager(), "renameDialogFile");
                }
            }
        }
    }

    public void commandTakePhoto() {
        Log.i(TAG, "[commandTakePhoto]");

        String path_tmp = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getString(R.string.app_name) + "/tmp/";

        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File directory = new File(path_tmp);

        if(!directory.exists()) {
            directory.mkdirs();
        }

        File image = new File(directory, filename + ".jpg");

        actionItems = new ArrayList<String>();
        actionItems.add(image.getAbsolutePath());

        Uri uri = Uri.fromFile(image);

        if (uri == null) {
            return;
        }

        Log.i(TAG, "[commandTakePhoto] uri: " + uri);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                    uri);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

        }
    }

    public void commandUpdate() {
        Log.i(TAG, "[commandUpdate]");
        plugUpdateFragment();
    }
}
