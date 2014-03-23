package com.tekxpace.musicplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.utility.Utility;

public class MasterActivity extends Activity {
	private static final String LOG_TAG = "MasterActivity";
	public static final String songObjectId = "3UljCjhopM";

	private MediaPlayer mediaPlayer = null;
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

		tvDevice.setText("Master device: Device A");

		// Master Device
		registerDevice("Device A");

	}

	private void registerParseInstallation(Device device) {
		ParseInstallation installation = ParseInstallation.getCurrentInstallation();
		installation.put(Utility.USER_DEVICE, device);
		installation.saveInBackground();
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
						registerParseInstallation(mDevice);
						receiveMediaFromServer(mDevice, songObjectId);
						// Utility.uploadFileToServer(MasterActivity.this,
						// mDevice);
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
									registerParseInstallation(newDevice);
									receiveMediaFromServer(newDevice, songObjectId);
									// Utility.uploadFileToServer(MasterActivity.this,
									// newDevice);
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

	private void receiveMediaFromServer(Device device, String objectId) {
		// request to receive file from server
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Media");
		query.whereEqualTo(Utility.USER_DEVICE, device);
		query.whereEqualTo(Utility.OBJECT_ID, objectId);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> users, ParseException e) {
				if (e == null) {
					Log.d(LOG_TAG, "Retrieved " + users.size() + " files");
					ParseFile mediaFile = (ParseFile) users.get(0).get("mediaFile");
					mediaFile.getDataInBackground(new GetDataCallback() {
						public void done(byte[] data, ParseException e) {
							if (e == null) {
								Log.d(LOG_TAG, "Data received from server");
								mediaPlayer = prepareMediaPlayer(data);
								// playMedia(mediaPlayer);
							} else {
								e.printStackTrace();
							}
						}
					});
				} else {
					Log.d(LOG_TAG, "Error: " + e.getMessage());
				}
			}
		});
	}

	// private void sendPushNotification(String slaveDeviceId, String
	// masterDeviceId) {
	// HashMap<String, String> hashMap = new HashMap<String, String>();
	// hashMap.put("slaveDeviceId", slaveDeviceId);
	// hashMap.put("masterDeviceId", masterDeviceId);
	// ParseCloud.callFunctionInBackground("push", hashMap, new
	// FunctionCallback<String>() {
	// public void done(String result, ParseException e) {
	// if (e == null) {
	// Log.i(LOG_TAG, result);
	// }
	// }
	// });
	// }

	private MediaPlayer prepareMediaPlayer(byte[] mp3SoundByteArray) {
		File tempMp3 = null;
		FileInputStream fis = null;
		try {
			// create temp file that will hold byte array
			tempMp3 = File.createTempFile("test", "mp3", getCacheDir());
			tempMp3.deleteOnExit();

			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(mp3SoundByteArray);
			fos.close();

			fis = new FileInputStream(tempMp3);
			// Tried reusing instance of media player
			// but that resulted in system crashes...
			killMediaPlayer();
			mediaPlayer = new MediaPlayer();
			// Tried passing path directly, but kept getting
			// "Prepare failed.: status=0x1"
			// so using file descriptor instead
			mediaPlayer.setDataSource(fis.getFD());
			mediaPlayer.prepare();
			return mediaPlayer;
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
			return null;
		} finally {
			if (tempMp3 != null) {
				tempMp3.delete();
				Log.d(LOG_TAG, "File deleted");
			}

			if (fis != null) {
				try {
					fis.close();
					Log.d(LOG_TAG, "InputStream closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void playMedia(MediaPlayer mediaPlayer) {
		Log.d(LOG_TAG, "Playing media");
		if (mediaPlayer != null)
			mediaPlayer.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		killMediaPlayer();
	}

	private void killMediaPlayer() {
		if (mediaPlayer != null) {
			try {
				mediaPlayer.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void playAudio(String url) throws Exception {
		killMediaPlayer();
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(url);
		mediaPlayer.prepare();
		mediaPlayer.start();
	}
}
