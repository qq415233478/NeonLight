package com.neonlight.demo;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

public class NeonLight extends View {

    /**
     * 前景色
     */
    public static final int COLOR_FOREGROUND = Color.argb(255, 0, 251, 251);
    /**
     * 背景色
     */
    public static final int COLOR_BACKGROUND = Color.argb(180, 31, 59, 251);
    /**
     * 错误提示颜色
     */
    public static final int COLOR_ERROR = Color.argb(200, 248, 162, 77);
    /**
     * 麦克风关闭提示颜色
     */
    public static final int COLOR_PRIVACY = Color.argb(200, 222, 38, 40);
    /**
     * 点的初始半径
     */
    private static int mPointRadius;
    private Paint mPaint;

    private float[] mEdgeStops;
    private Point mPoint1;
    private Point mPoint2;
    private Point mLeftPoint;
    private Point mRightPoint;
    private int mWidth;

    private State mCurrentState = State.IDLE;

    private State mLastState = State.IDLE;

    private boolean isPrivacy;

    private RadialGradient mLeftRadialGradient;

    private RadialGradient mRightRadialGradient;

    private ValueAnimator mCurrentAnimator;

    private ValueAnimator mRecoveryAnimator;

    private AnimationCallback mAnimationCallback;

    public NeonLight(Context context) {
        this(context, null, 0, 0);
    }

