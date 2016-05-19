
package name.xunicorn.iocipherbrowserext.providers;

// inspired by https://github.com/commonsguy/cw-omnibus/tree/master/ContentProvider/Pipe

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.util.Log;
import android.webkit.MimeTypeMap;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import name.xunicorn.iocipherbrowserext.components.IOCipherProviderHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class IOCipherContentProvider extends ContentProvider {
    public static final String TAG = "IOCipherContentProvider";

    public static final String AUTHORITY = "name.xunicorn.iocipherbrowserext";

    public static final Uri FILES_URI = Uri
            .parse("content://" + AUTHORITY);
    private MimeTypeMap mimeTypeMap;

    @Override
    public boolean onCreate() {
        mimeTypeMap = MimeTypeMap.getSingleton();
        return true;
    }

    @Override
    public String getType(Uri uri) {
        Log.i(TAG, "[getType] uri: " + uri);

        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        return mimeTypeMap.getMimeTypeFromExtension(fileExtension);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        ParcelFileDescriptor[] pipe = null;
        InputStream in = null;

        try {
            pipe = ParcelFileDescriptor.createPipe();
            String path = uri.getPath();
            Log.i(TAG, "[openFile] streaming " + path);
            // BufferedInputStream could help, AutoCloseOutputStream conflicts

            in = new FileInputStream(new File(path));

            new PipeFeederThread(in, new AutoCloseOutputStream(pipe[1])).start();
        } catch (IOException e) {
            Log.e(TAG, "[openFile] Error opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                    + uri.toString());
        }

        return (pipe[0]);
    }

    //region Not supported
    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }
    //endregion

    static class PipeFeederThread extends Thread {
        InputStream in;
        OutputStream out;

        PipeFeederThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;

            try {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "File transfer failed:", e);
            }
        }
    }
}
