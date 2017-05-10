package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import rawrgbcamera.xrite.com.rawrgbcameracapture.Constants;

public class BonjourNetworkConnection
{
	private CameraSensitivityListener cameraHandler_;
    private ConnectionServer server_;

    private static final String TAG = "NetworkConnection";

    private Socket socket_;
    private int port_ = -1;

    public BonjourNetworkConnection(CameraSensitivityListener handlerF)
    {
        cameraHandler_ = handlerF;
        server_ = new ConnectionServer();
    }

    public int getLocalPort()
    {
    	return port_;
    }

    public synchronized void sendPhotograph(byte[] pictureBytesF)
    {
    	try
		{
    		PictureTakenCommand pictureRequested;
    		pictureRequested = new PictureTakenCommand(pictureBytesF);
			DataOutputStream dataOutputStream = new DataOutputStream(socket_.getOutputStream());
			ByteBuffer byteBuffer = ByteBuffer.allocate(PictureTakenCommand.PICTURE_TAKEN_TYPE.length() + CommandManager.COMMAND_SEPARATOR.length() + pictureBytesF.length + CommandManager.COMMAND_COMPLETION_CHARACTERS.length());
			byteBuffer.put(pictureRequested.getData(false).getBytes());
			byteBuffer.put(pictureRequested.getPictureData());
			byteBuffer.put(CommandManager.COMMAND_COMPLETION_CHARACTERS.getBytes());
			dataOutputStream.write(byteBuffer.array());
		} catch(IOException e)
		{
			e.printStackTrace();
		}
    }

    public synchronized void sendPhotoAcknowledgement(int pLogNumber){
        try{
            PictureTakenCommand pictureRequested;
            pictureRequested = new PictureTakenCommand(pLogNumber);
            DataOutputStream dataOutputStream = new DataOutputStream(socket_.getOutputStream());
            ByteBuffer byteBuffer = ByteBuffer.allocate(PictureTakenCommand.PICTURE_TAKEN_TYPE.length() + CommandManager.COMMAND_SEPARATOR.length() + Integer.SIZE + CommandManager.COMMAND_COMPLETION_CHARACTERS.length());
            byteBuffer.put(pictureRequested.getData(false).getBytes());
            byteBuffer.putInt(pictureRequested.getPictureIdentifier());
            byteBuffer.put(CommandManager.COMMAND_COMPLETION_CHARACTERS.getBytes());
            dataOutputStream.write(byteBuffer.array());
        }catch(IOException pException){
            pException.printStackTrace();
        }
    }

    public void tearDown()
    {
        server_.disconnectServer();
    }

    private class ConnectionServer
    {
        ServerSocket serverSocket_ = null;
        Thread serverThread_ = null;

        public ConnectionServer()
        {
            serverThread_ = new Thread(new ServerThread());
            serverThread_.start();
        }

        public void disconnectServer()
        {
            serverThread_.interrupt();
            try
            {
                serverSocket_.close();
            } catch (IOException ioe)
            {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        class ServerThread implements Runnable
        {
        	DataOutputStream dataOutStream_;
        	DataInputStream dataInputStream_;

            @Override
            public void run()
            {
                try
                {
                    serverSocket_ = new ServerSocket(0);

                    port_ = serverSocket_.getLocalPort();

                    socket_ = serverSocket_.accept();

                    dataOutStream_ = new DataOutputStream(socket_.getOutputStream());
                    dataOutStream_.write(CommandManager.getDefaultCameraResponseMessage(cameraHandler_).getBytes()); //Report camera capabilities to remote application.

                    dataInputStream_ = new DataInputStream(socket_.getInputStream());

                    while (!Thread.currentThread().isInterrupted())
                    {
                    	try
                    	{
                    		CommandManager.handleCommand(dataInputStream_.readLine(), cameraHandler_);
                    	}catch(Exception exceptionF)
                    	{
                    		Log.e(Constants.LOG_TAG, "Exception during communication -> " + exceptionF.getStackTrace());
                    	}
                    }

                    try
    				{
                		if(serverSocket_ != null)
                		{
                			serverSocket_.close();
                		}
    				} catch(IOException exceptionF)
    				{
    					exceptionF.printStackTrace();
    				}
                    serverSocket_ = null;
            		cameraHandler_.releaseCamera();
                } catch (IOException exceptionF)
                {
                    Log.e(TAG, "Error at the server thread: ", exceptionF);
                }
            }

            public void tearDown()
            {
            	try
				{
            		if(serverSocket_ != null)
            		{
            			serverSocket_.close();
            		}
				} catch(IOException exceptionF)
				{
					exceptionF.printStackTrace();
				}
                serverSocket_ = null;
        		cameraHandler_.releaseCamera();
            }
        }
    }
}
