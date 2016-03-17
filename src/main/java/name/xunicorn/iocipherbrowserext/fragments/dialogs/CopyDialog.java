package name.xunicorn.iocipherbrowserext.fragments.dialogs;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import name.xunicorn.iocipherbrowserext.components.InterruptThreadListener;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.ImportActivity;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class CopyDialog extends DialogFragment {

    public interface OnCopyActionComplete {
        void onCopyComplete(ACTION action);
    }

    final String TAG = "CopyDialog";

    public enum ACTION { COPY, MOVE, DELETE, EXPORT, IMPORT }

    public final static String BUNDLE_CURRENT_PATH = "currentPath";
    public final static String BUNDLE_MENU_ACTION  = "menuAction";
    public final static String BUNDLE_ACTION_ITEMS = "actionItems";

    final int HANDLER_COPY_FILES   = 1;
    final int HANDLER_DELETE_FILES = 2;
    final int HANDLER_EXPORT_FILES = 3;
    final int HANDLER_SHOW_WORKING = 4;
    final int HANDLER_HIDE_WORKING = 5;

    final String TEXT_COPY          = "Copy items";
    final String TEXT_MOVE          = "Move items";
    final String TEXT_DELETE        = "Delete items";
    final String TEXT_EXPORT        = "Export items";
    final String TEXT_IMPORT        = "Import items";

    protected ACTION menuAction;
    protected String currentPath;
    protected List<String> actionItems;

    Handler handler;

    TextView tv_mainText;
    TextView tv_currentProgress;
    TextView tv_totalProgress;

    ProgressBar pb_working;
    ProgressBar pb_current;
    ProgressBar pb_total;

    Button btnCancel;
    Button btnHide;

    String mainText;

    OnCopyActionComplete listener;

    CopyRun copyRun;

    Thread t;

    public CopyDialog() {
        // Required empty public constructor
    }


    public static CopyDialog newInstance(String currentPath, ACTION action, List<String> items) {
        Bundle bundle = new Bundle();

        bundle.putString(BUNDLE_CURRENT_PATH, currentPath);
        bundle.putString(BUNDLE_MENU_ACTION, action.toString());
        bundle.putStringArrayList(CopyDialog.BUNDLE_ACTION_ITEMS, (ArrayList) items);

        CopyDialog dialog = new CopyDialog();
        dialog.setArguments(bundle);

        return dialog;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle bundle = this.getArguments();

        currentPath = bundle.getString(BUNDLE_CURRENT_PATH);
        menuAction  = ACTION.valueOf(bundle.getString(BUNDLE_MENU_ACTION));
        actionItems = bundle.getStringArrayList(BUNDLE_ACTION_ITEMS);

        Log.d(TAG, "[onCreateView] currentPath: " + currentPath + " | menuAction: " + menuAction + " | actionItems count: " + actionItems.size());

        switch (menuAction) {
            case COPY: mainText = TEXT_COPY; break;

            case MOVE: mainText = TEXT_MOVE; break;

            case DELETE: mainText = TEXT_DELETE; break;

            case EXPORT: mainText = TEXT_EXPORT; break;

            case IMPORT: mainText = TEXT_IMPORT; break;
        }

        copyRun = new CopyRun();

        View v = inflater.inflate(R.layout.dialog_copy, null);

        tv_mainText        = (TextView)v.findViewById(R.id.textViewMain);
        tv_currentProgress = (TextView)v.findViewById(R.id.textViewProgressCurrent);
        tv_totalProgress   = (TextView)v.findViewById(R.id.textViewProgressTotal);

        pb_working = (ProgressBar) v.findViewById(R.id.progressBarWorking);
        pb_current = (ProgressBar) v.findViewById(R.id.progressBarCurrent);
        pb_total   = (ProgressBar) v.findViewById(R.id.progressBarTotal);

        btnCancel = (Button)v.findViewById(R.id.btnCancel);
        btnHide   = (Button)v.findViewById(R.id.btnHide);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t.interrupt();
                copyRun.onInterruptThread();

                dismiss();
            }
        });

        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        pb_current.setMax(100);
        pb_total.setMax(100);

        tv_mainText.setText(mainText);
        tv_totalProgress.setText("0% ( 0 / " + String.valueOf(actionItems.size()) + " )");

        //region Handler
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch(msg.what) {
                    case HANDLER_COPY_FILES:
                    case HANDLER_DELETE_FILES:
                    case HANDLER_EXPORT_FILES:
                        ImportActivity.HandleObject obj = (ImportActivity.HandleObject) msg.obj;

                        pb_current.setProgress(obj.percentsCurrent);
                        pb_total.setProgress(obj.percentsTotal);

                        tv_currentProgress.setText(obj.percentsCurrent.toString() + "%");
                        tv_totalProgress.setText(String.format("%d%% ( %d / %d )", obj.percentsTotal, obj.filesCounter, obj.filesTotal));

                        if(obj.isOver()) {
                            listener.onCopyComplete(menuAction);
                            dismiss();
                        }

                        if(obj.isCurrentOver()) {
                            pb_current.setProgress(0);
                        }
                        break;

                    case HANDLER_HIDE_WORKING:
                        pb_working.setVisibility(View.INVISIBLE);
                        break;

                    case HANDLER_SHOW_WORKING:
                        pb_working.setVisibility(View.VISIBLE);
                        break;
                }

            }
        };
        //endregion

        t = new Thread(copyRun);
        t.start();

        return v;
    }


    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (OnCopyActionComplete) activity;
    }

    class CopyRun implements Runnable, InterruptThreadListener {
        String TAG = "CopyRun";

        long totalBytes;
        long totalBytesSave;
        int filesCounter = 1;
        int totalFilesCount;

        boolean interrupt = false;

        @Override
        public void run() {
            if(actionItems.isEmpty()) {
                return;
            }

            try {
                switch (menuAction) {
                    case DELETE:
                        deleteItems();
                        break;

                    case COPY:
                    case MOVE:
                        try {
                            copyItems();
                        } catch (InterruptedException ex) {
                            Log.e(TAG, "[run] action: " + menuAction + " - INTERRUPTED");
                            unDoCopy();

                            dismiss();
                        }
                        break;

                    case EXPORT:
                        exportItems();
                        break;

                    case IMPORT:
                        importItems();
                        break;
                }
            } catch (InterruptedException ex) {
                Log.e(TAG, "[run] action: " + menuAction + " - INTERRUPTED");

                Thread.currentThread().interrupt();

                dismiss();
            }
        }

        @Override
        public void onInterruptThread(){
            Log.i(TAG, "[onInterruptThread]");

            interrupt = true;
        }

        private void unDoCopy() {
            Log.i(TAG, "[unDoCopy]");
            List<String> items = new ArrayList<String>();

            for(String item_path: actionItems) {

                List<String> _items = getItems(item_path, true);

                if(!_items.isEmpty()) {
                    items.addAll(_items);
                }
            }

            if(!items.isEmpty()) {
                for(String path: items) {
                    File item_curr = new File(path);
                    File item = new File(currentPath + "/" + item_curr.getName());

                    if(item.exists()) {
                        Log.d(TAG, "[unDoCopy] delete: " + item.getAbsolutePath());
                        item.delete();
                    }
                }
            }
        }

        protected void copyItems() throws InterruptedException {
            Log.d(TAG, "[copyItems] copy " + actionItems.size() + " items");

            totalFilesCount = actionItems.size();

            for(String item_path: actionItems) {
                File item_f = new File(item_path);

                totalBytes += item_f.length();
            }

            for(String item_path: actionItems) {
                File item_f = new File(item_path);
                if (item_f.exists()) {
                    Log.d(TAG, "[copyItems] copy: " + item_f.getAbsolutePath());

                    File new_file = new File(currentPath + "/" + item_f.getName());

                    try {
                        handler.sendEmptyMessage(HANDLER_SHOW_WORKING);

                        InputStream in = new FileInputStream(item_f);
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new_file));

                        handler.sendEmptyMessage(HANDLER_HIDE_WORKING);

                        readBytesAndClose(in, out, item_f.length());

                        if(menuAction == ACTION.MOVE) {
                            Log.d(TAG, "[copyItems] delete: " + item_f.getAbsolutePath());

                            handler.sendEmptyMessage(HANDLER_SHOW_WORKING);

                            item_f.delete();

                            handler.sendEmptyMessage(HANDLER_HIDE_WORKING);
                        }
                    } catch(IOException ex) {
                        Log.e(TAG, "[onOptionsItemSelected] error: " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        throw ex;
                    }
                }
            }
        }

        protected void deleteItems() throws InterruptedException {
            Log.d(TAG, "[deleteItems] delete " + actionItems.size() + " items");

            handler.sendEmptyMessage(HANDLER_SHOW_WORKING);

            List<String> items = new ArrayList<String>();

            for(String item_path: actionItems) {

                List<String> _items = getItems(item_path, true);

                if(!_items.isEmpty()) {
                    items.addAll(_items);
                }
            }

            if(!items.isEmpty()) {
                Log.d(TAG, "[deleteItems] Items: " + items);

                totalFilesCount = items.size();

                for(String _item : items) {
                    if(interrupt) {
                        throw new InterruptedException();
                    }

                    File item_f = new File(_item);

                    Log.d(TAG, "[deleteItems] delete: " + _item);

                    if(!item_f.delete()) {
                        Log.e(TAG, "[deleteItems] delete: " + _item + " - ERROR!");
                    }

                    Message msg = handler.obtainMessage(
                            HANDLER_DELETE_FILES,
                            new ImportActivity.HandleObject(filesCounter/totalFilesCount*100, 100, filesCounter, totalFilesCount)
                    );

                    handler.sendMessage(msg);



                    filesCounter++;
                }
            }

            handler.sendEmptyMessage(HANDLER_HIDE_WORKING);
        }

        protected void exportItems() throws InterruptedException {
            Log.d(TAG, "[exportItems] export " + actionItems.size() + " items");

            String[] containers_path = VirtualFileSystem.get().getContainerPath().split("/");

            String path_export = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getString(R.string.app_name) + "/export/" + containers_path[containers_path.length-1];

            handler.sendEmptyMessage(HANDLER_SHOW_WORKING);

            List<String> items = new ArrayList<String>();

            for(String item_path: actionItems) {

                List<String> _items = getItems(item_path, false);

                if(!_items.isEmpty()) {
                    items.addAll(_items);

                    for(String _item : _items) {
                        File _item_file = new File(_item);

                        totalBytes += _item_file.length();
                    }
                }
            }

            Log.d(TAG, "[exportItems] Items: " + items);

            totalFilesCount = items.size();

            for(String _item: items) {

                File _internal = new File(_item);
                java.io.File _export = new java.io.File(path_export + _item);

                java.io.File _export_dir = new java.io.File(path_export + _internal.getParent());

                if(!_export_dir.exists()) {
                    _export_dir.mkdirs();

                    if(_internal.isDirectory()) {
                        Log.d(TAG, "[exportItems] internal item is dir: " + _internal.getAbsolutePath());
                        filesCounter++;
                        continue;
                    }
                }

                try {
                    InputStream in = new FileInputStream(_internal);
                    BufferedOutputStream out = new BufferedOutputStream(new java.io.FileOutputStream(_export));

                    readBytesAndClose(in, out, _internal.length());
                } catch (IOException e) {
                    Log.e(TAG, "[exportItems] error", e);
                } catch(InterruptedException e) {
                    throw  e;
                }

                filesCounter++;
            }
        }

        protected void importItems() throws InterruptedException {
            Log.d(TAG, "[importItems] import " + actionItems.size() + " items");

            for(String item_path: actionItems) {

                java.io.File _item_file = new java.io.File(item_path);

                totalBytes += _item_file.length();
            }

            Log.d(TAG, "[importItems] Items: " + actionItems);

            totalFilesCount = actionItems.size();

            if(!currentPath.endsWith("/")) {
                currentPath += "/";
            }

            for(String _item: actionItems) {
                java.io.File _external = new java.io.File(_item);
                File         _internal = new File(currentPath + _external.getName());

                try {
                    InputStream in = new java.io.FileInputStream(_external);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(_internal));

                    readBytesAndClose(in, out, _external.length());
                } catch (IOException e) {
                    Log.e(TAG, "[importItems] error", e);
                } catch(InterruptedException e) {
                    throw  e;
                }

                if(!_external.delete()) {
                    Log.e(TAG, "[importItems] could not delete import file: " + _external.getAbsolutePath());
                }

                filesCounter++;
            }
        }

        private List<String> getItems(String path, boolean for_delete) {
            List<String> items = new ArrayList<String>();

            File item = new File(path);

            if(item.exists()) {

                if(!for_delete) {
                    items.add(path);
                }

                if(item.isDirectory()) {
                    for(File _item: item.listFiles()) {
                        List<String> _items = getItems(_item.getAbsolutePath(), for_delete);

                        if(!_items.isEmpty()) {
                            items.addAll(_items);
                        }
                    }
                }

                if(for_delete) {
                    items.add(path);
                }
            }

            return items;
        }

        private void readBytesAndClose(InputStream in, OutputStream out, Long totalBytesCurrent)
                throws IOException, InterruptedException {

            try {
                int block = 8 * 1024; // IOCipher works best with 8k blocks
                byte[] buff = new byte[block];

                long total_save = 0;

                while (true) {
                    if(interrupt) {
                        throw new InterruptedException();
                    }

                    int len = in.read(buff, 0, block);
                    if (len < 0) {
                        break;
                    }

                    total_save += len;

                    totalBytesSave += len;

                    float percentsTotal   = (float) totalBytesSave / totalBytes * 100;
                    float percentsCurrent = (float) total_save / totalBytesCurrent * 100;
/*
                    Log.d(TAG, "[readBytesAndClose] Save. Total: " + totalBytesSave + " | " + String.valueOf(percentsTotal) + " %."
                                    + " Current: " + total_save + " | " + percentsCurrent + " %"
                    );
*/
                    Message msg = handler.obtainMessage(
                            HANDLER_COPY_FILES,
                            new ImportActivity.HandleObject((int)percentsTotal, (int)percentsCurrent, filesCounter, totalFilesCount)
                    );

                    handler.sendMessage(msg);

                    out.write(buff, 0, len);
                }
            } finally {
                in.close();
                out.flush();
                out.close();

                //filesCounter++;
            }
        }

    }

}
