package com.smallnew.pullfling.lib;

public interface IGo {
	public int getLastVelocity();
	public void goFling(int distance);
	public void setScrollStateChangeListener(ScrollStateListener l);
	public int getScrollYGo();
	public float getIdlingVelocity();
	public float getCurrentVelocity();
}
