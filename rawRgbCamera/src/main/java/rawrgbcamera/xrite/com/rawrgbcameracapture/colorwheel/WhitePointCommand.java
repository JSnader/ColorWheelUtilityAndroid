package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import java.util.ArrayList;

public class WhitePointCommand extends Command
{
	public final static String WHITE_POINT_TYPE = "WhiteBalance";
	private static final String WHITE_POINT_VALUE = "typeAndroid";
	private	static final String	WHITE_POINT_VALUE_ARRAY = "typeArrayAndroid";
	private static final String WHITE_POINT_LOCK_AVAILABLE = "whitePointLockAvailability";
	private static final String WHITE_POINT_LOCKED = "whitePointLocked";

	public WhitePointBalance whitePoint;
	ArrayList<WhitePointBalance> availableWhitePoints;
	public boolean isWhitePointLocked;
	boolean isWhitePointLockAvailable;

	public enum WhitePointBalance
	{
		AUTO("auto"),
		INCANDESCENT("incandescent"),
		FLUORESCENT("fluorescent"),
		WARM_FLUORESCENT("warm-fluorescent"),
		DAYLIGHT("daylight"),
		CLOUDY_DAYLIGHT("cloudy-daylight"),
		TWILIGHT("twilight"),
		SHADE("shade");

		public String description;

		WhitePointBalance(String descriptionF)
		{
			description = descriptionF;
		}

		public static WhitePointBalance getWhitePointBalance(String descriptionF)
		{
			if(descriptionF.equals("auto")){return AUTO;}
			else if(descriptionF.equals("incadescent")){return INCANDESCENT;}
			else if(descriptionF.equals("fluorescent")){return FLUORESCENT;}
			else if(descriptionF.equals("warm-fluorescent")){return WARM_FLUORESCENT;}
			else if(descriptionF.equals("daylight")){return DAYLIGHT;}
			else if(descriptionF.equals("cloudy-daylight")){return CLOUDY_DAYLIGHT;}
			else if(descriptionF.equals("twilight")){return TWILIGHT;}
			else if(descriptionF.equals("shade")){return SHADE;}

			return AUTO;
		}
	}

	public WhitePointCommand(WhitePointBalance whitePointF, ArrayList<WhitePointBalance> availableWhitePointsF, boolean isWhitePointLockAvailableF, boolean isWhitePointLockedF)
	{
		super(WHITE_POINT_TYPE);
		whitePoint = whitePointF;
		availableWhitePoints = availableWhitePointsF;
		isWhitePointLockAvailable = isWhitePointLockAvailableF;
		isWhitePointLocked = isWhitePointLockedF;
	}

	public String getData(boolean lineEndingF)
	{
		String commandString = "";

		commandString += type + CommandManager.COMMAND_SEPARATOR;
		commandString += WHITE_POINT_VALUE + CommandManager.COMMAND_VARIABLE_SEPARATOR + whitePoint.description + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		for(WhitePointBalance whitePoint : availableWhitePoints)
		{
			commandString += WHITE_POINT_VALUE_ARRAY + CommandManager.COMMAND_VARIABLE_SEPARATOR + whitePoint.description + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		}
		commandString += WHITE_POINT_LOCK_AVAILABLE + CommandManager.COMMAND_VARIABLE_SEPARATOR + isWhitePointLockAvailable + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		commandString += WHITE_POINT_LOCKED + CommandManager.COMMAND_VARIABLE_SEPARATOR + isWhitePointLocked;

		if(lineEndingF)
		{
			commandString += "\r\n";
		}

		return commandString;
	}

	public static WhitePointCommand parseData(String dataF)
	{
		WhitePointCommand whitePointCommand;
		WhitePointBalance whitePoint = WhitePointBalance.AUTO;
		ArrayList<WhitePointBalance> availableWhitePoints = new ArrayList<WhitePointBalance>();
		boolean isWhitePointLocked = false;
		boolean isWhitePointLockAvailable = false;

		String[] parameters = dataF.split(CommandManager.COMMAND_PARAMETER_SEPARATOR);
		String[] values;

		for(String parameter : parameters)
		{
			values = parameter.split(CommandManager.COMMAND_VARIABLE_SEPARATOR);

			if(values[0].equals(WHITE_POINT_VALUE))
			{
				whitePoint = WhitePointBalance.getWhitePointBalance(values[1]);
			}
			else if(values[0].equals(WHITE_POINT_VALUE_ARRAY))
			{
				availableWhitePoints.add(WhitePointBalance.getWhitePointBalance(values[1]));
			}
			else if(values[0].equals(WHITE_POINT_LOCK_AVAILABLE))
			{
				if(values[1].equals("true"))
				{
					isWhitePointLockAvailable = true;
				}
				else
				{
					isWhitePointLockAvailable = false;
				}
			}
			else if(values[0].equals(WHITE_POINT_LOCKED))
			{
				if(values[1].equals("true"))
				{
					isWhitePointLocked = true;
				}
				else
				{
					isWhitePointLocked = false;
				}
			}
		}

		whitePointCommand = new WhitePointCommand(whitePoint, availableWhitePoints, isWhitePointLockAvailable, isWhitePointLocked);

		return whitePointCommand;
	}
}
