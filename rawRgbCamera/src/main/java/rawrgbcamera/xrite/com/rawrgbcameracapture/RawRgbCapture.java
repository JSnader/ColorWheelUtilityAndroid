package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Range;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.xrite.imageclasses.UcpImage;
import com.xrite.xritecamera.SnapshotSettings;
import com.xrite.xritecamera.UcpImageCallback;
import com.xrite.xritecamera.XriteCameraCallback;
import com.xrite.xritecamera.XriteCameraException;
import com.xrite.xritecamera.XriteCameraExposureMode;
import com.xrite.xritecamera.XriteCameraFactory;
import com.xrite.xritecamera.XriteCameraFocusMode;
import com.xrite.xritecamera.XriteCameraSettings;
import com.xrite.xritecamera.XriteSize;
import com.xrite.xritecamera.XriteTextureView;
import com.xrite.xritecamera.XriteUcpCamera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static rawrgbcamera.xrite.com.rawrgbcameracapture.Constants.MAXIMUM_SEEK_BAR_SETTING;
import static rawrgbcamera.xrite.com.rawrgbcameracapture.R.id.imageView;

/**
 * A simple activity that is responsible for saving out rgb data in a dark environment with
 * controlled exposure.  The information saved off is what fits within the overlay on the
 * screen.  The rgbs from a raw capture device will be saved off.
 */
public class RawRgbCapture extends AppCompatActivity implements UcpImageCallback, ListenerDataCompletion {
    private static AudioManager mAudioManager;
    private static CoordinatorLayout mCoordinatorLayout;
    private ImageView mOverlay;
    private SeekBar mExposureBar, mExposureTimeBar, mIsoBar, mOverlaySizeBar;
    private TextView mExposureTextView, mExposureTimeTextView, mIsoTextView;
    private EditText mExposureEditTextView;
    private static TextView mRgbTextView;
    private int mViewWidth = 0;
    private static Activity mActivityContext;

    private SharedPreferences mSharedPreferences;

    private XriteUcpCamera mXriteCamera;
    private XriteCameraFactory mXriteFactory;
    private CameraCharacteristics mCharacteristics;
    private int mDisplayWidth, mDisplayHeight;
    private TextureView.SurfaceTextureListener mTextureListener;
    private XriteSize mPreviewWindowSize;
    private XriteTextureView mTextureView;

    private static int mSharedPrefSession;
    private GestureDetector mGestureDetector;

    int mFilterArrangement = 0;//RGGB
    private boolean mHasRequestedCapture = false;
    private boolean mAnalyzingFrame = false;

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

    public static int PORTION_OF_IMAGE_DIVISOR = 1;
    public static final double ASPECT_RATIO_TOLERANCE = 0.005;
    public static final float OVERLAY_PADDING = 0.80f;
    private static final String TAG = "Camera2RawFragment";

    /**mSharedPrefSession
     * An {@link OrientationEventListener} used to determine when device rotation has occurred.
     * This is mainly necessary for when the device is rotated by 180 degrees, in which case
     * onCreate or onConfigurationChanged is not called as the view dimensions remain the same,
     * but the orientation of the has changed, and thus the preview rotation must be updated.
     */
    private OrientationEventListener mOrientationListener;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mActivityContext = RawRgbCapture.this;
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mOverlay = (ImageView)findViewById(R.id.imageView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mDisplayWidth = size.x; //reversed here since we are always dealing with rotated display.
        mDisplayHeight = size.y;

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
//        mExposureTextView = (TextView) findViewById(R.id.textViewExposure);
        mExposureEditTextView = (EditText) findViewById(R.id.editTextExposureTime);
        mExposureEditTextView.setFocusableInTouchMode(true);
        mExposureEditTextView.requestFocus();
        mExposureTimeTextView = (TextView) findViewById(R.id.textViewExposureTime);
        mIsoTextView = (TextView) findViewById(R.id.textViewISO);
        mRgbTextView = (TextView) findViewById(R.id.textViewRgbs);
        mRgbTextView.bringToFront();
//        mFocusLockSwitch = (Switch) findViewById(R.id.switchFocusLock);
//        mExposureLockSwitch = (Switch) findViewById(R.id.switchExposureLock);
//        mExposureLockSwitch.setChecked(true);
//        mExposureLockSwitch.setEnabled(false);
        mExposureEditTextView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Range<Long> range2 = mXriteCamera.getExposureTimeRange();
                    long max = range2.getUpper();
                    long min = range2.getLower();
                    long upperLimit = max - min;

//                    int exposureTime = (int)(progress * (upperLimit / MAXIMUM_SEEK_BAR_SETTING) + min);
//                    mExposureTimeTextView.setText("Exposure Time (ms): " + (exposureTime / 1000000));
                    int exposureValue = Integer.parseInt(mExposureEditTextView.getText().toString())  * 1000000;
                    if(exposureValue < min || exposureValue > max){
                        return true;
                    }
                    int progressValue = (int)Math.floor(((double)exposureValue - min) / ((double)upperLimit / (double)MAXIMUM_SEEK_BAR_SETTING)) + 1;
                    mExposureTimeBar.setProgress(progressValue);
                    return true;
                }
                return false;
            }
        });

        mExposureTimeBar = (SeekBar)findViewById(R.id.seekBarExposureTime);
        mExposureTimeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Range<Long> range2 = mXriteCamera.getExposureTimeRange();
                long max = range2.getUpper();
                long min = range2.getLower();
                long upperLimit = max - min;
                int exposureTime = (int) Math.floor((progress * ((double)upperLimit / (double)MAXIMUM_SEEK_BAR_SETTING) + min));
                mExposureEditTextView.setText("" + exposureTime / 1000000);
                if(fromUser) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(Constants.EXPOSURE_TIME_SETTING, progress);
                    editor.apply();
                    editor.commit();
                }
                mXriteCamera.setExposureTime(exposureTime);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
