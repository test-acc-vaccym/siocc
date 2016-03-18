package name.xunicorn.iocipherbrowserext.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import info.guardianproject.iocipher.VirtualFileSystem;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.fragments.*;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.*;
import name.xunicorn.iocipherbrowserext.models.Containers;
import name.xunicorn.iocipherbrowserext.models.Preferences;
import name.xunicorn.iocipherbrowserext.providers.IOCipherContentProvider;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity
        extends AppCompatActivity
        implements
            NewNameDialog.onSetNewNameListener,
            CopyDialog.OnCopyActionComplete,
        SelectContainerDialog.OnSelectContainerPathListener,
        PasswordDialog.OnSetContainerPasswordListener,
        NavigationView.OnNavigationItemSelectedListener {

    //region Class variables

    private final static String TAG = "MainActivity";

    private final int REQUEST_TAKE_PHOTO = 1;

    private List<String> actionItems = new ArrayList<String>();
    private CopyDialog.ACTION menuAction;

    public android.support.v7.widget.Toolbar toolbar;
    public DrawerLayout                      drawerLayout;
    public NavigationView                    navigationView;

    private VirtualFileSystem vfs;

    private PluggedContainer container;

    ContainerListFragment   containerFragment;
    DefaultFragment         defaultFragment;
    SettingsFragment        settingsFragment;


    //endregion

    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        containerFragment = new ContainerListFragment();
        defaultFragment   = new DefaultFragment();
        settingsFragment  = new SettingsFragment();

        toolbar  = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.openDrawer, R.string.closeDrawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };
        //Setting the actionbarToggle to drawer layout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

        vfs = VirtualFileSystem.get();

        Log.i(TAG, "[onCreate]");

        try {
            String dataDirectory  = Environment.getDataDirectory().getAbsolutePath();
            String cacheDirectory = Environment.getDownloadCacheDirectory().getAbsolutePath();
            String externalDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
            String rootDirectory  = Environment.getRootDirectory().getAbsolutePath();

            Log.d(TAG, "[onCreate] data directory: "  + dataDirectory);
            Log.d(TAG, "[onCreate] cache directory: " + cacheDirectory);
            Log.d(TAG, "[onCreate] external directory: " + externalDirectory);
            Log.d(TAG, "[onCreate] root directory: "  + rootDirectory);

            File file = new File("/storage/sdcard1/Android/data/" + getApplicationContext().getPackageName() + "/" + getString(R.string.app_name));
            file.mkdirs();

            Log.d(TAG, "[onCreate] ext dirpath: " + file.getAbsolutePath() + " | exists: " + file.exists());

        } catch (Exception ex) {
            Log.e(TAG, "[onCreate] error: " + ex.getMessage());
            ex.printStackTrace();
        }

        plugDefaultFragment();
    }

    //region Menu block
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "[onCreateOptionsMenu]");

        getMenuInflater().inflate(R.menu.menu_main_quick, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.i(TAG, "[onPrepareOptionsMenu]");

        menu.setGroupVisible(R.id.menuItemActionsGroup, containerFragment.isVisible());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.menuUnPlugContainer:
                Log.d(TAG, "[onOptionsItemSelected] MENU_UNPLUG_CONTAINER");
                commandUnPlugContainer();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        List<String> items = new ArrayList<String>();

        if(containerFragment.isVisible()) {
            items = containerFragment.getSelectedItems();

            Log.d(TAG, "[onNavigationItemSelected] checked items: " + items.size());
        }

        switch(menuItem.getItemId()) {

            case R.id.menuSettings:
                Log.d(TAG, "[onNavigationItemSelected] MENU_SETTINGS");
                commandSettings();
                break;

            case R.id.menuCreateContainer:
                Log.d(TAG, "[onNavigationItemSelected] MENU_CREATE_CONTAINER");
                commandCreateContainer();
                break;

            case R.id.menuPlugContainer:
                Log.d(TAG, "[onNavigationItemSelected] MENU_PLUG_CONTAINER");
                commandPlugContainer();
                break;

            case R.id.menuUnPlugContainer:
                Log.d(TAG, "[onNavigationItemSelected] MENU_UNPLUG_CONTAINER");
                commandUnPlugContainer();
                break;

            case R.id.menuDeleteContainer:
                Log.d(TAG, "[onNavigationItemSelected] MENU_DELETE_CONTAINER");
                commandDeleteContainer();
                break;

            case R.id.menuDebug:
                commandDebug();
                break;

            case R.id.menuNewFolder:
                Log.d(TAG, "[onNavigationItemSelected] MENU_CREATE_FOLDER");
                commandCreateDir();
                break;

            case R.id.menuCopy:
                Log.d(TAG, "[onNavigationItemSelected] MENU_COPY_ITEMS");
                commandCopyItems(false);
                break;

            case R.id.menuCut:
                Log.d(TAG, "[onNavigationItemSelected] MENU_MOVE_ITEMS");
                commandCopyItems(true);
                break;

            case R.id.menuRename:
                Log.d(TAG, "[onNavigationItemSelected] MENU_RENAME_ITEMS");
                commandRename();
                break;

            case R.id.menuPaste:
                Log.d(TAG, "[onNavigationItemSelected] MENU_PASTE_ITEMS action items: " + actionItems.size() + " | action: " + menuAction);

                if(actionItems.isEmpty()) {
                    break;
                }

                commandPasteItems();
                break;

            case R.id.menuDelete:
                Log.d(TAG, "[onNavigationItemSelected] MENU_DELETE");
                commandDeleteItems();
                break;

            case R.id.menuExport:
                Log.d(TAG, "[onNavigationItemSelected] MENU_EXPORT");
                commandExport();
                break;

            case R.id.menuFeedback:
                Log.d(TAG, "[onNavigationItemSelected] MENU_FEEDBACK");
                commandFeedback();
                break;

            case R.id.menuTakePhoto:
                Log.d(TAG, "[onNavigationItemSelected] MENU_TAKE_PHOTO");
                commandTakePhoto();
                break;

            case R.id.menuShare:
                Log.d(TAG, "[onNavigationItemSelected] MENU_SHARE");
                commandShare();
                break;

            case R.id.menuUpdate:
                Log.d(TAG, "[onNavigationItemSelected] MENU_UPDATE");
                commandUpdate();
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    protected void hideItemsMenuGroup() {
        navigationView.getMenu().setGroupVisible(R.id.menuItemActionsGroup, false);
        navigationView.getMenu().setGroupVisible(R.id.menuContainerDeleteGroup, false);
    }

    protected void containerMenuGroupsShow() {
        navigationView.getMenu().setGroupVisible(R.id.menuItemActionsGroup, true);
        navigationView.getMenu().setGroupVisible(R.id.menuContainerDeleteGroup, true);
    }

    //endregion

    //region Activity events
    @Override
    protected void onResume() {
        super.onResume();
        /*
         * do NOT use a hard-coded password! Either prompt the user for the
         * password, or use the CacheWord library to handle password prompting
         * and caching.  https://github.com/guardianproject/cacheword
         */

        //vfsMount();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[onDestroy]");

        vfsUnMount();

        super.onDestroy();
    }

/*
    @Override
    protected void onPause() {
        Log.i(TAG, "[onPause]");

        super.onPause();
        vfsUnMount();
    }
*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, "[onActivityResult] requestCode: " + requestCode + " | resultCode: " + resultCode + " | intent: " + data);

        switch(requestCode) {
            case REQUEST_TAKE_PHOTO:
                if(resultCode == Activity.RESULT_OK) {
                    if(data == null) {
                        Log.w(TAG, "[onActivityResult] intent is null");
                    } else {
                        Log.i(TAG, "[onActivityResult] photo uri: " + data.getData());
                    }

                    CopyDialog
                            .newInstance("/", CopyDialog.ACTION.IMPORT, actionItems)
                            .show(getFragmentManager(), "ImportDialog");

                    actionItems = new ArrayList<String>();
                } else {
                    Log.e(TAG, "[onActivityResult] result is FALSE");
                }

                containerFragment.getFileListForCurrent();
                break;

        }
    }

    //endregion

    //region Dialogs Listeners
    @Override
    public void setNewName(String oldName, String new_name, NewNameDialog.TYPE type, NewNameDialog.ACTION action) {
        Log.d(TAG, "[setNewName] new name: " + new_name + " | type: " + type.toString());

        boolean is_new = action == NewNameDialog.ACTION.NEW;

        switch (type) {
            case DIR:
                containerFragment.updateDir(oldName, new_name, action);
                containerFragment.getFileListForCurrent();
                break;
            case FILE:
                containerFragment.renameFile(oldName, new_name);
                containerFragment.getFileListForCurrent();
                break;

            case CONTAINER:
                this.container.name = new_name;

                //this.container.path = this.container.path.substring(0, this.container.path.lastIndexOf("/")) + "/" + new_name;

                File file = new File(this.container.getPath());

                Log.i(TAG, "[setNewName] new container path: " + file.getAbsolutePath());

                if(!file.exists()) {
                    PasswordDialog.newInstance().show(getFragmentManager(), "passwordDialogNewContainer");
                } else {
                    Log.e(TAG, "[setNewName] such container file exists: " + this.container.getPath());
                    Toast.makeText(this, "Such container file exists: " + this.container.getPath(), Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    public void onCopyComplete(CopyDialog.ACTION action) {
        Log.d(TAG, "[onCopyComplete] action: " + action);

        String action_str = "copying";

        switch (action) {
            case MOVE: action_str = "moving"; break;
            case DELETE: action_str = "deleting"; break;
            case EXPORT: action_str = "exporting"; break;
            case IMPORT: action_str = "importing"; break;
            default: break;
        }

        Toast.makeText(this, "Items " + action_str + " was successfull", Toast.LENGTH_SHORT).show();

        containerFragment.getFileListForCurrent();
    }

    @Override
    public void onSelectContainerPath(SelectContainerDialog.SelectedContainer container) {
        Log.d(TAG, "[onSelectContainerPath] container: " + container);

        String path = Containers.model(this).convertContainerPath(container.path, container.custom);

        if(container.filename != null) {
            path = path.substring(0, path.lastIndexOf("/")) + "/" + container.filename;
        }

        java.io.File container_file = new java.io.File(path);

        if(!container_file.exists() && !container.isCreate()) {
            Log.e(TAG, "[onSelectContainerPath] container file does not exists: " + path);
            return;
        }

        this.container = new PluggedContainer(container);

        if(container.isOpen()) {
            PasswordDialog.newInstance().show(getFragmentManager(), "passwordDialog");
        } else if(container.isCreate()) {
            NewNameDialog
                    .newInstance(NewNameDialog.TYPE.CONTAINER, NewNameDialog.ACTION.NEW)
                    .show(getFragmentManager(), "newNameDialogContainer");
        } else if(container.isDelete()) {
            vfsUnMount();

            Log.i(TAG, "[onSelectContainerPath] deleting container path: " + container_file.getAbsolutePath());

            if(container_file.delete()) {
                Toast.makeText(this, "Container \"" + container.filename + "\" was successfully deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Occurred some errors in deleting container", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSetContainerPassword(String password) {
        Log.d(TAG, "[onSetContainerPassword] password: " + password);

        //containerSecret = password;

        this.container.setPassword(password);

        vfsMount();
    }
    //endregion

    //region Vfs actions
    protected void vfsMount() {
        if(this.container == null) {
            Log.w(TAG, "[vfsMount] container path not set");
            return;
        }

        Log.i(TAG, "[vfsMount] vfs isMounted: " + vfs.isMounted() + " | " + this.container);

        try {
            if(this.container.isCreate()) {
                vfs.createNewContainer(this.container.getPath(), this.container.getPassword());

                String importContainer = Preferences.model(getBaseContext()).getImportContainer();

                if(importContainer == null) {
                    Preferences.model(getBaseContext()).setImportContainer(this.container.getPath());
                }
            } else {
                vfs.setContainerPath(this.container.getPath());
            }

            if (!vfs.isMounted()) {
                vfs.mount(this.container.getPassword());

                Log.i(TAG, "[vfsMount] vfs isMounted: " + vfs.isMounted());

                Containers.model(this).savePath(this.container.getPath(), this.container.selectedContainer.path);
            }

            plugContainerFragment();

            Log.d(TAG, "[vfsMount] vfs path: " + vfs.getContainerPath());
        } catch (Exception ex) {
            Log.e(TAG, "[vfsMount] error: " + ex.getMessage(), ex);

            if(ex instanceof IllegalArgumentException) {
                Toast.makeText(getBaseContext(), R.string.txtContainerBadPassword, Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void vfsUnMount() {
        Log.i(TAG, "[vfsUnMount] vfs isMounted: " + vfs.isMounted());

        if (vfs.isMounted()) {

            try {
                vfs.unmount();

                Log.i(TAG, "[vfsUnMount] vfs successfully unmounted");
            } catch(Exception ex) {
                Log.e(TAG, "[vfsUnMount] error: " + ex.getMessage(), ex);
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void vfsDelete() {
        Log.i(TAG, "[vfsDelete] vfs isMounted: " + vfs.isMounted());

        if (vfs.isMounted()) {

            String path = vfs.getContainerPath();

            try {
                vfs.unmount();
                File file = new File(path);

                if(file.delete()) {
                    Log.i(TAG, "[vfsDelete] container file was successfully deleted. " + path);

                    Toast.makeText(getBaseContext(), R.string.containerDeleteSuccess, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "[vfsDelete] could not delete container file");

                    Toast.makeText(getBaseContext(), R.string.containerDeleteFailed, Toast.LENGTH_SHORT).show();
                }
            } catch (IllegalStateException e) {
                Toast.makeText(getBaseContext(), R.string.containerDeleteFailed, Toast.LENGTH_SHORT).show();

                Log.e(TAG, "[vfsDelete] error", e);
            }

        }
    }
    //endregion

    //region Fragments actions
    protected void plugSettingsContainer() {
        Log.i(TAG, "[plugSettingsContainer]");

        //vfsUnMount();

        FragmentTransaction fTrans = getFragmentManager().beginTransaction();

        fTrans.replace(R.id.frgmContent, settingsFragment);

        fTrans.addToBackStack(null);

        fTrans.commit();

        hideItemsMenuGroup();
    }

    protected void plugContainerFragment() {
        Log.i(TAG, "[plugContainerFragment]");

        FragmentTransaction fTrans = getFragmentManager().beginTransaction();

        fTrans.replace(R.id.frgmContent, containerFragment);

        fTrans.addToBackStack(null);

        fTrans.commit();

        containerMenuGroupsShow();
    }

    protected void plugDefaultFragment() {
        Log.i(TAG, "[plugDefaultFragment]");

        vfsUnMount();

        FragmentTransaction fTrans = getFragmentManager().beginTransaction();

        fTrans.replace(R.id.frgmContent, defaultFragment);

        fTrans.addToBackStack(null);

        fTrans.commit();

        hideItemsMenuGroup();
    }

    protected void plugFeedbackFragment() {
        Log.i(TAG, "[plugFeedbackFragment]");

        startActivity(new Intent(this, FeedbackActivity.class));
        //finish();
    }

    protected void plugUpdateFragment() {
        Log.i(TAG, "[plugUpdateFragment]");

        startActivity(new Intent(this, UpdateActivity.class));

    }
    //endregion

    //region Menu commands
    public void commandSettings() {
        Log.i(TAG, "[commandSettings]");
        plugSettingsContainer();
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
    //endregion

    class PluggedContainer {
        public String name;
        protected String password;
        public SelectContainerDialog.SelectedContainer selectedContainer;

        public PluggedContainer(SelectContainerDialog.SelectedContainer container) {
            this.selectedContainer = container;
        }

        public String getPath() {
            String path = Containers.model(getBaseContext()).convertContainerPath(selectedContainer.path, selectedContainer.custom);

            if(selectedContainer.filename != null) {
                path = path.substring(0, path.lastIndexOf("/")) + "/" + selectedContainer.filename;
            }

            if(container.isCreate() && !TextUtils.isEmpty(name)) {
                path = path.substring(0, path.lastIndexOf("/")) + "/" + name;
            }

            return path;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isCreate() {
            return selectedContainer.isCreate();
        }

        public boolean isOpen() {
            return selectedContainer.isOpen();
        }

        public boolean isDelete() {
            return selectedContainer.isDelete();
        }

        @Override
        public String toString() {
            return "PluggedContainer{" +
                    "name='" + name + '\'' +
                    ", password='" + password + '\'' +
                    ", selectedContainer=" + selectedContainer +
                    '}';
        }
    }
}
