package com.putao.ptx.camera.util;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by Administrator on 2016/5/16.
 */
public class AnimUtil {
    /**
     * 按钮隐藏动画
     *
     * @param view
     */
    public static void alphaHideView(final View view) {
        ViewPropertyAnimator animator = view.animate().alpha(0f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setClickable(false);
                view.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();

    }

    /**
     * 按钮显示动画
     *
     * @param view
     */
    public static void alphaShowView(final View view) {
        ViewPropertyAnimator animator = view.animate().alpha(1f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setClickable(true);
                view.setEnabled(true);
                view.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }

    /**
     * 放大
     *
     * @param view
     */
    public static void zoomIn(final View view) {
        ViewPropertyAnimator animator = view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }


    public static void zoomOut(final View view) {
        ViewPropertyAnimator animator = view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }


    public static void alphaView(View view, float alpha) {
        view.animate().alpha(alpha).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        view.setClickable(false);
    }

    public static void zoomView(View view, float scale) {
        view.animate().scaleX(scale).scaleY(scale).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    public static void translationYView(View view, float y) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", y, 0);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    public static void translationXView(View view, float x) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", x, 0);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

}
