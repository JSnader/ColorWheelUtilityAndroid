package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

public class PictureTakenCommand extends Command
{
	public static final String PICTURE_TAKEN_TYPE = "PictureTaken";
	private byte[] pictureBytes_;

	public PictureTakenCommand(byte[] pictureBytesF)
	{
		super(PICTURE_TAKEN_TYPE);
		pictureBytes_ = pictureBytesF;
	}

	@Override
	public String getData(boolean lineEndingF)
	{
		String command = PICTURE_TAKEN_TYPE + CommandManager.COMMAND_SEPARATOR;

		return command;
	}

	public byte[] getPictureData()
	{
		return pictureBytes_;
	}

	public static PictureTakenCommand parseData(String dataF)
	{
		int indexOfCommand = dataF.indexOf(CommandManager.COMMAND_PARAMETER_SEPARATOR);
		byte[] pictureData = dataF.substring(indexOfCommand + 1, dataF.length() - 1).getBytes();

		return new PictureTakenCommand(pictureData);
	}
}
