package name.xunicorn.iocipherbrowserext.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import name.xunicorn.iocipherbrowserext.R;
import name.xunicorn.iocipherbrowserext.components.Configs;
import name.xunicorn.iocipherbrowserext.components.DBHelper;
import name.xunicorn.iocipherbrowserext.fragments.dialogs.SelectContainerDialog;

import java.util.ArrayList;
import java.util.List;

public class Containers {
    final String TAG = "Containers";

    public static final String TABLE = "containers";

    public static final String COLUMN_ID   = "id";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_INTERNAL = "external";
    public static final String COLUMN_EXTERNAL = "internal";
    public static final String COLUMN_CUSTOM   = "custom";

    Context context;

    public Containers(Context context) {
        this.context = context;
    }

    public static Containers model(Context context) {
        return new Containers(context);
    }

    public void savePath(String path, SelectContainerDialog.CONTAINER_PATH containerPath) {
        Log.i(TAG, "[savePath] path: " + path + " | containerPath: " + containerPath);

        if(containerExists(path)) {
            return;
        }

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_PATH, path);
        cv.put(COLUMN_INTERNAL, (containerPath == SelectContainerDialog.CONTAINER_PATH.INTERNAL) ? 1 : 0);
        cv.put(COLUMN_EXTERNAL, (containerPath == SelectContainerDialog.CONTAINER_PATH.EXTERNAL) ? 1 : 0);
        cv.put(COLUMN_CUSTOM,   (containerPath == SelectContainerDialog.CONTAINER_PATH.CUSTOM)   ? 1 : 0);

        db.insert(TABLE, null, cv);

        db.close();
        dbHelper.close();
    }

    public boolean containerExists(String path) {
        Log.i(TAG, "[containerExists] path: " + path);

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cur = db.query(TABLE, null, COLUMN_PATH + " = ?", new String[]{path}, null, null, null);

        boolean exists = cur.moveToFirst();

        cur.close();

        db.close();
        dbHelper.close();

        return  exists;
    }

    public List<String> getContainers(SelectContainerDialog.CONTAINER_PATH path) {
        Log.i(TAG, "[getContainers] path: " + path);

        List<String> containers = new ArrayList<String>();

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String selection = "";

        switch (path) {
            case INTERNAL: selection = COLUMN_INTERNAL + "=1"; break;
            case EXTERNAL: selection = COLUMN_EXTERNAL + "=1"; break;
            case CUSTOM:   selection = COLUMN_CUSTOM   + "=1"; break;
        }

        Cursor cur = db.query(TABLE, new String[]{COLUMN_PATH}, selection, null, null, null, null);

        if(cur.moveToFirst()) {
            do {
                containers.add(cur.getString(cur.getColumnIndex(COLUMN_PATH)));
            } while (cur.moveToNext());
        }

        cur.close();

        db.close();
        dbHelper.close();

        return containers;
    }

    public String convertContainerPath(SelectContainerDialog.CONTAINER_PATH path, String custom) {
        Log.i(TAG, "[convertContainerPath] path: " + path + " | custom: " + custom);

        String containerPath = null;

        switch(path){
            case INTERNAL:
                containerPath = context.getFilesDir().getAbsolutePath() + "/" + Configs.containerFileName;
                break;

            case EXTERNAL:
                java.io.File external = Environment.getExternalStorageDirectory();
                external = new java.io.File(external.getAbsolutePath() + "/" + context.getString(R.string.app_name));
                external.mkdirs();

                containerPath = external.getAbsolutePath() + "/" + Configs.containerFileName;
                break;

            case CUSTOM:
                containerPath = custom;
                break;
        }

        return containerPath;
    }

    public void deleteContainer(String path) {
        Log.i(TAG, "[deleteContainer] path: " + path);

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(TABLE, COLUMN_PATH + " = ?", new String[]{ path });

        db.close();
        dbHelper.close();
    }
}
