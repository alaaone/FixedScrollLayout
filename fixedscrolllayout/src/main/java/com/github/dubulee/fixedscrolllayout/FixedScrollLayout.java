package com.github.dubulee.fixedscrolllayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
/**
 * FixedScrollLayout.java
 * Created by mugku on 2015/12/28.
 * Copyright (c) mugku. All rights reserved.
 */
public class FixedScrollLayout extends FrameLayout {

    private static final long DEFAULT_IDLE_CLOSE_UP_ANIMATION = 200L;
    private static final int DEFAULT_CONSIDER_IDLE_MILLIS = 100;

    private FixedScrollScroller mScroller;
    private GestureDetector mScrollDetector;
    private GestureDetector mFlingDetector;

    private CanScrollVerticallyDelegate mCanScrollVerticallyDelegate;
    private OnScrollChangedListener mOnScrollChangedListener;

    private int mMaxScrollY;

    private boolean mIsScrolling;
    private boolean mIsFlinging;

    private MotionEventHook mMotionEventHook;

    private CloseUpLogic mCloseUpLogic;
    private ObjectAnimator mCloseUpAnimator;

    private boolean mSelfUpdateScroll;
    private boolean mSelfUpdateFling;

    private boolean mIsTouchOngoing;

    private CloseUpIdleAnimationTime mCloseUpIdleAnimationTime;
    private CloseUpAnimatorConfigurator mCloseAnimatorConfigurator;

    private View mDraggableView;
    private boolean mIsDraggingDraggable;
    private final Rect mDraggableRect;
    {
        mDraggableRect = new Rect();
    }

    private long mConsiderIdleMillis;

    public FixedScrollLayout(Context context) {
        super(context);
        init(context, null);
    }

    public FixedScrollLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FixedScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {

        final TypedArray array = context.obtainStyledAttributes(attributeSet, R.styleable.FixedScrollLayout);
        try {

            final boolean flyWheel = array.getBoolean(R.styleable.FixedScrollLayout_scrollable_scrollerFlywheel, false);
            mScroller = initScroller(context, null, flyWheel);

            final float friction = array.getFloat(R.styleable.FixedScrollLayout_scrollable_friction, Float.NaN);
            if (friction == friction) {
                setFriction(friction);
            }

            mMaxScrollY = array.getDimensionPixelSize(R.styleable.FixedScrollLayout_scrollable_maxScroll, 0);

            final long considerIdleMillis = array.getInteger(
                    R.styleable.FixedScrollLayout_scrollable_considerIdleMillis,
                    DEFAULT_CONSIDER_IDLE_MILLIS
            );
            setConsiderIdleMillis(considerIdleMillis);

            final boolean useDefaultCloseUp = array.getBoolean(R.styleable.FixedScrollLayout_scrollable_defaultCloseUp, false);
            if (useDefaultCloseUp) {
                setCloseUpAlgorithm(new DefaultCloseUpLogic());
            }

            final int closeUpAnimationMillis = array.getInteger(R.styleable.FixedScrollLayout_scrollable_closeUpAnimationMillis, -1);
            if (closeUpAnimationMillis != -1) {
                setCloseUpIdleAnimationTime(new SimpleCloseUpIdleAnimationTime(closeUpAnimationMillis));
            }

            final int interpolatorResId = array.getResourceId(R.styleable.FixedScrollLayout_scrollable_closeUpAnimatorInterpolator, 0);
            if (interpolatorResId != 0) {
                final Interpolator interpolator = AnimationUtils.loadInterpolator(context, interpolatorResId);
                setCloseAnimatorConfigurator(new InterpolatorCloseUpAnimatorConfigurator(interpolator));
            }

        } finally {
            array.recycle();
        }

        setVerticalScrollBarEnabled(true);

        mScrollDetector = new GestureDetector(context, new ScrollGestureListener());
        mFlingDetector  = new GestureDetector(context, new FlingGestureListener(context));

        mMotionEventHook = new MotionEventHook(new MotionEventHookCallback() {
            @Override
            public void apply(MotionEvent event) {
                FixedScrollLayout.super.dispatchTouchEvent(event);
            }
        });
    }

