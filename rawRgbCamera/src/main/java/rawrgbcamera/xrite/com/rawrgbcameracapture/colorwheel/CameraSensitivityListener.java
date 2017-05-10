package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import com.xrite.xritecamera.XriteUcpCamera;

public interface CameraSensitivityListener
{
	public XriteUcpCamera getCamera();

	public void exposureUpdate(ExposureCommand exposureF);
	public void whitePointUpdate(WhitePointCommand whitePointF);
	public void focusUpdate(FocusCommand focusF);
	public void requestPicture();
	public void releaseCamera();
}
