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

import cz.chladek.swipe_status_bar.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class TestScreenActivity extends Activity implements OnSeekBarChangeListener {

	private Editor editor;
	private SeekBar sensitivitySB, widthSB, positionSB;
	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_screen_layout);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		editor = prefs.edit();

		sensitivitySB = (SeekBar) findViewById(R.id.swipeSensitivitySB);
		sensitivitySB.setProgress(prefs.getInt(getString(R.string.pref_key_swipe_sensitivity), 20));
		sensitivitySB.setOnSeekBarChangeListener(this);

		widthSB = (SeekBar) findViewById(R.id.areaWidthSB);
		widthSB.setProgress((int) (prefs.getFloat(getString(R.string.pref_key_area_width), widthSB.getMax()) * 10000));
		widthSB.setOnSeekBarChangeListener(this);

		positionSB = (SeekBar) findViewById(R.id.areaPositionSB);
		positionSB.setProgress((int) (prefs.getFloat(getString(R.string.pref_key_area_position), 0.5f) * 10000));
		positionSB.setOnSeekBarChangeListener(this);

		((ImageView) findViewById(R.id.gift_small_iv)).setVisibility(MainActivity.giftSmall ? View.VISIBLE : View.INVISIBLE);
		((ImageView) findViewById(R.id.gift_medium_iv)).setVisibility(MainActivity.giftMedium ? View.VISIBLE : View.INVISIBLE);
		((ImageView) findViewById(R.id.gift_large_iv)).setVisibility(MainActivity.giftLarge ? View.VISIBLE : View.INVISIBLE);

		StatusBarService sbs = StatusBarService.getInstance();
		if (sbs != null)
			sbs.setAreaVisibility(true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		StatusBarService sbs = StatusBarService.getInstance();
		if (sbs != null)
			sbs.setAreaVisibility(false);
		finish();
	}

	@Override
	public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
		StatusBarService sbs = StatusBarService.getInstance();
		if (fromUser && sbs != null)
			if (sb == sensitivitySB)
				sbs.setAreaHeight(progress + 5);
			else if (sb == widthSB)
				sbs.setAreaWidth(progress / 10000f);
			else if (sb == positionSB)
				sbs.setAreaPosition(progress / 10000f);
	}

	@Override
	public void onStartTrackingTouch(SeekBar sb) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar sb) {
		int progress = sb.getProgress();
		if (sb == sensitivitySB)
			editor.putInt(getString(R.string.pref_key_swipe_sensitivity), progress + 5);
		else if (sb == widthSB)
			editor.putFloat(getString(R.string.pref_key_area_width), progress / 10000f);
		else if (sb == positionSB)
			editor.putFloat(getString(R.string.pref_key_area_position), progress / 10000f);
		editor.commit();
	}
}
