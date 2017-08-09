package com.putao.ptx.camera.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.putao.ptx.camera.R;
import com.putao.ptx.camera.model.EmCameraMode;
import com.putao.ptx.camera.util.CommonUtils;

import org.greenrobot.eventbus.EventBus;

public class CircleMenuLayout extends FrameLayout/*ViewGroup*/ {
    public static final float MIN_DEGREE = 90;
    public static final float MAX_DEGREE = 180;
    public static final int SIZE_BKG = 620;
    public static final int SIZE_CENTER = 155;
    public static final int CNT_ANI = 3;//声波动画的个数
    public static final float ANIM_MAX_SCALE = 1.8f;
    public static final int VIDEO_ANGLE = 150;
    public static final float MIN_ALPHA_MENU = 0.5f;
    private final int[] mItemTexts = {EmCameraMode.PANORAMA.mStrId, EmCameraMode.VIDEO.mStrId, EmCameraMode.COMMON.mStrId, EmCameraMode.VOICE.mStrId};
    private MenuView[] mItemViews;
    private Context mContext;
    private float layoutRadius;// 父类半径
    private float halfWidth;// 子元素半径
    private final float totalAngle = 120f;
    private final float mStepDegree = (MAX_DEGREE - MIN_DEGREE) / (mItemTexts.length - 1);
    private float angleDelay = 0f;
    private final float RADIO_DEFAULT_CHILD_DIMENSION = 0.7f/*0.35*/;// 该容器内childitem的默认尺寸
    private float RADIO_DEFAULT_CENTERITEM_DIMENSION = 0.7f;// 菜单的中心child的默认尺寸
    private float mStartAngle = 120;// 布局时的开始角度


    private ImageView centerView;
    private ImageView background;
    private int mMenuItemCount;// 菜单的个数
    private View[] mWaves = new View[CNT_ANI];
    private AnimationSet[] mAnimationSets = new AnimationSet[CNT_ANI];
    private AnimationSet mAnimationCenter;
    private OnMenuItemClickListener mOnMenuItemClickListener;// MenuItem的点击事件接口
    private int cursor = 2;
    private boolean mbSeatDisable = true;
    private View mItemPano;
    private View mItemPic;

