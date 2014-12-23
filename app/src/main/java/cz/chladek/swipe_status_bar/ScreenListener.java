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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;

public class ScreenListener extends BroadcastReceiver {

	private ContextWrapper cw;
	private IntentFilter filter;
	private boolean enabled;

	public static boolean screenON = true;

	public ScreenListener(ContextWrapper cw) {
		this.cw = cw;
		filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		cw.registerReceiver(this, filter);
		enabled = true;
	}

	public void setEnabled(boolean enabled) {
		if (!this.enabled && enabled) {
			cw.registerReceiver(this, filter);
			this.enabled = true;
		} else if (this.enabled && !enabled) {
			cw.unregisterReceiver(this);
			this.enabled = false;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
			screenON = false;
		else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			screenON = true;

		StatusBarService sbs = StatusBarService.getInstance();
		if (sbs != null)
			sbs.forceRunning(screenON);
	}
}