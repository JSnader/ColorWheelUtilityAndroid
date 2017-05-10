package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import android.content.Context;
import android.os.Vibrator;

import com.xrite.xritecamera.XriteUcpCamera;

import rawrgbcamera.xrite.com.rawrgbcameracapture.RawRgbCapture;

public class ColorWheelCameraSensitivityListener implements CameraSensitivityListener
{
   	XriteUcpCamera 	mCamera;
	Context 		mContext;

	public ColorWheelCameraSensitivityListener(Context pContext, XriteUcpCamera pCamera)
	{
		mContext = pContext;
		mCamera = pCamera;
	}

	@Override
	public XriteUcpCamera getCamera()
	{
		return null;
	}
	public void exposureUpdate(ExposureCommand exposureF)
	{
//		if(mCamera != null)
//		{
//			mCamera.setExposure(exposureF.currentExposure);
//			mCamera.setExposureLock(exposureF.isExposureLocked);
//		}
	}

	@Override
	public void whitePointUpdate(WhitePointCommand whitePointCommandF)
	{
//		if(mCamera != null)
//		{
//			//mCamera.setWhiteBalance(whitePointCommandF.whitePoint.description);
//			mCamera.setWhiteBalanceLock(whitePointCommandF.isWhitePointLocked);
//		}
	}

	@Override
	public void focusUpdate(FocusCommand focusF)
	{
//		if(mCamera != null)
//		{
//
//		}
	}

	@Override
	public void requestPicture()
	{
		((RawRgbCapture)mContext).mHasRequestedCapture = true;
		Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(10);
	}

	@Override
	public void releaseCamera()
	{
//		mCamera.releaseCamera();
	}
}
