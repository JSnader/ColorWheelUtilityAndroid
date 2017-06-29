/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.content.Context;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.DngCreator;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import com.xrite.imageclasses.UcpImage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static rawrgbcamera.xrite.com.rawrgbcameracapture.RawRgbCapture.OVERLAY_PADDING;
import static rawrgbcamera.xrite.com.rawrgbcameracapture.RawRgbCapture.PORTION_OF_IMAGE_DIVISOR;

/**
 * Created by jsnader on 1/16/17.
 */

public class RecordRgbsAsyncTask extends AsyncTask<Void, Void, Void> {
    private Context mContext;
    private UcpImage mUcpImage;
    private ByteBuffer mRawBuffer;
    private TextView mTextView;

    public static int mSharedPrefSession;
    public static String mFilePrefix;

    private static boolean mIsCheckingPosition = false;
    private static double[] mCorrectedPixel = new double[3];
    private static double[] mBayerPixel = new double[4];
    private double mAverageR, mAverageG, mAverageB;
    private String mTimestamp;
    private ListenerDataCompletion mCallback;

    public RecordRgbsAsyncTask(Context pContext, ListenerDataCompletion pCallback, UcpImage pUcpImage, TextView pTextView, int pSharedPrefSession, String pTimestamp)
    {
        mContext = pContext;
        mCallback = pCallback;
        mUcpImage = pUcpImage;
        mTextView = pTextView;
        mRawBuffer = ByteBuffer.wrap(mUcpImage.getRawBytes());
        mSharedPrefSession = pSharedPrefSession;
        mTimestamp = pTimestamp;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void pUnused) {
        super.onPostExecute(pUnused);
        mCallback.onDataCompletion();
    }

    @Override
    protected void onProgressUpdate(Void... pUnusedValues) {
        super.onProgressUpdate(pUnusedValues);
        String rRounded = String.format("%.2f", mAverageR);
        String gRounded = String.format("%.2f", mAverageG);
        String bRounded = String.format("%.2f", mAverageB);
        mTextView.setText("R: " + rRounded + "\n" +
                          "G: " + gRounded + "\n" +
                          "B: " + bRounded);
    }

    @Override
    protected Void doInBackground(Void... pUnusuedParams) {
        try
        {
            recordRgbData(mUcpImage);
        } catch (IOException e) {
            e.printStackTrace();
            mCallback.onDataCompletion();
        }

        if(mIsCheckingPosition) {
            recordImageData();
        }

        publishProgress();

        return null;
    }

    private void reformatForRggb(int pBayerFormat) {
        switch (pBayerFormat) {
            case 0: //RGGB
                mCorrectedPixel[0] = mBayerPixel[0];
                mCorrectedPixel[1] = (mBayerPixel[1] + mBayerPixel[2]) / 2.0;
                mCorrectedPixel[2] = mBayerPixel[3];
                break;
            case 1: //GRBG
                mCorrectedPixel[0] = mBayerPixel[1];
                mCorrectedPixel[1] = (mBayerPixel[0] + mBayerPixel[3]) / 2.0;
                mCorrectedPixel[2] = mBayerPixel[2];
                break;
            case 2: //GBRG
                mCorrectedPixel[0] = mBayerPixel[2];
                mCorrectedPixel[1] = (mBayerPixel[3] + mBayerPixel[0]) / 2.0;
                mCorrectedPixel[2] = mBayerPixel[1];
                break;
            case 3: //BGGR
                mCorrectedPixel[0] = mBayerPixel[3];
                mCorrectedPixel[1] = (mBayerPixel[1] + mBayerPixel[2]) / 2.0;
                mCorrectedPixel[2] = mBayerPixel[0];
                break;
            case 4: //RGB Sensor is not Bayer; output has 3 16-bit values for each pixel, instead of just 1 16-bit value per pixel.
                break;
        }
    }

