package com.tekxpace.musicplayer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.utility.Utility;

public class HomeActivity extends Activity {
	private static final String LOG_TAG = "HomeActivity";

	private MediaPlayer mediaPlayer = null;
	private Device device = null;

	private void registerDevice() {
		final String deviceId = Utility.getUniqueDeviceId(this);
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Device");
		query.whereEqualTo("deviceId", deviceId);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> devices, ParseException e) {
				if (e == null) {
					Log.d(LOG_TAG, "Retrieved " + devices.size() + " devices");
					if (devices.size() > 0) {
						// device exists
						device = (Device) devices.get(0);
						Log.d(LOG_TAG, "DeviceId[" + device.getDeviceId() + "]");
					} else {
						// register user
						Device user = new Device();
						user.setDeviceName("A");
						user.setDeviceId(deviceId);
						user.saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									Log.d(LOG_TAG, "user registered");
								} else {
									Log.d(LOG_TAG, e.getMessage());
								}
							}
						});
					}
				} else {
					Log.d("score", "Error: " + e.getMessage());
				}
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		ParseAnalytics.trackAppOpened(getIntent());

		registerDevice();

		// request to receive file from server
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Media");
		query.whereEqualTo("username", "Sheikh Muneeb");
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
								playMedia(mediaPlayer);
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
				Log.d(LOG_TAG, "file deleted");
			}

			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
					Log.d(LOG_TAG, "file input stream closed");
				}
			}
		}
	}

	private void playMedia(MediaPlayer mediaPlayer) {
		Log.d(LOG_TAG, "Playing mp3");
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

	private void uploadFileToServer() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		try {
			AssetFileDescriptor afd = getAssets().openFd("test.mp3");
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

			ParseObject mediaObject = new ParseObject("Media");
			mediaObject.put("username", "Sheikh Muneeb");
			mediaObject.put("mediaFile", file);
			mediaObject.saveInBackground(new SaveCallback() {
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

	private void playAudio(String url) throws Exception {
		killMediaPlayer();
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(url);
		mediaPlayer.prepare();
		mediaPlayer.start();
	}
}
