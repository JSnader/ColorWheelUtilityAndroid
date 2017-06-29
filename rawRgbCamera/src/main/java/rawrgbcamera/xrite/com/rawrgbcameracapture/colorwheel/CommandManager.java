/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import com.xrite.xritecamera.XriteUcpCamera;

import java.util.ArrayList;

import rawrgbcamera.xrite.com.rawrgbcameracapture.RawRgbCapture;

public class CommandManager
{
	public static final String 	COMMAND_CAMERA_RESPONSE_PARAM_SEPARATOR = "&";
	public static final String 	COMMAND_SEPARATOR = "$";
	private static final byte[] COMMAND_COMPLETION_BYTES = new byte[] {0x65,0x38,0x73,0x7A,0x62,0x6A,0x21,0x2E,0x3A,0x66,0x09,0x11,0x0D,0x0A};
	private static final int    COMMAND_DELIMITER_BYTE_LENGTH = COMMAND_COMPLETION_BYTES.length - 2;
	public static final String 	COMMAND_COMPLETION_CHARACTERS = new String(COMMAND_COMPLETION_BYTES);
	public static final String 	COMMAND_VARIABLE_SEPARATOR = ":";
	public static final String 	COMMAND_PARAMETER_SEPARATOR = ";";

	public static String getDefaultCameraResponseMessage(CameraSensitivityListener pListener)
	{
		//Focus
		String currentFocusModeString = "";//pListener.getCamera().getFocusMode();
		FocusCommand.FocusMode focusMode = FocusCommand.FocusMode.getFocusMode(currentFocusModeString);
		ArrayList<String> supportedFocusStrings = new ArrayList<String>();//pListener.getCamera().getSupportedFocusModes();
		ArrayList<FocusCommand.FocusMode> supportedFocusModes = new ArrayList<FocusCommand.FocusMode>();
		for(String focusModeString : supportedFocusStrings)
		{
			supportedFocusModes.add(FocusCommand.FocusMode.getFocusMode(focusModeString));
		}
		FocusCommand focusCommand = new FocusCommand(focusMode, supportedFocusModes);

		//White Point
		String currentWhiteBalanceString = "";//pListener.getCamera().getWhiteBalance();
		WhitePointCommand.WhitePointBalance whitePointBalance = WhitePointCommand.WhitePointBalance.getWhitePointBalance(currentWhiteBalanceString);
		ArrayList<String> supportedWhiteBalanceStrings = new ArrayList<String>();//pListener.getCamera().getSupportedWhiteBalances();
		ArrayList<WhitePointCommand.WhitePointBalance> supportedWhitePointBalances = new ArrayList<WhitePointCommand.WhitePointBalance>();
		for(String whitePointString : supportedWhiteBalanceStrings)
		{
			supportedWhitePointBalances.add(WhitePointCommand.WhitePointBalance.getWhitePointBalance(whitePointString));
		}
//		boolean isWhiteBalanceLocked = pListener.getCamera().isWhiteBalanceLocked();
//		boolean isWhiteBalanceLockAvailable = pListener.getCamera().isWhiteBalanceLockAvailable();
		WhitePointCommand whitePointCommand = new WhitePointCommand(whitePointBalance, supportedWhitePointBalances, false, false);

		//Exposure
//		int minExposure = pListener.getCamera().getMinimumExposure();
//		int maxExposure = pListener.getCamera().getMaximumExposure();
//		int currentExposure = pListener.getCamera().getExposure();
//		float exposureCompensationStep = pListener.getCamera().getExposureCompensationStep();
//		boolean isExposureLocked = pListener.getCamera().isAutoExposureLocked();
//		boolean isExosureLockAvailable = pListener.getCamera().isExposureLockAvailable();
		ExposureCommand exposureCommand = new ExposureCommand(0, -5, 5, 0.333f, false, false);

		ArrayList<Command> defaultConfigurations = new ArrayList<Command>();
		defaultConfigurations.add(focusCommand);
		defaultConfigurations.add(whitePointCommand);
		defaultConfigurations.add(exposureCommand);
		CameraResponseCommand defaultCameraSettings = new CameraResponseCommand(defaultConfigurations);
		String responseDefaultData = defaultCameraSettings.createCommand(true);
//		Log.i(Constants.LOG_TAG, "Going out -> " + responseDefaultData);
		return responseDefaultData;
	}

	public static void loadCameraResponseSettings(XriteUcpCamera pCamera, CameraResponseCommand pCameraResponse)
	{

	}

    public static void handleCommand(String pCommandData, CameraSensitivityListener pListener)
    {
    	if(pCommandData == null)
    	{
    		return;
    	}

        String parsedCommand[] = parseCommandTypeFrom(pCommandData);
        String command = parsedCommand[0];
        String commandData = parsedCommand[1];

        if(command.equals(FocusCommand.FOCUS_TYPE))
        {
        	pListener.focusUpdate(FocusCommand.parseData(commandData));
        }
        else if(command.equals(ExposureCommand.EXPOSURE_TYPE))
        {
        	pListener.exposureUpdate(ExposureCommand.parseData(commandData));
        }
        else if(command.equals(WhitePointCommand.WHITE_POINT_TYPE))
        {
        	pListener.whitePointUpdate(WhitePointCommand.parseData(commandData));
        }
        else if(command.equals(RawCommand.RAW_PIC_COMMAND_TYPE))
        {
        	RawRgbCapture.mHasRequestedCapture = true;//pListener.requestPicture();
        }
    }

    private static String[] parseCommandTypeFrom(String pEntireCommand)
    {
        int commandIndex = pEntireCommand.indexOf(CommandManager.COMMAND_SEPARATOR);
        String command = pEntireCommand.substring(0, commandIndex);
        String parameters = pEntireCommand.substring(commandIndex + 1, pEntireCommand.length() - CommandManager.COMMAND_DELIMITER_BYTE_LENGTH);

//        Log.d(Constants.LOG_TAG, "command = " + command + " parameters = " + parameters);

        return new String[] {command, parameters};
    }
}
