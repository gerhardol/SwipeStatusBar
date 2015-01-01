/* SwipeStatusBar  - Access the Status Bar Anywhere, Anytime
        Copyright (C) 2013 Thomas.

        This file is part of SwipeStatusBar.
        SwipeStatusBar is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 2 of the License, or
        (at your option) any later version.
        SwipeStatusBar is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
        GNU General Public License for more details.
        You should have received a copy of the GNU General Public License
        along with SwipeStatusBar. If not, see <http://www.gnu.org/licenses/>.
*/
package cz.chladek.swipe_status_bar;

import cz.chladek.swipe_status_bar.FullscreenDetector.OnFullscreenListener;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

public class StatusBarService extends Service implements OnTouchListener, OnKeyListener {

	private static StatusBarService instance;
	private ActivityManager activityManager;
	private Vibrator vibrator;
	private SharedPreferences prefs;
	private WindowManager windowManager;
	private View triggerArea, leftView, rightView, viewAlternative, viewPermanently;
	private LayoutParams layoutParams, layoutParamsAlternative, layoutParamsPernamently;
	private float firstTouch;
	private Intent statusBarActivity;
	private String packageName;
	private LinearLayout gestureArea;
	private FullscreenDetector detector;
	private boolean areaVisible, inWindow;
	private ScreenListener screenListener;

	private StatusBarController statusBarController;

	private static boolean running;

