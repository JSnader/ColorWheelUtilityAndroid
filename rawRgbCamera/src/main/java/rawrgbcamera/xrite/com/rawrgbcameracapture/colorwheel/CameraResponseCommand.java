package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import android.util.Log;
import java.util.ArrayList;

import rawrgbcamera.xrite.com.rawrgbcameracapture.Constants;

public class CameraResponseCommand extends Command
{
	private static final String CAMERA_RESPONSE_TYPE = "CameraResponse";
	public ArrayList<Command> commands;


	public CameraResponseCommand(ArrayList<Command> commandsF)
	{
		super(CAMERA_RESPONSE_TYPE);
		commands = commandsF;
	}

	public String getData(boolean lineEndingF)
	{
		String commandString = CAMERA_RESPONSE_TYPE + CommandManager.COMMAND_SEPARATOR;

		int index = 0;
		for(Command command : commands)
		{
			commandString += command.getData(false);
			if(index++ < commands.size() - 1)
			{
				commandString += "&";
			}
		}

		if(lineEndingF)
		{
			commandString += CommandManager.COMMAND_COMPLETION_CHARACTERS;
		}

		return commandString;
	}

	public static CameraResponseCommand parseData(String dataF)
	{
		ArrayList<Command> commands = new ArrayList<Command>();
		String[] parsedStr = dataF.split(CommandManager.COMMAND_CAMERA_RESPONSE_PARAM_SEPARATOR);

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
