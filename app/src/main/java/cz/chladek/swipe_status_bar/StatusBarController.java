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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class StatusBarController {

	private static final String EXPANDED_DESKTOP_STATE = "expanded_desktop_state";
	private static final String EXPANDED_DESKTOP_STYLE = "expanded_desktop_style";
	private static final String EXPANDED_DESKTOP_MODE = "expanded_desktop_mode";
	private static final String STATUS_BAR_HIDDEN = "statusbar_hidden";
	private static final String GRAVITYBOX_EXPANDED_DESKTOP_STATE = "gravitybox_expanded_desktop_state";
	private static final String GRAVITYBOX_EXPANDED_DESKTOP_MODE = "gravitybox_expanded_desktop_mode";

	public static final int STATUS_BAR_NOTIFICATION = 1;
	public static final int STATUS_BAR_SETTINGS = 2;

	private int originalDesktopStyle, originalDesktopMode, originalDesktopState, gravityboxOriginalDesktopState, gravityboxOriginalDesktopMode;
	private boolean originalStatusBarHidden;
	private Object systemServiceHandle;
	private static Method expandNotification, expandSettings;
	private ContentResolver resolver;
	

	public StatusBarController(Context context) {
		resolver = context.getContentResolver();
		systemServiceHandle = context.getSystemService("statusbar");
		try {
			Class<?> clazz = Class.forName("android.app.StatusBarManager");
			Class<?>[] emptyClass = new Class<?>[0];
			if (Build.VERSION.SDK_INT >= 17) {
				expandNotification = clazz.getMethod("expandNotificationsPanel", emptyClass);
				expandSettings = clazz.getMethod("expandSettingsPanel", emptyClass);
				expandSettings.setAccessible(true);
			} else
				expandNotification = clazz.getMethod("expand", emptyClass);
			expandNotification.setAccessible(true);
		} catch (ClassNotFoundException e) {
		} catch (NoSuchMethodException e) {
		}
	}

	public void overrideExpandedDesktopStyle() {
		int state = Settings.System.getInt(resolver, EXPANDED_DESKTOP_STATE, 0);
		if (state == 1) {
			originalDesktopStyle = 0;
			int style = Settings.System.getInt(resolver, EXPANDED_DESKTOP_STYLE, Integer.MAX_VALUE);
			boolean settedStyle = style != 0 && style != Integer.MAX_VALUE;
			if (settedStyle) {
				originalDesktopStyle = style;
				Settings.System.putInt(resolver, EXPANDED_DESKTOP_STYLE, 1);
			}
			originalDesktopMode = 0;
			int mode = Settings.System.getInt(resolver, EXPANDED_DESKTOP_MODE, Integer.MAX_VALUE);
			boolean settedMode = mode != 0 && mode != Integer.MAX_VALUE;
			if (settedMode) {
				originalDesktopMode = mode;
				Settings.System.putInt(resolver, EXPANDED_DESKTOP_MODE, 1);
			}
			originalDesktopState = state;
			if (settedMode && settedStyle) {
				Settings.System.putInt(resolver, EXPANDED_DESKTOP_STATE, 0);
			}
		}

		String statusbarHidden = Settings.System.getString(resolver, STATUS_BAR_HIDDEN);
		if (statusbarHidden != null)
			if (statusbarHidden.equals("1")) {
				originalStatusBarHidden = true;
				Settings.System.putString(resolver, STATUS_BAR_HIDDEN, "0");
			} else
				originalStatusBarHidden = false;

		int mode = Settings.System.getInt(resolver, GRAVITYBOX_EXPANDED_DESKTOP_MODE, 0);
		if (mode != 0) {
			gravityboxOriginalDesktopMode = mode;
			Settings.System.putInt(resolver, GRAVITYBOX_EXPANDED_DESKTOP_MODE, 2);
			state = Settings.System.getInt(resolver, GRAVITYBOX_EXPANDED_DESKTOP_STATE, 0);
			if (state == 1) {
				gravityboxOriginalDesktopState = state;
				Settings.System.putInt(resolver, GRAVITYBOX_EXPANDED_DESKTOP_STATE, 0);
			}
		}
	}

	public void setOriginalDesktopState() {
		if (originalDesktopStyle != 0)
			Settings.System.putInt(resolver, EXPANDED_DESKTOP_STYLE, originalDesktopStyle);
		if (originalDesktopMode != 0)
			Settings.System.putInt(resolver, EXPANDED_DESKTOP_MODE, originalDesktopMode);
		if (originalDesktopState != 0)
			Settings.System.putInt(resolver, EXPANDED_DESKTOP_STATE, originalDesktopState);
		if (gravityboxOriginalDesktopMode != 0)
			Settings.System.putInt(resolver, GRAVITYBOX_EXPANDED_DESKTOP_MODE, gravityboxOriginalDesktopMode);
		if (gravityboxOriginalDesktopState != 0)
			Settings.System.putInt(resolver, GRAVITYBOX_EXPANDED_DESKTOP_STATE, gravityboxOriginalDesktopState);
		if (originalStatusBarHidden)
			Settings.System.putString(resolver, STATUS_BAR_HIDDEN, "1");
	}

	public void showStatusBar(int bar) {
		try {
			if (bar == STATUS_BAR_SETTINGS) {
				//expandSettings.invoke(systemServiceHandle);
				expandSettings.invoke(systemServiceHandle);
			} else {
				//expandNotification.invoke(systemServiceHandle);
				expandNotification.invoke(systemServiceHandle);
			}
		} catch (InvocationTargetException e) {
		} catch (IllegalAccessException e) {
		} catch (Exception e) {
		}
	}
}
