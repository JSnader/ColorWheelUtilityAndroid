package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import com.xrite.xritecamera.XriteUcpCamera;

import java.util.ArrayList;

public class CommandManager
{
	public static final String COMMAND_CAMERA_RESPONSE_PARAM_SEPARATOR = "&";
	public static final String COMMAND_SEPARATOR = "$";
	private static final byte[] COMMAND_COMPLETION_BYTES = new byte[] {0x65,0x38,0x73,0x7A,0x62,0x6A,0x21,0x2E,0x3A,0x66,0x09,0x11,0x0D,0x0A};
	private static final int    COMMAND_DELIMITER_BYTE_LENGTH = COMMAND_COMPLETION_BYTES.length - 2;
	public static final String COMMAND_COMPLETION_CHARACTERS = new String(COMMAND_COMPLETION_BYTES);
	public static final String COMMAND_VARIABLE_SEPARATOR = ":";
	public static final String COMMAND_PARAMETER_SEPARATOR = ";";

	public static String getDefaultCameraResponseMessage(CameraSensitivityListener cameraListenerF)
	{
		//Focus
		String currentFocusModeString = "";//cameraListenerF.getCamera().getFocusMode();
		FocusCommand.FocusMode focusMode = FocusCommand.FocusMode.getFocusMode(currentFocusModeString);
		ArrayList<String> supportedFocusStrings = new ArrayList<String>();//cameraListenerF.getCamera().getSupportedFocusModes();
		ArrayList<FocusCommand.FocusMode> supportedFocusModes = new ArrayList<FocusCommand.FocusMode>();
		for(String focusModeString : supportedFocusStrings)
		{
			supportedFocusModes.add(FocusCommand.FocusMode.getFocusMode(focusModeString));
		}
		FocusCommand focusCommand = new FocusCommand(focusMode, supportedFocusModes);

		//White Point
		String currentWhiteBalanceString = "";//cameraListenerF.getCamera().getWhiteBalance();
		WhitePointCommand.WhitePointBalance whitePointBalance = WhitePointCommand.WhitePointBalance.getWhitePointBalance(currentWhiteBalanceString);
		ArrayList<String> supportedWhiteBalanceStrings = new ArrayList<String>();//cameraListenerF.getCamera().getSupportedWhiteBalances();
		ArrayList<WhitePointCommand.WhitePointBalance> supportedWhitePointBalances = new ArrayList<WhitePointCommand.WhitePointBalance>();
		for(String whitePointString : supportedWhiteBalanceStrings)
		{
			supportedWhitePointBalances.add(WhitePointCommand.WhitePointBalance.getWhitePointBalance(whitePointString));
		}
		boolean isWhiteBalanceLocked = cameraListenerF.getCamera().isWhiteBalanceLocked();
		boolean isWhiteBalanceLockAvailable = cameraListenerF.getCamera().isWhiteBalanceLockAvailable();
		WhitePointCommand whitePointCommand = new WhitePointCommand(whitePointBalance, supportedWhitePointBalances, isWhiteBalanceLockAvailable, isWhiteBalanceLocked);

		//Exposure
		int minExposure = cameraListenerF.getCamera().getMinimumExposure();
		int maxExposure = cameraListenerF.getCamera().getMaximumExposure();
		int currentExposure = cameraListenerF.getCamera().getExposure();
		float exposureCompensationStep = cameraListenerF.getCamera().getExposureCompensationStep();
		boolean isExposureLocked = cameraListenerF.getCamera().isAutoExposureLocked();
		boolean isExosureLockAvailable = cameraListenerF.getCamera().isExposureLockAvailable();
		ExposureCommand exposureCommand = new ExposureCommand(currentExposure, minExposure, maxExposure, exposureCompensationStep, isExosureLockAvailable, isExposureLocked);

		ArrayList<Command> defaultConfigurations = new ArrayList<Command>();
		defaultConfigurations.add(focusCommand);
		defaultConfigurations.add(whitePointCommand);
		defaultConfigurations.add(exposureCommand);
		CameraResponseCommand defaultCameraSettings = new CameraResponseCommand(defaultConfigurations);
		String responseDefaultData = defaultCameraSettings.getData(true);
//		Log.i(Constants.LOG_TAG, "Going out -> " + responseDefaultData);
		return responseDefaultData;
	}

	public static void loadCameraResponseSettings(XriteUcpCamera cameraF, CameraResponseCommand cameraResponseF)
	{

	}

    public static void handleCommand(String commandDataF, CameraSensitivityListener cameraListenerF)
    {
    	if(commandDataF == null)
    	{
    		return;
    	}

        String parsedCommand[] = parseCommandTypeFrom(commandDataF);
        String command = parsedCommand[0];
        String commandData = parsedCommand[1];

        if(command.equals(FocusCommand.FOCUS_TYPE))
        {
        	cameraListenerF.focusUpdate(FocusCommand.parseData(commandData));
        }
        else if(command.equals(ExposureCommand.EXPOSURE_TYPE))
        {
        	cameraListenerF.exposureUpdate(ExposureCommand.parseData(commandData));
        }
        else if(command.equals(WhitePointCommand.WHITE_POINT_TYPE))
        {
        	cameraListenerF.whitePointUpdate(WhitePointCommand.parseData(commandData));
        }
        else if(command.equals(PictureTakenCommand.PICTURE_TAKEN_TYPE))
        {
        	cameraListenerF.requestPicture();
        }
    }

    private static String[] parseCommandTypeFrom(String entireCommandF)
    {
        int commandIndex = entireCommandF.indexOf(CommandManager.COMMAND_SEPARATOR);
        String command = entireCommandF.substring(0, commandIndex);
        String parameters = entireCommandF.substring(commandIndex + 1, entireCommandF.length() - CommandManager.COMMAND_DELIMITER_BYTE_LENGTH);

//        Log.d(Constants.LOG_TAG, "command = " + command + " parameters = " + parameters);

        return new String[] {command, parameters};
    }
}
