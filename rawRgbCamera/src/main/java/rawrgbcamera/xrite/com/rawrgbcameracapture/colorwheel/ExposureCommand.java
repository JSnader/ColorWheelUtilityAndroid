/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

public class ExposureCommand extends Command
{
	public static final String 	EXPOSURE_TYPE = "ExposureCommand";
	private static final String EXPOSURE_VALUE = "exposureValue";
	private	static final String	EXPOSURE_MIN = "exposureMin";
	private	static final String	EXPOSURE_MAX = "exposureMax";
	private	static final String	EXPOSURE_COMPENSATION_STEP = "exposureCompensationStep";
	private static final String EXPOSURE_LOCKED = "exposureLocked";
	private static final String EXPOSURE_LOCK_AVAILABLE = "exposureLockAvailability";

	private int 				mMinExposure;
	private int 				mMaxExposure;
	private int 				mCurrentExposure;
	private float 				mExposureCompensationStep;
	private boolean 			mIsExposureLocked;
	private boolean 			mIsExposureLockAvailable;

	public ExposureCommand(int pCurrentExposure, int pMinExposure, int pMaxExposure, float pExposureCompensationStep, boolean pIsExposureLockAvailable, boolean pIsExposureLocked)
	{
		super(EXPOSURE_TYPE);
		mCurrentExposure = pCurrentExposure;
		mMinExposure = pMinExposure;
		mMaxExposure = pMaxExposure;
		mExposureCompensationStep = pExposureCompensationStep;
		mIsExposureLocked = pIsExposureLocked;
		mIsExposureLockAvailable = pIsExposureLockAvailable;
	}

	public String createCommand(boolean pShouldProvideCommandCompletion)
	{
		String commandString = "";

		commandString += mCommandType + CommandManager.COMMAND_SEPARATOR;
		commandString += EXPOSURE_VALUE + CommandManager.COMMAND_VARIABLE_SEPARATOR + mCurrentExposure + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_MIN + CommandManager.COMMAND_VARIABLE_SEPARATOR + mMinExposure + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_MAX + CommandManager.COMMAND_VARIABLE_SEPARATOR + mMaxExposure + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_COMPENSATION_STEP + CommandManager.COMMAND_VARIABLE_SEPARATOR + mExposureCompensationStep + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_LOCK_AVAILABLE + CommandManager.COMMAND_VARIABLE_SEPARATOR + mIsExposureLockAvailable + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_LOCKED + CommandManager.COMMAND_VARIABLE_SEPARATOR + mIsExposureLocked;

		if(pShouldProvideCommandCompletion)
		{
			commandString += "\r\n";
		}

		return commandString;
	}

	public static ExposureCommand parseData(String pData)
	{
		ExposureCommand exposureCommand;
		int minExposure = 0;
		int maxExposure = 0;
		int currentExposure = 0;
		float exposureCompensationStep = 0;
		boolean isExposureLocked = false;
		boolean isExposureLockAvailable = false;

		String[] parameters = pData.split(CommandManager.COMMAND_PARAMETER_SEPARATOR);
		String[] values;

		for(String parameter : parameters)
		{
			values = parameter.split(CommandManager.COMMAND_VARIABLE_SEPARATOR);

			if(values[0].equals(EXPOSURE_VALUE))
			{
				currentExposure = Integer.parseInt(values[1]);
			}
			else if(values[0].equals(EXPOSURE_MIN))
			{
				minExposure = Integer.parseInt(values[1]);
			}
			else if(values[0].equals(EXPOSURE_MAX))
			{
				maxExposure = Integer.parseInt(values[1]);
			}
			else if(values[0].equals(EXPOSURE_COMPENSATION_STEP))
			{
				exposureCompensationStep = Float.parseFloat(values[1]);
			}
			else if(values[0].equals(EXPOSURE_LOCK_AVAILABLE))
			{
				if(values[1].equals("true"))
				{
					isExposureLockAvailable = true;
				}
				else
				{
					isExposureLockAvailable = false;
				}
			}
			else if(values[0].equals(EXPOSURE_LOCKED))
			{
				if(values[1].equals("true"))
				{
					isExposureLocked = true;
				}
				else
				{
					isExposureLocked = false;
				}
			}
		}

		exposureCommand = new ExposureCommand(currentExposure, minExposure, maxExposure, exposureCompensationStep, isExposureLockAvailable, isExposureLocked);

		return exposureCommand;
	}
}
