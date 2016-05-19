package name.xunicorn.iocipherbrowserext.components.activities.main;

import android.util.Log;
import android.widget.Toast;
import info.guardianproject.iocipher.VirtualFileSystem;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.MainActivity;
import name.xunicorn.iocipherbrowserext.components.Cryptor;
import name.xunicorn.iocipherbrowserext.components.exceptions.NotInitializedException;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.SelectContainerDialog;
import name.xunicorn.iocipherbrowserext.models.Containers;
import name.xunicorn.iocipherbrowserext.models.Preferences;

import java.io.File;


public class VfsComponent {
    public final static String TAG = "VfsComponent";

    private static VfsComponent instance;

    final MainActivity activity;

    PluggedContainer container;

    VirtualFileSystem vfs;

    private VfsComponent(MainActivity activity) {
        this.activity = activity;

        container = new PluggedContainer();

        vfs = VirtualFileSystem.get();
    }

    public static VfsComponent getComponent(MainActivity activity) {
        if(instance == null) {
            instance = new VfsComponent(activity);
        }

        return instance;
    }

    public static VfsComponent getComponent() throws NotInitializedException {
        if(instance == null) {
            throw  new NotInitializedException(VfsComponent.class);
        }

        return instance;
    }

    protected void vfsMount() {
        if(this.container == null) {
            Log.w(TAG, "[vfsMount] container path not set");
            return;
        }

        //vfs = null;

        Log.i(TAG, "[vfsMount] vfs isMounted: " + vfs.isMounted() + " | " + this.container);

        try {
            if(this.container.isCreate()) {
                vfs.createNewContainer(this.container.getPath(), this.container.getPassword());

                String importContainer = Preferences.model(getBaseContext()).getImportContainer();

                if(importContainer == null) {
                    Preferences.model(getBaseContext()).setImportContainer(this.container.getPath());
                }
            }

            if (!vfs.isMounted()) {
                vfs.mount(this.container.getPath(), this.container.getPassword());

                Log.i(TAG, "[vfsMount] vfs isMounted: " + vfs.isMounted());

                passwords.put(this.container.getPath(), this.container.getPassword());

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
                //IOCipherContentProvider.closeAllPipes();

                vfs.unmount();

                Log.i(TAG, "[vfsUnMount] vfs successfully unmounted");
            } catch(Exception ex) {
                Log.e(TAG, "[vfsUnMount] error: " + ex.getMessage(), ex);
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

    private class PluggedContainer {

        public String name;
        protected char[] password;
        protected byte[] passwordHash;
        public SelectContainerDialog.SelectedContainer selectedContainer;

        private String selectedContainerPath;

        public PluggedContainer() {
        }

        public PluggedContainer(SelectContainerDialog.SelectedContainer container) {
            this.selectedContainer = container;

            selectedContainerPath = Containers.model(getApplicationContext()).convertContainerPath(selectedContainer.path, selectedContainer.custom);
        }

        public String getPath() {
            String path = selectedContainerPath;

            if(selectedContainer.filename != null) {
                path = path.substring(0, path.lastIndexOf("/")) + "/" + selectedContainer.filename;
            }

            if(this.isCreate() && (name != null && !name.equals(""))) {
                path = path.substring(0, path.lastIndexOf("/")) + "/" + name;
            }

            return path;
        }

        public byte[] getPassword() {
            if(passwordHash != null) {
                return passwordHash;
            }

            return Cryptor.cryptPassword(password);
        }

        public void setPassword(char[] password) {
            this.password = password;
        }

        public void setPasswordHash(byte[] passwordHash) {
            this.passwordHash = passwordHash;
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
                    ", selectedContainer=" + selectedContainer +
                    '}';
        }
    }
}