    protected FixedScrollScroller initScroller(Context context, Interpolator interpolator, boolean flywheel) {
        return new FixedScrollScroller(context, interpolator, flywheel);
    }

    public void setFriction(float friction) {
        mScroller.setFriction(friction);
    }

    public void setCanScrollVerticallyDelegate(CanScrollVerticallyDelegate delegate) {
        this.mCanScrollVerticallyDelegate = delegate;
    }

    public void setMaxScrollY(int maxY) {
        this.mMaxScrollY = maxY;
    }

    public int getMaxScrollY() {
        return mMaxScrollY;
    }

    public void setConsiderIdleMillis(long millis) {
        mConsiderIdleMillis = millis;
    }

    public long getConsiderIdleMillis() {
        return mConsiderIdleMillis;
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        this.mOnScrollChangedListener = listener;
    }

    @Override
    public void onScrollChanged(int l, int t, int oldL, int oldT) {

        final boolean changed = t != oldT;

        if (changed && mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(t, oldT, mMaxScrollY);
        }

        if (mCloseUpLogic != null) {
            removeCallbacks(mIdleRunnable);
            if (!mSelfUpdateScroll && changed && !mIsTouchOngoing) {
                postDelayed(mIdleRunnable, mConsiderIdleMillis);
            }
        }
    }

    protected void setSelfUpdateScroll(boolean value) {
        mSelfUpdateScroll = value;
    }

    protected boolean isSelfUpdateScroll() {
        return mSelfUpdateScroll;
    }

    public void setCloseUpAlgorithm(CloseUpLogic closeUpLogic) {
        this.mCloseUpLogic = closeUpLogic;
    }

    public void setCloseUpIdleAnimationTime(CloseUpIdleAnimationTime closeUpIdleAnimationTime) {
        this.mCloseUpIdleAnimationTime = closeUpIdleAnimationTime;
    }

    public void setCloseAnimatorConfigurator(CloseUpAnimatorConfigurator configurator) {
        this.mCloseAnimatorConfigurator = configurator;
    }

    @Override
    public void scrollTo(int x, int y) {

        final int newY = getNewY(y);

        if (newY < 0) {
            return;
        }

        super.scrollTo(0, newY);
    }

    protected int getNewY(int y) {

        final int currentY = getScrollY();

        if (currentY == y) {
            return -1;
        }

        final int direction = y - currentY;
        final boolean isScrollingBottomTop = direction < 0;

        if (mCanScrollVerticallyDelegate != null) {

            if (isScrollingBottomTop) {

                // if not dragging draggable then return, else do not return
                if (!mIsDraggingDraggable
                        && !mSelfUpdateScroll
                        && mCanScrollVerticallyDelegate.canScrollVertically(direction)) {
                    return -1;
                }
            } else {

                // else check if we are at max scroll
                if (currentY == mMaxScrollY
                        && !mCanScrollVerticallyDelegate.canScrollVertically(direction)) {
                    return -1;
                }
            }
        }

        if (y < 0) {
            y = 0;
        } else if (y > mMaxScrollY) {
            y = mMaxScrollY;
        }

        return y;
    }

    public void setDraggableView(View view) {
        mDraggableView = view;
    }

    @Override
    public boolean dispatchTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {

        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {

            mIsTouchOngoing = true;
            mScroller.abortAnimation();

            if (mDraggableView != null && mDraggableView.getGlobalVisibleRect(mDraggableRect)) {
                final int x = (int) (event.getRawX() + .5F);
                final int y = (int) (event.getRawY() + .5F);
                mIsDraggingDraggable = mDraggableRect.contains(x, y);
            } else {
                mIsDraggingDraggable = false;
            }
        } else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL){


            mIsTouchOngoing = false;

            if (mCloseUpLogic != null) {
                removeCallbacks(mIdleRunnable);
                postDelayed(mIdleRunnable, mConsiderIdleMillis);
            }
        }

