package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.content.Context;
import android.hardware.camera2.DngCreator;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Size;

import com.xrite.imageclasses.UcpImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jsnader on 1/16/17.
 */

public class RecordImagesAsyncTask extends AsyncTask<Void, Void, Void> {
    private Context mContext;
    private UcpImage mUcpImage;
    private ByteBuffer mRawBuffer;
    private String mTimestamp;
    private int mSharedPrefSession;

    public RecordImagesAsyncTask(Context pContext, UcpImage pUcpImage, int pSharedPrefSession, String pTimestamp)
    {
        mContext = pContext;
        mUcpImage = pUcpImage;
        mRawBuffer = ByteBuffer.wrap(mUcpImage.getRawBytes());
        mSharedPrefSession = pSharedPrefSession;
        mTimestamp = pTimestamp;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected Void doInBackground(Void... params) {
        recordImageData();
        return null;
    }

    private void recordImageData()
    {
        File customDirectory = new File(Environment.getExternalStorageDirectory(), "rawRgbCaptureData");
        if (!customDirectory.exists()) {
            customDirectory.mkdir();
        }
        File sessionDirectory = new File(customDirectory, "Session" + mSharedPrefSession);
        if (!sessionDirectory.exists()) {
            sessionDirectory.mkdir();
        }

        saveJpeg(sessionDirectory);
        saveRaw(sessionDirectory);

    }

    private void saveJpeg(File pSessionDirectory) {
        File jpegFile = new File(pSessionDirectory, "JPEG_" + mTimestamp + ".jpg");

        ByteBuffer buffer = ByteBuffer.wrap(mUcpImage.getImageBytes());
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(jpegFile);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(output != null) {
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MediaScannerConnection.scanFile(mContext, new String[]{jpegFile.getAbsolutePath()}, null, new MyOnScanCompletedListener());
    }

    private void saveRaw(File pSessionDirectory) {
        File rawFile = new File(pSessionDirectory, "RAW_" + mTimestamp + ".dng");

        DngCreator dngCreator = new DngCreator(mUcpImage.getCameraCharacteristics(), mUcpImage.getCaptureResult());
        FileOutputStream output = null;
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(mUcpImage.getRawBytes());
            byteBuffer.rewind();
            output = new FileOutputStream(rawFile);
            dngCreator.writeByteBuffer(output,
                    new Size(mUcpImage.getRawImageSize().getWidth(), mUcpImage.getRawImageSize().getHeight()),
                    byteBuffer, 0);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(output != null) {
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MediaScannerConnection.scanFile(mContext, new String[]{rawFile.getAbsolutePath()}, null, new MyOnScanCompletedListener());
    }
}