    private ObjectAnimator mAnimSwitchMenu = new ObjectAnimator();
    private ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mStartAngle = (float) animation.getAnimatedValue();
            updateUi();
            Log.d(TAG, "switchByDegree onAnimationUpdate: " + mStartAngle);
        }
    };
    private AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            int idx = Math.round((mStartAngle - MIN_DEGREE) / mStepDegree);
            EmCameraMode byIdx = EmCameraMode.getByIdx(idx);
            EventBus.getDefault().post(byIdx);
            Log.d(TAG, "onAnimationEnd: EmCameraMode:" + byIdx);
        }
    };
    private ValueAnimator mAniWave;
    private long mLastTimeShow;

    public CircleMenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public CircleMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public CircleMenuLayout(Context context) {
        super(context);
        mContext = context;
        init();
        initAnim();
    }

    private void init() {
        background = new ImageView(mContext);
        LayoutParams center = new LayoutParams(SIZE_BKG, SIZE_BKG);
        //center.gravity = Gravity.CENTER;
        //background.setBackgroundResource(R.drawable.bg_kuaimen);
        background.setImageResource(R.drawable.bg_kuaimen);
        addView(background, center);
        int padding = 40;
        background.setPadding(padding, padding, padding, padding);

        centerView = new ImageView(mContext);
        //centerView.setClickable(false);
        LayoutParams centerP = new LayoutParams(SIZE_CENTER, SIZE_CENTER);
        centerView.setBackgroundResource(R.drawable.icon_camera_shoot_bg);
//        centerView.setImageResource(R.drawable.icon_camera_shoot);
        addView(centerView, centerP);
        centerView.setSoundEffectsEnabled(false);
        setMenuItemTexts();
    }

    private void setMenuItemTexts() {
        mMenuItemCount = mItemTexts.length;
        mItemViews = new MenuView[mMenuItemCount];

        float size = getTextSize();
        Log.d(TAG, "setMenuItemTexts:  getTextSize:" + size);
        for (int i = 0; i < mMenuItemCount; i++) {
            final int j = i;
            Resources resources = getResources();
            MenuView tv = new MenuView(getContext(), resources.getText(mItemTexts[i]).toString(),
                    resources.getDimensionPixelSize(R.dimen.text_size_menu));
            tv.setBackgroundColor(Color.TRANSPARENT);
            if (mItemTexts[i] == EmCameraMode.PANORAMA.mStrId) {
                mItemPano = tv;
            } else if (mItemTexts[i] == EmCameraMode.COMMON.mStrId) {
                mItemPic = tv;
            }
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isMenuAnimRunning()) {
                        return;//菜单正在进行旋转动画时，禁止响应点击事件
                    }
                    boolean allowClick = false;
                    if (mOnMenuItemClickListener != null
                            && SystemClock.elapsedRealtime() - mLastTimeShow > 500) {
                        allowClick = mOnMenuItemClickListener.itemClick(v, j);
                    }
                    if (!allowClick) {
                        return;
                    }
                    float degree = mStepDegree * (mItemTexts.length - j - 1) + MIN_DEGREE - mStartAngle;
                    switchByDegree(degree);
                    Log.d(TAG, "onClick: switchByDegree() 点击菜单列表" + degree);
                }
            });
            mItemViews[i] = tv;
            // 添加view到容器中
            addView(tv);
        }
        mItemPano.setVisibility(mbSeatDisable ? View.INVISIBLE : View.VISIBLE);
    }

    private float getTextSize() {
        return getResources().getDimension(R.dimen.text_size_menu);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resWidth = 0;
        int resHeight = 0;

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        // 得到宽和高
        resWidth = resHeight = Math.min(width, height);
        //setMeasuredDimension(resWidth, resHeight);//TODO
        setMeasuredDimension(Math.max(width, SIZE_BKG), Math.max(height, SIZE_BKG));

        // 得到半径
        //layoutRadius = Math.max(getMeasuredWidth(), getMeasuredHeight()) / 2f;
        layoutRadius = SIZE_BKG / 2;
        final int count = getChildCount();
        // 得到menuitem尺寸
        int childSize = (int) getMenuItemHeight();//(int) (layoutRadius * RADIO_DEFAULT_CHILD_DIMENSION);
        int childSizeWidth = getMenuItemWidth();
        // 设置menuitem测量模式
        int childMode = MeasureSpec.EXACTLY;
        // 迭代测量menuitem
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            // 计算menu item的尺寸；以及和设置好的模式，去对item进行测量
            int makeMeasureSpec = -1;
            int makeMeasureSpecW = -1;
            if (child == centerView) {
                //makeMeasureSpec = MeasureSpec.makeMeasureSpec((int) (layoutRadius * RADIO_DEFAULT_CENTERITEM_DIMENSION), childMode);
                makeMeasureSpec = MeasureSpec.makeMeasureSpec(SIZE_CENTER, childMode);
                child.measure(makeMeasureSpec, makeMeasureSpec);
            } else if (child == background) {
                makeMeasureSpec = MeasureSpec.makeMeasureSpec(SIZE_BKG, childMode);
                child.measure(makeMeasureSpec, makeMeasureSpec);
            } else {
                if (child instanceof MenuView) {
                    makeMeasureSpec = MeasureSpec.makeMeasureSpec(childSize, childMode);
                    makeMeasureSpecW = MeasureSpec.makeMeasureSpec(childSizeWidth, childMode);
                    child.measure(makeMeasureSpec, makeMeasureSpecW);
                }
            }
        }
    }

    public boolean isTouchOnContent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float rx = event.getRawX();
        float ry = event.getRawY();
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        if (rx < loc[0] || rx > loc[0] + getWidth() || ry < loc[1] || ry > loc[1] + getHeight()) {
            return false;
        }