//        mExposureBar = (SeekBar) findViewById(R.id.seekBarExposure);
//        mExposureBar.setEnabled(false);
//        mExposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                int exposureLevel = progress - (int) Math.ceil((double) mExposureBar.getMax() / (double) 2.0f);
//                mExposureTextView.setText("Exposure: " + exposureLevel);
//                if(fromUser) {
//                    SharedPreferences.Editor editor = mSharedPreferences.edit();
//                    editor.putInt(Constants.EXPOSURE_SETTING, progress);
//                    editor.apply();
//                    editor.commit();
//                }
//                mXriteCamera.setExposure(exposureLevel);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
        mOverlaySizeBar = (SeekBar) findViewById(R.id.seekBarSize);
        mOverlaySizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PORTION_OF_IMAGE_DIVISOR = progress + 1;
                ViewGroup.LayoutParams params = ((ImageView) findViewById(imageView)).getLayoutParams();//.width
                params.width = mViewWidth / PORTION_OF_IMAGE_DIVISOR;
                params.height = mViewWidth / PORTION_OF_IMAGE_DIVISOR;
                mOverlay.setLayoutParams(params);
                mOverlay.bringToFront();
                mOverlay.requestLayout();
                if(fromUser) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(Constants.OVERLAY_SIZE_SETTING, progress);
                    editor.apply();
                    editor.commit();
                }
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
                Range<Integer> range2 = mXriteCamera.getIsoRange();
                int max = range2.getUpper();
                int min = range2.getLower();
                int upperLimit = max - min;
                int iso = (int) Math.floor((progress * ((float)upperLimit / (float)MAXIMUM_SEEK_BAR_SETTING) + min));
                mIsoTextView.setText("ISO: " + iso);
                if(fromUser) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(Constants.ISO_SETTING, progress);
                    editor.apply();
                    editor.commit();
                }
                mXriteCamera.setIso(iso);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
//        mFocusLockSwitch.setChecked(false);
//        mFocusLockSwitch.setEnabled(false);
//        mFocusLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton pButtonView, boolean pIsChecked) {
//                if(pIsChecked) {
//                    mXriteCamera.setFocusMode(XriteCameraFocusMode.FIXED);
//                }else{
//                    mXriteCamera.setFocusMode(XriteCameraFocusMode.CONTINUOUS_PICTURE);
//                }
//            }
//        });
        mGestureDetector = new GestureDetector(RawRgbCapture.this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent pEvent) {
                boolean visible = (mCoordinatorLayout.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                if (visible) {
                    //hideSystemUI();
                } else {
                    //showSystemUI();
                }
                return true;
            }
        });
