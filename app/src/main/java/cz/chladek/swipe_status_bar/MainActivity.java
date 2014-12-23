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

import java.util.ArrayList;
import java.util.Locale;

import com.android.vending.billing.utils.IabHelper;
import com.android.vending.billing.utils.IabResult;
import com.android.vending.billing.utils.Inventory;
import com.android.vending.billing.utils.Purchase;

import cz.chladek.android.preferences.BlinkingPreference;
import cz.chladek.swipe_status_bar.R;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;

@SuppressWarnings("deprecation")
public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {

	private static final int RC_REQUEST = 394867;

	private Intent swipeService;
	private SharedPreferences prefs;
	private Vibrator vibrator;
	private IabHelper mHelper;
	private static String sku_gift_small, sku_gift_medium, sku_gift_large;
	public static boolean giftSmall, giftMedium, giftLarge;
	private int keyMenuPressedCount;
	private String versionName, about, changelog, emailText;
	private int versionCode;
	private PreferenceCategory othersPC;
	private PreferenceScreen devPreference;
	private boolean devVisible;

	private static MainActivity instance;

	public static MainActivity getInstance() {
		return instance;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_main);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);

		findPreference(getString(R.string.pref_key_about)).setOnPreferenceClickListener(this);
		//findPreference(getString(R.string.pref_key_bug_suggestions)).setOnPreferenceClickListener(this);
		findPreference(getString(R.string.pref_key_try_it)).setOnPreferenceClickListener(this);
		findPreference(getString(R.string.pref_key_donate)).setOnPreferenceClickListener(this);
		findPreference(getString(R.string.pref_key_vibrate_strength)).setOnPreferenceClickListener(this);

		boolean enable = prefs.getBoolean(getString(R.string.pref_key_vibrate), true);
		findPreference(getString(R.string.pref_key_vibrate_strength)).setEnabled(enable);

		othersPC = (PreferenceCategory) findPreference(getString(R.string.pref_key_others_category));
		devPreference = (PreferenceScreen) findPreference(getString(R.string.pref_key_dev_preference));
		othersPC.removePreference(devPreference);

		initBilling();
		checkService();
		checkVersion();

		//prefs.edit().putBoolean(getString(R.string.pref_key_status_bar_permanent_visibility), false).commit();
	}

	private void checkService() {
		swipeService = new Intent(this, StatusBarService.class);
		boolean start = prefs.getBoolean("startService", false);
		boolean running = StatusBarService.isRunning();
		if (start) {
			if (!running)
				startService(swipeService);
		} else
			findPreference(getString(R.string.pref_key_try_it)).setEnabled(false);
	}

	private void checkVersion() {
		if (Build.VERSION.SDK_INT < 17) {
			CheckBoxPreference chbp = (CheckBoxPreference) findPreference(getString(R.string.pref_key_expand_settings));
			chbp.setEnabled(false);
			chbp.setSummaryOn(R.string.pref_expand_settings_error);
			chbp.setSummaryOff(R.string.pref_expand_settings_error);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionName = pInfo.versionName;
			versionCode = pInfo.versionCode;
		} catch (NameNotFoundException e) {
			versionName = "1.0";
			versionCode = 0;
		}

		int lastVersionCode = prefs.getInt("lastVersionCode", 0);
		if (lastVersionCode < versionCode) {
			prefs.edit().putInt("lastVersionCode", versionCode).commit();
			/*
			 * AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			 * dialog.setTitle(R.string.app_name_space);
			 * dialog.setMessage(R.string.pref_alternative_mode_summary);
			 * dialog.setPositiveButton(R.string.button_ok, null);
			 * dialog.show();
			 */
		}
	}

	private void initBilling() {
		sku_gift_small = getString(R.string.donation_small);
		sku_gift_medium = getString(R.string.donation_medium);
		sku_gift_large = getString(R.string.donation_large);

		mHelper = new IabHelper(getApplicationContext(), getString(R.string.licence_key));
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess() || mHelper == null) {
					mHelper = null;
					return;
				}
				BlinkingPreference pref = (BlinkingPreference) findPreference(getString(R.string.pref_key_donate));
				pref.setEnabled(true);
				pref.startBlinking();
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		instance = this;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			keyMenuPressedCount++;
			if (keyMenuPressedCount == 5) {
				keyMenuPressedCount = 0;
				if (devVisible)
					othersPC.removePreference(devPreference);
				else
					othersPC.addPreference(devPreference);
				devVisible = !devVisible;
			}
			return true;
		} else
			keyMenuPressedCount = 0;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mHelper == null)
			return;
		if (!mHelper.handleActivityResult(requestCode, resultCode, data))
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals(getString(R.string.pref_key_start_service))) {
			boolean enable = prefs.getBoolean(getString(R.string.pref_key_start_service), false);
			enableService(enable);
			findPreference(getString(R.string.pref_key_try_it)).setEnabled(enable);
		} else if (key.equals(getString(R.string.pref_key_vibrate))) {
			boolean enable = prefs.getBoolean(getString(R.string.pref_key_vibrate), false);
			findPreference(getString(R.string.pref_key_vibrate_strength)).setEnabled(enable);
		} else if (key.equals(getString(R.string.pref_key_permanent_visibility))) {
			StatusBarService sbs = StatusBarService.getInstance();
			if (sbs != null)
				sbs.setAreaVisibility(prefs.getBoolean(key, false));
		} else if (key.equals(getString(R.string.pref_key_check_life_interval))) {
			StatusBarService sbs = StatusBarService.getInstance();
			if (sbs != null)
				sbs.forceRunning(true);
		} else if (key.equals(getString(R.string.pref_key_language)))
			updateLanguageSettings();
		else if (key.equals(getString(R.string.pref_key_status_bar_permanent_visibility))) {
			StatusBarService sbs = StatusBarService.getInstance();
			if (sbs != null)
				sbs.permanentVisibility(prefs.getBoolean(key, false));
		}
	}

	private void updateLanguageSettings() {
		String language = prefs.getString(getString(R.string.pref_key_language), "*");
		Locale locale;
		if (language.equals("*"))
			locale = Locale.getDefault();
		else
			locale = new Locale(language);
		Resources res = getResources();
		Configuration config = res.getConfiguration();
		config.locale = locale;
		res.updateConfiguration(config, res.getDisplayMetrics());
		Intent refresh = new Intent(this, MainActivity.class);
		startActivity(refresh);
	}

	private void enableService(boolean enable) {
		boolean running = StatusBarService.isRunning();
		if (enable && !running)
			startService(swipeService);
		else if (!enable && running) {
			StatusBarService sbs = StatusBarService.getInstance();
			if (sbs != null)
				sbs.forceRunning(false);
			stopService(swipeService);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (key.equals(getString(R.string.pref_key_about)))
			showAbout();
		else if (key.equals(getString(R.string.pref_key_bug_suggestions)))
			sendEmail();
		else if (key.equals(getString(R.string.pref_key_try_it)))
			startActivity(new Intent(getApplicationContext(), TestScreenActivity.class));
		else if (key.equals(getString(R.string.pref_key_donate)))
			donate();
		else if (key.equals(getString(R.string.pref_key_vibrate_strength)))
			showVibrationStrength();
		return true;
	}

	private void showVibrationStrength() {
		if (vibrator == null)
			vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		SeekBar bar = new SeekBar(this);
		bar.setMax(100);
		bar.setProgress(prefs.getInt(getString(R.string.pref_key_vibrate_strength), 15));
		bar.setPadding(40, 15, 40, 15);
		bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar sb) {
				int progress = sb.getProgress();
				vibrator.vibrate(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
				if (fromUser) {
					Editor editor = prefs.edit();
					editor.putInt(getString(R.string.pref_key_vibrate_strength), progress);
					editor.commit();
				}
			}
		});
		dialog.setTitle(R.string.pref_vibrate_strength);
		dialog.setView(bar);
		dialog.setPositiveButton(R.string.button_ok, null);
		dialog.show();
	}

	private void showAbout() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.app_name);
		if (about == null)
			about = getString(R.string.app_descriptions) + "\n\n" + getString(R.string.app_author) + "\n" + getString(R.string.app_author_name) + "\n\n" + getString(R.string.app_translation) + "\n"
					+ getString(R.string.app_translation_names) + "\n\n" + getString(R.string.app_version) + " " + versionName;
		builder.setMessage(about);
		builder.setPositiveButton(R.string.button_ok, null);
		builder.setNeutralButton(R.string.app_changelog, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle(R.string.app_changelog);
				if (changelog == null)
					changelog = getString(R.string.changelog_1_5) + "\n" + getString(R.string.changelog_1_4_5) + "\n" + getString(R.string.changelog_1_4_4) + "\n" + getString(R.string.changelog_1_4_3) + "\n"
							+ getString(R.string.changelog_1_4_2) + getString(R.string.changelog_1_4_1) + "\n" + getString(R.string.changelog_1_4) + "\n" + getString(R.string.changelog_1_3_9) + "\n"
							+ getString(R.string.changelog_1_3_8) + "\n" + getString(R.string.changelog_1_3_7) + "\n" + getString(R.string.changelog_1_3_6) + "\n" + getString(R.string.changelog_1_3_5) + "\n"
							+ getString(R.string.changelog_1_3_4) + "\n" + getString(R.string.changelog_1_3_3) + "\n" + getString(R.string.changelog_1_3_2) + "\n" + getString(R.string.changelog_1_3_1) + "\n"
							+ getString(R.string.changelog_1_3) + "\n" + getString(R.string.changelog_1_2_5) + "\n" + getString(R.string.changelog_1_2_4) + "\n" + getString(R.string.changelog_1_2_3) + "\n"
							+ getString(R.string.changelog_1_2_2) + "\n" + getString(R.string.changelog_1_2_1) + "\n" + getString(R.string.changelog_1_2) + "\n" + getString(R.string.changelog_1_1) + "\n"
							+ getString(R.string.changelog_1_0_2) + "\n" + getString(R.string.changelog_1_0_1) + "\n" + getString(R.string.changelog_1_0);
				builder.setMessage(changelog);
				builder.setPositiveButton(R.string.button_ok, null);
				AlertDialog d = builder.show();
				TextView textView = (TextView) d.findViewById(android.R.id.message);
				textView.setTextSize(15);
			}
		});
		builder.show();
	}

	private void sendEmail() {
		Intent email = new Intent(Intent.ACTION_SEND);
		email.setType("plain/text");
		email.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.email_report_email) });
		email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
		if (emailText == null)
			emailText = "\n\nDevice: " + android.os.Build.MODEL + "(" + android.os.Build.DEVICE + ")" + "\nAndroid " + android.os.Build.VERSION.RELEASE + "\nFirmware: " + android.os.Build.DISPLAY;
		email.putExtra(Intent.EXTRA_TEXT, emailText);
		email.setType("message/utf8");
		try {
			startActivity(email);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "No email client!", Toast.LENGTH_LONG).show();
		}
	}

	private void donate() {
		AlertDialog.Builder donationMsg = new AlertDialog.Builder(this);
		ArrayList<String> tmp = new ArrayList<String>(3);
		if (!giftSmall)
			tmp.add(getString(R.string.donate_small));
		if (!giftMedium)
			tmp.add(getString(R.string.donate_medium));
		if (!giftLarge)
			tmp.add(getString(R.string.donate_large));
		String[] gifts = tmp.toArray(new String[tmp.size()]);
		donationMsg.setTitle(R.string.pref_donate);
		donationMsg.setSingleChoiceItems(gifts, -1, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mHelper.flagEndAsync();
				mHelper.launchPurchaseFlow(MainActivity.this, which == 0 ? sku_gift_small : which == 1 ? sku_gift_medium : sku_gift_large, RC_REQUEST, mPurchaseFinishedListener, "DONATION");
				dialog.dismiss();
			}
		});
		donationMsg.setNegativeButton(R.string.button_cancel, null);
		donationMsg.create().show();
	}

	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			if (result.isFailure() || mHelper == null)
				return;
			giftSmall = inventory.getPurchase(sku_gift_small) != null;
			giftMedium = inventory.getPurchase(sku_gift_medium) != null;
			giftLarge = inventory.getPurchase(sku_gift_large) != null;
			if (giftLarge && giftMedium && giftSmall)
				findPreference(getString(R.string.pref_key_donate)).setEnabled(false);
		}
	};

	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			if (result.isFailure() || mHelper == null)
				return;
			AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
			bld.setMessage(R.string.donate_successful);
			bld.setNeutralButton(R.string.button_ok, null);
			bld.show();
		}
	};
}
