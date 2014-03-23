package com.tekxpace.musicplayer;

import java.util.List;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.nsdchat.NsdChatActivity;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.model.ConnectionModel;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.utility.Utility;

public class MasterActivity extends NsdChatActivity {
	private static final String LOG_TAG = "MasterActivity";
	public static final String songObjectId = "3UljCjhopM";

	public static String slaveDeviceId = null;
	private MediaPlayer mediaPlayer = null;
	public static Device mDevice = null;
	private Device newDevice = null;

	public static TextView tvConnectionStatus;
	public static TextView tvDevice;
	public static Button btPlayPause;

	boolean mDeviceReady = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_master);
		ParseAnalytics.trackAppOpened(getIntent());

		tvDevice = (TextView) findViewById(R.id.textViewDevice);
		tvConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);
		btPlayPause = (Button) findViewById(R.id.buttonPlayPause);

		tvDevice.setText("Master device: Device A");

		// Master Device
		registerDevice("Device A");

		btPlayPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mDeviceReady) {
					String state = btPlayPause.getText().toString();
					int currentPlayBackPosition = mediaPlayer.getCurrentPosition();

					if (state.equalsIgnoreCase(Utility.STATUS_PLAY)) {
						btPlayPause.setText("Pause");
						// send play push
						playPause(true, currentPlayBackPosition);
					} else {
						btPlayPause.setText("Play");
						// send pause push
						playPause(false, currentPlayBackPosition);
					}
				} else {
					Log.d(LOG_TAG, "Master device not ready");
				}

			}
		});
	}

	private void playPause(boolean isPlay, int currentPosition) {
		// send notification to play the song to slave
		ConnectionModel play = new ConnectionModel();
		if (isPlay) {
			Utility.playMedia(mediaPlayer, currentPosition);
			play.status = Utility.STATUS_PLAY;
		} else {
			Utility.pauseMedia(mediaPlayer, currentPosition);
			play.status = Utility.STATUS_PAUSE;
		}

		play.playBackPosition = currentPosition;
		play.action = Utility.ACTION_UPDATE_STATUS;
		play.senderDeviceId = MasterActivity.mDevice.getDeviceId();
		play.senderDeviceName = MasterActivity.mDevice.getDeviceName();

		// notifying slave to play
		Utility.sendPushNotification(play.toJson(), slaveDeviceId);
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
									Utility.registerParseInstallation(newDevice);
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
								mediaPlayer = Utility.prepareMediaPlayer(MasterActivity.this, mediaPlayer, data);
								mDeviceReady = true;
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Utility.killMediaPlayer(mediaPlayer);
	}

}
