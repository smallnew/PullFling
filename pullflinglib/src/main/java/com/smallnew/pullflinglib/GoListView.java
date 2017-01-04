package com.smallnew.pullflinglib;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.OverScroller;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

public class GoListView extends ListView implements IGo{

	private int mVelocityYLast;
	private ScrollStateListener mStateListener;
	private float mVelocityYIdling;//快到顶部时的fling速度
	private OverScroller mRealScroller;//滚动对象
	private HashMap<Integer,Integer> mHeights;//<postion,height>
	
	public GoListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mHeights = new HashMap<Integer,Integer>();
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	public GoListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mHeights = new HashMap<Integer,Integer>();
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	public GoListView(Context context) {
		super(context);
		mHeights = new HashMap<Integer,Integer>();
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}
	
	private void notifyScrollState(int state) {
		if (mStateListener != null) {
			mStateListener.onScrollStateChange(state);
		}
	}
	
	private void initListener(){
		this.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
					case OnScrollListener.SCROLL_STATE_IDLE:
						notifyScrollState(ScrollStateListener.STATE_IDLE);
						break;
					case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
						notifyScrollState(ScrollStateListener.STATE_TOUCH);
						break;
					case OnScrollListener.SCROLL_STATE_FLING:
						notifyScrollState(ScrollStateListener.STATE_FLING);
						break;
				}

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
								 int totalItemCount) {
				//float curV2 = getCurrentVelocity();
				mVelocityYIdling = 0;
				int topPos = -GoListView.this.getHeaderViewsCount();
				if (firstVisibleItem == topPos) {
					float curV = getCurrentVelocity();
					Log.e("GoList onScroll", "firstVisibleItem=" + firstVisibleItem + " topPos=" + topPos + " getCurrentVelocity" + curV);
					mVelocityYIdling = curV;
				}
			}
		});
	}
	
	@Override
	public void setScrollStateChangeListener(ScrollStateListener l) {
		mStateListener = l;
		initListener();
	}

	@Override
	public int getLastVelocity() {
		return mVelocityYLast;
	}

	@Override
	public void goFling(int distance) {
		int leaveVelocity = PullFlingLayout.getVelocityByDistance(distance);
		leaveVelocity = distance<0?-leaveVelocity:leaveVelocity;
		mVelocityYLast=leaveVelocity;
		selfFling(leaveVelocity);
	}

	@Override
	public int getScrollYGo() {
		return this.getSelfScrollY();
	}
	
	private int getSelfScrollY(){
		int scrollHeight=0;
		int firstVisiblePos = getFirstVisiblePosition();
		//获取单个item的视图
		View itemView = this.getChildAt(0);
		//值得非常注意的是firstVisiblePos是从0开始算的，所以headCount正好对应listview的第一个item的索引
		if (itemView != null) {
			//这里计算的是从headview到当前可见的item之间已经被完全滚过去的item的总高度
			scrollHeight += (firstVisiblePos) * itemView.getHeight();
		}
		//最后加上当前可见的item，已经滚动的部分
		scrollHeight += (-itemView.getTop());
		return scrollHeight;
		/*
		try{
			if (mRealScroller == null) {
				Field flingRun = AbsListView.class.getDeclaredField("mFlingRunnable");
				flingRun.setAccessible(true);
				Field scroller = flingRun.getType().getDeclaredField("mScroller");
				scroller.setAccessible(true);
				mRealScroller= (OverScroller) scroller.get(flingRun.get(this));
			}
			return mRealScroller.getCurrY();
		}catch (Exception e) {
			e.printStackTrace();
			return 0;
		}*/
	}
	
	@Override
	public float getIdlingVelocity(){
		return mVelocityYIdling;
	}
	
	@Override
	public float getCurrentVelocity(){
		try{

			if (mRealScroller != null) {
				return mRealScroller.getCurrVelocity();
			}
			Field flingRun = AbsListView.class.getDeclaredField("mFlingRunnable");
			flingRun.setAccessible(true);
			Field scroller = flingRun.getType().getDeclaredField("mScroller");
			scroller.setAccessible(true);
			mRealScroller = (OverScroller)scroller.get(flingRun.get(this));
			return mRealScroller.getCurrVelocity();//TODO bug sometimes get NaN value
		}catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	private void selfFling(int velocity){
		if(Build.VERSION.SDK_INT>20){
			this.fling(velocity);
			return;
		}
		smoothScrollBy(2, 10);//smoothScrollBy a little distants to instants mFlingRunnable
		try{
			Field flingRun = AbsListView.class.getDeclaredField("mFlingRunnable");
			flingRun.setAccessible(true);
			String methodStr = "startOverfling";
			Log.e("selfFling", "velocity="+velocity);
			Method flingMethod = flingRun.getType().getDeclaredMethod(methodStr, int.class);
			flingMethod.setAccessible(true);
			flingMethod.invoke(flingRun.get(this), velocity);
			Log.e("selfFling invoke", "velocity="+velocity);
		}catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}
	

}
