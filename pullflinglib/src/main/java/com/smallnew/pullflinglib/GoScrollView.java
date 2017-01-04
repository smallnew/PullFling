package com.smallnew.pullflinglib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.OverScroller;
import android.widget.ScrollView;

import java.lang.reflect.Field;

public class GoScrollView extends ScrollView implements IGo{

	private ScrollStateListener mStateListener;
	private float mVelocityYIdling;//快到顶部时的fling速度
	private int mVelocityYLast;

	public GoScrollView(Context context) {
		super(context);
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	public GoScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	public GoScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}


	public void setScrollStateChangeListener(ScrollStateListener l) {
		mStateListener = l;
	}

	private void notifyScrollState(int state) {
		if (mStateListener != null) {
			mStateListener.onScrollStateChange(state);
		}
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		// Log.e("onScrollChanged", "l="+l+" t="+t+" oldl="+oldl+" oldt="+oldt
		// +" deltaY="+(t-oldt)
		// +" curTime=" + System.currentTimeMillis());
		super.onScrollChanged(l, t, oldl, oldt);
	}

	
	
	@Override
	public void goFling(int distance) {
		Log.e("IGO goFling","distance="+distance);
		int leaveVelocity = PullFlingLayout.getVelocityByDistance(distance);
		leaveVelocity = distance<0?-leaveVelocity:leaveVelocity;
		mVelocityYLast=leaveVelocity;
		super.fling(leaveVelocity);
	}

	@Override
	public void fling(int velocityY) {
		Log.e("fling velocityY", "velocityY=" + velocityY);
		mVelocityYLast=velocityY;
		super.fling(velocityY);
	}
	
	@Override
	public int getLastVelocity(){
		return mVelocityYLast;
	}
	
	@Override
	public float getCurrentVelocity(){
		try{
			Field scroller = ScrollView.class.getDeclaredField("mScroller");
			scroller.setAccessible(true);
			OverScroller realScroller = (OverScroller)scroller.get(this);
			return realScroller.getCurrVelocity();
		}catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	@Override
	public float getIdlingVelocity(){
		return mVelocityYIdling;
	}

	@Override
	public int getScrollYGo() {
		return this.getScrollY();
	}

	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
			int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		if (!isTouchEvent) {
			Log.e("overScrollBy", "overScrollBy " + "deltaY=" + deltaY
					+ " scrollY=" + scrollY + " scrollRangeY=" + scrollRangeY
					+ " maxOverScrollY=" + maxOverScrollY);
			
			if (deltaY <= 2&&(deltaY+scrollY)<=0) {// going to sleep down
				notifyScrollState(ScrollStateListener.STATE_IDLE);
			} else {
				notifyScrollState(ScrollStateListener.STATE_FLING);
			}
		} else {
			notifyScrollState(ScrollStateListener.STATE_TOUCH);
		}
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY,
				scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY,
				isTouchEvent);
	}

}
