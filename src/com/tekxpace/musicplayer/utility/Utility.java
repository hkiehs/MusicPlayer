package com.tekxpace.musicplayer.utility;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;
import com.parse.PushService;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.HomeActivity;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.parse.Media;

public class Utility {
	private final static String LOG_TAG = "Utility";
	public final static String USER_DEVICE = "userDevice";
	public final static String OBJECT_ID = "objectId";

	public static final String ACTION_UPDATE_STATUS = "com.tekxpace.musicplayer.UPDATE_STATUS";
	public static final String ACTION_PAYLOAD_INFO = "com.tekxpace.musicplayer.PAYLOAD_INFO";

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
			pushQuery.whereEqualTo(USER_DEVICE, destination);

			// Send push notification to query
			ParsePush push = new ParsePush();
			push.setQuery(pushQuery); // Set our Installation query
			push.setData(data);
			push.sendInBackground();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static void uploadFileToServer(Context context, Device device) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		try {
			AssetFileDescriptor afd = context.getAssets().openFd("test.mp3");
			FileInputStream fis = afd.createInputStream();

			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				// Writes to this byte array output stream
				bos.write(buf, 0, readNum);
				// System.out.println("read " + readNum + " bytes,");
			}
			afd.close();

			byte[] bytes = bos.toByteArray();

			// save file to the server
			ParseFile file = new ParseFile("test.mp3", bytes);
			file.saveInBackground(new SaveCallback() {
				@Override
				public void done(ParseException e) {
					if (e == null)
						Log.i(LOG_TAG, "file saved");
					else
						e.printStackTrace();
				}
			}, new ProgressCallback() {
				public void done(Integer percentDone) {
					if (percentDone != null) {
						Log.i(LOG_TAG, "progress [" + percentDone + "]");
					}
				}
			});

			Media media = new Media();
			media.setSongName("Remix");
			media.setAlbumName("Remix Songs");
			media.setArtistName("Atif Aslam");
			media.setMediaFile(file);
			media.setDeviceId(device);

			media.saveInBackground(new SaveCallback() {
				@Override
				public void done(ParseException e) {
					if (e == null) {
						Log.i(LOG_TAG, "file referenced");
					} else {
						e.printStackTrace();
					}
				}
			});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
