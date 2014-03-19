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

public class HomeActivity extends Activity {
	private static final String LOG_TAG = "HomeActivity";

	private MediaPlayer mediaPlayer = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		ParseAnalytics.trackAppOpened(getIntent());

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
						ParseQuery<ParseObject> query = ParseQuery.getQuery("Media");
						query.whereEqualTo("username", "Sheikh Muneeb");

						query.findInBackground(new FindCallback<ParseObject>() {
							public void done(List<ParseObject> users, ParseException e) {
								// request to receive a file
								if (e == null) {
									Log.d(LOG_TAG, "Retrieved " + users.size() + " users");
									ParseFile mediaFile = (ParseFile) users.get(0).get("mediaFile");
									mediaFile.getDataInBackground(new GetDataCallback() {
										public void done(byte[] data, ParseException e) {
											if (e == null) {
												Log.d(LOG_TAG, "Data received from server... Playing mp3");
												playMp3(data);
											} else {
												e.printStackTrace();
											}
										}
									});
									// Log.d(LOG_TAG, "File URL: " +
									// mediaFile.getUrl());
									// try {
									// playAudio(mediaFile.getUrl());
									// } catch (Exception e1) {
									// e1.printStackTrace();
									// }
								} else {
									Log.d(LOG_TAG, "Error: " + e.getMessage());
								}
							}
						});
					} else {
						e.printStackTrace();
					}
				}
			});

			// MediaPlayer player = new MediaPlayer();
			// player.setDataSource(afd.getFileDescriptor(),
			// afd.getStartOffset(), afd.getLength());
			// player.prepare();
			// player.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create FileInputStream which obtains input bytes from a file in a
		// file system
		// FileInputStream is meant for reading streams of raw bytes such as
		// image data. For reading streams of characters, consider using
		// FileReader.

	}

	private void playMp3(byte[] mp3SoundByteArray) {
		File tempMp3 = null;
		FileInputStream fis = null;
		try {
			// create temp file that will hold byte array
			tempMp3 = File.createTempFile("test", "mp3", getCacheDir());
			tempMp3.deleteOnExit();

			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(mp3SoundByteArray);
			fos.close();

			// Tried reusing instance of media player
			// but that resulted in system crashes...
			MediaPlayer mediaPlayer = new MediaPlayer();

			// Tried passing path directly, but kept getting
			// "Prepare failed.: status=0x1"
			// so using file descriptor instead
			fis = new FileInputStream(tempMp3);
			mediaPlayer.setDataSource(fis.getFD());

			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
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
	private void playAudio(String url) throws Exception {
		killMediaPlayer();
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(url);
		mediaPlayer.prepare();
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
}
