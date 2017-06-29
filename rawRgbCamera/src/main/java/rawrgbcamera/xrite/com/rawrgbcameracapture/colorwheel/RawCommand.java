package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

/**
 * Created by jsnader on 5/12/17.
 */

public class RawCommand extends Command
{
    public static final String RAW_PIC_COMMAND_TYPE = "Raw";
    private String 			   mRawFilePrefix;

    public RawCommand(String pRawFilePrefix){
        super(RAW_PIC_COMMAND_TYPE);
        mRawFilePrefix = pRawFilePrefix;
    }

    @Override
    public String createCommand(boolean pShouldProvideCommandCompletion)
    {
        String command = RAW_PIC_COMMAND_TYPE + CommandManager.COMMAND_SEPARATOR;



        return command;
    }

    public String getRawFilePrefix(){ return mRawFilePrefix; }

    public static RawCommand parseData(String dataF)
    {
        int indexOfCommand = dataF.indexOf(CommandManager.COMMAND_PARAMETER_SEPARATOR);
        byte[] pictureData = dataF.substring(indexOfCommand + 1, dataF.length() - 1).getBytes();

        return new RawCommand("");
    }
}
