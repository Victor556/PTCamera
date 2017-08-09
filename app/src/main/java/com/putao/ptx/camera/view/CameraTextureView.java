package com.putao.ptx.camera.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private Context context;

    public CameraTextureView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initView();
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    private void initView() {
        this.setSurfaceTextureListener(this);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (callBack != null)
            callBack.onSurfaceTextureAvailable(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (callBack != null)
            callBack.onSurfaceTextureDestroyed(surface);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public interface CameraTextureViewCallBack {
        void onSurfaceTextureAvailable(SurfaceTexture surface);

        void onSurfaceTextureDestroyed(SurfaceTexture surface);
    }

    CameraTextureViewCallBack callBack = null;

    public void setCameraTextureViewCallBack(CameraTextureView.CameraTextureViewCallBack callBack) {
        this.callBack = callBack;
    }



}
