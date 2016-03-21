package name.xunicorn.iocipherbrowserext.fragments;


import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.webkit.MimeTypeMap;
import android.widget.*;
import info.guardianproject.iocipher.File;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.activities.MainActivity;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.NewNameDialog;
import name.xunicorn.iocipherbrowserext.providers.IOCipherContentProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContainerListFragment extends Fragment {
    final static String TAG = "ContainerListFragment";

    private TextView fileInfo;
    private ListView listView;

    private List<String>        item = null;
    private Map<String, String> mapedPath = null;

    private String[] items;
    private String   root = "/";
    private String   currentPath = "/"; // if subdir - without last /

    public ContainerListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "[onCreateView]");

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_container_list, container, false);

        listView = (ListView) v.findViewById(R.id.listView);

        //region ListView config
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                CheckedTextView checked = (CheckedTextView) view.findViewById(R.id.label);

                checked.setChecked(!checked.isChecked());

            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Log.d(TAG, "[setOnItemLongClickListener] pos: " + pos + " | filename: " + items[pos] + " | current: " + currentPath);

                String filePath = currentPath;

                if (!currentPath.equals(root)) {
                    filePath += "/";
                }

                filePath += items[pos];

                final File file = new File(filePath);

                final Uri uri = Uri.parse(IOCipherContentProvider.FILES_URI + filePath);
                //final Uri uri = Uri.parse(IOCipherContentProvider.FILES_URI + file.getParent());

                Log.i(TAG, "[setOnItemLongClickListener] open URL: " + uri);

                new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.icon)
                        .setTitle("[" + file.getName() + "]")
                        .setNeutralButton(R.string.txtContainerListItemView,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        try {
                                            Intent intent = new Intent(Intent.ACTION_VIEW);

                                            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
                                            String mimeType = MimeTypeMap.getSingleton()
                                                    .getMimeTypeFromExtension(fileExtension.toLowerCase());

                                            if (mimeType == null)
                                                mimeType = "application/octet-stream";

                                            intent.setDataAndType(uri, mimeType);

                                            Log.i(TAG, "[setOnItemLongClickListener][AlertDialog][View] Intent{"
                                                    + "act=" + intent.getAction() + " "
                                                    + "dat=" + intent.getData() + " "
                                                    + "typ=" + intent.getType()
                                                    + "}");

                                            //startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                            startActivity(intent);
                                        } catch (ActivityNotFoundException e) {
                                            Log.e(TAG, "No relevant Activity found", e);
                                        }
                                    }
                                })

                        .setPositiveButton(R.string.txtContainerListItemShare,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        Intent intent = new Intent(Intent.ACTION_SEND);
                                        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                                                .toString());
                                        String mimeType = MimeTypeMap.getSingleton()
                                                .getMimeTypeFromExtension(fileExtension);
                                        if (mimeType == null)
                                            mimeType = "application/octet-stream";

                                        intent.setType(mimeType);
                                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                                        //intent.setDataAndType(uri, mimeType);

                                        Log.i(TAG, "[setOnItemLongClickListener][AlertDialog][Share] Intent{"
                                                + "act=" + intent.getAction() + " "
                                                + "dat=" + intent.getData() + " "
                                                + "typ=" + intent.getType()
                                                + "}");

                                        try {
                                            startActivity(Intent.createChooser(intent, getString(R.string.txtContainerListItemShareThis)));
                                        } catch (ActivityNotFoundException e) {
                                            Log.e(TAG, "No relevant Activity found", e);
                                        }
                                    }
                                })

                        .setNegativeButton(R.string.txtContainerListItemExport,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                        Log.i(TAG, "[setOnItemLongClickListener][AlertDialog][Export]");

                                        ((MainActivity) getActivity()).commandExport();
                                    }
                                }).show();

                return false;
            }
        });
        //endregion

        fileInfo = (TextView) v.findViewById(R.id.info);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.i(TAG, "[onStart]");

        getFileListForCurrent();
    }



    //region Public actions

    public void createNewDir(String dir) {
        Log.d(TAG, "[createNewDir] dir: " + dir);

        File new_dir = new File(currentPath + "/" + dir);

        if(new_dir.exists()) {
            Log.e(TAG, "[createNewDir] path exists: " + new_dir.getAbsolutePath());
        } else {
            new_dir.mkdirs();
            getFileList(currentPath);
        }
    }

    public void renameDir(String old_name, String new_name) {
        Log.i(TAG, "[renameDir] old name: " + old_name + " | new name: " + new_name);

        File dir     = new File(currentPath + "/" + old_name);
        File newName = new File(currentPath + "/" + new_name);

        if ( dir.isDirectory() ) {
            dir.renameTo(newName);
        } else {
            dir.mkdir();
            dir.renameTo(newName);
        }
    }

    public void updateDir(String oldName, String newName, NewNameDialog.ACTION action) {
        switch (action) {
            case NEW:
                createNewDir(newName);
                break;

            case RENAME:
                renameDir(oldName, newName);
                break;
        }
    }

    public void renameFile(String old_name, String new_name) {
        Log.i(TAG, "[renameFile] old name: " + old_name + " | new name: " + new_name);

        File file    = new File(currentPath + "/" + old_name);
        File newName = new File(currentPath + "/" + new_name);

        if(newName.exists()) {
            Log.e(TAG, "[renameFile] file: " + newName.getAbsolutePath() + " exists");
            Toast.makeText(getActivity(), "File " + newName.getAbsolutePath() + " exsits. Rename FAILED!", Toast.LENGTH_SHORT).show();
        } else {
            file.renameTo(newName);
        }
    }

    public boolean isDirectory(String itemPath) {
        File item = new File(currentPath + "/" + itemPath);

        return item.isDirectory();
    }

    public String getCurrentPath() {
        return currentPath;
    }

    //region items parsing
    public void getFileList(String dirPath) {
        Log.i(TAG, "[getFileList] path: " + dirPath);

        //region Parse directory. Initialize adapter
        item = new ArrayList<String>();
        mapedPath = new Hashtable<String, String>();

        List<String> directories = new ArrayList<String>();
        List<String> files_list = new ArrayList<String>();

        File file = new File(dirPath);
        File[] files = file.listFiles();

        if (!dirPath.equals(root)) {
            item.add(root);
            mapedPath.put(root, root);// to get back to main list

            item.add("..");
            mapedPath.put("..", file.getParent()); // back one level
        }

        for (int i = 0; i < files.length; i++) {

            File fileItem = files[i];

            String filename = fileItem.getName();


            if (fileItem.isDirectory()) {
                // input name directory to array list
                filename = "[" + fileItem.getName() + "]";
                directories.add(filename);
            } else {
                // input name file to array list
                files_list.add(fileItem.getName());
            }

            mapedPath.put(filename, fileItem.getPath());
        }

        Collections.sort(directories);
        Collections.sort(files_list);

        item.addAll(directories);
        item.addAll(files_list);

        fileInfo.setText("Info: " + dirPath + " [ " + files.length + " item ]");
        // declare array with specific number of items
        items = new String[item.size()];
        // send data arraylist(item) to array(items)
        item.toArray(items);

        updateAdapter(items);

        //endregion
    }

    public void getFileListForCurrent() {
        Log.i(TAG, "[getFileListForCurrent] current path: " + currentPath);
        getFileList(currentPath);
    }

    public List<String> getSelectedItems() {
        Log.i(TAG, "[getSelectedItems]");

        List<String> items = new ArrayList<String>();

        SparseBooleanArray sbArray = listView.getCheckedItemPositions();

        for (int i = 0; i < sbArray.size(); i++) {
            int key = sbArray.keyAt(i);
            if (sbArray.get(key))
                items.add(mapedPath.get(this.items[key]));
        }

        Log.i(TAG, "[getSelectedItems] items count: " + items.size());

        return items;
    }
    //endregion

    //endregion

    //region Adapter actions
    public void updateAdapter(String[] items) {
        Log.i(TAG, "[updateAdapter] items: " + items.length);

        Log.d(TAG, "[updateAdapter] items: " + items + " | path: " + mapedPath);

        listView.setAdapter(new IconicList(items));
    }

    public void resetAdapter() {
        Log.i(TAG, "[resetAdapter]");

        updateAdapter(new String[0]);
    }
    //endregion

    //region List adapter
    class IconicList extends ArrayAdapter<String> {

        public IconicList(String[] items) {
            super(getActivity(), R.layout.fragment_container_list_row, items);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_container_list_row, null);
            }

            final CheckedTextView label = (CheckedTextView) convertView.findViewById(R.id.label);
            ImageView icon        = (ImageView) convertView.findViewById(R.id.icon);

            label.setText(this.getItem(position));

            DirsOnClickListener dirsListener = new DirsOnClickListener(mapedPath.get(items[position]));

            icon.setOnClickListener(dirsListener);

            File f = new File(mapedPath.get(items[position])); // get the file according the position
            if (f.isDirectory()) {
                icon.setImageResource(R.drawable.folder);
            } else {
                icon.setImageResource(R.drawable.file);
            }
            return (convertView);
        }
    }
    //endregion

    //region Directory click listener
    class DirsOnClickListener implements View.OnClickListener {
        String path;

        public DirsOnClickListener(String path) {
            this.path = path;
        }

        @Override
        public void onClick(View v) {
            File file = new File(path);

            if (file.isDirectory()) {
                if (file.canRead()) {
                    getFileList(path);
                    currentPath = path;
                }
            }
        }
    }
    //endregion
}