//        mExposureLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                mXriteCamera.setExposureLock(isChecked);
//            }
//        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHasRequestedCapture = true;
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(10);
            }
        });
        mTextureView = (XriteTextureView) findViewById(R.id.texture);
        mTextureView.setOpaque(false);

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

        mTextureListener = new TextureView.SurfaceTextureListener() {
            boolean firstPass = true;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTextureF, int widthF, int heightF)
            {
                mViewWidth = widthF;
                mXriteFactory.openBackFacingUcpCamera(mTextureView, new XriteCameraSettings(XriteCameraSettings.PreviewSizeType.SUGGESTED), new XriteSize(500, 500)/*new XriteSize(widthF, heightF)*/, new UcpCameraListener());
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface)
            {
                if(firstPass) {
                    firstPass = false;
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int widthF, int heightF)
            {
                if(mXriteCamera == null)
                {
                    return;
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
            {
                return true;
            }
        };

        mTextureView.setSurfaceTextureListener(mTextureListener);

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

    private void deleteDirectory(File pDirectory) {
        if (pDirectory.exists()) {
            File[] filesInDirectory = pDirectory.listFiles();
            if (null != filesInDirectory) {
                for (int index = 0; index < filesInDirectory.length; index++) {
                    if (filesInDirectory[index].isDirectory()) {
                        deleteDirectory(filesInDirectory[index]);
                    } else {
                        filesInDirectory[index].delete();
                    }
                }
            }
            pDirectory.delete();
            MediaScannerConnection.scanFile(RawRgbCapture.this, new String[]{pDirectory.getAbsolutePath()}, null, new ListenerFileScannerCompletion());
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

    @Override
    public void onResume() {
        super.onResume();
//        startBackgroundThread();
//        openCamera();
        mXriteFactory = XriteCameraFactory.getInstance(getApplicationContext());

        if(mTextureView.isAvailable())
        {
            mXriteFactory.openBackFacingUcpCamera(mTextureView, new XriteCameraSettings(XriteCameraSettings.PreviewSizeType.SUGGESTED), new XriteSize(mDisplayWidth, mDisplayHeight)/*mPreviewWindowSize*/, new UcpCameraListener());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }

        if(mXriteCamera != null)
        {
            mXriteCamera.releaseCamera();
        }

        finish();
    }



    private void assignSavedOrDefaultSettings() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mXriteCamera.setExposureMode(XriteCameraExposureMode.OFF);
                if(mXriteCamera.getSupportedFocusModes().contains(XriteCameraFocusMode.OFF)) {
                    mXriteCamera.setFocusMode(XriteCameraFocusMode.AUTO);
                }

//                Range<Integer> exposureRange = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
//                mExposureBar.setMax(Math.abs(exposureRange.getLower()) + exposureRange.getUpper());
//                if (mSharedPreferences.getInt(Constants.EXPOSURE_SETTING, Constants.BAD_SHARED_PREF_INT) != Constants.BAD_SHARED_PREF_INT) {
//                    mExposureBar.setProgress(mSharedPreferences.getInt(Constants.EXPOSURE_SETTING, Constants.BAD_SHARED_PREF_INT));
//                } else {
//                    mExposureBar.setProgress(mExposureBar.getMax() / 2);
//                }
                int iso = mSharedPreferences.getInt(Constants.ISO_SETTING, Constants.BAD_SHARED_PREF_INT);
                mIsoBar.setMax(MAXIMUM_SEEK_BAR_SETTING);
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
                mExposureTimeBar.setMax(MAXIMUM_SEEK_BAR_SETTING);
                if (mSharedPreferences.getInt(Constants.EXPOSURE_TIME_SETTING, Constants.BAD_SHARED_PREF_INT) != Constants.BAD_SHARED_PREF_INT) {
                    mExposureTimeBar.setProgress(mSharedPreferences.getInt(Constants.EXPOSURE_TIME_SETTING, Constants.BAD_SHARED_PREF_INT));
                } else {
                    mExposureTimeBar.setProgress(0);//Keep it low as a default.
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_HEADSETHOOK)) {
            mHasRequestedCapture = true;
        }
        return true;
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
         * Shows a {@link Toast} on the UI thread.
         *
         * @param text The message to show.
         */
        private void showToast(String text) {
            Snackbar.make(mCoordinatorLayout, text, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
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

    @Override
    public void onPictureCaptured() {

    }

    @Override
    public void onPictureFrameReady(UcpImage ucpImage) {

        if(mHasRequestedCapture && !mAnalyzingFrame) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                showSpinner();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mHasRequestedCapture = false;
            mAnalyzingFrame = true;

            String timeStamp = generateTimestamp();
            new RecordRgbsAsyncTask(mActivityContext, RawRgbCapture.this, ucpImage, mRgbTextView, mSharedPrefSession, timeStamp).execute();
            new RecordImagesAsyncTask(mActivityContext, ucpImage, mSharedPrefSession, timeStamp).execute();
        }
    }

    /**
     * Generate a string containing a formatted timestamp with the current date and time.
     *
     * @return a {@link String} representing a time.
     */
    private String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }

    private void dismissSpinner(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.viewDarkOverlay).setVisibility(View.INVISIBLE);
                findViewById(R.id.spinner).setVisibility(View.INVISIBLE);
            }
        });
    }

    private void showSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.viewDarkOverlay).setVisibility(View.VISIBLE);
                findViewById(R.id.spinner).setVisibility(View.VISIBLE);
                findViewById(R.id.viewDarkOverlay).bringToFront();
                findViewById(R.id.spinner).bringToFront();
            }
        });
    }

    // *********************************************************************************************

    @Override
    public void onDataCompletion() {
        mAnalyzingFrame = false;
        dismissSpinner();
    }

    private class UcpCameraListener implements XriteCameraCallback
    {
        @Override
        public void onCameraOpened(XriteUcpCamera cameraF)
        {
            mXriteCamera = cameraF;
            mPreviewWindowSize = mXriteCamera.getBestFittingPreviewSize();//.getSuggestedPreviewSize();
            final XriteSize swappedPreviewWindowSize = new XriteSize(mPreviewWindowSize.height, mPreviewWindowSize.width);
            if(mTextureView != null)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(0, 0);
                        float heightRatio, widthRatio;
                        if(swappedPreviewWindowSize.width < mDisplayWidth && swappedPreviewWindowSize.height < mDisplayHeight) {
                            widthRatio = (float)mDisplayWidth / (float)swappedPreviewWindowSize.width;
                            heightRatio = (float)mDisplayHeight / (float)swappedPreviewWindowSize.height;
                            if((float)mDisplayWidth * ((float)swappedPreviewWindowSize.height / (float)swappedPreviewWindowSize.width) < mDisplayHeight){
                                params.width = mDisplayWidth;
                                params.height = (int)((float)mDisplayWidth * ((float)swappedPreviewWindowSize.height / (float)swappedPreviewWindowSize.width));
                            }else {
                                params.width = (int)((float)mDisplayHeight * ((float)swappedPreviewWindowSize.width / (float)swappedPreviewWindowSize.height));
                                params.height = mDisplayHeight;
                            }
                        } else if(swappedPreviewWindowSize.width >= mDisplayWidth && swappedPreviewWindowSize.height >= mDisplayHeight)
                        {
                            if(swappedPreviewWindowSize.width - mDisplayWidth < swappedPreviewWindowSize.height - mDisplayHeight) {
                                params.width = (int)((float) mDisplayHeight * ((float)swappedPreviewWindowSize.width / (float)swappedPreviewWindowSize.height));
                                params.height = mDisplayHeight;
                            }else {
                                params.width = mDisplayWidth;
                                params.height = (int)((float) mDisplayWidth * ((float)swappedPreviewWindowSize.height / (float)swappedPreviewWindowSize.width));
                            }
                        } else if(swappedPreviewWindowSize.width >= mDisplayWidth) { //swappedPreviewWindowSize.height < mDisplayHeight by process of elimination
                            params.width = mDisplayWidth;
                            params.height = (int)((float)mDisplayWidth * ((float)swappedPreviewWindowSize.height / (float)swappedPreviewWindowSize.width));
                        } else {//swappedPreviewWindowSize.width < mDisplayHeight) && swappedPreviewWindowSize.height < mDisplayHeight by process of elimination
                            params.width = (int)((float)mDisplayHeight * ((float)swappedPreviewWindowSize.width / (float)swappedPreviewWindowSize.height));
                            params.height = mDisplayHeight;
                        }

                        params.setMargins(0, 0, 0, 0);
                        params.gravity = Gravity.CENTER;
//                        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                        mTextureView.getSurfaceTexture().setDefaultBufferSize(params.width, params.height);
                        mTextureView.setAspectRatio(params.width, params.height);
                        mTextureView.setLayoutParams(params);
                        mXriteCamera.updateTextureViewDimensions(new XriteSize(params.width, params.height));
                        mTextureView.requestLayout();
                        mOverlay.bringToFront();
                        mOverlay.requestLayout();
                    }
                });
            }

            mCharacteristics = mXriteCamera.getCameraConfiguration().getCharacteristics();
            android.os.Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    assignSavedOrDefaultSettings();
                }
            }, 500);

            mXriteCamera.takeSnapshots(RawRgbCapture.this, new SnapshotSettings(500));
            mTextureView.setOnTouchListener(new ListenerTextureViewOnTouch(mXriteCamera, mTextureView)); //Touch to focus
        }

        @Override
        public void onCameraClosed()
        {
            Log.d(Constants.LOG_TAG, "Camera closed");
        }

        @Override
        public void onExceptionThrown(XriteCameraException exceptionF)
        {
//            reset();
            Toast.makeText(getApplicationContext(), "Camera Error", Toast.LENGTH_LONG).show();
            exceptionF.printStackTrace();
        }
    }
}
