package name.xunicorn.iocipherbrowserext.components;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IOCipherProviderHelper {
    private static final String TAG = "IOCipherProviderHelper";

    private static IOCipherProviderHelper instance;

    private List<ParcelFileDescriptor> descriptors;

    private IOCipherProviderHelper() {
        descriptors = new ArrayList<ParcelFileDescriptor>();
    }

    public static IOCipherProviderHelper initialize() {
        if(instance == null) {
            instance = new IOCipherProviderHelper();
        }

        return instance;
    }

    public void addDescriptor(ParcelFileDescriptor descr) {
        Log.i(TAG, "[addDescriptor] descriptors count: " + descriptors.size());

        descriptors.add(descr);
    }

    public void closeAll() {
        Log.i(TAG, "[closeAll] descriptors count: " + descriptors.size());

        for(ParcelFileDescriptor desc: descriptors) {
            try {
                desc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        descriptors = new ArrayList<ParcelFileDescriptor>();
    }
}
