package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

public abstract class Command
{
	public String type;

	public Command(String typeF)
	{
		type = typeF;
	}

	public abstract String getData(boolean lineEndingF);
}
