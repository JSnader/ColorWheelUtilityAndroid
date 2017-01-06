package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RawRgbCapture extends AppCompatActivity {
    private static AudioManager         mAudioManager;
    private static CoordinatorLayout    mCoordinatorLayout;
    private SeekBar                     mExposureBar, mIsoBar, mOverlaySizeBar;
    private TextView                    mExposureTextView, mIsoTextView;
    private static TextView             mRgbTextView;
    private Switch                      mFocusLockSwitch, mExposureLockSwitch;
    private static boolean              mIsCheckingPosition = false;
    private int                         mViewWidth = 0;
    private static Activity             mActivityContext;
    private SharedPreferences           mSharedPreferences;

    private static int                  mSharedPrefSession;
    final GestureDetector               mClickDetector = new GestureDetector(RawRgbCapture.this, new GestureDetector.SimpleOnGestureListener(){
                                                        @Override
                                                        public boolean onSingleTapUp(MotionEvent pEvent) {
                                                            boolean visible = (mCoordinatorLayout.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                                                            if(visible) {
                                                                hideSystemUI();
                                                            }else {
                                                                showSystemUI();
                                                            }
                                                            return true;
                                                        }
                                                    });

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static int          PORTION_OF_IMAGE_DIVISOR = 1;
    private static final int    MAX_IMAGES = 5;
    private static final long   PRECAPTURE_TIMEOUT_MS = 1000;
    private static final double ASPECT_RATIO_TOLERANCE = 0.005;
    private static final String TAG = "Camera2RawFragment";

    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPENED = 1;
    private static final int STATE_PREVIEW = 2;

    private static final int STATE_WAITING_FOR_3A_CONVERGENCE = 3;

    /**mSharedPrefSession
     * An {@link OrientationEventListener} used to determine when device rotation has occurred.
     * This is mainly necessary for when the device is rotated by 180 degrees, in which case
     * onCreate or onConfigurationChanged is not called as the view dimensions remain the same,
     * but the orientation of the has changed, and thus the preview rotation must be updated.
     */
    private OrientationEventListener    mOrientationListener;
    private static String               mCurrentDateTime;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            mCoordinatorLayout.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_rgb_capture);
        mActivityContext = RawRgbCapture.this;
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_LOGGING, MODE_PRIVATE);
        mSharedPrefSession = mSharedPreferences.getInt(Constants.SESSION_NUMBER, 1);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(Constants.SESSION_NUMBER, mSharedPrefSession + 1);
        editor.apply();
        editor.commit();

        //Set maximum volume for device so that we can here the shutter sound.
        int origionalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mExposureTextView = (TextView) findViewById(R.id.textViewExposure);
        mIsoTextView = (TextView) findViewById(R.id.textViewISO);
        mRgbTextView = (TextView) findViewById(R.id.textViewRgbs);
        mRgbTextView.bringToFront();
        mExposureBar = (SeekBar) findViewById(R.id.seekBarExposure);
        mFocusLockSwitch = (Switch) findViewById(R.id.switchFocusLock);
        mExposureLockSwitch = (Switch) findViewById(R.id.switchExposureLock);
        mExposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int exposureLevel = progress - (int) Math.ceil((double) mExposureBar.getMax() / (double) 2.0f);
                mExposureTextView.setText("Exposure: " + exposureLevel);
                if (mPreviewRequestBuilder != null) {
                    synchronized (mCameraStateLock) {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureLevel);
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.EXPOSURE_SETTING, progress);
                        editor.apply();
                        editor.commit();
                        try {
                            if(fromUser) {
                                mCaptureSession.setRepeatingRequest(
                                        mPreviewRequestBuilder.build(),
                                        mPreCaptureCallback, mBackgroundHandler);
                            }
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mOverlaySizeBar = (SeekBar) findViewById(R.id.seekBarSize);
        mOverlaySizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PORTION_OF_IMAGE_DIVISOR = progress + 1;
                ViewGroup.LayoutParams params = ((ImageView) findViewById(R.id.imageView)).getLayoutParams();//.width
                params.width = mViewWidth / PORTION_OF_IMAGE_DIVISOR;
                params.height = mViewWidth / PORTION_OF_IMAGE_DIVISOR;
                ((ImageView) findViewById(R.id.imageView)).setLayoutParams(params);
                ((ImageView) findViewById(R.id.imageView)).invalidate();
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putInt(Constants.OVERLAY_SIZE_SETTING, progress);
                editor.apply();
                editor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mIsoBar = (SeekBar) findViewById(R.id.seekBarISO);
        mIsoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Range<Integer> range2 = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                int max = range2.getUpper();
                int min = range2.getLower();
                int iso = progress + min;
                mIsoTextView.setText("ISO: " + iso);
                if (mPreviewRequestBuilder != null) {
                    synchronized (mCameraStateLock) {
                        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.ISO_SETTING, progress);
                        editor.apply();
                        editor.commit();
                        try {
                            if(fromUser) {
                                mCaptureSession.setRepeatingRequest(
                                        mPreviewRequestBuilder.build(),
                                        mPreCaptureCallback, mBackgroundHandler);
                            }
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mFocusLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                synchronized (mCameraStateLock) {
                    if (isChecked) {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                    } else {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                    }
                    try {
                        mCaptureSession.setRepeatingRequest(
                                mPreviewRequestBuilder.build(),
                                mPreCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mExposureLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                synchronized (mCameraStateLock) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, isChecked);
                    try {
                        mCaptureSession.setRepeatingRequest(
                                mPreviewRequestBuilder.build(),
                                mPreCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureStillPictureLocked();
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(10);
            }
        });
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);

        // Setup a new OrientationEventListener.  This is used to handle rotation events like a
        // 180 degree rotation that do not normally trigger a call to onCreate to do view re-layout
        // or otherwise cause the preview TextureView's size to change.
        mOrientationListener = new OrientationEventListener(RawRgbCapture.this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (mTextureView != null && mTextureView.isAvailable()) {
//                    configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
                }
            }
        };

        // Assume thisActivity is the current activity
        final int PERMISSIONS_ID = 101;
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(RawRgbCapture.this, Manifest.permission.CAMERA);
        int writeExtPermissionCheck = ContextCompat.checkSelfPermission(RawRgbCapture.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (PackageManager.PERMISSION_GRANTED != writeExtPermissionCheck ||
                PackageManager.PERMISSION_GRANTED != cameraPermissionCheck) {
            ActivityCompat.requestPermissions(RawRgbCapture.this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_ID);
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_raw_rgb_capture, menu);
        return true;
    }

    private void deleteDirectory(File pDirectory){
        if (pDirectory.exists()) {
            File[] filesInDirectory = pDirectory.listFiles();
            if(null != filesInDirectory) {
                for(int index = 0; index < filesInDirectory.length; index++) {
                    if(filesInDirectory[index].isDirectory()) {
                        deleteDirectory(filesInDirectory[index]);
                    }
                    else {
                        filesInDirectory[index].delete();
                    }
                }
            }
            pDirectory.delete();
            MediaScannerConnection.scanFile(RawRgbCapture.this, new String[]{pDirectory.getAbsolutePath()}, null, new MyOnScanCompletedListener());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(RawRgbCapture.this, R.style.MyAlertDialogStyle);
            builder.setTitle("Log Clear");
            builder.setMessage("Do not confirm unless you want the logs to be completely erased. This will clear up all logging on the device.");
            builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RawRgbCapture.this, R.style.MyAlertDialogStyle);
                    builder.setTitle("Are you sure James?");
                    builder.setMessage("This will clear the logs and remove previous logging.  Take anything off you might need first.");
                    builder.setPositiveButton("Sure, DELETE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteDirectory(new File(Environment.getExternalStorageDirectory(), "rawRgbCaptureData"));
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events of a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            synchronized (mCameraStateLock) {
                mPreviewSize = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * An additional thread for running tasks that shouldn't block the UI.  This is used for all
     * callbacks from the {@link CameraDevice} and {@link CameraCaptureSession}s.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A counter for tracking corresponding {@link CaptureRequest}s and {@link CaptureResult}s
     * across the {@link CameraCaptureSession} capture callbacks.
     */
    private final AtomicInteger mRequestCounter = new AtomicInteger();

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * A lock protecting camera state.
     */
    private final Object mCameraStateLock = new Object();

    // *********************************************************************************************
    // State protected by mCameraStateLock.
    //
    // The following state is used across both the UI and background threads.  Methods with "Locked"
    // in the name expect mCameraStateLock to be held while calling.

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the open {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link CameraCharacteristics} for the currently configured camera device.
     */
    private CameraCharacteristics mCharacteristics;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A reference counted holder wrapping the {@link ImageReader} that handles JPEG image captures.
     * This is used to allow us to clean up the {@link ImageReader} when all background tasks using
     * its {@link Image}s have completed.
     */
    private RefCountedAutoCloseable<ImageReader> mJpegImageReader;

    /**
     * A reference counted holder wrapping the {@link ImageReader} that handles RAW image captures.
     * This is used to allow us to clean up the {@link ImageReader} when all background tasks using
     * its {@link Image}s have completed.
     */
    private RefCountedAutoCloseable<ImageReader> mRawImageReader;

    /**
     * Whether or not the currently configured camera device is fixed-focus.
     */
    private boolean mNoAFRun = false;

    /**
     * Number of pending user requests to capture a photo.
     */
    private int mPendingUserCaptures = 0;

    /**
     * Request ID to {@link ImageSaver.ImageSaverBuilder} mapping for in-progress JPEG captures.
     */
    private final TreeMap<Integer, ImageSaver.ImageSaverBuilder> mJpegResultQueue = new TreeMap<>();

    /**
     * Request ID to {@link ImageSaver.ImageSaverBuilder} mapping for in-progress RAW captures.
     */
    private final TreeMap<Integer, ImageSaver.ImageSaverBuilder> mRawResultQueue = new TreeMap<>();

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * The state of the camera device.
     *
     * @see #mPreCaptureCallback
     */
    private int mState = STATE_CLOSED;

    /**
     * Timer to use with pre-capture sequence to ensure a timely capture if 3A convergence is taking
     * too long.
     */
    private long mCaptureTimer;

    //**********************************************************************************************

    /**
     * {@link CameraDevice.StateCallback} is called when the currently active {@link CameraDevice}
     * changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here if
            // the TextureView displaying this has been set up.
            synchronized (mCameraStateLock) {
                mState = STATE_OPENED;
                mCameraOpenCloseLock.release();
                mCameraDevice = cameraDevice;

                if (mPreviewSize != null && mTextureView.isAvailable()) {
                    createCameraPreviewSessionLocked();
                }
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            synchronized (mCameraStateLock) {
                mState = STATE_CLOSED;
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Received camera device error: " + error);
            synchronized (mCameraStateLock) {
                mState = STATE_CLOSED;
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }

            if (null != RawRgbCapture.this) {
                finish();
            }
        }
    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * JPEG image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnJpegImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            dequeueAndSaveImage(mJpegResultQueue, mJpegImageReader);
        }
    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * RAW image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnRawImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            dequeueAndSaveImage(mRawResultQueue, mRawImageReader);
        }
    };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events for the preview and
     * pre-capture sequence.
     */
    private CameraCaptureSession.CaptureCallback mPreCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private boolean hasSetupIsoSeekbar = false;

        private void process(CaptureResult result) {
            synchronized (mCameraStateLock) {
                switch (mState) {
                    case STATE_PREVIEW: {
                        // We have nothing to do when the camera preview is running normally.
                        break;
                    }
                    case STATE_WAITING_FOR_3A_CONVERGENCE: {
                        boolean readyToCapture = true;
                        if (!mNoAFRun) {
                            int afState = result.get(CaptureResult.CONTROL_AF_STATE);

                            // If auto-focus has reached locked state, we are ready to capture
                            readyToCapture = true;
//                                    (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
//                                            afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
                        }

                        // If we are running on an non-legacy device, we should also wait until
                        // auto-exposure and auto-white-balance have converged as well before
                        // taking a picture.
                        if (!isLegacyLocked()) {
                            int aeState = CaptureResult.CONTROL_AE_STATE_CONVERGED;//result.get(CaptureResult.CONTROL_AE_STATE);
                            int awbState = CaptureResult.CONTROL_AWB_STATE_CONVERGED;//result.get(CaptureResult.CONTROL_AWB_STATE);

                            readyToCapture = readyToCapture &&
                                    aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED &&
                                    awbState == CaptureResult.CONTROL_AWB_STATE_CONVERGED;
                        }

                        // If we haven't finished the pre-capture sequence but have hit our maximum
                        // wait timeout, too bad! Begin capture anyway.
//                        if (!readyToCapture && hitTimeoutLocked()) {
                        Log.w(TAG, "Timed out waiting for pre-capture sequence to complete.");
                        readyToCapture = true;
//                        }

                        if (readyToCapture && mPendingUserCaptures > 0) {
                            // Capture once for each user tap of the "Picture" button.
                            while (mPendingUserCaptures > 0) {
                                captureStillPictureLocked();
                                mPendingUserCaptures--;
                            }
                            // After this, the camera will go back to the normal state of preview.
                            mState = STATE_PREVIEW;
                        }
                    }
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            if (!hasSetupIsoSeekbar) {
                Range<Integer> isoRange = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                if (mSharedPreferences.getInt(Constants.ISO_SETTING, Constants.BAD_SHARED_PREF_INT) != Constants.BAD_SHARED_PREF_INT) {
                    mIsoBar.setProgress(mSharedPreferences.getInt(Constants.ISO_SETTING, Constants.BAD_SHARED_PREF_INT));
                } else {
                    mIsoBar.setProgress(request.get(CaptureRequest.SENSOR_SENSITIVITY) - isoRange.getLower());
                }

                hasSetupIsoSeekbar = true;
            }
            process(result);
        }
    };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles the still JPEG and RAW capture
     * request.
     */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                     long timestamp, long frameNumber) {
            mCurrentDateTime = generateTimestamp();
            File customDirectory = new File(Environment.getExternalStorageDirectory(), "rawRgbCaptureData");
            if (!customDirectory.exists()) {
                customDirectory.mkdir();
            }
            File sessionDirectory = new File(customDirectory, "Session" + mSharedPrefSession);
            if(!sessionDirectory.exists()) {
                sessionDirectory.mkdir();
            }
            File rawFile = new File(sessionDirectory,
                    "RAW_" + mCurrentDateTime + ".dng");
            File jpegFile = new File(sessionDirectory,
                    "JPEG_" + mCurrentDateTime + ".jpg");

            // Look up the ImageSaverBuilder for this request and update it with the file name
            // based on the capture start time.
            ImageSaver.ImageSaverBuilder jpegBuilder;
            ImageSaver.ImageSaverBuilder rawBuilder;
            int requestId = (int) request.getTag();
            synchronized (mCameraStateLock) {
                jpegBuilder = mJpegResultQueue.get(requestId);
                rawBuilder = mRawResultQueue.get(requestId);
            }

            if (jpegBuilder != null) jpegBuilder.setFile(jpegFile, mCurrentDateTime);
            if (rawBuilder != null) rawBuilder.setFile(rawFile, mCurrentDateTime);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            int requestId = (int) request.getTag();
            ImageSaver.ImageSaverBuilder jpegBuilder;
            ImageSaver.ImageSaverBuilder rawBuilder;
            StringBuilder sb = new StringBuilder();

            // Look up the ImageSaverBuilder for this request and update it with the CaptureResult
            synchronized (mCameraStateLock) {
                jpegBuilder = mJpegResultQueue.get(requestId);
                rawBuilder = mRawResultQueue.get(requestId);

                // If we have all the results necessary, save the image to a file in the background.
                handleCompletionLocked(requestId, jpegBuilder, mJpegResultQueue);
                handleCompletionLocked(requestId, rawBuilder, mRawResultQueue);

                if (jpegBuilder != null) {
                    jpegBuilder.setResult(result);
//                    sb.append("Saving JPEG as: ");
//                    sb.append(jpegBuilder.getSaveLocation());
                }
                if (rawBuilder != null) {
                    rawBuilder.setResult(result);
                    sb.append("Raw picture info saved out.");
//                    if (jpegBuilder != null) sb.append(", ");
//                    sb.append("Saving RAW as: ");
//                    sb.append(rawBuilder.getSaveLocation());
                }
                finishedCaptureLocked();
            }

//            showToast(sb.toString());
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureFailure failure) {
            int requestId = (int) request.getTag();
            synchronized (mCameraStateLock) {
                mJpegResultQueue.remove(requestId);
                mRawResultQueue.remove(requestId);
                finishedCaptureLocked();
            }
        }
    };

    /**
     * A {@link Handler} for showing {@link Toast}s on the UI thread.
     */
    private final Handler mMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Activity activity = RawRgbCapture.this;
            if (activity != null) {
                Toast.makeText(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        openCamera();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we should
        // configure the preview bounds here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        if (mOrientationListener != null && mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }

    @Override
    public void onPause() {
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }
        closeCamera();
        stopBackgroundThread();
        finish();
        super.onPause();
    }

    /**
     * Sets up state related to camera that is needed before opening a {@link CameraDevice}.
     */
    private boolean setUpCameraOutputs() {
        Activity activity = RawRgbCapture.this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            ErrorDialog.buildErrorDialog("This device doesn't support Camera2 API.").
                    show(getFragmentManager(), "dialog");
            return false;
        }
        try {
            // Find a CameraDevice that supports RAW captures, and configure state.
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We only use a camera that supports RAW in this sample.
                if (!contains(characteristics.get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                Size largestJpeg = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                Size largestRaw = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)),
                        new CompareSizesByArea());

                synchronized (mCameraStateLock) {
                    // Set up ImageReaders for JPEG and RAW outputs.  Place these in a reference
                    // counted wrapper to ensure they are only closed when all background tasks
                    // using them are finished.
                    if (mJpegImageReader == null || mJpegImageReader.getAndRetain() == null) {
                        mJpegImageReader = new RefCountedAutoCloseable<>(
                                ImageReader.newInstance(largestJpeg.getWidth(),
                                        largestJpeg.getHeight(), ImageFormat.JPEG, MAX_IMAGES));
                    }
                    mJpegImageReader.get().setOnImageAvailableListener(
                            mOnJpegImageAvailableListener, mBackgroundHandler);

                    if (mRawImageReader == null || mRawImageReader.getAndRetain() == null) {
                        mRawImageReader = new RefCountedAutoCloseable<>(
                                ImageReader.newInstance(largestRaw.getWidth(),
                                        largestRaw.getHeight(), ImageFormat.RAW_SENSOR, MAX_IMAGES));
                    }
                    mRawImageReader.get().setOnImageAvailableListener(
                            mOnRawImageAvailableListener, mBackgroundHandler);

                    mCharacteristics = characteristics;
                    mCameraId = cameraId;
                }
                return true;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // If we found no suitable cameras for capturing RAW, warn the user.
        ErrorDialog.buildErrorDialog("This device doesn't support capturing RAW photos").
                show(getFragmentManager(), "dialog");
        return false;
    }

    /**
     * Opens the camera specified by {@link #mCameraId}.
     */
    private void openCamera() {
        if (!setUpCameraOutputs()) {
            return;
        }

        Activity activity = RawRgbCapture.this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            // Wait for any previously running session to finish.
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId;
            Handler backgroundHandler;
            synchronized (mCameraStateLock) {
                cameraId = mCameraId;
                backgroundHandler = mBackgroundHandler;
            }

            // Attempt to open the camera. mStateCallback will be called on the background handler's
            // thread when this succeeds or fails.
            manager.openCamera(cameraId, mStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            synchronized (mCameraStateLock) {

                // Reset state and clean up resources used by the camera.
                // Note: After calling this, the ImageReaders will be closed after any background
                // tasks saving Images from these readers have been completed.
                mPendingUserCaptures = 0;
                mState = STATE_CLOSED;
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                if (null != mJpegImageReader) {
                    mJpegImageReader.close();
                    mJpegImageReader = null;
                }
                if (null != mRawImageReader) {
                    mRawImageReader.close();
                    mRawImageReader = null;
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        synchronized (mCameraStateLock) {
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            synchronized (mCameraStateLock) {
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assignSavedOrDefaultSettings() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Range<Integer> exposureRange = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);

                mExposureBar.setMax(Math.abs(exposureRange.getLower()) + exposureRange.getUpper());
                if (mSharedPreferences.getInt(Constants.EXPOSURE_SETTING, Constants.BAD_SHARED_PREF_INT) != Constants.BAD_SHARED_PREF_INT) {
                    mExposureBar.setProgress(mSharedPreferences.getInt(Constants.EXPOSURE_SETTING, Constants.BAD_SHARED_PREF_INT));
                } else {
                    mExposureBar.setProgress(mExposureBar.getMax() / 2);
                }
                Range<Integer> isoRange = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                mIsoBar.setMax(Math.abs(isoRange.getUpper() - isoRange.getLower()));
                if (mSharedPreferences.getInt(Constants.ISO_SETTING, Constants.BAD_SHARED_PREF_INT) != Constants.BAD_SHARED_PREF_INT) {
                    mIsoBar.setProgress(mSharedPreferences.getInt(Constants.ISO_SETTING, Constants.BAD_SHARED_PREF_INT));
                } else {
                    mIsoBar.setProgress(0);//Keep it fast as a default.
                }
                if (mSharedPreferences.getInt(Constants.OVERLAY_SIZE_SETTING, Constants.BAD_SHARED_PREF_INT) != Constants.BAD_SHARED_PREF_INT) {
                    mOverlaySizeBar.setProgress(mSharedPreferences.getInt(Constants.OVERLAY_SIZE_SETTING, Constants.BAD_SHARED_PREF_INT));
                } else {
                    mOverlaySizeBar.setProgress(PORTION_OF_IMAGE_DIVISOR - 1);
                }
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mCaptureSession.setRepeatingRequest(
                            mPreviewRequestBuilder.build(),
                            mPreCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void createCameraPreviewSessionLocked() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface,
                    mJpegImageReader.get().getSurface(),
                    mRawImageReader.get().getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            synchronized (mCameraStateLock) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                try {
                                    setup3AControlsLocked(mPreviewRequestBuilder);
                                    // Finally, we start displaying the camera preview.
                                    cameraCaptureSession.setRepeatingRequest(
                                            mPreviewRequestBuilder.build(),
                                            mPreCaptureCallback, mBackgroundHandler);
                                    mState = STATE_PREVIEW;
                                } catch (CameraAccessException | IllegalStateException e) {
                                    e.printStackTrace();
                                    return;
                                }
                                // When the session is ready, we start displaying the preview.
                                mCaptureSession = cameraCaptureSession;
                                assignSavedOrDefaultSettings();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
//                            showToast("Failed to configure camera.");
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configure the given {@link CaptureRequest.Builder} to use auto-focus, auto-exposure, and
     * auto-white-balance controls if available.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param builder the builder to configure.
     */
    private void setup3AControlsLocked(CaptureRequest.Builder builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO);

        Float minFocusDist =
                mCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = (minFocusDist == null || minFocusDist == 0);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
//            builder.set(CaptureRequest.CONTROL_AE_MODE,
//                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        } else {
//            builder.set(CaptureRequest.CONTROL_AE_MODE,
//                    CaptureRequest.CONTROL_AE_MODE_ON);
        }

        // If there is an auto-magical white balance control mode available, use it.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_HEADSETHOOK)) {
            captureStillPictureLocked();//takePicture();
        }
        return true;
    }

    /**
     * Configure the necessary {@link Matrix} transformation to `mTextureView`,
     * and start/restart the preview capture session if necessary.
     * <p>
     * This method should be called after the camera state has been initialized in
     * setUpCameraOutputs.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        mViewWidth = viewWidth;
        ((ImageView) findViewById(R.id.imageView)).getLayoutParams().width = viewWidth / PORTION_OF_IMAGE_DIVISOR;
        ((ImageView) findViewById(R.id.imageView)).getLayoutParams().height = viewWidth / PORTION_OF_IMAGE_DIVISOR;
        ((ImageView) findViewById(R.id.imageView)).setVisibility(View.VISIBLE);
        Activity activity = RawRgbCapture.this;
        synchronized (mCameraStateLock) {
            if (null == mTextureView || null == activity || null == mCharacteristics) {
                return;
            }

            StreamConfigurationMap map = mCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // For still image captures, we always use the largest available size.
            Size largestJpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            // Find the rotation of the device relative to the native device orientation.
            int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

            // Find the rotation of the device relative to the camera sensor's orientation.
            int totalRotation = sensorToDeviceRotation(mCharacteristics, deviceRotation);

            // Swap the view dimensions for calculation as needed if they are rotated relative to
            // the sensor.
            boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;
            int rotatedViewWidth = viewWidth;
            int rotatedViewHeight = viewHeight;
            if (swappedDimensions) {
                rotatedViewWidth = viewHeight;
                rotatedViewHeight = viewWidth;
            }

            // Find the best preview size for these view dimensions and configured JPEG size.
            Size previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedViewWidth, rotatedViewHeight, largestJpeg);

            if (swappedDimensions) {
                mTextureView.setAspectRatio(
                        previewSize.getHeight(), previewSize.getWidth());
            } else {
                mTextureView.setAspectRatio(
                        previewSize.getWidth(), previewSize.getHeight());
            }

            // Find rotation of device in degrees (reverse device orientation for front-facing
            // cameras).
            int rotation = (mCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) ?
                    (360 + ORIENTATIONS.get(deviceRotation)) % 360 :
                    (360 - ORIENTATIONS.get(deviceRotation)) % 360;

            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();

            // Initially, output stream images from the Camera2 API will be rotated to the native
            // device orientation from the sensor's orientation, and the TextureView will default to
            // scaling these buffers to fill it's view bounds.  If the aspect ratios and relative
            // orientations are correct, this is fine.
            //
            // However, if the device orientation has been rotated relative to its native
            // orientation so that the TextureView's dimensions are swapped relative to the
            // native device orientation, we must do the following to ensure the output stream
            // images are not incorrectly scaled by the TextureView:
            //   - Undo the scale-to-fill from the output buffer's dimensions (i.e. its dimensions
            //     in the native device orientation) to the TextureView's dimension.
            //   - Apply a scale-to-fill from the output buffer's rotated dimensions
            //     (i.e. its dimensions in the current device orientation) to the TextureView's
            //     dimensions.
            //   - Apply the rotation from the native device orientation to the current device
            //     rotation.
            if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / previewSize.getHeight(),
                        (float) viewWidth / previewSize.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);

            }
            matrix.postRotate(rotation, centerX, centerY);

            mTextureView.setTransform(matrix);

            // Start or restart the active capture session if the preview was initialized or
            // if its aspect ratio changed significantly.
            if (mPreviewSize == null || !checkAspectsEqual(previewSize, mPreviewSize)) {
                mPreviewSize = previewSize;
                if (mState != STATE_CLOSED) {
                    createCameraPreviewSessionLocked();
                }
            }
        }
    }

    /**
     * Initiate a still image capture.
     * <p>
     * This function sends a capture request that initiates a pre-capture sequence in our state
     * machine that waits for auto-focus to finish, ending in a "locked" state where the lens is no
     * longer moving, waits for auto-exposure to choose a good exposure value, and waits for
     * auto-white-balance to converge.
     */
    private void takePicture() {
//        if(isPhotoInProgress) {
//            return;
//        }
//        isPhotoInProgress = true;
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
        synchronized (mCameraStateLock) {
            mPendingUserCaptures++;

            // If we already triggered a pre-capture sequence, or are in a state where we cannot
            // do this, return immediately.
            if (mState != STATE_PREVIEW) {
                return;
            }

            try {
                // Trigger an auto-focus run if camera is capable. If the camera is already focused,
                // this should do nothing.
                if (!mNoAFRun) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CameraMetadata.CONTROL_AF_TRIGGER_START);
                }

                // If this is not a legacy device, we can also trigger an auto-exposure metering
                // run.
                if (!isLegacyLocked()) {
                    // Tell the camera to lock focus.
//                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
//                            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                }

                // Update state machine to wait for auto-focus, auto-exposure, and
                // auto-white-balance (aka. "3A") to converge.
                mState = STATE_WAITING_FOR_3A_CONVERGENCE;

                // Start a timer for the pre-capture sequence.
                startTimerLocked();

                // Replace the existing repeating request with one with updated 3A triggers.
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send a capture request to the camera device that initiates a capture targeting the JPEG and
     * RAW outputs.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void captureStillPictureLocked() {
        try {
//            if(isPhotoInProgress) {
//                return;
//            }
//            isPhotoInProgress = true;
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);

            final Activity activity = RawRgbCapture.this;
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            if (mPreviewRequestBuilder == null) {
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }

            mPreviewRequestBuilder.addTarget(mJpegImageReader.get().getSurface());
            mPreviewRequestBuilder.addTarget(mRawImageReader.get().getSurface());

            // Use the same AE and AF modes as the preview.
            setup3AControlsLocked(mPreviewRequestBuilder);

            // Set orientation.
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    sensorToDeviceRotation(mCharacteristics, rotation));

            // Set request tag to easily track results in callbacks.
            mPreviewRequestBuilder.setTag(mRequestCounter.getAndIncrement());

            CaptureRequest request = mPreviewRequestBuilder.build();

            // Create an ImageSaverBuilder in which to collect results, and add it to the queue
            // of active requests.
            ImageSaver.ImageSaverBuilder jpegBuilder = new ImageSaver.ImageSaverBuilder(activity)
                    .setCharacteristics(mCharacteristics);
            ImageSaver.ImageSaverBuilder rawBuilder = new ImageSaver.ImageSaverBuilder(activity)
                    .setCharacteristics(mCharacteristics);

            mJpegResultQueue.put((int) request.getTag(), jpegBuilder);
            mRawResultQueue.put((int) request.getTag(), rawBuilder);

            mCaptureSession.capture(request, mCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called after a RAW/JPEG capture has completed; resets the AF trigger state for the
     * pre-capture sequence.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void finishedCaptureLocked() {
//        try {
//            // Reset the auto-focus trigger in case AF didn't run quickly enough.
//            if (!mNoAFRun) {
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//
//                mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
//                        mBackgroundHandler);
//
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Retrieve the next {@link Image} from a reference counted {@link ImageReader}, retaining
     * that {@link ImageReader} until that {@link Image} is no longer in use, and set this
     * {@link Image} as the result for the next request in the queue of pending requests.  If
     * all necessary information is available, begin saving the image to a file in a background
     * thread.
     *
     * @param pendingQueue the currently active requests.
     * @param reader       a reference counted wrapper containing an {@link ImageReader} from which to
     *                     acquire an image.
     */
    private void dequeueAndSaveImage(TreeMap<Integer, ImageSaver.ImageSaverBuilder> pendingQueue,
                                     RefCountedAutoCloseable<ImageReader> reader) {
        synchronized (mCameraStateLock) {
            Map.Entry<Integer, ImageSaver.ImageSaverBuilder> entry =
                    pendingQueue.firstEntry();

            ImageSaver.ImageSaverBuilder builder = entry.getValue();

            // Increment reference count to prevent ImageReader from being closed while we
            // are saving its Images in a background thread (otherwise their resources may
            // be freed while we are writing to a file).
            if (reader == null || reader.getAndRetain() == null) {
                Log.e(TAG, "Paused the activity before we could save the image," +
                        " ImageReader already closed.");
                pendingQueue.remove(entry.getKey());
                return;
            }

            Image image;
            try {
                image = reader.get().acquireNextImage();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Too many images queued for saving, dropping image for request: " +
                        entry.getKey());
                pendingQueue.remove(entry.getKey());
                return;
            }

            builder.setRefCountedReader(reader).setImage(image);

            handleCompletionLocked(entry.getKey(), builder, pendingQueue);
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("RawRgbCapture Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    /**
     * Runnable that saves an {@link Image} into the specified {@link File}, and updates
     * {@link MediaStore} to include the resulting file.
     * <p>
     * This can be constructed through an {@link ImageSaverBuilder} as the necessary image and
     * result information becomes available.
     */
    public static class ImageSaver implements Runnable {

        private final Image mImage;
        private final String mTimestamp;
        private final File mFile;
        private final CaptureResult mCaptureResult;
        private final CameraCharacteristics mCharacteristics;
        private final Context mContext;
        private final RefCountedAutoCloseable<ImageReader> mReader;

        private ImageSaver(Image image, File file, String timestamp, CaptureResult result,
                           CameraCharacteristics characteristics, Context context,
                           RefCountedAutoCloseable<ImageReader> reader) {
            mImage = image;
            mFile = file;
            mTimestamp = timestamp;
            mCaptureResult = result;
            mCharacteristics = characteristics;
            mContext = context;
            mReader = reader;
        }

        private double[] reformatForRggb(int formatF, double[] rggbPixelF) {
            double[] correctedPixel = new double[3];

            switch (formatF) {
                case 0: //RGGB
                    correctedPixel[0] = rggbPixelF[0];
                    correctedPixel[1] = (rggbPixelF[1] + rggbPixelF[2]) / 2;
                    correctedPixel[2] = rggbPixelF[3];
                    break;
                case 1: //GRBG
                    correctedPixel[0] = rggbPixelF[1];
                    correctedPixel[1] = (rggbPixelF[0] + rggbPixelF[3]) / 2;
                    correctedPixel[2] = rggbPixelF[2];
                    break;
                case 2: //GBRG
                    correctedPixel[0] = rggbPixelF[2];
                    correctedPixel[1] = (rggbPixelF[3] + rggbPixelF[0]) / 2;
                    correctedPixel[2] = rggbPixelF[1];
                    break;
                case 3: //BGGR
                    correctedPixel[0] = rggbPixelF[3];
                    correctedPixel[1] = (rggbPixelF[1] + rggbPixelF[2]) / 2;
                    correctedPixel[2] = rggbPixelF[0];
                    break;
                case 4: //RGB Sensor is not Bayer; output has 3 16-bit values for each pixel, instead of just 1 16-bit value per pixel.
                    break;
            }

            return correctedPixel;
        }

        private double[] getRawPixelBayerFormat(int rowF, int colF) {
            double[] bayerPixel = new double[4];
            ByteBuffer rawBuffer = mImage.getPlanes()[0].getBuffer();
            rawBuffer.order(ByteOrder.nativeOrder());

            if (colF % 2 == 0) //Even column
            {
                if (rowF % 2 == 0) //Even row or Red Channel
                {
                    rawBuffer.position((colF * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[0] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position(((colF + 1) * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[1] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((colF * 2) + ((rowF + 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[2] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position(((colF + 1) * 2) + ((rowF + 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[3] = rawBuffer.getShort() & 0x7FFF;
                    if (mIsCheckingPosition) {
                        rawBuffer.putShort((short) 1000);
                    }
                } else { //Odd row or Green 2 Channel
                    rawBuffer.position((colF * 2) + ((rowF - 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[0] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((((colF + 1) * 2) + ((rowF - 1) * mImage.getPlanes()[0].getRowStride())));
                    bayerPixel[1] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((colF * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[2] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position(((colF + 1) * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[3] = rawBuffer.getShort() & 0x7FFF;
                    if (mIsCheckingPosition) {
                        rawBuffer.putShort((short) 1000);
                    }
                }
            } else //Odd column
            {
                if (rowF % 2 == 0) //Even row
                {
                    rawBuffer.position(((colF - 1) * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[0] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((colF * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[1] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position(((colF - 1) * 2) + ((rowF + 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[2] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((colF * 2) + ((rowF + 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[3] = rawBuffer.getShort() & 0x7FFF;
                    if (mIsCheckingPosition) {
                        rawBuffer.putShort((short) 1000);
                    }
                } else { //Odd row
                    rawBuffer.position(((colF - 1) * 2) + ((rowF - 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[0] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((colF * 2) + ((rowF - 1) * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[1] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position(((colF - 1) * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[2] = rawBuffer.getShort() & 0x7FFF;
                    rawBuffer.position((colF * 2) + (rowF * mImage.getPlanes()[0].getRowStride()));
                    bayerPixel[3] = rawBuffer.getShort() & 0x7FFF;
                    if (mIsCheckingPosition) {
                        rawBuffer.putShort((short) 1000);
                    }
                }
            }
            return bayerPixel;
        }

        private double[] getRawPixel(int rowF, int colF) {
            Integer filterArrangement = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
            double[] correctedPixel = reformatForRggb(filterArrangement, getRawPixelBayerFormat(rowF, colF));
            return correctedPixel;
        }

        private void recordRgbData() throws IOException {
            float regionOfInterestLength = mImage.getHeight() / (PORTION_OF_IMAGE_DIVISOR) * 0.95f;
            int startRow = (int) ((mImage.getHeight() / 2.0f) - (regionOfInterestLength / 2.0f));
            int startCol = (int) ((mImage.getWidth() / 2.0f) - (regionOfInterestLength / 2.0f));
            int endRow = (int) ((mImage.getHeight() / 2.0f) + (regionOfInterestLength / 2.0f));
            int endCol = (int) ((mImage.getWidth() / 2.0f) + (regionOfInterestLength / 2.0f));
            Point centerPoint = new Point((int)((float)mImage.getWidth() / 2.0f), (int)((float)mImage.getHeight() / 2.0f));
            int radius = (int) (regionOfInterestLength / 2.0f * 0.95f);

            PrintWriter writer = null;
            BufferedWriter writerCombined = null;
            File rgbRawFile = null, combinedRgbRawFile = null;
            File customDirectory = null;
            File sessionDirectory = null;
            OutputStream outputStream = null;
            WritableByteChannel fileChannel;
            ByteBuffer buffer;
            int averageR = 0, averageG = 0, averageB = 0;
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
                combinedRgbRawFile = new File(sessionDirectory, "RAW_combinedRGBs.txt");

                writer = new PrintWriter(rgbRawFile, "UTF-8");
                writerCombined = new BufferedWriter(new FileWriter(combinedRgbRawFile, true));
                double rawPixel[];
                int numberOfPixels = 0;
                for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
                    for (int colIndex = startCol; colIndex < endCol; colIndex++) {
                        if(insideCircle(colIndex, rowIndex, centerPoint, radius)) {
                            rawPixel = getRawPixel(rowIndex, colIndex);
                            averageR += rawPixel[0];
                            averageG += rawPixel[1];
                            averageB += rawPixel[2];
                            writer.println("" + rawPixel[0] + ", " + rawPixel[1] + ", " + rawPixel[2]);
                            numberOfPixels++;
                        }
                    }
                }

                writerCombined.append("" + (averageR / numberOfPixels) + ", " +
                        (averageG / numberOfPixels) + ", " +
                        (averageB / numberOfPixels) + "\n");

                final int avgR = averageR / numberOfPixels, avgG = averageG / numberOfPixels, avgB = averageB / numberOfPixels;
                mActivityContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRgbTextView.setText("R: " + avgR + "\n" +
                                "G: " + avgG + "\n" +
                                "B: " + avgB);
                    }
                });
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
                MediaScannerConnection.scanFile(mContext, new String[]{rgbRawFile.getAbsolutePath()}, null, new MyOnScanCompletedListener());
            }
            if (combinedRgbRawFile != null) {
                MediaScannerConnection.scanFile(mContext, new String[]{combinedRgbRawFile.getAbsolutePath()}, null, new MyOnScanCompletedListener());
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

        /**
         * Shows a {@link Toast} on the UI thread.
         *
         * @param text The message to show.
         */
        private void showToast(String text) {
//            isPhotoInProgress = false;
            Snackbar.make(mCoordinatorLayout, text, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            //Set maximum volume for device so that we can here the shutter sound.
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
        }


        @Override
        public void run() {
            boolean success = false;
            int format = mImage.getFormat();
            switch (format) {
                case ImageFormat.JPEG: {
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(mFile);
                        output.write(bytes);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        closeOutput(output);
                    }
                    break;
                }
                case ImageFormat.RAW_SENSOR: {
                    DngCreator dngCreator = new DngCreator(mCharacteristics, mCaptureResult);
                    FileOutputStream output = null;
                    try {
                        recordRgbData();
                        output = new FileOutputStream(mFile);
                        dngCreator.writeImage(output, mImage);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        closeOutput(output);
                        showToast("Raw picture info saved out.");
                    }
                    break;
                }
                default: {
                    Log.e(TAG, "Cannot save image, unexpected image format:" + format);
                    break;
                }
            }

            // Decrement reference count to allow ImageReader to be closed to free up resources.
            mReader.close();

            // If saving the file succeeded, update MediaStore.
            if (success) {
                MediaScannerConnection.scanFile(mContext, new String[]{mFile.getPath()},
                /*mimeTypes*/null, new MyOnScanCompletedListener());
            }


        }

        /**
         * Builder class for constructing {@link ImageSaver}s.
         * <p>
         * This class is thread safe.
         */
        public static class ImageSaverBuilder {
            private Image mImage;
            private File mFile;
            private String mTimestamp;
            private CaptureResult mCaptureResult;
            private CameraCharacteristics mCharacteristics;
            private Context mContext;
            private RefCountedAutoCloseable<ImageReader> mReader;

            /**
             * Construct a new ImageSaverBuilder using the given {@link Context}.
             *
             * @param context a {@link Context} to for accessing the
             *                {@link MediaStore}.
             */
            public ImageSaverBuilder(final Context context) {
                mContext = context;
            }

            public synchronized ImageSaverBuilder setRefCountedReader(
                    RefCountedAutoCloseable<ImageReader> reader) {
                if (reader == null) throw new NullPointerException();

                mReader = reader;
                return this;
            }

            public synchronized ImageSaverBuilder setImage(final Image image) {
                if (image == null) throw new NullPointerException();
                mImage = image;
                return this;
            }

            public synchronized ImageSaverBuilder setFile(final File file, final String timestamp) {
                if (file == null) throw new NullPointerException();
                mFile = file;
                mTimestamp = timestamp;
                return this;
            }

            public synchronized ImageSaverBuilder setResult(final CaptureResult result) {
                if (result == null) throw new NullPointerException();
                mCaptureResult = result;
                return this;
            }

            public synchronized ImageSaverBuilder setCharacteristics(
                    final CameraCharacteristics characteristics) {
                if (characteristics == null) throw new NullPointerException();
                mCharacteristics = characteristics;
                return this;
            }

            public synchronized ImageSaver buildIfComplete() {
                if (!isComplete()) {
                    return null;
                }
                return new ImageSaver(mImage, mFile, mTimestamp, mCaptureResult, mCharacteristics, mContext,
                        mReader);
            }

            public synchronized String getSaveLocation() {
                return (mFile == null) ? "Unknown" : mFile.toString();
            }

            private boolean isComplete() {
                return mImage != null && mFile != null && mCaptureResult != null
                        && mCharacteristics != null;
            }
        }
    }

    // Utility classes and methods:
    // *********************************************************************************************

    /**
     * Comparator based on area of the given {@link Size} objects.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * A dialog fragment for displaying non-recoverable errors; this {@ling Activity} will be
     * finished once the dialog has been acknowledged by the user.
     */
    public static class ErrorDialog extends DialogFragment {

        private String mErrorMessage;

        public ErrorDialog() {
            mErrorMessage = "Unknown error occurred!";
        }

        // Build a dialog with a custom message (Fragments require default constructor).
        public static ErrorDialog buildErrorDialog(String errorMessage) {
            ErrorDialog dialog = new ErrorDialog();
            dialog.mErrorMessage = errorMessage;
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(mErrorMessage)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    /**
     * A wrapper for an {@link AutoCloseable} object that implements reference counting to allow
     * for resource management.
     */
    public static class RefCountedAutoCloseable<T extends AutoCloseable> implements AutoCloseable {
        private T mObject;
        private long mRefCount = 0;

        /**
         * Wrap the given object.
         *
         * @param object an object to wrap.
         */
        public RefCountedAutoCloseable(T object) {
            if (object == null) throw new NullPointerException();
            mObject = object;
        }

        /**
         * Increment the reference count and return the wrapped object.
         *
         * @return the wrapped object, or null if the object has been released.
         */
        public synchronized T getAndRetain() {
            if (mRefCount < 0) {
                return null;
            }
            mRefCount++;
            return mObject;
        }

        /**
         * Return the wrapped object.
         *
         * @return the wrapped object, or null if the object has been released.
         */
        public synchronized T get() {
            return mObject;
        }

        /**
         * Decrement the reference count and release the wrapped object if there are no other
         * users retaining this object.
         */
        @Override
        public synchronized void close() {
            if (mRefCount >= 0) {
                mRefCount--;
                if (mRefCount < 0) {
                    try {
                        mObject.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        mObject = null;
                    }
                }
            }
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Generate a string containing a formatted timestamp with the current date and time.
     *
     * @return a {@link String} representing a time.
     */
    private static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Cleanup the given {@link OutputStream}.
     *
     * @param outputStream the stream to close.
     */
    private static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Return true if the given array contains the given integer.
     *
     * @param modes array to check.
     * @param mode  integer to get for.
     * @return true if the array contains the given integer, otherwise false.
     */
    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if the two given {@link Size}s have the same aspect ratio.
     *
     * @param a first {@link Size} to compare.
     * @param b second {@link Size} to compare.
     * @return true if the sizes have the same aspect ratio, otherwise false.
     */
    private static boolean checkAspectsEqual(Size a, Size b) {
        double aAspect = a.getWidth() / (double) a.getHeight();
        double bAspect = b.getWidth() / (double) b.getHeight();
        return Math.abs(aAspect - bAspect) <= ASPECT_RATIO_TOLERANCE;
    }

    /**
     * Rotation need to transform from the camera sensor orientation to the device's current
     * orientation.
     *
     * @param c                 the {@link CameraCharacteristics} to query for the camera sensor orientation.
     * @param deviceOrientation the current device orientation relative to the native device
     *                          orientation.
     * @return the total rotation from the sensor orientation to the current device orientation.
     */
    private static int sensorToDeviceRotation(CameraCharacteristics c, int deviceOrientation) {
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Get device orientation in degrees
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        // Reverse device orientation for front-facing cameras
        if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    /**
     * If the given request has been completed, remove it from the queue of active requests and
     * send an {@link ImageSaver} with the results from this request to a background thread to
     * save a file.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param requestId the ID of the {@link CaptureRequest} to handle.
     * @param builder   the {@link ImageSaver.ImageSaverBuilder} for this request.
     * @param queue     the queue to remove this request from, if completed.
     */
    private void handleCompletionLocked(int requestId, ImageSaver.ImageSaverBuilder builder,
                                        TreeMap<Integer, ImageSaver.ImageSaverBuilder> queue) {
        if (builder == null) return;
        ImageSaver saver = builder.buildIfComplete();
        if (saver != null) {
            queue.remove(requestId);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(saver);
        }
    }

    /**
     * Check if we are using a device that only supports the LEGACY hardware level.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @return true if this is a legacy device.
     */
    private boolean isLegacyLocked() {
        return mCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    /**
     * Start the timer for the pre-capture sequence.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void startTimerLocked() {
        mCaptureTimer = SystemClock.elapsedRealtime();
    }

    /**
     * Check if the timer for the pre-capture sequence has been hit.
     * <p>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @return true if the timeout occurred.
     */
    private boolean hitTimeoutLocked() {
        return (SystemClock.elapsedRealtime() - mCaptureTimer) > PRECAPTURE_TIMEOUT_MS;
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mCoordinatorLayout.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        mCoordinatorLayout.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // *********************************************************************************************

    public static class MyOnScanCompletedListener implements MediaScannerConnection.OnScanCompletedListener {
        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (uri != null && path != null) {
                Log.d(Constants.LOG_TAG, String.format("Scanned path %s -> URI = %s", path, uri.toString()));
            }
        }
    }
}
