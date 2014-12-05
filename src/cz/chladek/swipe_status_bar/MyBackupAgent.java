package cz.chladek.swipe_status_bar;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class MyBackupAgent extends BackupAgentHelper {

	@Override
	public void onCreate() {
		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, getPackageName() + "_preferences");
		addHelper("prefs", helper);
	}
}