	public static StatusBarService getInstance() {
		return instance;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
		packageName = getApplicationContext().getPackageName();

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		statusBarActivity = new Intent(this, StatusBarHelperActivity.class);
		statusBarActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		gestureArea = new LinearLayout(this);
		gestureArea.setOrientation(LinearLayout.HORIZONTAL);

		leftView = new View(this);
		rightView = new View(this);
		triggerArea = new View(this);
		triggerArea.setOnTouchListener(this);

		gestureArea.addView(leftView);
		gestureArea.addView(triggerArea);
		gestureArea.addView(rightView);

		int height = prefs.getInt(getString(R.string.pref_key_swipe_sensitivity), 25);
		layoutParams = new WindowManager.LayoutParams(LayoutParams.MATCH_PARENT, height, LayoutParams.TYPE_PRIORITY_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
		layoutParams.gravity = Gravity.TOP;

		detector = new FullscreenDetector(this);
		detector.addOnFullscreenListener(onFullscreenListener);

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		if (detector.isFullscreen()) {
			windowManager.addView(gestureArea, layoutParams);
			inWindow = true;
		}

		float position = prefs.getFloat(getString(R.string.pref_key_area_position), 0.5f);
		float width = prefs.getFloat(getString(R.string.pref_key_area_width), 1);
		updateView(position, width);

		if (prefs.getBoolean(getString(R.string.pref_key_permanent_visibility), false))
			setAreaVisibility(true);

		statusBarController = new StatusBarController(this);

		viewAlternative = new View(this);
		viewAlternative.setOnTouchListener(this);
		viewAlternative.setOnKeyListener(this);

		viewPermanently = new View(this);

		layoutParamsAlternative = new WindowManager.LayoutParams(0, 0, LayoutParams.TYPE_SYSTEM_ALERT, LayoutParams.FLAG_FORCE_NOT_FULLSCREEN | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
				| LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);

		layoutParamsPernamently = new WindowManager.LayoutParams(0, 0, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_FORCE_NOT_FULLSCREEN | LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);

		forceRunning(true);

		screenListener = new ScreenListener(this);

		if (prefs.getBoolean(getString(R.string.pref_key_status_bar_permanent_visibility), false))
			permanentVisibility(true);

		instance = this;
		running = true;
	}

	private OnFullscreenListener onFullscreenListener = new OnFullscreenListener() {
		@Override
		public void fullscreenChanged(boolean fullscreen) {
			if (fullscreen && !inWindow) {
				windowManager.addView(gestureArea, layoutParams);
				inWindow = true;
			} else if (!fullscreen && inWindow) {
				windowManager.removeView(gestureArea);
				inWindow = false;
			}
		}
	};

//	public boolean isFullscreen() {
//		return detector != null ? detector.isFullscreen() : false;
//	}

	public void setAreaVisibility(boolean visible) {
		if (visible && !areaVisible) {
			triggerArea.setBackgroundColor(0xAA33B5E5);
			areaVisible = true;
		} else if (!visible && areaVisible && !prefs.getBoolean(getString(R.string.pref_key_permanent_visibility), false)) {
			triggerArea.setBackgroundColor(Color.TRANSPARENT);
			areaVisible = false;
		}
	}

	public void forceRunning(boolean enable) {
		Intent intent = new Intent(getApplicationContext(), StatusBarService.class);
		PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
		AlarmManager alarm = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pending);
		if (enable) {
			String intervalString = prefs.getString(getString(R.string.pref_key_check_life_interval), "2500");
			int interval = Integer.parseInt(intervalString);
			alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), interval, pending);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (inWindow && windowManager != null && gestureArea != null)
			windowManager.removeView(gestureArea);
		screenListener.setEnabled(false);
		detector.dispose();
		running = false;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (v == viewAlternative) {
			try {
				windowManager.removeView(viewAlternative);
			} catch (Exception e) {
			}
			statusBarController.setOriginalDesktopState();
		}
		return false;
	}

	@Override
    public boolean onTouch(View v, MotionEvent me) {
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            ComponentName cn = activityManager.getRunningTasks(1).get(0).topActivity;
            if (!packageName.equals(cn.getPackageName()))
                activity.finish();
        }

        if (v == viewAlternative) {
            try {
                windowManager.removeView(viewAlternative);
            } catch (Exception e) {
            }
            statusBarController.setOriginalDesktopState();
            return true;
        }

        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstTouch = me.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                boolean multiTouchMode = prefs.getBoolean(getString(R.string.pref_key_multi_touch), false);
                if (multiTouchMode && me.getPointerCount() < 2)
                    return false;
                if (me.getRawY() - firstTouch > 5) {
                    if (prefs.getBoolean(getString(R.string.pref_key_vibrate), false))
                        vibrator.vibrate(prefs.getInt(getString(R.string.pref_key_vibrate_strength), 15));

                    boolean openSettings = prefs.getBoolean(getString(R.string.pref_key_expand_settings), false) && me.getX() > v.getWidth() / 2;
                    boolean alternativeMode = prefs.getBoolean(getString(R.string.pref_key_alternative_mode), false);
                    if (alternativeMode) {
                        try {
                            windowManager.addView(viewAlternative, layoutParamsAlternative);
                        } catch (Exception e) {
                        }
                        statusBarController.overrideExpandedDesktopStyle();
                        if (!prefs.getBoolean(getString(R.string.pref_key_show_only), false))
                            statusBarController.showStatusBar(openSettings ? StatusBarController.STATUS_BAR_SETTINGS : StatusBarController.STATUS_BAR_NOTIFICATION);
                    } else {
                        statusBarActivity.putExtra(StatusBarHelperActivity.STATUC_BAR_NAME, openSettings ? StatusBarController.STATUS_BAR_SETTINGS : StatusBarController.STATUS_BAR_NOTIFICATION);
                        startActivity(statusBarActivity);
                    }
                    firstTouch = Float.MAX_VALUE;
                }
                break;
        }
        return true;
    }

    public void showStatusBar(int mode) {
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            ComponentName cn = activityManager.getRunningTasks(1).get(0).topActivity;
            if (!packageName.equals(cn.getPackageName()))
                activity.finish();
        }

        boolean alternativeMode = prefs.getBoolean(getString(R.string.pref_key_alternative_mode), false);
        if (alternativeMode) {
            try {
                windowManager.addView(viewAlternative, layoutParamsAlternative);
            } catch (Exception e) {
            }
            statusBarController.overrideExpandedDesktopStyle();
            if ((mode == StatusBarController.STATUS_BAR_SETTINGS) || (mode == StatusBarController.STATUS_BAR_NOTIFICATION)) {
                statusBarController.showStatusBar(mode);
            }
        } else {
            statusBarActivity.putExtra(StatusBarHelperActivity.STATUC_BAR_NAME, mode);
            startActivity(statusBarActivity);
        }
    }

    public void permanentVisibility(boolean flag) {
		if (flag) {
			statusBarController.overrideExpandedDesktopStyle();
			windowManager.addView(viewPermanently, layoutParamsPernamently);
		} else {
			windowManager.removeView(viewPermanently);
			statusBarController.setOriginalDesktopState();
		}
	}

//	public FullscreenDetector getFullscreenDetector() {
//		return detector;
//	}

	public void setAreaHeight(int height) {
		if (inWindow) {
			ViewGroup.LayoutParams layout = gestureArea.getLayoutParams();
			layout.height = height;
			windowManager.updateViewLayout(gestureArea, layout);
		}
	}

	public void setAreaWidth(float width) {
		float position = prefs.getFloat(getString(R.string.pref_key_area_position), 0.5f);
		updateView(position, width);
	}

	public void setAreaPosition(float position) {
		float width = prefs.getFloat(getString(R.string.pref_key_area_width), 1);
		updateView(position, width);
	}

	private void updateView(float position, float width) {
		triggerArea.setLayoutParams(new LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT, width));
		float widthHalf = width * 0.5f;
		float wR = Math.max((1 - position) - widthHalf, 0);
		float wL = Math.min(position - widthHalf, 1 - width - wR);
		leftView.setLayoutParams(new LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT, wL));
		rightView.setLayoutParams(new LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT, wR));
	}

	public static boolean isRunning() {
		return running;
	}

	public StatusBarController getStatusBarController() {
		return statusBarController;
	}
}
