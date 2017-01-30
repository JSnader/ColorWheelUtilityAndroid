package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

/**
 * Created by jsnader on 1/16/17.
 */

public class ListenerFileScannerCompletion implements MediaScannerConnection.OnScanCompletedListener {
    @Override
    public void onScanCompleted(String path, Uri uri) {
        if (uri != null && path != null) {
            Log.d(Constants.LOG_TAG, String.format("Scanned path %s -> URI = %s", path, uri.toString()));
        }
    }
}
