package com.putao.ptx.camera.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.putao.ptx.camera.R;
import com.putao.ptx.camera.model.FaceModel;
import com.putao.ptx.camera.util.BitmapUtil;
import com.sunnybear.library.util.DensityUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/3/18.
 */
public class CameraDrawView extends SurfaceView implements SurfaceHolder.Callback {
    public static final float COE_OFFSET = 0.125f;
    private final String TAG = CameraDrawView.class.getSimpleName();
    private Context mContext;
    private SurfaceHolder mSurfaceHolder;
    private Paint paint;
    private List<FaceModel> currentFace;
    private Bitmap sadBmp;
    private Bitmap happyBmp;
    private Bitmap noFaceBmp;
    private RectF centerRectF;
    private List<FaceModel> mFacesDraw;
    //private boolean mbShowSmile;
    private OnIsAllowDrawSmileListener mOnIsAllowDrawSmileListener;
    private FaceModel mFaceMostSmile;
    private boolean mbPause = false;
    private boolean mbStopThread = false;
    //private boolean mbAllowDraw = true;
    private OnIsAllowDrawListener mOnIsAllowDrawListener;
    private VectorDrawable mSvgSmileNo;
    private VectorDrawable mSvgSmileYes;


    public CameraDrawView(Context context) {
        super(context);
        this.mContext = context;
        initView();
    }

