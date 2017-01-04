package com.smallnew.pullflinglib;

public interface ScrollStateListener {
	public final static int STATE_IDLE = 0;
	public final static int STATE_FLING = 1;
	public final static int STATE_TOUCH = 2;
	public final static int STATE_FLING_BY_SELF = 3;
	public void onScrollStateChange(int state);
}
