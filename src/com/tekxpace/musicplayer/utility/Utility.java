package com.tekxpace.musicplayer.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;
import com.parse.PushService;
import com.parse.SaveCallback;
import com.tekxpace.musicplayer.activity.UseActivity;
import com.tekxpace.musicplayer.model.MediaModel;
import com.tekxpace.musicplayer.parse.Device;
import com.tekxpace.musicplayer.parse.Media;

public class Utility {
	private final static String LOG_TAG = "Utility";
	public static final String songObjectId = "3UljCjhopM";

	public final static String USER_DEVICE = "userDevice";
	public final static String OBJECT_ID = "objectId";

	public static final String ACTION_UPDATE_STATUS = "com.tekxpace.musicplayer.UPDATE_STATUS";
	public static final String ACTION_PAYLOAD_INFO = "com.tekxpace.musicplayer.PAYLOAD_INFO";

	public static final String STATUS_CONNECTED = "connected";
	public static final String STATUS_READY = "ready";
	public static final String STATUS_PLAY = "play";
	public static final String STATUS_PAUSE = "pause";

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
		PushService.subscribe(context, objectId, UseActivity.class);
	}

	public static void sendPushNotification(String json, String deviceId) {
		try {
			JSONObject data = new JSONObject(json);
			// Create our Installation query
			ParseQuery<ParseInstallation> pushQuery = ParseInstallation.getQuery();
			pushQuery.whereEqualTo(Device.DEVICE_ID, deviceId);

			// Send push notification to query
			ParsePush push = new ParsePush();
			push.setQuery(pushQuery); // Set our Installation query
			push.setData(data);
			push.sendInBackground();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static void registerParseInstallation(Device device) {
		ParseInstallation installation = ParseInstallation.getCurrentInstallation();
		installation.put(Utility.USER_DEVICE, device);
		installation.put(Device.DEVICE_ID, device.getDeviceId());
		installation.saveInBackground();
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

	public static MediaPlayer prepareMediaPlayer(Context context, MediaPlayer mediaPlayer, byte[] mp3SoundByteArray) {
		File tempMp3 = null;
		FileInputStream fis = null;
		try {
			// create temp file that will hold byte array
			tempMp3 = File.createTempFile("test", "mp3", context.getCacheDir());
			tempMp3.deleteOnExit();

			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(mp3SoundByteArray);
			fos.close();

			fis = new FileInputStream(tempMp3);
			// Tried reusing instance of media player
			// but that resulted in system crashes...
			killMediaPlayer(mediaPlayer);
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

	public static void playMedia(MediaPlayer mediaPlayer, int playBackPosition) {
		if (mediaPlayer != null) {
			mediaPlayer.seekTo(playBackPosition);
			mediaPlayer.start();
		}
	}

	public static void pauseMedia(MediaPlayer mediaPlayer, int playBackPosition) {
		if (mediaPlayer != null) {
			mediaPlayer.pause();
			mediaPlayer.seekTo(playBackPosition);
		}
	}


	public static void killMediaPlayer(MediaPlayer mediaPlayer) {
		if (mediaPlayer != null) {
			try {
				mediaPlayer.release();
				mediaPlayer = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void playAudio(MediaPlayer mediaPlayer, String url) throws Exception {
		killMediaPlayer(mediaPlayer);
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(url);
		mediaPlayer.prepare();
		mediaPlayer.start();
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

	static Device newDevice = null;
	public static void registerDevice(Context context, final String mDeviceName, final RegisterInterface registerInterface) {
		final String deviceId = getUniqueDeviceId(context);
		ParseQuery<ParseObject> query = ParseQuery.getQuery(Device.TABLE);
		query.whereEqualTo(Device.DEVICE_ID, deviceId);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> devices, ParseException e) {
				if (e == null) {
					if (devices.size() > 0) {
						Log.d(LOG_TAG, "Old user");
						registerInterface.onRegistrationComplete((Device) devices.get(0));
						// Utility.registerParseInstallation(mApplication.mDevice);
						// receiveMediaFromServer(mDevice, songObjectId);
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
									registerInterface.onRegistrationComplete(newDevice);
									newDevice = null;
									// Utility.registerParseInstallation(newDevice);
									// receiveMediaFromServer(newDevice,
									// songObjectId);
									// Utility.uploadFileToServer(MasterActivity.this,
									// newDevice);
								} else {
									registerInterface.onRegistrationComplete(null);
									Log.d(LOG_TAG, e.getMessage());
								}
							}
						});
					}

				} else {
					registerInterface.onRegistrationComplete(null);
					Log.d(LOG_TAG, "Error: " + e.getMessage());
				}
			}
		});
	}

	public static void receiveMediaFromServer(Device device, String objectId, final DownloadInterface downloadInterface) {
		// request to receive file from server
		ParseQuery<ParseObject> query = ParseQuery.getQuery(Media.TABLE);
		query.whereEqualTo(Utility.OBJECT_ID, objectId);
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> users, ParseException e) {
				if (e == null) {
					Log.d(LOG_TAG, "Retrieved " + users.size() + " files");
					ParseFile mediaFile = (ParseFile) users.get(0).get(Media.MEDIA_FILE);
					mediaFile.getDataInBackground(new GetDataCallback() {
						public void done(byte[] data, ParseException e) {
							if (e == null) {
								Log.d(LOG_TAG, "Data received from server");
								downloadInterface.onDownloadSuccess(data);
							} else {
								downloadInterface.onDownloadSuccess(null);
								e.printStackTrace();
							}
						}
					});
				} else {
					downloadInterface.onDownloadSuccess(null);
					Log.d(LOG_TAG, "Error: " + e.getMessage());
				}
			}
		});
	}

}