        final boolean isPrevScrolling = mIsScrolling;
        final boolean isPrevFlinging  = mIsFlinging;

        mIsFlinging     = mFlingDetector .onTouchEvent(event);
        mIsScrolling    = mScrollDetector.onTouchEvent(event);

        removeCallbacks(mScrollRunnable);
        post(mScrollRunnable);

        final boolean isIntercepted     = mIsScrolling || mIsFlinging;
        final boolean isPrevIntercepted = isPrevScrolling || isPrevFlinging;

        final boolean shouldRedirectDownTouch = action == MotionEvent.ACTION_MOVE
                && (!isIntercepted && isPrevIntercepted)
                && getScrollY() == mMaxScrollY;

        if (isIntercepted || isPrevIntercepted) {

            mMotionEventHook.hook(event, MotionEvent.ACTION_CANCEL);

            if (!isPrevIntercepted) {
                return true;
            }
        }

        if (shouldRedirectDownTouch) {
            mMotionEventHook.hook(event, MotionEvent.ACTION_DOWN);
        }

        super.dispatchTouchEvent(event);
        return true;
    }

    private void cancelIdleAnimationIfRunning(boolean removeCallbacks) {

        if (removeCallbacks) {
            removeCallbacks(mIdleRunnable);
        }

        if (mCloseUpAnimator != null && mCloseUpAnimator.isRunning()) {
            mCloseUpAnimator.cancel();
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            final int oldY = getScrollY();
            final int nowY = mScroller.getCurrY();
            scrollTo(0, nowY);
            if (oldY != nowY) {
                onScrollChanged(0, getScrollY(), 0, oldY);
            }
            postInvalidate();
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mMaxScrollY;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //int childTop = top;
		int childTop = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View view = getChildAt(i);
            view.layout(left, childTop, right, childTop + view.getMeasuredHeight());
            childTop += view.getMeasuredHeight();
        }
    }

    private final Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {

            final boolean isContinue = mScroller.computeScrollOffset();
            mSelfUpdateFling = isContinue;

            if (isContinue) {

                final int y = mScroller.getCurrY();
                final int nowY = getScrollY();
                final int diff = y - nowY;

                if (diff != 0) {
                    scrollBy(0, diff);
                }

                post(this);
            }
        }
    };

    private final Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {

            cancelIdleAnimationIfRunning(false);

            if (mSelfUpdateScroll || mSelfUpdateFling) {
                return;
            }

            final int nowY = getScrollY();

            if (nowY == 0
                    || nowY == mMaxScrollY) {
                return;
            }

            final int endY = mCloseUpLogic.getIdleFinalY(FixedScrollLayout.this, nowY, mMaxScrollY);

            if (nowY == endY) {
                return;
            }

            mCloseUpAnimator = ObjectAnimator.ofInt(FixedScrollLayout.this, mCloseUpAnimationProperty, nowY, endY);

            final long duration = mCloseUpIdleAnimationTime != null
                    ? mCloseUpIdleAnimationTime.compute(FixedScrollLayout.this, nowY, endY, mMaxScrollY)
                    : DEFAULT_IDLE_CLOSE_UP_ANIMATION;

            mCloseUpAnimator.setDuration(duration);
            mCloseUpAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mSelfUpdateScroll = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mSelfUpdateScroll = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mSelfUpdateScroll = false;
                }
            });

            if (mCloseAnimatorConfigurator != null) {
                mCloseAnimatorConfigurator.configure(mCloseUpAnimator);
            }

            mCloseUpAnimator.start();
        }
    };

    private class ScrollGestureListener extends GestureListenerAdapter {

        private final int mTouchSlop;
        {
            final ViewConfiguration vc = ViewConfiguration.get(getContext());
            mTouchSlop = vc.getScaledTouchSlop();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            final float absX = Math.abs(distanceX);
            if (absX > Math.abs(distanceY)
                    || absX > mTouchSlop) {
                return false;
            }

            final int now = getScrollY();
            scrollBy(0, (int) (distanceY + .5F));

            return now != getScrollY();
        }
    }

    private class FlingGestureListener extends GestureListenerAdapter {

        private static final int MIN_FLING_DISTANCE_DIP = 12;

        private final int mMinFlingDistance;
        private final float mMinVelocity;

        FlingGestureListener(Context context) {
            this.mMinFlingDistance = DipUtils.dipToPx(context, MIN_FLING_DISTANCE_DIP);

            final ViewConfiguration configuration = ViewConfiguration.get(context);
            this.mMinVelocity = configuration.getScaledMinimumFlingVelocity();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (Math.abs(velocityY) < mMinVelocity) {
                return false;
            }

            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                return false;
            }

            final int nowY = getScrollY();
            if (nowY < 0 || nowY > mMaxScrollY) {
                return false;
            }

            mScroller.fling(0, nowY, 0, -(int) (velocityY + .5F), 0, 0, 0, mMaxScrollY);

            if (mScroller.computeScrollOffset()) {

                final int suggestedY = mScroller.getFinalY();

                if (Math.abs(nowY - suggestedY) < mMinFlingDistance) {
                    mScroller.abortAnimation();
                    return false;
                }

                final int finalY;
                if (suggestedY == nowY || mCloseUpLogic == null) {
                    finalY = suggestedY;
                } else {
                    finalY = mCloseUpLogic.getFlingFinalY(
                            FixedScrollLayout.this,
                            suggestedY - nowY < 0,
                            nowY,
                            suggestedY,
                            mMaxScrollY
                    );
                    mScroller.setFinalY(finalY);
                }

                final int newY = getNewY(finalY);

                return !(finalY == nowY || newY < 0);
            }

            return false;
        }
    }

    private static class MotionEventHook {

        final MotionEventHookCallback callback;

        MotionEventHook(MotionEventHookCallback callback) {
            this.callback = callback;
        }

        void hook(MotionEvent event, int action) {
            final int historyAction = event.getAction();
            event.setAction(action);
            callback.apply(event);
            event.setAction(historyAction);
        }
    }

    private interface MotionEventHookCallback {
        void apply(MotionEvent event);
    }

    private final Property<FixedScrollLayout, Integer> mCloseUpAnimationProperty
            = new Property<FixedScrollLayout, Integer>(Integer.class, "scrollY") {

        @Override
        public Integer get(FixedScrollLayout object) {
            return object.getScrollY();
        }

        @Override
        public void set(final FixedScrollLayout layout, final Integer value) {
            layout.setScrollY(value);
        }
    };

    @Override
    public Parcelable onSaveInstanceState() {
    	final Parcelable superState = super.onSaveInstanceState();
    	final ScrollableLayoutSavedState savedState = new ScrollableLayoutSavedState(superState);

        savedState.scrollY = getScrollY();

    	return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

    	if (!(state instanceof ScrollableLayoutSavedState)) {
    		super.onRestoreInstanceState(state);
    		return;
    	}

    	final ScrollableLayoutSavedState in = (ScrollableLayoutSavedState) state;
    	super.onRestoreInstanceState(in.getSuperState());

        setScrollY(in.scrollY);
    }

    private static class ScrollableLayoutSavedState extends BaseSavedState {

        int scrollY;

    	public ScrollableLayoutSavedState(Parcel source) {
    		super(source);

            scrollY = source.readInt();
    	}

    	public ScrollableLayoutSavedState(Parcelable superState) {
    		super(superState);
    	}

    	@Override
    	public void writeToParcel(Parcel out, int flags) {
    		super.writeToParcel(out, flags);

            out.writeInt(scrollY);
    	}

    	public static final Creator<ScrollableLayoutSavedState> CREATOR
    			= new Creator<ScrollableLayoutSavedState>() {

    		@Override
    		public ScrollableLayoutSavedState createFromParcel(Parcel in) {
    			return new ScrollableLayoutSavedState(in);
    		}

    		@Override
    		public ScrollableLayoutSavedState[] newArray(int size) {
    			return new ScrollableLayoutSavedState[size];
    		}
    	};
    }
}
