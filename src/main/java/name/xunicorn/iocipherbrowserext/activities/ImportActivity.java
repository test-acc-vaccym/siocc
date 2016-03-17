package name.xunicorn.iocipherbrowserext.activities;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.PasswordDialog;
import name.xunicorn.iocipherbrowserext.models.Files;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImportActivity extends Activity implements PasswordDialog.OnSetContainerPasswordListener {
    public static final String TAG = "ImportActivity";

    public enum FILE_ACTION { COPY, MOVE }

    static final int HANDLE_SINGLE_FILE    = 1;
    static final int HANDLE_MULTIPLE_FILES = 2;
    static final int HANDLE_ERROR          = 3;

    TextView hint;
    TextView filesCounter;

    ProgressBar progress;

    Handler handler;

    boolean is_spinner_visible = true;

    String containerPath = null;
    String password      = null;

    Thread t;

    FILE_ACTION fileAction = FILE_ACTION.COPY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setMax(100);

        hint = (TextView) findViewById(R.id.loadingHint);
        filesCounter = (TextView) findViewById(R.id.filesCounter);

        //region Handler
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(is_spinner_visible) {
                    //(findViewById(R.id.progressBarSpinner)).setVisibility(View.INVISIBLE);
                    progress.setVisibility(View.VISIBLE);

                    is_spinner_visible = false;

                    if(msg.what == HANDLE_MULTIPLE_FILES) {
                        filesCounter.setVisibility(View.VISIBLE);
                    }
                }


                switch(msg.what) {
                    case HANDLE_SINGLE_FILE:
                        progress.setProgress(msg.arg1);

                        filesCounter.setText("1 / 1 files");

                        if(msg.what == 100) {
                            hint.setText("File successfully uploaded >:)");
                        }
                        break;

                    case HANDLE_MULTIPLE_FILES:
                        HandleObject obj = (HandleObject)msg.obj;

                        progress.setProgress(obj.percentsTotal);

                        String plural = (obj.filesCounter) > 1 ? "files" : "file";

                        filesCounter.setText(obj.filesCounter + " / " + obj.filesTotal + " " + plural);

                        if(obj.isOver()) {
                            hint.setText(obj.filesTotal + " " + plural + " successfully uploaded >:)");
                            finishActivity();

                            Toast.makeText(getBaseContext(), "All files successfully uploaded", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case HANDLE_ERROR:
                        String error = (String) msg.obj;
                        hint.setText("Error: " + error);
                        (findViewById(R.id.progressBarSpinner)).setVisibility(View.INVISIBLE);

                        unMountVfs();
                        finishActivityCrash();
                        break;
                }


            }
        };
        //endregion

        //defineFileAction();

        //Files.model(getBaseContext()).selectMediaTable();

        try {
            prepareVfs();
        } catch(Exception ex) {
            Log.e(TAG, "[onCreate] error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "[onDestroy]");

        super.onDestroy();
    }

    //region Finishing
    private void finishActivity() {
        Log.i(TAG, "[finishActivity]");

        t.interrupt();

        try {
            Thread.currentThread().sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        showAlert();
    }

    private void finishActivityCrash() {
        Log.i(TAG, "[finishActivityCrash]");
        t.interrupt();

        Intent _new = new Intent(getBaseContext(), MainActivity.class);
        startActivity(_new);

        finish();
    }

    private void showAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.importCautionTitle))
                .setMessage(getResources().getString(R.string.importCautionText))
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        unMountVfs();

                        Intent _new = new Intent(getBaseContext(), MainActivity.class);
                        startActivity(_new);

                        finish();
                    }
                })
                .show();
    }
    //endregion

    @Override
    public void onSetContainerPassword(String password) {
        Log.i(TAG, "[onSetContainerPassword] password: " + password);
        this.password = password;
        mountVfs();
    }


    //region vfs
    private void prepareVfs() throws Exception{
        Log.i(TAG, "[prepareVfs]");

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        containerPath = sp.getString(Configs.PREF_CONTAINER_PATH, null);

        if(TextUtils.isEmpty(containerPath)) {
            Log.e(TAG, "[mountVfs] settings container path is empty");

            throw new Exception("settings container path is empty");
        }

        Log.i(TAG, "[prepareVfs] container path: " + containerPath);

        PasswordDialog.newInstance().show(getFragmentManager(), "passwordDialog");
    }

    private void mountVfs() {
        Log.i(TAG, "[mountVfs]");

        //unMountVfs();

        VirtualFileSystem vfs = VirtualFileSystem.get();

        if(vfs.isMounted()) {
            if(!vfs.getContainerPath().equals(containerPath)) {
                try {
                    vfs.unmount();
                } catch (IllegalStateException e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "[mountVfs] error: ", e);

                    finishActivityCrash();
                }
            }
        }

        if(!vfs.isMounted()) {
            vfs.setContainerPath(containerPath);

            vfs.mount(password);

            startCopy();
        }
    }

    private void unMountVfs() {
        Log.i(TAG, "[unMountVfs]");

        VirtualFileSystem vfs = VirtualFileSystem.get();

        if(vfs.isMounted()) {
            try {
                vfs.unmount();
            } catch (IllegalStateException e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "[unMountVfs] error: ", e);

                finishActivityCrash();
            }
        }
    }
    //endregion

    private void defineFileAction() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        String action = sp.getString(Configs.PREF_IMPORT_ACTION, null);

        if(TextUtils.isEmpty(action)) {
            fileAction = FILE_ACTION.COPY;
        } else {
            fileAction = FILE_ACTION.valueOf(action);
        }

        Log.i(TAG, "[defineFileAction] file action: " + fileAction);
    }

    private void startCopy() {
        Log.i(TAG, "[startCopy]");
        t = new Thread(new ActionRun());

        t.start();
    }

    //region Runnable
    class ActionRun implements Runnable {
        public final String TAG = "ActionRun";

        protected Long totalBytes         = 0l;
        protected Long totalBytesSave     = 0l;
        protected Integer totalFilesCount = 1;
        protected Integer filesCounter    = 1;

        @Override
        public void run() {
            //region Parse Intent
            Intent intent = getIntent();
            String action = intent.getAction();
            String type   = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                    Log.i(TAG, "save extra stream URI: " + uri);
                    try {
                        handleSendUri(uri);
                    } catch (Exception e) {
                        e.printStackTrace();
                        handleException(e);
                    }
                } else {
                    Log.i(TAG, "save data");
                    handleSendUri(intent.getData());
                }
            } else if(Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    List<Uri> uris = (ArrayList<Uri>)intent.getExtras().get(Intent.EXTRA_STREAM);

                    Log.d(TAG, "[run] uris: " + uris );

                    try {
                        handleMultipleUri(uris);
                    } catch (Exception e) {
                        e.printStackTrace();
                        handleException(e);
                    }
                }
            }

            //endregion
        }

        private void handleSendUri(Uri dataUri) {
            try {
                String filepath = Files.model(getApplicationContext()).getFilePathFromURI(dataUri);;
                InputStream in;

                if(!dataUri.getScheme().equals("file")) {
                    Log.d(TAG, "[handleSendUri] get input stream");
                    ContentResolver cr = getContentResolver();
                    in = cr.openInputStream(dataUri);
                    Log.i(TAG, "incoming URI: " + dataUri.toString());

                    Log.d(TAG, "[handleSendUri] get path from uri");
                    //filepath = Files.model(getApplicationContext()).getFilePathFromURI(dataUri);
                } else {
                    //filepath = dataUri.getPath();
                    in = new FileInputStream(filepath);
                }

                String[] fileInfo = filepath.split("/");
                String fileName   = fileInfo[fileInfo.length - 1];

                File f = new File("/" + fileName.toLowerCase());
                java.io.File data = new java.io.File(filepath);

                totalBytes = data.length();

                Log.d(TAG, "[handleSendUri] get output stream");
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

                Log.d(TAG, "[handleSendUri] start copy");
                readBytesAndClose(in, out, data.length());

                if(fileAction == FILE_ACTION.MOVE) {
                    deleteFile(dataUri);
                }

                Log.v(TAG, f.getAbsolutePath() + " size: " + String.valueOf(f.length()));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                handleException(e);
            }
        }

        private void handleMultipleUri(List<Uri> uris) {
            totalFilesCount = 0;

            Iterator<Uri> it = uris.iterator();

            while(it.hasNext()) {
                Uri uri = it.next();

                String filePath = Files.model(getBaseContext()).getFilePathFromURI(uri);

                java.io.File file = new java.io.File(filePath);

                if(file.exists()) {
                    totalBytes += file.length();
                    totalFilesCount++;
                } else {
                    it.remove();
                }
            }

            Log.i(TAG, "[handleMultipleUri] files count: " + totalFilesCount + ". Total size: " + ((float) totalBytes /1024/1024) + " MB");

            if(uris.isEmpty()) {
                return;
            }

            for(final Uri uri: uris) {
                try {
                    String filepath = Files.model(getBaseContext()).getFilePathFromURI(uri);

                    String[] fileInfo = filepath.split("/");
                    String fileName = fileInfo[fileInfo.length - 1];

                    InputStream in = new FileInputStream(filepath);

                    File f = new File("/" + fileName.toLowerCase());
                    java.io.File data = new java.io.File(filepath);

                    Log.d(TAG, "[handleSendUri] get output stream");
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

                    Log.d(TAG, "[handleSendUri] start copy");
                    readBytesAndClose(in, out, data.length());

                    if(fileAction == FILE_ACTION.MOVE) {
                        deleteFile(uri);
                    }

                    Log.v(TAG, f.getAbsolutePath() + " size: " + String.valueOf(f.length()));

                } catch(Exception ex) {
                    Log.e(TAG, "[handleMultipleUri] file: " + uri.getPath() + ", error: " + ex.getMessage());
                    ex.printStackTrace();
                    handleException(ex);
                }
            }
        }

        private void deleteFile(Uri uri) {
            Log.d(TAG, "[deleteFile] file action: " + fileAction + " | uri: " + uri + " | path: " + uri.getPath());

            /*
            getContentResolver().delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + " = ?",
                    new String[] { uri.getPath() }
            );
            */
            Files.model(getBaseContext()).deleteImage(uri);

            if(uri.getScheme().equals("file")) {
                Log.d(TAG, "[deleteFile] check physically file existence");

                java.io.File file = new java.io.File(uri.getPath());

                if (file.exists()) {
                    if (!file.delete()) {
                        Log.e(TAG, "[deleteFile] could not delete the file: " + file.getAbsolutePath());
                    } else {
                        Log.d(TAG, "[deleteFile] file successfully deleted: " + file.getAbsolutePath());
                    }
                } else {
                    Log.w(TAG, "[deleteFile] file does not exists: " + file.getAbsolutePath());
                }
            }
        }

        private void readBytesAndClose(InputStream in, OutputStream out, Long totalBytesCurrent)
                throws IOException {

            try {
                int block = 8 * 1024; // IOCipher works best with 8k blocks
                byte[] buff = new byte[block];

                long total_save = 0;

                while (true) {
                    int len = in.read(buff, 0, block);
                    if (len < 0) {
                        break;
                    }

                    total_save += len;

                    totalBytesSave += len;

                    float percentsTotal   = (float) totalBytesSave / totalBytes * 100;
                    float percentsCurrent = (float) total_save / totalBytesCurrent * 100;

                    Log.d(TAG, "[readBytesAndClose] Save. Current: " + total_save + " | " + percentsCurrent + " %."
                            + " Total: " + totalBytesSave + " | " + String.valueOf(percentsTotal) + " %."
                    );

                    Message msg = handler.obtainMessage(
                            HANDLE_MULTIPLE_FILES,
                            new HandleObject((int)percentsTotal, (int)percentsCurrent, filesCounter, totalFilesCount)
                    );

                    handler.sendMessage(msg);

                    out.write(buff, 0, len);
                }
            } finally {
                in.close();
                out.flush();
                out.close();

                filesCounter++;
            }
        }


        private void handleException(Exception ex) {
            Message msg = handler.obtainMessage(HANDLE_ERROR, ex.getMessage());
            handler.sendMessage(msg);

            Thread.currentThread().interrupt();
        }
    }
    //endregion

    //region HandleObject
    public static class HandleObject {
        public Integer percentsTotal;
        public Integer percentsCurrent;
        public Integer filesCounter;
        public Integer filesTotal;

        public HandleObject(int percentsTotal, int percentsCurrent, int filesCounter, int filesTotal) {
            this.percentsTotal = percentsTotal;
            this.percentsCurrent = percentsCurrent;
            this.filesCounter    = filesCounter;
            this.filesTotal      = filesTotal;
        }

        public boolean isOver() {
            return percentsTotal == 100 && (filesCounter.equals(filesTotal));
        }

        public boolean isCurrentOver() { return percentsCurrent == 100; }
    }
    //endregion
}
