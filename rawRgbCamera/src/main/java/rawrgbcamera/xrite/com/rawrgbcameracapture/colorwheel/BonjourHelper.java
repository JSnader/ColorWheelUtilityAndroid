/*
 * Copyright (c) 2017 X-Rite, Inc. All rights reserved.
 */
package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

import rawrgbcamera.xrite.com.rawrgbcameracapture.Constants;

public class BonjourHelper
{
    public final static String          TAG = "NsdHelper";
    public final static String          SERVICE_TYPE = "_camerasen._tcp";
    public       static String          SERVICE_NAME = "Android_" + Build.MANUFACTURER + "_" + Build.MODEL;

    Context                             mContext;

    NsdManager                          mNsdManager;
    NsdManager.ResolveListener          mResolveListener;
    NsdManager.RegistrationListener     mRegistrationListener;
    NsdServiceInfo                      mService;

    public BonjourHelper(Context pContext)
    {
        mContext = pContext;
        mNsdManager = (NsdManager) pContext.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd()
    {
        initializeResolveListener();
        initializeRegistrationListener();
    }

    public void initializeResolveListener()
    {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfoF, int errorCodeF)
            {
                Log.e(TAG, "Resolve failed" + errorCodeF);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfoF)
            {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfoF);

                if (serviceInfoF.getServiceName().equals(SERVICE_NAME))
                {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfoF;
            }
        };
    }

    public void initializeRegistrationListener()
    {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfoF)
            {
                SERVICE_NAME = nsdServiceInfoF.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfoF, int errorCodeF)
            {
            	Log.w(Constants.LOG_TAG, "Registration failed with the server.");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfoF)
            {
            	Log.i(Constants.LOG_TAG, "Client unregistered successfully.");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfoF, int errorCodeF)
            {
            	Log.w(Constants.LOG_TAG, "Un-registration failed to detach fro network.");
            }
        };
    }

    public void registerService(int pPort)
    {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(pPort);
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public NsdServiceInfo getChosenServiceInfo()
    {
        return mService;
    }

    public void tearDown()
    {
        try
		{
			mNsdManager.unregisterService(mRegistrationListener);
		} catch(Exception exceptionF)
		{
			Log.w(Constants.LOG_TAG, "Registration listener already unregistered.");
		}
    }
}
