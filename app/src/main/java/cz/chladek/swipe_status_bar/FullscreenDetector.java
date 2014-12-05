package cz.chladek.swipe_status_bar;

import java.util.HashSet;
import java.util.Iterator;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class FullscreenDetector {

	private WindowManager windowManager;
	private HashSet<OnFullscreenListener> listeners;
	private Detector detector;
	private boolean fullscreen;

	public FullscreenDetector(Context context) {
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		detector = new Detector(context);
		listeners = new HashSet<OnFullscreenListener>(2);
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void addOnFullscreenListener(OnFullscreenListener listener) {
		listeners.add(listener);
	}

	public void removeOnFillscreenListener(OnFullscreenListener listener) {
		listeners.remove(listener);
	}

	public void dispose() {
		windowManager.removeView(detector);
	}

	private class Detector extends RelativeLayout {
		private DisplayMetrics displayMetrics;

		private Detector(Context context) {
			super(context);
			WindowManager.LayoutParams layout = new WindowManager.LayoutParams(0, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
			layout.gravity = Gravity.LEFT;
			windowManager.addView(this, layout);
			displayMetrics = new DisplayMetrics();
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			windowManager.getDefaultDisplay().getMetrics(displayMetrics);
			boolean pre = b == displayMetrics.heightPixels || b == displayMetrics.widthPixels;
			if (pre != fullscreen) {
				fullscreen = pre;
				if (listeners.size() != 0) {
					Iterator<OnFullscreenListener> it = listeners.iterator();
					while (it.hasNext())
						it.next().fullscreenChanged(fullscreen);
				}
			}
		}
	}

	public interface OnFullscreenListener {
		public void fullscreenChanged(boolean fullscreen);
	}
}
