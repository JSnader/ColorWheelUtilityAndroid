package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

public class ExposureCommand extends Command
{
	public static final String EXPOSURE_TYPE = "ExposureCommand";
	private static final String EXPOSURE_VALUE = "exposureValue";
	private	static final String	EXPOSURE_MIN = "exposureMin";
	private	static final String	EXPOSURE_MAX = "exposureMax";
	private	static final String	EXPOSURE_COMPENSATION_STEP = "exposureCompensationStep";
	private static final String EXPOSURE_LOCKED = "exposureLocked";
	private static final String EXPOSURE_LOCK_AVAILABLE = "exposureLockAvailability";

	int minExposure_;
	int maxExposure_;
	public int currentExposure;
	float exposureCompensationStep_;
	public boolean isExposureLocked;
	boolean isExposureLockAvailable_;

	public ExposureCommand(int currentExposureF, int minExposureF, int maxExposureF, float exposureCompensationStepF, boolean isExposureLockAvailableF, boolean isExposureLockedF)
	{
		super(EXPOSURE_TYPE);
		currentExposure = currentExposureF;
		minExposure_ = minExposureF;
		maxExposure_ = maxExposureF;
		exposureCompensationStep_ = exposureCompensationStepF;
		isExposureLocked = isExposureLockedF;
		isExposureLockAvailable_ = isExposureLockAvailableF;
	}

	public String getData(boolean lineEndingF)
	{
		String commandString = "";

		commandString += type + CommandManager.COMMAND_SEPARATOR;
		commandString += EXPOSURE_VALUE + CommandManager.COMMAND_VARIABLE_SEPARATOR + currentExposure + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_MIN + CommandManager.COMMAND_VARIABLE_SEPARATOR + minExposure_ + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_MAX + CommandManager.COMMAND_VARIABLE_SEPARATOR + maxExposure_ + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_COMPENSATION_STEP + CommandManager.COMMAND_VARIABLE_SEPARATOR + exposureCompensationStep_ + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_LOCK_AVAILABLE + CommandManager.COMMAND_VARIABLE_SEPARATOR + isExposureLockAvailable_ + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += EXPOSURE_LOCKED + CommandManager.COMMAND_VARIABLE_SEPARATOR + isExposureLocked;

		if(lineEndingF)
		{
			commandString += "\r\n";
		}

		return commandString;
	}

	public static ExposureCommand parseData(String dataF)
	{
		ExposureCommand exposureCommand;
		int minExposure = 0;
		int maxExposure = 0;
		int currentExposure = 0;
		float exposureCompensationStep = 0;
		boolean isExposureLocked = false;
		boolean isExposureLockAvailable = false;

		String[] parameters = dataF.split(CommandManager.COMMAND_PARAMETER_SEPARATOR);
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
