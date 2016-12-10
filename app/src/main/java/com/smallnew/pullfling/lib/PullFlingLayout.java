package com.smallnew.pullfling.lib;


import android.content.Context;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.ScrollView;

import com.smallnew.pullfling.R;

public class PullFlingLayout extends LinearLayout implements ScrollStateListener {

    private boolean isParallax = true;
    private static final float FRICTION = 2f;
    private View mTop;
    private View mHead;
    private View mNav;
    private ViewPager mViewPager;

    private int mTopViewHeight;
    private ViewGroup mInnerScrollView;
    private boolean isTopHidden = false;//getScrollY() == mTopViewHeight 头部view被mInnerScrollView完全遮盖

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;

    private float mLastY;
    private boolean mDragging;
    private boolean mOverDragging;//原始位置的时候，往下面拉，有阻尼

    private boolean mFlinging;//整体从下往上滑的时候，是否需要底部的scrollview继续滑
    private int mTopToScrollDistance;

    private boolean isInControl = false;

    private boolean isInitedHeight;

    public PullFlingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);

        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();

        float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f;

//        double distanceEs = getSplineFlingDistance(1000);
//        Log.e("getSplineFlingDistance", "getSplineFlingDistance " + distanceEs);
//        int velocityRs = getVelocityByDistance(116.64459);
//        Log.e("getVelocityByDistance", "getVelocityByDistance " + velocityRs);
    }

    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    // Fling friction
    private static float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static float mPhysicalCoeff;
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));

    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private static double getSplineDecelerationByDistance(double distance) {
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return decelMinusOne * (Math.log(distance / (mFlingFriction * mPhysicalCoeff))) / DECELERATION_RATE;
    }

    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }

    public static int getVelocityByDistance(double distance) {
        final double l = getSplineDecelerationByDistance(distance);
        int velocity = (int) (Math.exp(l) * mFlingFriction * mPhysicalCoeff / INFLEXION);
        return Math.abs(velocity);
    }

    /* Returns the duration, expressed in milliseconds */
    private int getSplineFlingDuration(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTop = findViewById(R.id.id_pullflinglayout_topview);
        mHead = findViewById(R.id.id_pullflinglayout_headview);
        mNav = findViewById(R.id.id_pullflinglayout_indicator);
        View view = findViewById(R.id.id_pullflinglayout_viewpager);
        if (!(view instanceof ViewPager)) {
            throw new RuntimeException(
                    "id_stickynavlayout_viewpager show used by ViewPager !");
        }
        mViewPager = (ViewPager) view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        params.height = getMeasuredHeight() - mNav.getMeasuredHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!isInitedHeight) {
            mTopViewHeight = mTop.getMeasuredHeight();

            LayoutParams lp = (LayoutParams) mTop.getLayoutParams();
            lp.height = mTopViewHeight * 2;
            mTop.setLayoutParams(lp);
            setPadding(0, -mTopViewHeight, 0, 0);
        }
        isInitedHeight = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                Log.e("dispatchTouchEvent", "in ACTION_DOWN");
                mFlinging = false;//stop inner scrollview fling
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                getCurrentScrollView();
                isTopHidden = getScrollY() == mTopViewHeight;
                if (mInnerScrollView instanceof ScrollView) {
                    if (mInnerScrollView.getScrollY() == 0 && isTopHidden && dy > 0
                            && !isInControl) {
                        Log.e("dispatchTouchEvent", "in post CANCEL and DOWN");
                        isInControl = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent ev2 = MotionEvent.obtain(ev);
                        dispatchTouchEvent(ev);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        return dispatchTouchEvent(ev2);
                    }
                } else if (mInnerScrollView instanceof ListView) {

                    ListView lv = (ListView) mInnerScrollView;
                    View c = lv.getChildAt(lv.getFirstVisiblePosition());
                    if (!isInControl && c != null && c.getTop() == 0 && isTopHidden
                            && dy > 0) {
                        Log.e("dispatchTouchEvent", "in post CANCEL and DOWN");
                        isInControl = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent ev2 = MotionEvent.obtain(ev);
                        dispatchTouchEvent(ev);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        return dispatchTouchEvent(ev2);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     *
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                mScroller.abortAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                getCurrentScrollView();
                if (Math.abs(Math.floor(dy)) > mTouchSlop) {
                    mDragging = true;
                    mOverDragging = false;
                    isTopHidden = getScrollY() == mTopViewHeight;
                    if (mInnerScrollView instanceof ScrollView) {//TODO
                        if (!isTopHidden
                                || (mInnerScrollView.getScrollY() == 0
                                && isTopHidden && dy > 0)) {
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        } else {//test
                            Log.e("onInterceptTouchEvent", "in ALL ACTION_MOVE" + " getScrollY=" + getScrollY()
                                    + " isTopHidden = " + isTopHidden);
                        }
                    } else if (mInnerScrollView instanceof ListView) {

                        ListView lv = (ListView) mInnerScrollView;
                        View c = lv.getChildAt(lv.getFirstVisiblePosition());
                        if (!isTopHidden || //
                                (c != null //
                                        && c.getTop() == 0//
                                        && isTopHidden && dy > 0)) {
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mOverDragging = false;
                mDragging = false;
                recycleVelocityTracker();
                break;
        }
        return super.onInterceptTouchEvent(ev);//default is false
    }

    private void getCurrentScrollView() {

        int currentItem = mViewPager.getCurrentItem();
        PagerAdapter a = mViewPager.getAdapter();
        if (a instanceof FragmentPagerAdapter) {
            FragmentPagerAdapter fadapter = (FragmentPagerAdapter) a;
            Fragment item = (Fragment) fadapter.instantiateItem(mViewPager,
                    currentItem);
            mInnerScrollView = (ViewGroup) (item.getView()
                    .findViewById(R.id.id_pullflinglayout_innerscrollview));
        } else if (a instanceof FragmentStatePagerAdapter) {
            FragmentStatePagerAdapter fsAdapter = (FragmentStatePagerAdapter) a;
            Fragment item = (Fragment) fsAdapter.instantiateItem(mViewPager,
                    currentItem);
            mInnerScrollView = (ViewGroup) (item.getView()
                    .findViewById(R.id.id_pullflinglayout_innerscrollview));
        }

        setScrollListener(mInnerScrollView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        int action = event.getAction();
        float y = event.getY();//这里的y指的是手指的位置

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                mLastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;

                Log.e("onTouchEvent move", "dy = " + dy + " , y = " + y + " , mLastY = " + mLastY);

                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                    mOverDragging = false;
                }
                if (mDragging) {
                    if (getScrollY() <= 0 && dy > 0) {//向下拖，有阻尼
                        dy = dy / FRICTION;
                        if (getScrollY() <= -mTopViewHeight / FRICTION) {//下拉界限为-mTopViewHeight/FRICTION
                            dy = 0;
                        } else {
                            mOverDragging = true;
                        }
                    }
                    scrollBy(0, (int) -dy);
                    if (getScrollY() == mTopViewHeight && dy < 0) {//列表已经置顶，实践交给列表
                        event.setAction(MotionEvent.ACTION_DOWN);
                        dispatchTouchEvent(event);
                        isInControl = false;
                    }
                }

                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                mOverDragging = false;
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                mOverDragging = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityY = (int) mVelocityTracker.getYVelocity();
                Log.e("onTouchEvent ACTION_UP", "velocityY = " + velocityY);
                if (Math.abs(velocityY) > mMinimumVelocity /*&& getScrollY()>0*/) {

                    fling(-velocityY);//底部viewpager滑动一段距离
                    int deltaY = mScroller.getFinalY() - mScroller.getStartY();//总共滑动的距离
                    mTopToScrollDistance = (int) getSplineFlingDistance(velocityY) - Math.abs(deltaY);//这个速度多滑的距离
                    if (mTopToScrollDistance > 0) {
                        mFlinging = true;
                        mTopToScrollDistance = velocityY < 0 ? mTopToScrollDistance : -mTopToScrollDistance;
                        if (getScrollY() < 0) {
                            mTopToScrollDistance = 0;
                        }
                    }
                } else {
                    invalidate();
                }

                recycleVelocityTracker();
                break;
        }

        return super.onTouchEvent(event);
    }

    private void setScrollListener(ViewGroup innerView) {
        if (innerView instanceof IGo) {
            IGo innerScrollView = (IGo) innerView;
            innerScrollView.setScrollStateChangeListener(this);
        }
    }

    private int lastInnerState = 0;
    private int lastInnerScrollY = 0;

    @Override
    public void onScrollStateChange(int state) {

        if (lastInnerState == ScrollStateListener.STATE_FLING_BY_SELF
                && state != ScrollStateListener.STATE_TOUCH)//if fling by <void fling(int velocityY)>.
            return;

        if (lastInnerState == ScrollStateListener.STATE_FLING
                && state == ScrollStateListener.STATE_FLING) {
            //flinging
        } else if (lastInnerState == ScrollStateListener.STATE_FLING
                && state == ScrollStateListener.STATE_IDLE) {

            if (mInnerScrollView instanceof GoListView) {
                int idlingVelocity = (int) ((IGo) mInnerScrollView).getIdlingVelocity();
                fling(-idlingVelocity);
                lastInnerState = ScrollStateListener.STATE_FLING_BY_SELF;
                return;
            }

            //fling to down
            int currentScroll = ((IGo) mInnerScrollView).getScrollYGo();//ListView下currentScroll和lastInnerScrollY都是0
            int deltaY = currentScroll - lastInnerScrollY;

            int curVelocity = (int) ((IGo) mInnerScrollView).getCurrentVelocity();
            int absVelocity = Math.abs(((IGo) mInnerScrollView).getLastVelocity());
            Log.e("onScrollStat STATE_IDLE", "currentScroll " + currentScroll
                    + " deltaY " + deltaY);

            double distanceEs = getSplineFlingDistance(absVelocity);
            Log.e("onScrollStat STATE_IDLE", "getSplineFlingDistance " + distanceEs);
            Log.e("onScrollStat STATE_IDLE", "curVelocity " + curVelocity);
            if (//currentScroll<=1 &&
                    Math.abs(deltaY)
                            < distanceEs
                    ) {
                int deltaAbsFling = (int) distanceEs - Math.abs(deltaY);
                int absDeltaVelocity = getVelocityByDistance(deltaAbsFling);
                int deltaVelocity = deltaY < 0 ? -absDeltaVelocity : absDeltaVelocity;
                fling(deltaVelocity);
            }
            lastInnerState = ScrollStateListener.STATE_FLING_BY_SELF;
            return;
        } else if (state == ScrollStateListener.STATE_TOUCH) {
            //touch by hand
        } else if ((lastInnerState == ScrollStateListener.STATE_IDLE || lastInnerState == ScrollStateListener.STATE_TOUCH)
                && state == ScrollStateListener.STATE_FLING) {
            //start fling
            lastInnerScrollY = ((IGo) mInnerScrollView).getScrollYGo();
        }
        lastInnerState = state;
    }


    public void fling(int velocityY) {
        mDragging = false;
        mOverDragging = false;
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, -mTopViewHeight, mTopViewHeight);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < -mTopViewHeight) {//滚动超过了两倍的mTopViewHeight，强制设置为滚动两倍，因设定就是最大滚动只能为两倍
            y = -mTopViewHeight;
        }
        if (y < 0 && !mDragging) {//向下滚动已经超过原始位置，y为负数
            y = (int) (y / FRICTION);
            int height = mTopViewHeight - y;//头部实际高度
            float overPercent = (float) (mTopViewHeight - y) / mTopViewHeight;
            overScrolled(overPercent);
            //lp.height =  height;
            //mTop.setLayoutParams(lp);
            //return;
        } else if (y < 0 && mOverDragging) {//向下拖动已经超过原始位置，y为负数
            float overPercent = (float) (mTopViewHeight - y) / mTopViewHeight;
            overScrolled(overPercent);
        }
        if (y > mTopViewHeight) {
            y = mTopViewHeight;
        }
        if (y != getScrollY()) {
            super.scrollTo(x, y);
        }

        isTopHidden = getScrollY() == mTopViewHeight;
        if (getScrollY() > 0)
            onScrollPercent((float) getScrollY() / (float) mTopViewHeight);
    }

    /**
     *底部列表往上移动时的百分比，初始状态percent==0%，头部被遮住时percent==100%
     * @param percent
     */
    protected void onScrollPercent(float percent) {
        if (isParallax) {
            int parallax = -(int) (mTopViewHeight * ((percent) / 2));
            mHead.scrollTo(0, parallax);
        }
    }

    /**
     *初始状态往下拉后触发，初始状态percent==100%，往下拉后percent>100%
     * @param percent
     */
    protected void overScrolled(float percent) {
        Log.e("overPercent", "overPercent = " + percent);
        mHead.setScaleX(percent);
        mHead.setScaleY(percent);
        int parallax = -(int) (mTopViewHeight * ((1 - percent) / 2));
        mHead.scrollTo(0, parallax);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        } else if (mFlinging) {
            Log.e("computeScroll", "mTopToScrollDistance=" + mTopToScrollDistance);
//			int leaveVelocity = getVelocityByDistance(mTopToScrollDistance);
//			leaveVelocity = mTopToScrollDistance<0?-leaveVelocity:leaveVelocity;
            ((IGo) mInnerScrollView).goFling(mTopToScrollDistance);
            invalidate();
            mFlinging = false;
        } else {
            if (getScrollY() < 0 && !mDragging) {//当向下滑动时，且放开手指拖动，需要还原到原来位置
                Log.e("computeScroll", "getScrollY()=" + getScrollY() + " mTopViewHeight=" + mTopViewHeight);
                int leaveVelocity = getVelocityByDistance(getScrollY() - mTopViewHeight);
                mScroller.fling(0, getScrollY(), 0, leaveVelocity, 0, 0, -mTopViewHeight, mTopViewHeight);
                invalidate();
            } else {
            }
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

}