//        if (getVisibility() == View.VISIBLE &&
//                Math.pow(loc[0] + getWidth() / 2 - rx, 2)
//                        + Math.pow(loc[1] + getHeight() / 2 - ry, 2)
//                        < Math.pow(getHeight() / 4, 2)) {
//            return true;
//        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.getLocationOnScreen(loc);
            if (child.getVisibility() == View.VISIBLE
                    && child.isEnabled()
                    && rx >= loc[0] && rx < loc[0] + child.getWidth()
                    && ry >= loc[1] && ry < loc[1] + child.getHeight()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setOnTouchListener(final OnTouchListener l) {
        super.setOnTouchListener(l);
        OnTouchListener onTouchListener = new OnTouchListener() {
            float x;
            float y;
            final int SHRESHOULD_CLICK = 50;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                l.onTouch(v, event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    x = event.getX();
                    y = event.getY();
                    v.setPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getX() - x) + Math.abs(event.getY() - y) > SHRESHOULD_CLICK) {
                        v.setPressed(false);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(event.getX() - x) + Math.abs(event.getY() - y) < SHRESHOULD_CLICK) {
                        v.performClick();
                    }
                    v.setPressed(false);
                }
                return true;
            }
        };
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt != background) {
                childAt.setOnTouchListener(onTouchListener);
            } else {
                childAt.setEnabled(false);
                childAt.setClickable(false);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        float left, top;
        float cWidth = getMenuItemWidth();
        halfWidth = cWidth / 2f;
        angleDelay = 30;//totalAngle / (mItemTexts.length - 1);
        float angle = mStartAngle;

        int w = getMeasuredWidth();
        int h = SIZE_BKG;
        int bw = background.getMeasuredWidth();
        int bh = background.getMeasuredHeight();
        int cx = w - SIZE_BKG / 2;
        int cy = h - SIZE_BKG / 2;
        float halfHeight = getMenuItemHeight() * 0.5f;
        boolean bZh = CommonUtils.isZh(getContext());
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child == centerView)
                continue;

            if (child == background)
                continue;

            if (child.getVisibility() == GONE) {
                continue;
            }

            if (!(child instanceof MenuView)) {
                continue;
            }


            // 计算中心点到menu item中心的距离
            //float tmp = layoutRadius - halfWidth - 10;
            float tmp = layoutRadius * 0.8f;
            if (!bZh) {//英文影响布局
                tmp = layoutRadius * 0.9f;
            }

            // 计算item中心点的横坐标
            //left = layoutRadius + (int) Math.round(tmp * Math.cos(Math.toRadians(angle)) - halfWidth);
            //left = (float) (w - layoutRadius + tmp * Math.cos(Math.toRadians(angle)) - cWidth / 2);
            left = w - layoutRadius + (int) Math.round(tmp * Math.cos(Math.toRadians(angle)) - halfWidth);
            // 计算item的纵坐标
            //top = layoutRadius + (int) Math.round(tmp * Math.sin(Math.toRadians(angle)) - halfWidth);
            //top = h - layoutRadius + (int) Math.round(tmp * Math.sin(Math.toRadians(angle))) - offset;
            top = h - layoutRadius + (int) Math.round(tmp * Math.sin(Math.toRadians(angle)) - halfHeight);
            child.layout((int) left, (int) top, (int) (left + cWidth), (int) (top + halfHeight * 2));
