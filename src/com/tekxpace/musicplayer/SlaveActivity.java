package com.tekxpace.musicplayer;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.model.ConnectionModel;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.parse.Group;
import com.tekxpace.musicplayer.utility.Utility;

public class SlaveActivity extends Activity {
	private static final String LOG_TAG = "SlaveActivity";

	private Device newDevice = null;
	private Device mDevice = null;
	public static TextView tvDevice, tvConnectionStatus;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		ParseAnalytics.trackAppOpened(getIntent());

		tvDevice = (TextView) findViewById(R.id.textViewDevice);
		tvConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);

		tvDevice.setText("Slave device: Device B");

		// Slave Device
		registerDevice("Device B");
	}

	private void registerDevice(final String mDeviceName) {
		final String deviceId = Utility.getUniqueDeviceId(this);
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Device");
		query.whereEqualTo(Device.DEVICE_ID, deviceId);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> devices, ParseException e) {
				if (e == null) {

					if (devices.size() > 0) {
						Log.d(LOG_TAG, "Old user");
						mDevice = (Device) devices.get(0);
						Utility.registerParseInstallation(mDevice);

						// join the master group -- only for slave device
						joinGroup("Device A", mDevice);
					} else {
						// register new user
						newDevice = new Device();
						newDevice.setDeviceName(mDeviceName);
						newDevice.setDeviceId(deviceId);
						newDevice.saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									Log.d(LOG_TAG, "New user");
									Utility.registerParseInstallation(newDevice);
									joinGroup("Device A", newDevice);
									mDevice = newDevice;
									newDevice = null;
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

									ConnectionModel connectionModel = new ConnectionModel();
									connectionModel.senderDeviceName = slaveDevice.getDeviceName();
									connectionModel.senderDeviceId = slaveDevice.getDeviceId();
									connectionModel.status = "Connected";
									connectionModel.action = Utility.ACTION_UPDATE_STATUS;

									Utility.sendPushNotification(connectionModel.toJson(), masterDevice.getDeviceId());
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
