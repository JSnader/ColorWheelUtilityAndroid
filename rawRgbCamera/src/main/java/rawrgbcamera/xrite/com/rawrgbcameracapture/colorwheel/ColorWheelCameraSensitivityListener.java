package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import com.xrite.xritecamera.XriteUcpCamera;

public class ColorWheelCameraSensitivityListener implements CameraSensitivityListener
{
//

//	@OverrideXriteUcpCamera camera_;
	//	XritePreviewCallback previewCallback_;
//
//	public ColorWheelCameraSensitivityListener(XriteUcpCamera cameraF, XritePreviewCallback previewCallbackF)
//	{
//		camera_ = cameraF;
//		previewCallback_ = previewCallbackF;
//	}
//
	@Override
	public XriteUcpCamera getCamera()
	{
		return null;
	}
	public void exposureUpdate(ExposureCommand exposureF)
	{
//		if(camera_ != null)
//		{
//			camera_.setExposure(exposureF.currentExposure);
//			camera_.setExposureLock(exposureF.isExposureLocked);
//		}
	}

	@Override
	public void whitePointUpdate(WhitePointCommand whitePointCommandF)
	{
//		if(camera_ != null)
//		{
//			//camera_.setWhiteBalance(whitePointCommandF.whitePoint.description);
//			camera_.setWhiteBalanceLock(whitePointCommandF.isWhitePointLocked);
//		}
	}

	@Override
	public void focusUpdate(FocusCommand focusF)
	{
//		if(camera_ != null)
//		{
//
//		}
	}

	@Override
	public void requestPicture()
	{
//		camera_.takePicture(previewCallback_);
	}

	@Override
	public void releaseCamera()
	{
//		camera_.releaseCamera();
	}
}
