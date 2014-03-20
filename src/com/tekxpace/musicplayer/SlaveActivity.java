package com.tekxpace.musicplayer;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.parse.Group;
import com.tekxpace.musicplayer.utility.Utility;

public class SlaveActivity extends Activity {
	private static final String LOG_TAG = "SlaveActivity";

	private Device mDevice = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		ParseAnalytics.trackAppOpened(getIntent());

		// Slave Device
		registerDevice("Device B");
	}

	private void registerParseInstallation(Device device) {
		ParseInstallation installation = ParseInstallation.getCurrentInstallation();
		installation.put("device", device);
		installation.saveInBackground();
	}

	private void sendPushNotification(Device slaveDevice, Device masterDevice) {
		// HashMap<String, String> hashMap = new HashMap<String, String>();
		// hashMap.put("slaveDeviceId", slaveDevice.getDeviceId());
		// hashMap.put("masterDeviceId", masterDevice.getDeviceId());
		// ParseCloud.callFunctionInBackground("push", hashMap, new
		// FunctionCallback<String>() {
		// public void done(String result, ParseException e) {
		// if (e == null) {
		// Log.i(LOG_TAG, result);
		// }
		// }
		// });

		try {
			JSONObject data = new JSONObject("{\"action\": \"com.tekxpace.musicplayer.UPDATE_STATUS\",\"name\": \"Vaughn\",\"newsItem\": \"Man bites dog\"}");
			// Create our Installation query
			ParseQuery pushQuery = ParseInstallation.getQuery();
			pushQuery.whereEqualTo("device", masterDevice);

			// Send push notification to query
			ParsePush push = new ParsePush();
			push.setQuery(pushQuery); // Set our Installation query
			push.setData(data);
			push.sendInBackground();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void registerDevice(final String mDeviceName) {
		final String deviceId = Utility.getUniqueDeviceId(this);
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Device");
		query.whereEqualTo("deviceId", deviceId);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> devices, ParseException e) {
				if (e == null) {

					if (devices.size() > 0) {
						Log.d(LOG_TAG, "Old user");
						mDevice = (Device) devices.get(0);
						registerParseInstallation(mDevice);

						// join the master group -- only for slave device
						joinGroup("Device A", mDevice);
					} else {
						// register new user
						mDevice = new Device();
						mDevice.setDeviceName(mDeviceName);
						mDevice.setDeviceId(deviceId);
						mDevice.saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									Log.d(LOG_TAG, "New user");
									registerParseInstallation(mDevice);
									joinGroup("Device A", mDevice);
								} else {
									Log.d(LOG_TAG, e.getMessage());
								}
							}
						});
					}

				} else {
					Log.d(LOG_TAG, "Error: " + e.getMessage());
				}
			}
		});
	}

	private void joinGroup(final String masterDeviceName, final Device slaveDevice) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Device");
		query.whereEqualTo("deviceName", masterDeviceName);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> devices, ParseException e) {
				if (e == null) {
					Log.d(LOG_TAG, "received " + devices.size() + " master devices");
					if (devices.size() > 0) {
						final Device masterDevice = (Device) devices.get(0);
						Group group = new Group();
						group.setMasterDevice(masterDevice);
						group.setSlaveDevice(slaveDevice);
						group.saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									Log.i(LOG_TAG, "group joined... sending push notification");
									sendPushNotification(slaveDevice, masterDevice);
								} else {
									e.printStackTrace();
								}
							}
						});
					} else {
						Toast.makeText(SlaveActivity.this, "No master device available", Toast.LENGTH_SHORT).show();
					}
				} else {
					Log.d(LOG_TAG, "Error: " + e.getMessage());
				}
			}
		});
	}

}
