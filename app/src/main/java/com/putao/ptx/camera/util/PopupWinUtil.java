package com.putao.ptx.camera.util;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.PopupWindow;

import com.putao.ptx.camera.R;


/**
 * Created by Administrator on 2016/3/18.
 */
public class PopupWinUtil {
    private static PopupWindow popWin = null;

    public enum PopStyle {
        CENTER, LEFT, RIGHT
    }

    public static void showPopupWindow(View rootView, View anchor, int width, int height, int offX, int offY, int gravity, PopStyle style, PopupWindow.OnDismissListener listener) {
        if (popWin != null && popWin.isShowing()) {
            return;
        }
        popWin = new PopupWindow(rootView, width, height);
        switch (style) {
            case CENTER:
                popWin.setAnimationStyle(R.style.popwin_center__style);
                break;
            case LEFT:
                popWin.setAnimationStyle(R.style.popwin_left_style);
                break;
            case RIGHT:
                popWin.setAnimationStyle(R.style.popwin_right_style);
                break;
        }
        popWin.setBackgroundDrawable(new ColorDrawable());
        popWin.setFocusable(true);
        popWin.setOutsideTouchable(true);
        popWin.update();
        if (listener != null) {
            popWin.setOnDismissListener(listener);
        }
        popWin.showAsDropDown(anchor, offX, offY, gravity);
    }

    public static void hidePopWindow() {
        if (popWin != null && popWin.isShowing()) {
            popWin.dismiss();
        }
        popWin = null;
    }
}
