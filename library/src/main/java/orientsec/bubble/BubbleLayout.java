package orientsec.bubble;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

import orientsec.bubble.library.R;

import static orientsec.bubble.Callback.DISMISS_EVENT_CONSECUTIVE;


public class BubbleLayout extends FrameLayout {
    private static final int ANIMATION_DURATION = 250;

    private ViewGroup mTargetParent;

    private static final Handler sHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MSG_SHOW:
                    ((BubbleLayout) message.obj).showView();
                    return true;
                case MSG_DISMISS:
                    int event = message.arg1;
                    BubbleLayout bubbleLayout = (BubbleLayout) message.obj;
                    if (event == DISMISS_EVENT_CONSECUTIVE) {
                        bubbleLayout.setVisibility(View.GONE);
                    }
                    bubbleLayout.hideView(event);
                    return true;
            }
            return false;
        }
    });
    private static final int MSG_SHOW = 0;
    private static final int MSG_DISMISS = 1;

    // On JB/KK versions of the platform sometimes View.setTranslationY does not
    // result in layout / draw pass, and CoordinatorLayout relies on a draw pass to
    // happen to sync vertical positioning of all its child views
    private static final boolean USE_OFFSET_API = (Build.VERSION.SDK_INT >= 16)
            && (Build.VERSION.SDK_INT <= 19);

    private int mDuration;

    private List<Callback> mCallbacks;

    private final AccessibilityManager mAccessibilityManager;

    private final BubbleManager.Callback mManagerCallback = new BubbleManager.Callback() {
        @Override
        public void show() {
            sHandler.sendMessage(sHandler.obtainMessage(MSG_SHOW, BubbleLayout.this));
        }

        @Override
        public void dismiss(int event) {
            sHandler.sendMessage(sHandler.obtainMessage(MSG_DISMISS, event, 0,
                    BubbleLayout.this));
        }
    };

    /**
     * Returns whether this {@link BubbleLayout} is currently being shown, or is queued
     * to be shown next.
     */
    private boolean isShownOrQueued() {
        return BubbleManager.getInstance().isCurrentOrNext(mManagerCallback);
    }

    public BubbleLayout(Context context) {
        this(context, null);
    }

    public BubbleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BubbleLayout);
        if (a.hasValue(R.styleable.BubbleLayout_elevation)) {
            ViewCompat.setElevation(this, a.getDimensionPixelSize(
                    R.styleable.BubbleLayout_elevation, 0));
        }
        a.recycle();

        setClickable(true);
        ViewCompat.setAccessibilityLiveRegion(this,
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        ViewCompat.setImportantForAccessibility(this,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        mAccessibilityManager = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mScaledTouchSlop = configuration.getScaledTouchSlop();
        mScaledMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity() * 3;
        mScaledMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();

        mScroller = new OverScroller(getContext());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (shouldAnimate()) {
            // If animations are enabled, animate it in
            animateViewIn();
        } else {
            // Else if anims are disabled just call back now
            onViewShown();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewCompat.requestApplyInsets(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isShownOrQueued()) {
            // If we haven't already been dismissed then this event is coming from a
            // non-user initiated action. Hence we need to make sure that we callback
            // and keep our state up to date. We need to post the call since
            // removeView() will call through to onDetachedFromWindow and thus overflow.
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    onViewHidden(Callback.DISMISS_EVENT_MANUAL);
                }
            });
        }
    }

    private void onViewHidden(int event) {
        // First tell the BubbleManager that it has been dismissed
        BubbleManager.getInstance().onDismissed(mManagerCallback);
        if (mCallbacks != null) {
            // Notify the callbacks. Do that from the end of the list so that if a callback
            // removes itself as the result of being called, it won't mess up with our iteration
            int callbackCount = mCallbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                mCallbacks.get(i).onDismissed(event);
            }
        }
        // Lastly, hide and remove the view from the parent (if attached)
        final ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(this);
        }
    }

    private void showView() {
        if (getParent() == null) {
            mTargetParent.addView(this);
        }

        if (ViewCompat.isLaidOut(this)) {
            if (shouldAnimate()) {
                // If animations are enabled, animate it in
                animateViewIn();
            } else {
                // Else if anims are disabled just call back now
                onViewShown();
            }
        }
    }

    private void animateViewIn() {
        final int viewHeight = getHeight();
        if (USE_OFFSET_API) {
            ViewCompat.offsetTopAndBottom(this, -viewHeight);
        } else {
            setTranslationY(-viewHeight);
        }
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(-viewHeight, 0);
        animator.setInterpolator(Constant.FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(ANIMATION_DURATION);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                onViewShown();
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mPreviousAnimatedIntValue = viewHeight;

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int currentAnimatedIntValue = (int) animator.getAnimatedValue();
                if (USE_OFFSET_API) {
                    ViewCompat.offsetTopAndBottom(BubbleLayout.this,
                            currentAnimatedIntValue - mPreviousAnimatedIntValue);
                } else {
                    setTranslationY(currentAnimatedIntValue);
                }
                mPreviousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();
    }

    private void animateViewOut(final int event) {
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(0, -getHeight());
        animator.setInterpolator(Constant.FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(ANIMATION_DURATION);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                onViewHidden(event);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mPreviousAnimatedIntValue = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int currentAnimatedIntValue = (int) animator.getAnimatedValue();
                if (USE_OFFSET_API) {
                    ViewCompat.offsetTopAndBottom(BubbleLayout.this,
                            currentAnimatedIntValue - mPreviousAnimatedIntValue);
                } else {
                    setTranslationY(currentAnimatedIntValue);
                }
                mPreviousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();
    }

    private void hideView(@Callback.DismissEvent final int event) {
        if (shouldAnimate() && getVisibility() == View.VISIBLE) {
            animateViewOut(event);
        } else {
            // If anims are disabled or the view isn't visible, just call back now
            onViewHidden(event);
        }
    }

    private void onViewShown() {
        BubbleManager.getInstance().onShown(mManagerCallback);
        if (mCallbacks != null) {
            // Notify the callbacks. Do that from the end of the list so that if a callback
            // removes itself as the result of being called, it won't mess up with our iteration
            int callbackCount = mCallbacks.size();
            for (int i = callbackCount - 1; i >= 0; i--) {
                mCallbacks.get(i).onShown();
            }
        }
    }

    /**
     * Returns true if we should animate the Bubble view in/out.
     */
    private boolean shouldAnimate() {
        return !mAccessibilityManager.isEnabled();
    }

    /**
     * Adds the specified callback to the list of callbacks that will be notified of transient
     * bottom bar events.
     *
     * @param callback Callback to notify when transient bottom bar events occur.
     * @see #removeCallback(Callback)
     */
    public void addCallback(@NonNull Callback callback) {
        if (mCallbacks == null) {
            mCallbacks = new ArrayList<>();
        }
        mCallbacks.add(callback);
    }

    /**
     * Removes the specified callback from the list of callbacks that will be notified of transient
     * bottom bar events.
     *
     * @param callback Callback to remove from being notified of transient bottom bar events
     * @see #addCallback(Callback)
     */
    void removeCallback(@NonNull Callback callback) {
        if (mCallbacks == null) {
            // This can happen if this method is called before the first call to addCallback
            return;
        }
        mCallbacks.remove(callback);
    }

    /**
     * Show the {@link BubbleLayout}.
     */
    void show(@NonNull ViewGroup parent) {
        this.mTargetParent = parent;
        BubbleManager.getInstance().show(mDuration, mManagerCallback);
    }

    /**
     * Dismiss the {@link BubbleLayout}.
     */
    void dismiss() {
        dispatchDismiss(Callback.DISMISS_EVENT_MANUAL);
    }

    void dispatchDismiss(@Callback.DismissEvent int event) {
        BubbleManager.getInstance().dismiss(mManagerCallback, event);
    }

    /**
     * Set how long to show the view for.
     *
     * @param duration either be one of the predefined lengths:
     *                 {@link Constant#LENGTH_SHORT}, {@link Constant#LENGTH_LONG}, or a custom duration
     *                 in milliseconds.
     */
    public void setDuration(@Constant.Duration int duration) {
        mDuration = duration;
    }

    /**
     * Return the duration.
     *
     * @see #setDuration
     */
    @Constant.Duration
    public int getDuration() {
        return mDuration;
    }

    private int mScaledTouchSlop;
    private int mLastX;
    private int mLastY;
    private int mDownX;
    private int mDownY;
    private boolean mDragging;
    private int mScaledMinimumFlingVelocity;
    private int mScaledMaximumFlingVelocity;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = mLastX = (int) ev.getX() + (int) getTranslationX();
                mDownY = (int) ev.getY();
                BubbleManager.getInstance().pauseTimeout(mManagerCallback);
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                int disX = (int) (ev.getX() + (int) getTranslationX() - mDownX);
                int disY = (int) (ev.getY() - mDownY);
                return Math.abs(disX) > mScaledTouchSlop && Math.abs(disX) > Math.abs(disY);
            }
            case MotionEvent.ACTION_UP: {
                BubbleManager.getInstance().restoreTimeoutIfPaused(mManagerCallback);
                return false;
            }
            case MotionEvent.ACTION_CANCEL: {
                BubbleManager.getInstance().restoreTimeoutIfPaused(mManagerCallback);
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(ev);
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastX = (int) ev.getX() + (int) getTranslationX();
                mLastY = (int) ev.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int translationX = (int) getTranslationX();
                int disX = (int) (ev.getX() + translationX - mLastX);
                int disY = (int) (ev.getY() - mLastY);
                if (!mDragging && Math.abs(disX) > mScaledTouchSlop && Math.abs(disX) > Math.abs(disY)) {
                    mDragging = true;
                }
                if (mDragging) {
                    //scrollBy(disX, 0);
                    setTranslationX(translationX + disX);
                    mLastX = (int) ev.getX() + translationX;
                    mLastY = (int) ev.getY();
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
                if (mDragging) {
                    mDragging = false;
                    int velocityX = (int) mVelocityTracker.getXVelocity();
                    int velocity = Math.abs(velocityX);
                    if (velocity > mScaledMinimumFlingVelocity) {
                        animateViewOutSwipe(Callback.DISMISS_EVENT_MANUAL);
                    } else {
                        judgeResetClose();
                    }
                } else {
                    int velocityY = (int) mVelocityTracker.getYVelocity();
                    if (velocityY < -mScaledMinimumFlingVelocity) {
                        animateViewOut(Callback.DISMISS_EVENT_MANUAL);
                    }
                }
                mVelocityTracker.clear();
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                if (Math.abs(mDownX - ev.getX() - (int) getTranslationX()) > mScaledTouchSlop
                        || Math.abs(mDownY - ev.getY()) > mScaledTouchSlop) {
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    BubbleManager.getInstance().restoreTimeoutIfPaused(mManagerCallback);
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mDragging = false;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                } else {
                    judgeResetClose();
                }
                break;
            }
        }
        return super.onTouchEvent(ev);
    }

    private void judgeResetClose() {
        float mOpenPercent = 0.5f;
        if (Math.abs(getTranslationX()) > getWidth() * mOpenPercent) {
            animateViewOutSwipe(MotionEvent.ACTION_CANCEL);
        } else {
            animateViewReset();
        }
    }

    private void animateViewReset() {
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues((int) getTranslationX(), 0);
        animator.setInterpolator(Constant.FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mPreviousAnimatedIntValue = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int currentAnimatedIntValue = (int) animator.getAnimatedValue();
                if (USE_OFFSET_API) {
                    ViewCompat.offsetLeftAndRight(BubbleLayout.this,
                            currentAnimatedIntValue - mPreviousAnimatedIntValue);
                } else {
                    setTranslationX(currentAnimatedIntValue);
                }
                mPreviousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();
    }

    private void animateViewOutSwipe(final int event) {
        setClickable(false);
        final ValueAnimator animator = new ValueAnimator();
        if (getTranslationX() > 0) {
            animator.setIntValues((int) getTranslationX(), getWidth());
        } else {
            animator.setIntValues((int) getTranslationX(), -getWidth());
        }
        animator.setInterpolator(Constant.FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(ANIMATION_DURATION);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                onViewHidden(event);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mPreviousAnimatedIntValue = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int currentAnimatedIntValue = (int) animator.getAnimatedValue();
                if (USE_OFFSET_API) {
                    ViewCompat.offsetLeftAndRight(BubbleLayout.this,
                            currentAnimatedIntValue - mPreviousAnimatedIntValue);
                } else {
                    setTranslationX(currentAnimatedIntValue);
                }
                mPreviousAnimatedIntValue = currentAnimatedIntValue;
            }
        });
        animator.start();
    }

}