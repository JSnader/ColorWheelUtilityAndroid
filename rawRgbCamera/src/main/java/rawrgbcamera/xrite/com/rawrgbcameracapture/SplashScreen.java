package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

public class SplashScreen extends Activity {
    int PERMISSION_CODE = 101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityCompat.requestPermissions(SplashScreen.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCodeF, String[] permissionsF, int[] grantResultsF) {
        if (grantResultsF.length > 0 && grantResultsF[0] == PackageManager.PERMISSION_DENIED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Camera Access Permission Needed", Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(), "Enable Camera from App Settings", Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(), "Logging features to share may be disabled until permission granted.", Toast.LENGTH_LONG).show();
                }
            });
        }else{
            Intent rawCapture = new Intent(SplashScreen.this, RawRgbCapture.class);
            startActivity(rawCapture);
            finish();
        }

        return;
    }
}