    private void getRawPixelBayerFormat(int pRow, int pCol) {

        mRawBuffer.order(ByteOrder.nativeOrder());

        if (pCol % 2 == 0) //Even column
        {
            if (pRow % 2 == 0) //Even row or Red Channel
            {
                mRawBuffer.position((pCol * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[0] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position(((pCol + 1) * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[1] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((pCol * 2) + ((pRow + 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[2] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position(((pCol + 1) * 2) + ((pRow + 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[3] = mRawBuffer.getShort() & 0x7FFF;
                if (mIsCheckingPosition) {
                    mRawBuffer.putShort((short) 1000);
                }
            } else { //Odd row or Green 2 Channel
                mRawBuffer.position((pCol * 2) + ((pRow - 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[0] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((((pCol + 1) * 2) + ((pRow - 1) * mUcpImage.getRawImageStride())));
                mBayerPixel[1] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((pCol * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[2] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position(((pCol + 1) * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[3] = mRawBuffer.getShort() & 0x7FFF;
                if (mIsCheckingPosition) {
                    mRawBuffer.putShort((short) 1000);
                }
            }
        } else //Odd column
        {
            if (pRow % 2 == 0) //Even row
            {
                mRawBuffer.position(((pCol - 1) * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[0] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((pCol * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[1] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position(((pCol - 1) * 2) + ((pRow + 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[2] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((pCol * 2) + ((pRow + 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[3] = mRawBuffer.getShort() & 0x7FFF;
                if (mIsCheckingPosition) {
                    mRawBuffer.putShort((short) 1000);
                }
            } else { //Odd row
                mRawBuffer.position(((pCol - 1) * 2) + ((pRow - 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[0] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((pCol * 2) + ((pRow - 1) * mUcpImage.getRawImageStride()));
                mBayerPixel[1] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position(((pCol - 1) * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[2] = mRawBuffer.getShort() & 0x7FFF;
                mRawBuffer.position((pCol * 2) + (pRow * mUcpImage.getRawImageStride()));
                mBayerPixel[3] = mRawBuffer.getShort() & 0x7FFF;
                if (mIsCheckingPosition) {
                    mRawBuffer.putShort((short) 1000);
                }
            }
        }
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

        MediaScannerConnection.scanFile(mContext, new String[]{jpegFile.getAbsolutePath()}, null, new ListenerFileScannerCompletion());
    }

    private void saveRaw(File pSessionDirectory) {
        File rawFile = new File(pSessionDirectory, "RAW_" + mTimestamp + "Debug.dng");
        mFilePrefix = "RAW_" + mTimestamp;
        DngCreator dngCreator = new DngCreator(mUcpImage.getCameraCharacteristics(), mUcpImage.getCaptureResult());
        FileOutputStream output = null;
        try {
            mRawBuffer.rewind();
            output = new FileOutputStream(rawFile);
            dngCreator.writeByteBuffer(output,
                                       new Size(mUcpImage.getRawImageSize().getWidth(), mUcpImage.getRawImageSize().getHeight()),
                                       mRawBuffer, 0);
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

        MediaScannerConnection.scanFile(mContext, new String[]{rawFile.getAbsolutePath()}, null, new ListenerFileScannerCompletion());
    }

    private void getRawPixel(int pRow, int pCol) {
        Integer filterArrangement = mUcpImage.getCameraCharacteristics().get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
        getRawPixelBayerFormat(pRow, pCol);
        reformatForRggb(filterArrangement);
    }

    private void recordRgbData(UcpImage pImage) throws IOException {
        float regionOfInterestLength = mUcpImage.getRawImageSize().getHeight() / (PORTION_OF_IMAGE_DIVISOR) * OVERLAY_PADDING;
        int startRow = (int) ((mUcpImage.getRawImageSize().getHeight() / 2.0f) - (regionOfInterestLength / 2.0f));
        int startCol = (int) ((mUcpImage.getRawImageSize().getWidth() / 2.0f) - (regionOfInterestLength / 2.0f));
        int endRow = (int) ((mUcpImage.getRawImageSize().getHeight() / 2.0f) + (regionOfInterestLength / 2.0f));
        int endCol = (int) ((mUcpImage.getRawImageSize().getWidth() / 2.0f) + (regionOfInterestLength / 2.0f));
        Point centerPoint = new Point((int)((float)mUcpImage.getRawImageSize().getWidth() / 2.0f), (int)((float)mUcpImage.getRawImageSize().getHeight() / 2.0f));
        int radius = (int) (regionOfInterestLength / 2.0f * OVERLAY_PADDING);

        PrintWriter writer = null;
        BufferedWriter writerCombined = null;
        File rgbRawFile = null, combinedRgbRawFile = null;
        File customDirectory = null;
        File sessionDirectory = null;
        double averageR = 0, averageG = 0, averageB = 0;
        try {
            customDirectory = new File(Environment.getExternalStorageDirectory(), "rawRgbCaptureData");
            if (!customDirectory.exists()) {
                customDirectory.mkdir();
            }
            sessionDirectory = new File(customDirectory, "Session" + mSharedPrefSession);
            if(!sessionDirectory.exists()) {
                sessionDirectory.mkdir();
            }
            rgbRawFile = new File(sessionDirectory, "RAW_" + mTimestamp + "rgbs.txt");
            mFilePrefix = "RAW_" + mTimestamp;
            combinedRgbRawFile = new File(sessionDirectory, "RAW_combinedRGBs.txt");

            writer = new PrintWriter(new BufferedWriter(new FileWriter(rgbRawFile)),false);
            writerCombined = new BufferedWriter(new FileWriter(combinedRgbRawFile, true));
            double numberOfPixels = 0;
            final String COMMA = ",";
            final String NEWLINE = "\n";
            long startTime = System.currentTimeMillis();
            for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
                for (int colIndex = startCol; colIndex < endCol; colIndex++) {
                    if(insideCircle(colIndex, rowIndex, centerPoint, radius)) {
                        getRawPixel(rowIndex, colIndex);
                        averageR += mCorrectedPixel[0];
                        averageG += mCorrectedPixel[1];
                        averageB += mCorrectedPixel[2];
                        writer.print(mCorrectedPixel[0] + COMMA + mCorrectedPixel[1] + COMMA + mCorrectedPixel[2] + NEWLINE);
                        numberOfPixels++;
                    }
                }
            }
            writer.flush();
            Log.e("Check", "Time for loop equals " + (System.currentTimeMillis() - startTime));
            writerCombined.append("" + (averageR / numberOfPixels) + ", " +
                    (averageG / numberOfPixels) + ", " +
                    (averageB / numberOfPixels) + "\n");

            mAverageR = averageR / numberOfPixels;
            mAverageG = averageG / numberOfPixels;
            mAverageB = averageB / numberOfPixels;

        } catch (IOException e) {
            Log.w(Constants.LOG_TAG, "IOException occurred at " + e.toString());
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (writerCombined != null) {
                writerCombined.close();
            }
        }

        if (rgbRawFile != null) {
            MediaScannerConnection.scanFile(mContext, new String[]{rgbRawFile.getAbsolutePath()}, null, new ListenerFileScannerCompletion());
        }
        if (combinedRgbRawFile != null) {
            MediaScannerConnection.scanFile(mContext, new String[]{combinedRgbRawFile.getAbsolutePath()}, null, new ListenerFileScannerCompletion());
        }
    }

    private boolean insideCircle(int pXcoord, int pYcoord, Point pCenterPoint, int pRadius){
        boolean isWithin = false;

        double distance = Math.sqrt((Math.pow((pXcoord - pCenterPoint.x), 2) + (Math.pow((pYcoord - pCenterPoint.y), 2))));
        if(distance < pRadius){
            isWithin = true;
        }

        return isWithin;
    }
}
