/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
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
    private static final String         TAG = "NetworkConnection";
	private CameraSensitivityListener   mListener;
    private ConnectionServer            mServer;
    private Socket                      mSocket;
    private int                         mPort = -1; //Designates to the server to choose a random available port.

    public BonjourNetworkConnection(CameraSensitivityListener pListener)
    {
        mListener = pListener;
        mServer = new ConnectionServer();
    }

    public int getLocalPort()
    {
    	return mPort;
    }

    public synchronized void sendPhotograph(byte[] pPictureBytes)
    {
    	try
		{
    		PictureTakenCommand pictureRequested;
    		pictureRequested = new PictureTakenCommand(pPictureBytes);
			DataOutputStream dataOutputStream = new DataOutputStream(mSocket.getOutputStream());
			ByteBuffer byteBuffer = ByteBuffer.allocate(PictureTakenCommand.PICTURE_TAKEN_TYPE.length() + CommandManager.COMMAND_SEPARATOR.length() + pPictureBytes.length + CommandManager.COMMAND_COMPLETION_CHARACTERS.length());
			byteBuffer.put(pictureRequested.createCommand(false).getBytes());
			byteBuffer.put(pictureRequested.getPictureData());
			byteBuffer.put(CommandManager.COMMAND_COMPLETION_CHARACTERS.getBytes());
			dataOutputStream.write(byteBuffer.array());
		} catch(IOException e)
		{
			e.printStackTrace();
		}
    }

    public synchronized void sendRawPhotoCapturePrefix(final int pSessionIdentifier, final String pLogFilePrefix){
            final RawCommand rawPicCommand;
            rawPicCommand = new RawCommand(pLogFilePrefix);
            if(mSocket != null) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            DataOutputStream dataOutputStream = new DataOutputStream(mSocket.getOutputStream());
                            ByteBuffer byteBuffer = ByteBuffer.allocate(RawCommand.RAW_PIC_COMMAND_TYPE.length() + CommandManager.COMMAND_SEPARATOR.length() + Integer.SIZE + CommandManager.COMMAND_COMPLETION_CHARACTERS.length());
                            byteBuffer.put(rawPicCommand.createCommand(false).getBytes());
                            byteBuffer.put(new String("" + pSessionIdentifier).getBytes());
                            byteBuffer.put("&".getBytes());
                            byteBuffer.put(pLogFilePrefix.getBytes());
                            byteBuffer.put(CommandManager.COMMAND_COMPLETION_CHARACTERS.getBytes());
                            dataOutputStream.write(byteBuffer.array());
                        }catch(IOException pException){
                            pException.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
    }

    public void tearDown()
    {
        mServer.disconnectServer();
    }

    private class ConnectionServer
    {
        ServerSocket mServerSocket = null;
        Thread mServerThread = null;

        public ConnectionServer()
        {
            mServerThread = new Thread(new ServerThread());
            mServerThread.start();
        }

        public void disconnectServer()
        {
            mServerThread.interrupt();
            try
            {
                mServerSocket.close();
            } catch (IOException ioe)
            {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        class ServerThread implements Runnable
        {
        	DataOutputStream mDataOutputStream;
        	DataInputStream  mDataInputStream;

            @Override
            public void run()
            {
                try
                {
                    mServerSocket = new ServerSocket(0);

                    mPort = mServerSocket.getLocalPort();

                    mSocket = mServerSocket.accept();

                    mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
                    mDataOutputStream.write(CommandManager.getDefaultCameraResponseMessage(mListener).getBytes()); //Report camera capabilities to remote application.

                    mDataInputStream = new DataInputStream(mSocket.getInputStream());

                    while (!Thread.currentThread().isInterrupted())
                    {
                    	try
                    	{
                    		CommandManager.handleCommand(mDataInputStream.readLine(), mListener);
                    	}catch(Exception exceptionF)
                    	{
                    		Log.e(Constants.LOG_TAG, "Exception during communication -> " + exceptionF.getStackTrace());
                    	}
                    }

                    try
    				{
                		if(mServerSocket != null)
                		{
                			mServerSocket.close();
                		}
    				} catch(IOException exceptionF)
    				{
    					exceptionF.printStackTrace();
    				}
                    mServerSocket = null;
            		mListener.releaseCamera();
                } catch (IOException exceptionF)
                {
                    Log.e(TAG, "Error at the server thread: ", exceptionF);
                }
            }

            public void tearDown()
            {
            	try
				{
            		if(mServerSocket != null)
            		{
            			mServerSocket.close();
            		}
				} catch(IOException exceptionF)
				{
					exceptionF.printStackTrace();
				}
                mServerSocket = null;
        		mListener.releaseCamera();
            }
        }
    }
}