    public NeonLight(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public NeonLight(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NeonLight(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPointRadius = dp2px(context, 120);
        mPaint = new Paint();
        mEdgeStops = new float[] { 0.75f, 1.0f };
        mWidth = getScreenWidth(context);
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = getMeasuredWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isAnimationRunning()) {
            if (mCurrentState == State.START) {
                if (mLeftPoint != null && mRightPoint != null) {
                    mPaint.setShader(mLeftRadialGradient);
                    canvas.drawRect(0, 0, (mLeftPoint.getCenterX(mWidth) + mRightPoint.getCenterX(mWidth)) / 2,
                            getHeight(), mPaint);
                    mPaint.setShader(mRightRadialGradient);
                    canvas.drawRect((mLeftPoint.getCenterX(mWidth) + mRightPoint.getCenterX(mWidth)) / 2, 0, mWidth,
                            getHeight(), mPaint);
                }
            } else {
                mPaint.setShader(mLeftRadialGradient);
                canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
            }
        } else {
            if (isPrivacy) {
                canvas.drawColor(COLOR_PRIVACY);
            } else if (mCurrentState == State.ERROR) {
                canvas.drawColor(COLOR_ERROR);
            }
        }
        super.onDraw(canvas);
    }

    /**
     * 刷新view,会调用invalidate
     *
     * @auther qujq
     * @time 2017/3/10
     */
    private void refresh() {
        if (mCurrentState == State.START) {
            if (mPoint1 != null && mPoint2 != null) {
                if (mPoint1.getCenterX(mWidth) < mPoint2.getCenterX(mWidth)) {
                    mLeftPoint = mPoint1;
                    mRightPoint = mPoint2;
                } else {
                    mLeftPoint = mPoint2;
                    mRightPoint = mPoint1;
                }
                mLeftRadialGradient = new RadialGradient(mLeftPoint.getCenterX(mWidth), getHeight() / 2,
                        mLeftPoint.radius, mLeftPoint.colors, mEdgeStops, Shader.TileMode.CLAMP);

                mRightRadialGradient = new RadialGradient(mRightPoint.getCenterX(mWidth), getHeight() / 2,
                        mRightPoint.radius, mRightPoint.colors, mEdgeStops, Shader.TileMode.CLAMP);
            }
        } else {
            if (mPoint1 != null) {
                mLeftPoint = mPoint1;
                mLeftRadialGradient = new RadialGradient(mLeftPoint.getCenterX(mWidth), getHeight() / 2,
                        mLeftPoint.radius, mLeftPoint.colors, mEdgeStops, Shader.TileMode.CLAMP);
            }
        }
        invalidate();
    }

    /**
     * 判断当前是否在执行动画
     *
     * @return
     */
    public boolean isAnimationRunning() {
        if ((mCurrentAnimator != null && mCurrentAnimator.isRunning())
                || (mRecoveryAnimator != null && mRecoveryAnimator.isRunning())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置是否为Privacy
     *
     * @param isPrivacy
     */
    public void setPrivacy(boolean isPrivacy) {
        this.isPrivacy = isPrivacy;
        if (!isAnimationRunning()) {
            if (isPrivacy) {
                privacyAnimation();
            } else {
                invalidate();
            }
        }
    }

    /**
     * 判断是否为Privacy
     *
     * @return
     */
    public boolean isPrivacy() {
        return isPrivacy;
    }

    static class Point {
        private float pos;
        private float radius = mPointRadius;

        int[] colors = new int[] { COLOR_FOREGROUND, COLOR_BACKGROUND };

        public Point(float pos) {
            this.pos = pos;
        }

        public float getCenterX(float width) {
            return width * pos;
        }

        @Override
        public String toString() {
            return "[Point]{ pos=" + pos + ",radius=" + radius + " }";
        }

    }

    /**
     * 清除动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void clearAnimation() {
        if (mRecoveryAnimator != null) {
            if (mRecoveryAnimator.isRunning()) {
                mRecoveryAnimator.cancel();
            }
            mRecoveryAnimator.removeAllUpdateListeners();
            mRecoveryAnimator.removeAllListeners();
        }
        if (mCurrentAnimator != null) {
            if (mCurrentAnimator.isRunning()) {
                mCurrentAnimator.cancel();
            }
            mCurrentAnimator.removeAllUpdateListeners();
            mCurrentAnimator.removeAllListeners();
        }
        mLeftPoint = null;
        mRightPoint = null;
        mPoint1 = null;
        mPoint2 = null;
    }

    /**
     * 动画恢复到IDEL状态
     *
     * @auther qujq
     * @time 2017/3/24
     */
    public void recoveryAnimation() {
        mCurrentAnimator = ValueAnimator.ofInt(255);
        mCurrentAnimator.setDuration(200);
        mCurrentAnimator.setInterpolator(new AccelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.IDLE) {
                    int val = (Integer) animation.getAnimatedValue();
                    if (mPoint1 != null) {
                        mPoint1.colors[0] = changeAlpha(mPoint1.colors[0], 255 - val);
                        mPoint1.colors[1] = changeAlpha(mPoint1.colors[1], 255 - val);
                    }
                    if (mPoint2 != null) {
                        mPoint2.colors[0] = changeAlpha(mPoint2.colors[0], 255 - val);
                        mPoint2.colors[1] = changeAlpha(mPoint2.colors[1], 255 - val);
                    }
                    refresh();
                }
            }
        });
        mCurrentAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (mCurrentState == State.IDLE && isPrivacy) {
                    privacyAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        if (mCurrentState == State.IDLE) {
            mCurrentAnimator.start();
        }
    }

    /**
     * 启动的动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void startAnimation() {
        clearAnimation();
        mPoint1 = new Point(0);
        mPoint2 = new Point(1);
        mCurrentAnimator = ValueAnimator.ofFloat(0.5f);
        mCurrentAnimator.setDuration(1000);
        mCurrentAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.START || mCurrentState == State.IDLE && mPoint1 != null && mPoint2 != null) {
                    float val = (Float) animation.getAnimatedValue();
                    mPoint1.pos = val;
                    mPoint2.pos = 1 - val;
                    refresh();
                }
            }
        });
        mCurrentAnimator.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimationCallback != null) {
                    mAnimationCallback.onStartAnimationEnd();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        if (mCurrentState == State.START) {
            mCurrentAnimator.start();
        }
    }

    /**
     * listening的动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void listeningAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mCurrentAnimator = ValueAnimator.ofFloat(1.0f, 0.7f, 1.0f, 0.7f, 1.1f, 0.6f, 0.8f, 1.0f, 0.7f, 1.0f);
        mCurrentAnimator.setDuration(1200);
        mCurrentAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mCurrentAnimator.setInterpolator(new AccelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.LISTENING || mCurrentState == State.IDLE && mPoint1 != null) {
                    float f = (Float) animation.getAnimatedValue();
                    mPoint1.radius = mPointRadius * f;
                    refresh();
                }
            }
        });
        mCurrentAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                if (mCurrentState == State.IDLE) {
                    mCurrentAnimator.cancel();
                    recoveryAnimation();
                }
            }
        });
        if (mCurrentState == State.LISTENING) {
            mCurrentAnimator.start();
        }

    }

    /**
     * thinking的动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void thinkingAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mCurrentAnimator = ValueAnimator.ofInt(mWidth);
        mCurrentAnimator.setDuration(700);
        mCurrentAnimator.setInterpolator(new AccelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.THINKING || mCurrentState == State.IDLE && mPoint1 != null) {
                    int val = (Integer) animation.getAnimatedValue();
                    mPoint1.radius = val + 100;
                    int alpha = (int) (255.0 * val / mWidth) + 100;
                    if (alpha > 255) {
                        alpha = 255;
                    }
                    mPoint1.colors[0] = changeAlpha(COLOR_FOREGROUND, alpha);
                    refresh();
                }
            }
        });

        mCurrentAnimator.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCurrentState == State.IDLE) {
                    recoveryAnimation();
                } else if (mCurrentState == State.THINKING) {
                    recoveryThinkingAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        if (mCurrentState == State.THINKING || mCurrentState == State.IDLE) {
            mCurrentAnimator.start();
        }

    }

    /**
     * thinking的恢复动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void recoveryThinkingAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mPoint1.radius = mWidth;
        mRecoveryAnimator = ValueAnimator.ofArgb(NeonLight.COLOR_FOREGROUND, NeonLight.COLOR_BACKGROUND);
        mRecoveryAnimator.setDuration(200);
        mRecoveryAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.THINKING || mCurrentState == State.IDLE && mPoint1 != null) {
                    int color = (Integer) animation.getAnimatedValue();
                    mPoint1.colors[0] = color;
                    refresh();
                }
            }
        });
        mRecoveryAnimator.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                thinkingAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        if (mCurrentState == State.THINKING || mCurrentState == State.IDLE) {
            mRecoveryAnimator.start();
        }

    }

    /**
     * speaking的动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void speakingAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mPoint1.radius = mWidth;
        mCurrentAnimator = ValueAnimator.ofArgb(NeonLight.COLOR_BACKGROUND, NeonLight.COLOR_FOREGROUND);
        mCurrentAnimator.setDuration(700);
        mCurrentAnimator.setInterpolator(new AccelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.SPEAKING || mCurrentState == State.IDLE && mPoint1 != null) {
                    int color = (Integer) animation.getAnimatedValue();
                    mPoint1.colors[0] = color;
                    refresh();
                }
            }
        });

        mCurrentAnimator.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCurrentState == State.IDLE) {
                    recoveryAnimation();
                } else if (mCurrentState == State.SPEAKING) {
                    recoverySpeakingAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        if (mCurrentState == State.SPEAKING || mCurrentState == State.IDLE) {
            mCurrentAnimator.start();
        }
    }

    /**
     * speaking的恢复动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void recoverySpeakingAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mPoint1.radius = mWidth;
        mRecoveryAnimator = ValueAnimator.ofArgb(NeonLight.COLOR_FOREGROUND, NeonLight.COLOR_BACKGROUND);
        mRecoveryAnimator.setDuration(700);
        mRecoveryAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.SPEAKING || mCurrentState == State.IDLE && mPoint1 != null) {
                    int color = (Integer) animation.getAnimatedValue();
                    mPoint1.colors[0] = color;
                    refresh();
                }
            }
        });
        mRecoveryAnimator.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                speakingAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        if (mCurrentState == State.SPEAKING || mCurrentState == State.IDLE) {
            mRecoveryAnimator.start();
        }
    }

    /**
     * 麦克风关闭提示动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void privacyAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mPoint1.radius = mWidth;
        mCurrentAnimator = ValueAnimator.ofInt(255);
        mCurrentAnimator.setDuration(300);
        mCurrentAnimator.setInterpolator(new AccelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (isPrivacy || mCurrentState == State.IDLE && mPoint1 != null) {
                    int alpha = (Integer) animation.getAnimatedValue();
                    mPoint1.colors[0] = changeAlpha(COLOR_PRIVACY, alpha);
                    refresh();
                }
            }
        });
        if (isPrivacy) {
            mCurrentAnimator.start();
        }
    }

    /**
     * 错误提示动画
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void errorAnimation() {
        clearAnimation();
        mPoint1 = new Point(0.5f);
        mPoint1.radius = mWidth;
        mCurrentAnimator = ValueAnimator.ofInt(255);
        mCurrentAnimator.setDuration(300);
        mCurrentAnimator.setInterpolator(new AccelerateInterpolator());
        mCurrentAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCurrentState == State.ERROR || mCurrentState == State.IDLE && mPoint1 != null) {
                    int alpha = (Integer) animation.getAnimatedValue();
                    mPoint1.colors[0] = changeAlpha(COLOR_ERROR, alpha);
                    refresh();
                }
            }
        });
        if (mCurrentState == State.ERROR) {
            mCurrentAnimator.start();
        }
    }

    /**
     * 更新状态方法
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public void setState(State state) {
        if (mCurrentState == state) {
            return;
        }
        this.mLastState = this.mCurrentState;
        this.mCurrentState = state;
        switch (mCurrentState) {
        case IDLE:
            if (mLastState == State.START || mLastState == State.ERROR) {
                recoveryAnimation();
            }
            break;
        case START:
            startAnimation();
            break;
        case LISTENING:
            listeningAnimation();
            break;
        case THINKING:
            thinkingAnimation();
            break;
        case SPEAKING:
            speakingAnimation();
            break;
        case ERROR:
            errorAnimation();
            break;
        }
    }

    /**
     * 修改颜色透明度
     *
     * @auther qujq
     * @time 2017/3/10
     */
    public int changeAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * dp转px
     *
     * @auther qujq
     * @time 2017/3/14
     */
    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 获取屏幕宽度
     *
     * @auther qujq
     * @time 2017/3/14
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    public void setAnimationCallback(AnimationCallback callback) {
        mAnimationCallback = callback;
    }

    public interface AnimationCallback {
        void onStartAnimationEnd();
    }
}