//            child.setX((int) left);
//            child.setY((int) top - offset);
            // 叠加尺寸
            angle += angleDelay;
            Log.d(TAG, "onLayout: angle:" + angle);
        }

        int cw = SIZE_CENTER;
        // 找到中心的view，如果存在设置onclick事件
        if (centerView != null) {
            centerView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnMenuItemClickListener != null) {
                        mOnMenuItemClickListener.itemCenterClick(mItemViews[cursor]);
                    }
                }
            });
            // 设置center item位置
            float cl = layoutRadius - centerView.getMeasuredWidth() / 2;
            float cr = cl + centerView.getMeasuredWidth();
            //centerView.layout((int) cl, (int) cl, (int) cr, (int) cr);
            centerView.layout((int) (w - SIZE_BKG / 2 - SIZE_CENTER / 2), SIZE_BKG / 2 - SIZE_CENTER / 2,
                    (int) (w - SIZE_BKG / 2 + SIZE_CENTER / 2), SIZE_BKG / 2 + SIZE_CENTER / 2);
        }

        if (background != null) {
            // 设置center item位置
//            background.layout(w / 2 - bw / 2, h / 2 - bh / 2, w / 2 + bw / 2, h / 2 + bh / 2);
            background.layout((int) (w - SIZE_BKG), 0, w, SIZE_BKG);
        }
        Log.d(TAG, "onLayout: w" + w + "  h:" + h + "  cw:" + cw + " bw:" + bw + "  layoutRadius:" + layoutRadius);
    }

    private int getMenuItemWidth() {
        //return (int) (layoutRadius * RADIO_DEFAULT_CHILD_DIMENSION);
        return (int) getResources().getDimension(R.dimen.width_menu_item);
    }

    private float getMenuItemHeight() {
        //return getTextSize() * 2.5f;//经验系数
        return (int) getResources().getDimension(R.dimen.height_menu_item);
    }

    public void setSeatDisable(boolean mbSeatDisable) {
        this.mbSeatDisable = mbSeatDisable;
        freshPanoItemVisibility();
    }

    public boolean isSeatDisable() {
        return mbSeatDisable;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        zoomItemView(cursor);
    }

    public void switchUp() {
        switchByDegree(mStepDegree);
    }

    private static final String TAG = "CircleMenuLayout";

    public boolean switchByDegree(float degree) {
        float v = mStartAngle + degree;
        if (v >= getMaxDegree()) {
            v = getMaxDegree();
        } else if (v < MIN_DEGREE) {
            v = MIN_DEGREE;
        }
        if (v == mStartAngle) {
            updateUi();
            return false;
        }
        if (mAnimSwitchMenu == null) {
            mAnimSwitchMenu = new ObjectAnimator();
        }
        if (mAnimSwitchMenu.isRunning()) {
            mAnimSwitchMenu.end();
        }
        mAnimSwitchMenu = new ObjectAnimator();
        initAnim();
        mAnimSwitchMenu.setObjectValues(mStartAngle, v);
        long duration = (long) (Math.abs(v - mStartAngle) * 8/*10*/);
        mAnimSwitchMenu.setDuration(duration);
        if (mStartAngle != v
                && ((int) mStartAngle == VIDEO_ANGLE || (int) v == VIDEO_ANGLE)) {
            if (mAnimUpdateListener != null) {
                mAnimSwitchMenu.removeAllUpdateListeners();
                mAnimSwitchMenu.addUpdateListener(mAnimUpdateListener);
            }
            if (mAnimListener != null) {
                mAnimSwitchMenu.removeAllListeners();
                mAnimSwitchMenu.addListener(mAnimListener);
            }
        }
        if (mOnSwitchListener != null) {
            EmCameraMode modeStart = getCameraMode(mStartAngle);
            EmCameraMode modeEnd = getCameraMode(v);
            mOnSwitchListener.onSwitchAnimateStart(modeStart, modeEnd, (int) duration);
        }

        Log.d(TAG, "switchByDegree: 动画时间：" + duration);
        //animator.ofFloat(mStartAngle, mStartAngle + mStepDegree * leval);
        mAnimSwitchMenu.start();

        return true;
    }

    public boolean isMenuAnimRunning() {
        boolean ret = mAnimSwitchMenu != null && mAnimSwitchMenu.isRunning();
        Log.d(TAG, "isMenuAnimRunning: " + ret);
        return ret;
    }

    private void initAnim() {
        ValueAnimator.setFrameDelay(25);
        mAnimSwitchMenu.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimSwitchMenu.removeAllUpdateListeners();
        mAnimSwitchMenu.addUpdateListener(mUpdateListener);
        mAnimSwitchMenu.removeAllListeners();
        mAnimSwitchMenu.addListener(mAnimatorListener);
    }

    private void updateUi() {
        updateColorAndSize();
        requestLayout();
    }


    public void switchDown() {
        switchByDegree(-mStepDegree);
    }


    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x001) {
                requestLayout();
            }
        }
    }

    private void zoomItemView(int cursor) {
        for (int i = 0; i < mMenuItemCount; i++) {
            if (i == cursor) {
                mItemViews[i].setTextColor(mContext.getResources().getColor(R.color.white_FFFFFF));
                mItemViews[i].animate().scaleX(1.25f).scaleY(1.25f).start();
            } else {
                mItemViews[i].animate().scaleX(1.0f).scaleY(1.0f).start();
                mItemViews[i].setTextColor(mContext.getResources().getColor(R.color.white_7FFFFFFF));
            }
        }
    }

    private void updateColorAndSize() {
        for (int i = 0; i < mMenuItemCount; i++) {
            float degree = mStartAngle + i * mStepDegree;
            float value = getValue(MIN_ALPHA_MENU, 1f, degree);
            mItemViews[i].setTextColor(Color.WHITE);
            mItemViews[i].setAlpha(value);
            float scale = getValue(1f, 1.25f, degree);
            mItemViews[i].setScaleX(scale);
            mItemViews[i].setScaleY(scale);
            mItemViews[i].invalidate();
        }
    }

    private float getValue(float min, float max, float degree) {
        float v = Math.abs(degree - 180) / mStepDegree;
        return min + (max - min) * (v > 1 ? 0 : (1 - v));
    }

    // MenuItem的点击事件接口
    public interface OnMenuItemClickListener {
        /**
         * @param view
         * @param pos
         * @return 点击是否有效
         */
        boolean itemClick(View view, int pos);

        void itemCenterClick(View view);
    }

    // 设置MenuItem的点击事件接口
    public void setOnMenuItemClickListener(OnMenuItemClickListener mOnMenuItemClickListener) {
        this.mOnMenuItemClickListener = mOnMenuItemClickListener;
    }

    public float getMaxDegree() {
        return MAX_DEGREE - (!mbSeatDisable ? 0 : mStepDegree);
    }

    public void hideSubItem() {
        for (int i = 0; i < mMenuItemCount; i++) {
            mItemViews[i].setEnabled(false);
            final int finalI = i;
            mItemViews[i].animate().alpha(0).setDuration(/*500*/0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mItemViews[finalI].setEnabled(false);
                    mItemViews[finalI].setVisibility(View.INVISIBLE);
                }
            }).start();

        }
    }

    public void showSubItem() {
        MenuView v = getCurrentCameraModeView();
        for (int i = 0; i < mMenuItemCount; i++) {
            final int finalI = i;
            mItemViews[i].animate().cancel();
            mItemViews[finalI].setVisibility(View.VISIBLE);
            mItemViews[i].animate().alpha(mItemViews[i] == v ? 1.0f : MIN_ALPHA_MENU)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mItemViews[finalI].setEnabled(true);
                            mItemViews[finalI].setVisibility(View.VISIBLE);
                            freshPanoItemVisibility();
                        }
                    }).setDuration(500).start();
        }
        mLastTimeShow = SystemClock.elapsedRealtime();
        freshPanoItemVisibility();
        invalidate();
    }

    private void freshPanoItemVisibility() {
        boolean bAllowVisible = !mbSeatDisable && mItemPic.getVisibility() == View.VISIBLE
                && getVisibility() == View.VISIBLE;
        mItemPano.setVisibility(bAllowVisible ? View.VISIBLE : View.INVISIBLE);
        mItemPano.setEnabled(bAllowVisible);
    }

    public void setCenterViewRes(int resId) {
        if (centerView != null) {
            centerView.setBackgroundResource(resId);
//            centerView.setImageResource(resId);
        }
    }

    public View getItemPic() {
        return mItemPic;
    }

    public boolean isPanoVisibile() {
        return mItemPano != null && mItemPano.getVisibility() == View.VISIBLE;
    }

    private static final int OFFSET = 800;  //每个动画的播放时间间隔

    public void startSoundWaves() {
        if (true) {
            startSoundWavesSingle();
        } else {
            startSoundWavesMulti();
        }
    }

    public void startSoundWavesSingle() {
        if (mWaves[0] == null) {
            for (int i = 0; i < mWaves.length; i++) {
                mWaves[i] = createWaveView();
            }
            mAnimationCenter = getAnimationCenter();
        }
        if (mAniWave == null) {
            initAnimateWave();
        }
        mAniWave.start();
        centerView.setAnimation(mAnimationCenter);
    }

    public void startSoundWavesMulti() {
        if (mWaves[0] == null) {
            for (int i = 0; i < mWaves.length; i++) {
                mWaves[i] = createWaveView();
                mAnimationSets[i] = getAnimationSet();
            }
            mAnimationCenter = getAnimationCenter();
        }

        for (int i = 0; i < mWaves.length; i++) {
            final int finalI = i;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWaves[finalI].setVisibility(View.VISIBLE);
                    mWaves[finalI].setAnimation(mAnimationSets[finalI]);
                }
            }, (long) (OFFSET * i + 50));
        }
        centerView.setAnimation(mAnimationCenter);
    }

    float maxDiffScale = 0.8f;

    private void initAnimateWave() {
        mAniWave = ValueAnimator.ofFloat(1.0f + maxDiffScale, 1.0f);
        mAniWave.setDuration(OFFSET * (CNT_ANI - 1));
        mAniWave.setRepeatCount(AnimationSet.INFINITE);
        mAniWave.setInterpolator(new LinearInterpolator());
        mAniWave.removeAllUpdateListeners();
        mAniWave.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            float diffScale = maxDiffScale / (CNT_ANI - 1);

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float v = (Float) animation.getAnimatedValue();
                //Log.d(TAG, "onAnimationUpdate: v:" + v);

                if (v >= 1.0 + maxDiffScale - 0.0005f) {
                    mWaves[0].setVisibility(View.VISIBLE);
                } else if (v >= 1.0 + maxDiffScale - diffScale - 0.0005f) {
                    mWaves[0 + 1].setVisibility(View.VISIBLE);
                } else if (v >= 1.0 + maxDiffScale - diffScale * 2 - 0.0005f) {
                    mWaves[0 + 2].setVisibility(View.VISIBLE);
                }

                float vTemp;
                for (int i = 0; i < mWaves.length; i++) {
                    vTemp = v + diffScale * i;
                    if (vTemp > maxDiffScale + 1.0f) {
                        vTemp -= maxDiffScale;
                    }
                    if (mWaves[i].getVisibility() == View.VISIBLE) {
                        mWaves[i].setAlpha(2.0f - vTemp);
                        mWaves[i].setScaleX(vTemp);
                        mWaves[i].setScaleY(vTemp);
                    }

                }
            }
        });
        mAniWave.removeAllListeners();
        mAniWave.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                for (View v : mWaves) {
                    if (v != null) {
                        //v.setVisibility(View.GONE);
                        v.animate().alpha(0).setDuration(500).start();
                        //v.setScaleX(1.8f);
                        //v.setScaleY(1.8f);
                    }
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                for (View v : mWaves) {
                    if (v != null) {
                        v.animate().cancel();
//                        v.setScaleX(1.8f);
//                        v.setScaleY(1.8f);
                    }
                }
            }
        });
    }

    public void stopSoundWaves() {
        if (mAniWave == null) {
            for (View v : mWaves) {
                if (v != null) {
                    v.clearAnimation();
                    v.setVisibility(View.GONE);
                }
            }
        }
        if (mAniWave != null) {
            mAniWave.cancel();
        }

        centerView.clearAnimation();
        //centerView.setScaleX(1.0f);
        //centerView.setScaleY(1.0f);;
        centerView.animate().cancel();
        centerView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).start();
    }

    private View createWaveView() {
        View v = new View(getContext());
        addView(v);
        v.layout(centerView.getLeft(), centerView.getTop(), centerView.getRight(), centerView.getBottom());
        v.setBackgroundResource(R.drawable.shape_circle);
        v.setVisibility(View.GONE);
        return v;
    }

    private AnimationSet getAnimationSet() {
        AnimationSet as = new AnimationSet(true);
        ScaleAnimation sa = new ScaleAnimation(ANIM_MAX_SCALE, 1.0f, ANIM_MAX_SCALE, 1.0f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(OFFSET * 3);
        sa.setRepeatCount(Animation.INFINITE);// 设置循环
        AlphaAnimation aa = new AlphaAnimation(0.1f, 1f);
        aa.setDuration(OFFSET * 3);
        aa.setRepeatCount(Animation.INFINITE);//设置循环
        as.addAnimation(sa);
        as.addAnimation(aa);
        return as;
    }


    private AnimationSet getAnimationCenter() {
        AnimationSet as = new AnimationSet(true);
        ScaleAnimation sa = new ScaleAnimation(1.05f, 0.95f, 0.95f, 1.05f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration((long) (OFFSET * 1.5));
        sa.setRepeatCount(Animation.INFINITE);// 设置循环
        sa.setRepeatMode(Animation.REVERSE);
        as.addAnimation(sa);
        return as;
    }

    public void setAnimUpdateListener(ValueAnimator.AnimatorUpdateListener animUpdateListener) {
        mAnimUpdateListener = animUpdateListener;
    }

    public void setAnimListener(ValueAnimator.AnimatorListener animListener) {
        mAnimListener = animListener;
    }

    private ValueAnimator.AnimatorUpdateListener mAnimUpdateListener;
    private ValueAnimator.AnimatorListener mAnimListener;
    private OnSwitchAnimateStartListener mOnSwitchListener;

    public OnSwitchAnimateStartListener getOnSwitchListener() {
        return mOnSwitchListener;
    }

    public void setOnSwitchListener(OnSwitchAnimateStartListener onSwitchListener) {
        mOnSwitchListener = onSwitchListener;
    }

    public interface OnSwitchAnimateStartListener {
        void onSwitchAnimateStart(EmCameraMode start, EmCameraMode end, int duration);
    }

    public float getDiffAngle(EmCameraMode start, EmCameraMode end) {
        if (start == null || end == null) {
            return 0;
        }
        return 30 * Math.abs(start.ordinal() - end.ordinal());
    }

    public EmCameraMode getCameraMode(float degree) {
        return EmCameraMode.getByIdx((int) ((degree - 90) / 30));
    }

    public MenuView getCurrentCameraModeView() {
        try {
            return mItemViews[mItemViews.length - 1 - ((int) ((mStartAngle - 90) / 30))];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void performClickCenterView() {
        if (centerView != null) {
            centerView.performClick();
        }
    }
}
