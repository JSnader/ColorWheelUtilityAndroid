/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import java.util.ArrayList;

public class FocusCommand extends Command
{
	public static final String FOCUS_TYPE = "Focus";
	private static final String FOCUS_VALUE = "typeAndroid";
	private static final String FOCUS_VALUE_ARRAY = "typeArrayAndroid";
	//TODO Could manipulate focus areas as needed for the demonstration application.

	FocusMode focus_;
	ArrayList<FocusMode> availableFoci_;

	public enum FocusMode
	{
		AUTO("auto"),
		CONTINUOUS_PICTURE("continuous-picture"),
		CONTINUOUS_VIDEO("continuous-video"),
		EXTENDED_DEPTH_OF_FIELD("edof"),
		FIXED("fixed"),
		INFINITY("infinity"),
		MACRO("macro");

		public String description;

		FocusMode(String descriptionF)
		{
			description = descriptionF;
		}

		public static FocusMode getFocusMode(String descriptionF)
		{
			if(descriptionF.equals("auto")){return AUTO;}
			else if(descriptionF.equals("continuous-picture")){return CONTINUOUS_PICTURE;}
			else if(descriptionF.equals("continuous-video")){return CONTINUOUS_VIDEO;}
			else if(descriptionF.equals("edof")){return EXTENDED_DEPTH_OF_FIELD;}
			else if(descriptionF.equals("fixed")){return FIXED;}
			else if(descriptionF.equals("infinity")){return INFINITY;}
			else if(descriptionF.equals("macro")){return MACRO;}

			return AUTO;
		}
	}

	public static String getType()
	{
		return FOCUS_TYPE;
	}

	public FocusCommand(FocusMode focusModeF, ArrayList<FocusMode> availableFocusModesF)
	{
		super(FOCUS_TYPE);
		focus_ = focusModeF;
		availableFoci_ = availableFocusModesF;
	}

	public String createCommand(boolean pShouldProvideCommandCompletion)
	{
		String commandString = "";

		commandString += mCommandType + CommandManager.COMMAND_SEPARATOR;
		commandString += FOCUS_VALUE + CommandManager.COMMAND_VARIABLE_SEPARATOR + focus_.description + CommandManager.COMMAND_PARAMETER_SEPARATOR;
		int index = 0;
		for(FocusMode focusMode : availableFoci_)
		{
			commandString += FOCUS_VALUE_ARRAY + CommandManager.COMMAND_VARIABLE_SEPARATOR + focusMode.description;
			if(index++ < availableFoci_.size() - 1)
			{
				commandString += CommandManager.COMMAND_PARAMETER_SEPARATOR;
			}
		}

		if(pShouldProvideCommandCompletion)
		{
			commandString += "\r\n";
		}

		return commandString;
	}

	public static FocusCommand parseData(String dataF)
	{
		FocusCommand focusCommand;
		FocusMode focusMode = FocusMode.AUTO;
		ArrayList<FocusMode> availableFocusModes = new ArrayList<FocusMode>();

		String[] parameters = dataF.split(CommandManager.COMMAND_PARAMETER_SEPARATOR);
		String[] values;

		for(String parameter : parameters)
		{
			values = parameter.split(CommandManager.COMMAND_VARIABLE_SEPARATOR);

			if(values[0].equals(FOCUS_VALUE))
			{
				focusMode = FocusMode.getFocusMode(values[1]);
			}
			else if(values[0].equals(FOCUS_VALUE_ARRAY))
			{
				availableFocusModes.add(FocusMode.getFocusMode(values[1]));
			}
		}

		focusCommand = new FocusCommand(focusMode, availableFocusModes);

		return focusCommand;
	}
}
