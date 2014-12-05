package cz.chladek.swipe_status_bar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

public class StatusBarHelperActivity extends Activity {

	public static final String STATUC_BAR_NAME = "cz.chladek.swipe_status_bar.StatusBar";

	private SharedPreferences prefs;
	private boolean first;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		StatusBarService sbs = StatusBarService.getInstance();
		if (sbs != null)
			sbs.getStatusBarController().setOriginalDesktopState();
		finish();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			if (!first)
				first = true;
			else
				finish();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_DOWN || e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)
			finish();
		return super.onTouchEvent(e);
	}

	@Override
	protected void onResume() {
		super.onResume();
		StatusBarService sbs = StatusBarService.getInstance();
		if (sbs != null) {
			StatusBarController sbc = sbs.getStatusBarController();
			sbc.overrideExpandedDesktopStyle();
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
			if (!prefs.getBoolean(getString(R.string.pref_key_show_only), false)) {
				Bundle extras = getIntent().getExtras();
				if (extras != null)
					sbc.showStatusBar(extras.getInt(STATUC_BAR_NAME));
			}
		}
	}
}