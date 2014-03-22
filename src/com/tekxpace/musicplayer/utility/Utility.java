package com.tekxpace.musicplayer.utility;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.PushService;
import com.tekxpace.musicplayer.HomeActivity;
import com.tekxpace.musicplayer.parse.Device;

public class Utility {

	private static final String DEVICE_ID = "deviceId";

	public static String getUniqueDeviceId(Context context) {
		final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

		UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		String deviceId = deviceUuid.toString();
		return deviceId;
	}

	public static String generateChannel(String objectId) {
		return "user_" + objectId;
	}

	public static void subscribeChannel(Context context, String objectId) {
		PushService.subscribe(context, objectId, HomeActivity.class);
	}

	public static void sendPushNotification(String json, Device destination) {
		try {
			JSONObject data = new JSONObject(json);
			// Create our Installation query
			ParseQuery<ParseInstallation> pushQuery = ParseInstallation.getQuery();
			pushQuery.whereEqualTo(DEVICE_ID, destination.getDeviceId());

			// Send push notification to query
			ParsePush push = new ParsePush();
			push.setQuery(pushQuery); // Set our Installation query
			push.setData(data);
			push.sendInBackground();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
