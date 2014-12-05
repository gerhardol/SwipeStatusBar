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

	public void setEnabled(boolean enbaled) {
		if (!this.enabled && enabled) {
			cw.registerReceiver(this, filter);
			enabled = true;
		} else if (this.enabled && !enabled) {
			cw.unregisterReceiver(this);
			enabled = false;
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