package rawrgbcamera.xrite.com.rawrgbcameracapture.colorwheel;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

import rawrgbcamera.xrite.com.rawrgbcameracapture.Constants;

public class BonjourHelper
{
    Context context_;

    NsdManager nsdManager_;
    NsdManager.ResolveListener resolveListener_;
    NsdManager.RegistrationListener registrationListener_;

    public static final String SERVICE_TYPE = "_camerasen._tcp";

    public static final String TAG = "NsdHelper";
    public String mServiceName = "Android_" + Build.MANUFACTURER + "_" + Build.MODEL;

    NsdServiceInfo service_;

    public BonjourHelper(Context contextF)
    {
        context_ = contextF;
        nsdManager_ = (NsdManager) contextF.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd()
    {
        initializeResolveListener();
        initializeRegistrationListener();
    }

    public void initializeResolveListener()
    {
        resolveListener_ = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfoF, int errorCodeF)
            {
                Log.e(TAG, "Resolve failed" + errorCodeF);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfoF)
            {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfoF);

                if (serviceInfoF.getServiceName().equals(mServiceName))
                {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                service_ = serviceInfoF;
            }
        };
    }

    public void initializeRegistrationListener()
    {
        registrationListener_ = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfoF)
            {
                mServiceName = nsdServiceInfoF.getServiceName();
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

    public void registerService(int port)
    {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        nsdManager_.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener_);
    }

    public NsdServiceInfo getChosenServiceInfo()
    {
        return service_;
    }

    public void tearDown()
    {
        try
		{
			nsdManager_.unregisterService(registrationListener_);
		} catch(Exception exceptionF)
		{
			Log.w(Constants.LOG_TAG, "Registration listener already unregistered.");
		}
    }
}
