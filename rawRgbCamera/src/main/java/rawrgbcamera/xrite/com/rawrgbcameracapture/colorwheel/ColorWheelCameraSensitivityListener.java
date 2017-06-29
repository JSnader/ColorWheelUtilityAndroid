/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
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
	public void exposureUpdate(ExposureCommand pExposure)
	{
//		if(mCamera != null)
//		{
//			mCamera.setExposure(pExposure.mCurrentExposure);
//			mCamera.setExposureLock(pExposure.mIsExposureLocked);
//		}
	}

	@Override
	public void whitePointUpdate(WhitePointCommand pWhitePoint)
	{
//		if(mCamera != null)
//		{
//			//mCamera.setWhiteBalance(pWhitePoint.whitePoint.description);
//			mCamera.setWhiteBalanceLock(pWhitePoint.isWhitePointLocked);
//		}
	}

	@Override
	public void focusUpdate(FocusCommand pFocus)
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
