package rawrgbcamera.xrite.com.rawrgbcameracapture;

import android.view.MotionEvent;
import android.view.View;

import cameralib.xrite.com.xritecameralibrary.XriteTextureView;
import cameralib.xrite.com.xritecameralibrary.XriteUcpCamera;


/**
 * Created by jsnader on 1/18/17.
 */

public class ListenerTextureViewOnTouch implements View.OnTouchListener{
    private XriteUcpCamera mCamera;
    private XriteTextureView mTextureView;

    public ListenerTextureViewOnTouch(XriteUcpCamera pXriteCamera, XriteTextureView pTextureView){
        mCamera = pXriteCamera;
        mTextureView = pTextureView;
    }

    @Override
    public boolean onTouch(View pView, MotionEvent pEvent) {
        if (pEvent.getAction() == android.view.MotionEvent.ACTION_UP) {
//            mCamera.focusOnPoint((int)pEvent.getX(), (int)pEvent.getY(), pView.getWidth(), pView.getHeight()); /* If we are dealing with landscape. */
            mCamera.focusOnPoint((int)pEvent.getY(), pView.getWidth() - (int)pEvent.getX(), pView.getHeight(), pView.getWidth()); /*  */
        }
        return true;
    }
}
