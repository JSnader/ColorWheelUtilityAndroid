/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

public abstract class Command
{
	public String mCommandType;

	public Command(String pCommandType)
	{
		mCommandType = pCommandType;
	}

	public abstract String createCommand(boolean pShouldProvideCommandCompletion);
}
