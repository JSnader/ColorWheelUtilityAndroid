/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import android.util.Log;
import java.util.ArrayList;

import rawrgbcamera.xrite.com.rawrgbcameracapture.Constants;

public class CameraResponseCommand extends Command
{
	private static final String CAMERA_RESPONSE_TYPE = "CameraResponse";
	public ArrayList<Command> 	mCommandList;


	public CameraResponseCommand(ArrayList<Command> pCommandList)
	{
		super(CAMERA_RESPONSE_TYPE);
		mCommandList = pCommandList;
	}

	public String createCommand(boolean pShouldProvideCommandCompletion)
	{
		String commandString = CAMERA_RESPONSE_TYPE + CommandManager.COMMAND_SEPARATOR;

		int index = 0;
		for(Command command : mCommandList)
		{
			commandString += command.createCommand(false);
			if(index++ < mCommandList.size() - 1)
			{
				commandString += "&";
			}
		}

		if(pShouldProvideCommandCompletion)
		{
			commandString += CommandManager.COMMAND_COMPLETION_CHARACTERS;
		}

		return commandString;
	}

	public static CameraResponseCommand parseCommandData(String pParseableData)
	{
		ArrayList<Command> commands = new ArrayList<Command>();
		String[] parsedStr = pParseableData.split(CommandManager.COMMAND_CAMERA_RESPONSE_PARAM_SEPARATOR);

		for(String commandStr : parsedStr)
		{
			String[] parseCommand = commandStr.split(CommandManager.COMMAND_SEPARATOR);
			Command commandToAdd = null;

			if(parseCommand[0].equals(FocusCommand.FOCUS_TYPE))
			{
				commandToAdd = FocusCommand.parseData(parseCommand[1]);
			}
			else if(parseCommand[0].equals(WhitePointCommand.WHITE_POINT_TYPE))
			{
				commandToAdd = WhitePointCommand.parseData(parseCommand[1]);
			}
			else if(parseCommand[0].equals(ExposureCommand.EXPOSURE_TYPE))
			{
                commandToAdd = ExposureCommand.parseData(parseCommand[1]);
			}
			else
			{
				 Log.w(Constants.LOG_TAG, "Command " + parseCommand[0] + " not found.");
			}

			if (commandToAdd != null)
			{
                commands.add(commandToAdd);
            }
		}

		return new CameraResponseCommand(commands);
	}
}
