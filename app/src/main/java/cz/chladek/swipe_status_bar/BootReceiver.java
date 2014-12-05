package cz.chladek.swipe_status_bar;

import cz.chladek.swipe_status_bar.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {

	private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(context.getString(R.string.pref_key_start_service), false) && BOOT_COMPLETED.equals(intent.getAction())) {
			Intent swipeService = new Intent(context, StatusBarService.class);
			swipeService.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(swipeService);
		}
	}
}
