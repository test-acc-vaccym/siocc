package name.xunicorn.iocipherbrowserext.components;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import name.xunicorn.iocipherbrowserext.models.Containers;

public class DBHelper extends SQLiteOpenHelper {
    final static String db = "iocipher_browser_ext";
    final static int version = 1;


    final String sql_containers = "CREATE TABLE " + Containers.TABLE + " ("
            + Containers.COLUMN_ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Containers.COLUMN_PATH     + " TEXT NOT NULL, "
            + Containers.COLUMN_INTERNAL + " INTEGER DEFAULT 0, "
            + Containers.COLUMN_EXTERNAL + " INTEGER DEFAULT 0, "
            + Containers.COLUMN_CUSTOM   + " INTEGER DEFAULT 0"
            + ");"
            ;

    public DBHelper(Context context) {
        super(context, db, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(sql_containers);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
