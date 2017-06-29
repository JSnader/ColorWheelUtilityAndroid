/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

public class PictureTakenCommand extends Command
{
	public static final String 	PICTURE_TAKEN_TYPE = "PictureTaken";
	private byte[] 				mPictureBytes;
	private int 				mAcknowledgementNumber;

	public PictureTakenCommand(byte[] pictureBytesF)
	{
		super(PICTURE_TAKEN_TYPE);
		mPictureBytes = pictureBytesF;
	}

	public PictureTakenCommand(int pAcknowledgemenetNumber){
		super(PICTURE_TAKEN_TYPE);
		mAcknowledgementNumber = pAcknowledgemenetNumber;
	}

	@Override
	public String createCommand(boolean pShouldProvideCommandCompletion)
	{
		String command = PICTURE_TAKEN_TYPE + CommandManager.COMMAND_SEPARATOR;

		return command;
	}

	public byte[] getPictureData()
	{
		return mPictureBytes;
	}

	public int getPictureIdentifier(){ return mAcknowledgementNumber; }

	public static PictureTakenCommand parseData(String dataF)
	{
		int indexOfCommand = dataF.indexOf(CommandManager.COMMAND_PARAMETER_SEPARATOR);
		byte[] pictureData = dataF.substring(indexOfCommand + 1, dataF.length() - 1).getBytes();

		return new PictureTakenCommand(pictureData);
	}
}
