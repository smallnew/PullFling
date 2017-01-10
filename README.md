# PullFlingLayout  
一个android的列表下拉、滑动联动头部布局，类似qq音乐歌手页面，可以在手指滑动后再惯性滑动，惯性滑动取自android源码内的计算方式，体验更流畅


		使用方式可见FlingActivity代码，支持视差Parallax和缩放Zoom，想实现更多特效可在PullFlingLayout类的onScrollPercent、overScrolled添加。
		具体效果如下
		
		

		支持手指往上滑屏幕并离开屏幕之后继续根据滑动速度再滑动一段距离


![image](https://github.com/smallnew/PullFling/raw/master/GIF0.gif)


		支持手指往下滑后到列表顶部后，根据速度再拉伸头部一段距离


![image](https://github.com/smallnew/PullFling/raw/master/GIF1.gif)


		暂时支持listview和scrollview，支持下拉放大、居中


![image](https://github.com/smallnew/PullFling/raw/master/GIF2.gif)

