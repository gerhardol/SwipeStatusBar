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