    public CameraDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView();
    }

    public CameraDrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView();
    }

    private void initView() {
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);

        paint = new Paint();
        paint.setStrokeWidth(4);
        paint.setStrokeWidth(2/*4*/);
        //paint.setColor(getResources().getColor(R.color.yellow_EEE31A));
        paint.setColor(getResources().getColor(R.color.face_detector));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        updateCenterRectF();
    }

    private void updateCenterRectF() {
        int centerX = DensityUtil.getDeviceWidth(mContext) / 2;
        int centerY = DensityUtil.getDeviceHeight(mContext) / 2;
        float left = centerX - 150;
        float top = centerY - 150;
        float right = centerX + 150;
        float bottom = centerY + 150;
        centerRectF = new RectF(left, top, right, bottom);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!drawThread.isAlive()) {
            try {
                drawThread.setDaemon(true);
                drawThread.setName("———thread drawing smile faces———");//绘制笑脸框的线程
                drawThread.start();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "surfaceCreated: " + e);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateCenterRectF();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


    Thread drawThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!isStopThread()) {
                if (!isAttachedToWindow()) {
                    Log.d(TAG, "run: isAttachedToWindow:false");
                    break;
                }
                if (!isPause()) {
                    draw(currentFace);
                }
                if (currentFace != null && currentFace.size() > 0) {
                    Log.d(TAG, "run: currentFace.size()=" + currentFace.size() + "  isShowSmile:" + isShowSmile());
                    synchronized (CameraDrawView.this) {
                        try {
                            CameraDrawView.this.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    draw(currentFace);
                    synchronized (CameraDrawView.this) {
                        try {
                            CameraDrawView.this.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.d(TAG, "run: exit thread" + TAG);
        }
    });


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private synchronized void draw(List<FaceModel> faces) {
        if (!isAttachedToWindow()) {
            return;
        }
        paint.setAlpha((int) (getAlpha() * 255));
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();//出现过崩溃
            if (canvas == null) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "draw: " + e);
            return;
        }

        try {
            if (!isAllowDraw()) {//不允许绘制就清空
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                return;
            }
            if (canvas == null) {
                return;
            }
            if (mFacesDraw == null) {
                mFacesDraw = new ArrayList<>();
            }
            mFacesDraw.clear();
            if (faces != null) {
                mFacesDraw.addAll(faces);
            }
            //Log.d(TAG, "draw: mFacesDraw.size() =  " + mFacesDraw.size());
            boolean bShowSmile = isShowSmile();
            if (mFacesDraw.size() == 0 || faces == null || faces.size() == 0) {
                if (bShowSmile) {
                    drawNoFace(canvas);
                } else {
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                }
            } else {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                if (faces != null && faces.size() > 0) {
                    if (bShowSmile) {
                        mFaceMostSmile = FaceModel.getSmilest(mFacesDraw);
                    } else {
                        mFaceMostSmile = null;
                    }
                    for (FaceModel f : mFacesDraw) {
                        drawFace(f, canvas, mFaceMostSmile);
                    }
                    Log.d(TAG, "draw: bShowSmile:" + bShowSmile + "  mFacesDraw.size:" + mFacesDraw.size());
                }
            }
            mFacesDraw.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "draw: " + e);
            }
        }
    }

    private void drawFace(FaceModel faceModel, Canvas canvas, FaceModel faceMostSmile) {
        //canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (faceModel != null) {

            //测试用
//                for (int i = 0; i < faceModel.landmarks.length / 2; i++) {
//                    canvas.drawPoint(faceModel.landmarks[i * 2], faceModel.landmarks[i * 2 + 1], paint);
//                    canvas.drawText(i + "", faceModel.landmarks[i * 2] + 5, faceModel.landmarks[i * 2 + 1] + 5, paint);
//                }

            //canvas.drawRect(faceModel.rectf, paint);//绘制笑脸的方框
            RectF drawRectf = new RectF(faceModel.rectf);
            drawRectf.offset(0, -faceModel.rectf.height() * COE_OFFSET);
            if (isAllowDraw() && !isShowSmile()/*faceModel != faceMostSmile*/) {
                canvas.drawRoundRect(drawRectf, 8, 8, paint);//绘制笑脸的方框
                return;//方框与圆脸只绘制一样
            }
            if (!isShowSmile() || !faceModel.isSmile() && faceModel != faceMostSmile) {
                return;
            }
            RectF smailRectF = new RectF();
            RectF sadRectF = new RectF();
            float width = Math.abs(faceModel.landmarks[31 * 2] - faceModel.landmarks[37 * 2]);
            float centerX = Math.min(faceModel.landmarks[31 * 2], faceModel.landmarks[37 * 2]) + width / 2f;
            float centerY = (faceModel.landmarks[31 * 2 + 1] + faceModel.landmarks[37 * 2 + 1]) / 2f;

            float left = centerX - width / 2f;
            float right = centerX + width / 2f;
            float top = centerY - width / 2f;
            float bottom = centerY + width / 2f;
            smailRectF.set(left, top - width / 2f, right, bottom - width / 2f);

            sadRectF.set(left, top, right, bottom);

            if (sadBmp == null) {
                sadBmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_smile_sad);
            }
            if (happyBmp == null) {
                happyBmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_smile_happy);
            }

            RectF smile = new RectF();
            smile.left = drawRectf.centerX() - drawRectf.width() / 3;
            smile.right = drawRectf.centerX() + drawRectf.width() / 3;
            smile.top = drawRectf.centerY() - drawRectf.height() / 3;
            smile.bottom = drawRectf.centerY() + drawRectf.height() / 3;
            if (isAllowDraw()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {//老的方式
                    canvas.drawOval(drawRectf, paint);
                    int offset = 8;
                    if (faceModel.isSmile()) {
                        //canvas.drawBitmap(happyBmp, null, drawRectf, paint);
                        canvas.drawArc(smile, 30 + offset, 120 - offset * 2, false, paint);
                    } else if (faceModel == faceMostSmile) {
                        //canvas.drawBitmap(sadBmp, null, drawRectf, paint);
                        smile.offset(0, smile.height() / 2 * 1.35f);
                        canvas.drawArc(smile, 210 + offset, 120 - offset * 2, false, paint);
                    }
                } else {
                    if (mSvgSmileNo == null || mSvgSmileYes == null) {
                        mSvgSmileNo = (VectorDrawable) getResources()
                                .getDrawable(R.drawable.svg_smile_no, null);
                        mSvgSmileYes = (VectorDrawable) getResources()
                                .getDrawable(R.drawable.svg_smile_yes, null);
                    }

                    Bitmap bm = BitmapUtil.getBitmap(
                            faceModel.isSmile() ? mSvgSmileYes : mSvgSmileNo,
                            (int) drawRectf.width(), (int) drawRectf.height());
                    canvas.drawBitmap(bm, new Rect(0, 0,
                            (int) drawRectf.width(), (int) drawRectf.height()), drawRectf, paint);
                }
            }
        } else {
            drawNoFace(canvas);
        }
    }

    private void drawNoFace(Canvas canvas) {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (noFaceBmp == null) {
            noFaceBmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_searing_face);
        }
        //没有检测到笑脸时，去掉问号。
        //canvas.drawBitmap(noFaceBmp, null, centerRectF, paint);
    }

    public void setData(List<FaceModel> faceModel) {
        long tm = SystemClock.elapsedRealtime();
        currentFace = faceModel;
        if (currentFace == null || currentFace.size() == 0) {
            //draw(currentFace);
        }
        synchronized (CameraDrawView.this) {
            CameraDrawView.this.notifyAll();
        }
        long waste = SystemClock.elapsedRealtime() - tm;
        if (waste > 1000) {
            Log.d(TAG, "setData: waste time:" + waste);
        }
    }

    public void clearData() {
        if (currentFace != null) {
            currentFace.clear();
        }
        setData(currentFace);
    }

    public void freshSmileState(/*boolean bShow*/) {
        //mbShowSmile = bShow;
        //draw(currentFace);
        boolean mbShowSmile = isShowSmile();
        if (!mbShowSmile) {
            synchronized (CameraDrawView.this) {
                CameraDrawView.this.notifyAll();
            }
        }
        Log.d(TAG, "setIsShowSmile: mbShowSmile:" + mbShowSmile);
    }

    public boolean isShowSmile() {
        //return mbShowSmile;
        return mOnIsAllowDrawSmileListener != null
                && mOnIsAllowDrawSmileListener.isAllowDrawSmile();
    }

    public boolean isPause() {
        return mbPause;
    }

    public void onPause() {
        this.mbPause = true;
    }

    public void onResume() {
        this.mbPause = false;
    }


    /**
     * 涉及到多线程，每次绘制都需要此判断
     *
     * @return
     */
    public boolean isAllowDraw() {
        //return mbAllowDraw;
        return mOnIsAllowDrawListener == null
                || mOnIsAllowDrawListener.isAllowDraw();
    }

//    public void setAllowDraw(boolean bAllowDraw) {
//        this.mbAllowDraw = bAllowDraw;
//        if (!mbAllowDraw) {
//            clearData();
//        }
//    }

    public void setOnIsAllowDrawListener(OnIsAllowDrawListener onIsAllowDrawListener) {
        mOnIsAllowDrawListener = onIsAllowDrawListener;
    }

    public void setOnIsAllowDrawSmileListener(OnIsAllowDrawSmileListener onIsAllowDrawSmileListener) {
        mOnIsAllowDrawSmileListener = onIsAllowDrawSmileListener;
    }

    public interface OnIsAllowDrawListener {
        boolean isAllowDraw();
    }

    public interface OnIsAllowDrawSmileListener {
        boolean isAllowDrawSmile();
    }

    public boolean isStopThread() {
        return mbStopThread;
    }

    public void setStopThread(boolean mbStop) {
        this.mbStopThread = mbStop;
    }
}
