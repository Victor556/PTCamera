package com.putao.ptx.camera.util;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class PanoDialog extends Dialog {
    private static int default_pano_width = 576; //默认宽度
    private static int default_pano_height = 264;//默认高度

    public PanoDialog(Context context, View layout, int style) {
        this(context, default_pano_width, default_pano_height, layout, style);
    }

    public PanoDialog(Context context, int width, int height, View layout, int style) {
        super(context, style);
        setContentView(layout);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = width;
        params.height = height;
        window.setAttributes(params);
    }
}