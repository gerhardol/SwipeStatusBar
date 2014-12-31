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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

public class ShortcutActivity extends Activity {

    private static Context sInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * instance set
         */
        sInstance = this;

        Bundle extras = getIntent().getExtras();

        //must match StatusBarService.ShowStatusBarModes
        final String ids[] = {"SHOW_STATUSBAR", "EXPAND_NOTIFICATIONS", "EXPAND_SETTINGS"};

        final Map<String, Integer> mActionToMode =
                new HashMap<String, Integer>(3);
        mActionToMode.put("SHOW_STATUSBAR", 0);
        mActionToMode.put("EXPAND_NOTIFICATIONS", StatusBarController.STATUS_BAR_NOTIFICATION);
        mActionToMode.put("EXPAND_SETTINGS", StatusBarController.STATUS_BAR_SETTINGS);
        final String names[] = new String[3];
        names[0] = getString(R.string.select_shortcut_show);
        names[1] = getString(R.string.select_shortcut_notifications);
        names[2] = getString(R.string.select_shortcut_settings);

        if (extras != null) {

            String action = extras.getString("ACTION_TO_RUN");
            if (action != null && mActionToMode.containsKey(action)) {
                int mode = mActionToMode.get(action);

                StatusBarService sbs = StatusBarService.getInstance();
                if (sbs != null) {
                    sbs.showStatusBar(mode);
                }
            }

            /**
             * extras was found, don't continue anything else now..
             */
            finish();
            return;
        }

        /**
         * show profiles selection dialog
         */
        showProfileDialog(ids, names);
    }

    private void showProfileDialog(final String ids[], final String names[]) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.dismiss();

                        //Intent.ShortcutIconResource icon =
                          //      Intent.ShortcutIconResource.fromContext(sInstance, profiles.get(i).getIcon());

                        Intent intent = new Intent();

                        Intent launchIntent = new Intent(sInstance, ShortcutActivity.class);
                        launchIntent.putExtra("ACTION_TO_RUN", ids[i]);

                        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
                        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, names[i]);
                        //intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

                        setResult(RESULT_OK, intent);
                        finish();

                    }
                })
                .setTitle(getString(R.string.select_shortcut_action));


        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();

    }


}
