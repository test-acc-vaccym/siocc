package name.xunicorn.iocipherbrowserext.models;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class Files {
    public static final String TAG = "Files";

    protected Context context;

    public Files(Context context) {
        this.context = context;
    }

    public static Files model(Context context) {
        return new Files(context);
    }

    public void deleteImage(Uri uri) {
        Log.i(TAG, "[deleteImage] uri: " + uri);

        ContentResolver resolver = context.getContentResolver();

        if(uri.toString().contains("content:")) {
            Log.i(TAG, "[deleteImage] delete content image");
            resolver.delete(uri, null, null);
            return;
        }

        Log.i(TAG, "[deleteImage] delete file image");

        Cursor c = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + " = ?",
                new String[]{uri.getPath()},
                null
        );

        if(c.moveToFirst()) {
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            Log.i(TAG, "[deleteImage] delete uri: " + deleteUri);

            resolver.delete(deleteUri, null, null);
        } else {
            Log.w(TAG, "[deleteImage] image not found");
        }

        c.close();
    }

    @Deprecated
    public void deleteRecordsForContainer(String containerPath) {
        Log.i(TAG, "[deleteRecordsForContainer] container path: " + containerPath);



        context.getContentResolver().delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.Media.DATA + " LIKE ?",
                new String[]{containerPath + "%"}
        );
    }


    public void selectMediaTable() {
        Log.i(TAG, "[selectMediaTable]");
        ContentResolver resolver = context.getContentResolver();

        Cursor c = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if(c.moveToFirst()) {
            do {
                long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                String data = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                Log.d(TAG, "[selectMediaTable] id: " + id + " | data: " + data);
            } while(c.moveToNext());
        } else {
            Log.w(TAG, "[selectMediaTable] there are no images in table");
        }

        c.close();
    }

    public String getFilePathFromURI(Uri contentUri) {
        if(contentUri.getScheme().equals("file")) {
            return contentUri.getPath();
        }

        Cursor cursor = null;
        try {
            String[] proj = {
                    MediaStore.Images.Media.DATA
            };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            //String[] elements = cursor.getString(column_index).split("/");
            //return elements[elements.length - 1];
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


}
