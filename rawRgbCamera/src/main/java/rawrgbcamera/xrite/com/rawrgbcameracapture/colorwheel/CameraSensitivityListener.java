/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import com.xrite.xritecamera.XriteUcpCamera;

public interface CameraSensitivityListener
{
	public XriteUcpCamera getCamera();

	public void exposureUpdate(ExposureCommand pExposure);
	public void whitePointUpdate(WhitePointCommand pWhitePoint);
	public void focusUpdate(FocusCommand pFocus);
	public void requestPicture();
	public void releaseCamera();
}
