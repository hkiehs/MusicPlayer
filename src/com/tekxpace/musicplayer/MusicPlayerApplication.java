package com.tekxpace.musicplayer;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.PushService;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.parse.Group;

public class MusicPlayerApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		ParseObject.registerSubclass(Device.class);
		ParseObject.registerSubclass(Group.class);

		Parse.initialize(this, "nrXYlTqKzxmp0mSynnblthvX5HcwdNykXTNoekWs", "lNCLSapoIIUCVylghKSR0lT4Gvb78FEo7kjHToSd");
		PushService.setDefaultPushCallback(this, HomeActivity.class);
		Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
		// PushService.setDefaultPushCallback(this, HomeActivity.class);
		// ParseUser.enableAutomaticUser();

		/*
		 * For more information on app security and Parse ACL:
		 * https://www.parse.com/docs/android_guide#security-recommendations
		 */
		ParseACL defaultACL = new ParseACL();

		// If you would like all objects to be private by default, remove this
		// line.
		defaultACL.setPublicReadAccess(true);

		ParseACL.setDefaultACL(defaultACL, true);
	}
}
