package com.noextent.groupjams.activity;

import java.util.List;

import android.app.Dialog;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.noextent.groupjams.MusicPlayerApplication;
import com.noextent.groupjams.R;
import com.noextent.groupjams.chat.AllJoynService;
import com.noextent.groupjams.chat.DialogBuilder;
import com.noextent.groupjams.model.MediaModel;
import com.noextent.groupjams.parse.Device;
import com.noextent.groupjams.utility.DownloadInterface;
import com.noextent.groupjams.utility.Observable;
import com.noextent.groupjams.utility.Observer;
import com.noextent.groupjams.utility.RegisterInterface;
import com.noextent.groupjams.utility.SampleList;
import com.noextent.groupjams.utility.Utility;

public class PlayerActivity extends SherlockFragmentActivity implements Observer, RegisterInterface, DownloadInterface {
	private static final String LOG_TAG = "PlayerActivity";
	private static final int CONTENT_VIEW_ID = 666;

	ActionMode mMode;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);

		// This is a workaround for http://b.android.com/15340 from
		// http://stackoverflow.com/a/5852198/132047
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
			bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			getSupportActionBar().setBackgroundDrawable(bg);

			BitmapDrawable bgSplit = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped_split_img);
			bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
		}
		getSupportActionBar().setSubtitle("No group selected");

		mHistoryList = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
		ListView hlv = (ListView) findViewById(R.id.useHistoryList);
		hlv.setAdapter(mHistoryList);

		mJoinButton = (Button) findViewById(R.id.useJoin);
		mJoinButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_JOIN_ID);
			}
		});

		mLeaveButton = (Button) findViewById(R.id.useLeave);
		mLeaveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_LEAVE_ID);
			}
		});

		mActionButton = (Button) findViewById(R.id.btAction);

		if (!Utility.MASTER_BUILD) {
			mActionButton.setVisibility(View.INVISIBLE);
		}

		mActionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String mediaState = mActionButton.getText().toString();
				int position = mChatApplication.mMediaPlayer.getCurrentPosition();

				MediaModel mMediaModel = new MediaModel();
				mMediaModel.playBackPosition = position;

				if (mediaState.equalsIgnoreCase(Utility.STATUS_PLAY)) {
					mMediaModel.action = Utility.STATUS_PLAY;
					mChatApplication.newLocalUserMessage(mMediaModel.toJson());
					mActionButton.setText("Pause");
				} else {
					mMediaModel.action = Utility.STATUS_PAUSE;
					mChatApplication.newLocalUserMessage(mMediaModel.toJson());
					mActionButton.setText("Play");
				}

				final MediaModel mediaModel = mMediaModel;
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mediaModel.action.equalsIgnoreCase(Utility.STATUS_PLAY)) {
							Utility.playMedia(mChatApplication.mMediaPlayer, mediaModel.playBackPosition);
						} else if (mediaModel.action.equalsIgnoreCase(Utility.STATUS_PAUSE)) {
							Utility.pauseMedia(mChatApplication.mMediaPlayer, mediaModel.playBackPosition);
						}
					}
				}, 300);
			}
		});

		mChannelName = (TextView) findViewById(R.id.useChannelName);
		mChannelStatus = (TextView) findViewById(R.id.useChannelStatus);

		/*
		 * Keep a pointer to the Android Appliation class around. We use this as
		 * the Model for our MVC-based application. Whenever we are started we
		 * need to "check in" with the application so it can ensure that our
		 * required services are running.
		 */
		mChatApplication = (MusicPlayerApplication) getApplication();
		mChatApplication.checkin();

		/*
		 * Call down into the model to get its current state. Since the model
		 * outlives its Activities, this may actually be a lot of state and not
		 * just empty.
		 */
		updateChannelState();
		updateHistory();

		mJoinButton.setEnabled(false);

		/*
		 * Now that we're all ready to go, we are ready to accept notifications
		 * from other components.
		 */
		mChatApplication.addObserver(this);

		Utility.registerDevice(this, "Device_A", this);
	}

	public void onDestroy() {
		Log.i(LOG_TAG, "onDestroy()");
		mChatApplication = (MusicPlayerApplication) getApplication();
		mChatApplication.deleteObserver(this);
		super.onDestroy();
	}

	public static final int DIALOG_JOIN_ID = 0;
	public static final int DIALOG_LEAVE_ID = 1;
	public static final int DIALOG_ALLJOYN_ERROR_ID = 2;

	protected Dialog onCreateDialog(int id) {
		Log.i(LOG_TAG, "onCreateDialog()");
		Dialog result = null;
		switch (id) {
			case DIALOG_JOIN_ID : {
				DialogBuilder builder = new DialogBuilder();
				result = builder.createUseJoinDialog(this, mChatApplication);
			}
				break;
			case DIALOG_LEAVE_ID : {
				DialogBuilder builder = new DialogBuilder();
				result = builder.createUseLeaveDialog(this, mChatApplication);
			}
				break;
			case DIALOG_ALLJOYN_ERROR_ID : {
				DialogBuilder builder = new DialogBuilder();
				result = builder.createAllJoynErrorDialog(this, mChatApplication);
			}
				break;
		}
		return result;
	}

	public synchronized void update(Observable o, Object arg) {
		Log.i(LOG_TAG, "update(" + arg + ")");
		String qualifier = (String) arg;

		if (qualifier.equals(MusicPlayerApplication.APPLICATION_QUIT_EVENT)) {
			Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
			mHandler.sendMessage(message);
		}

		if (qualifier.equals(MusicPlayerApplication.HISTORY_CHANGED_EVENT)) {
			Message message = mHandler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
			mHandler.sendMessage(message);
		}

		if (qualifier.equals(MusicPlayerApplication.USE_CHANNEL_STATE_CHANGED_EVENT)) {
			Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
			mHandler.sendMessage(message);
		}

		if (qualifier.equals(MusicPlayerApplication.ALLJOYN_ERROR_EVENT)) {
			Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
			mHandler.sendMessage(message);
		}
	}

	private void updateHistory() {
		Log.i(LOG_TAG, "updateHistory()");
		// mHistoryList.clear();
		List<String> messages = mChatApplication.getHistory();
		for (String message : messages) {
			mHistoryList.add(message);
		}
		mHistoryList.notifyDataSetChanged();
	}
	private void updateChannelState() {
		Log.i(LOG_TAG, "updateHistory()");
		AllJoynService.UseChannelState channelState = mChatApplication.useGetChannelState();
		String name = mChatApplication.useGetChannelName();
		if (name == null) {
			name = "Not set";
		}
		mChannelName.setText(name);

		switch (channelState) {
			case IDLE :
				mChannelStatus.setText("Idle");
				mJoinButton.setEnabled(true);
				mLeaveButton.setEnabled(false);
				break;
			case JOINED :
				mChannelStatus.setText("Joined");
				mJoinButton.setEnabled(false);
				mLeaveButton.setEnabled(true);
				break;
		}
	}

	/**
	 * An AllJoyn error has happened. Since this activity pops up first we
	 * handle the general errors. We also handle our own errors.
	 */
	private void alljoynError() {
		if (mChatApplication.getErrorModule() == MusicPlayerApplication.Module.GENERAL
				|| mChatApplication.getErrorModule() == MusicPlayerApplication.Module.USE) {
			showDialog(DIALOG_ALLJOYN_ERROR_ID);
		}
	}

	private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
	private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
	private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 2;
	private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case HANDLE_APPLICATION_QUIT_EVENT : {
					Log.i(LOG_TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
					finish();
				}
					break;
				case HANDLE_HISTORY_CHANGED_EVENT : {
					Log.i(LOG_TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
					updateHistory();
					break;
				}
				case HANDLE_CHANNEL_STATE_CHANGED_EVENT : {
					Log.i(LOG_TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
					updateChannelState();
					break;
				}
				case HANDLE_ALLJOYN_ERROR_EVENT : {
					Log.i(LOG_TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
					alljoynError();
					break;
				}
				default :
					break;
			}
		}
	};

	private void addMessageToList(String msg) {
		mHistoryList.add(msg);
		mHistoryList.notifyDataSetChanged();
	}

	private MusicPlayerApplication mChatApplication = null;
	private ArrayAdapter<String> mHistoryList;
	private Button mJoinButton;
	private Button mLeaveButton;
	private Button mActionButton;
	private TextView mChannelName;
	private TextView mChannelStatus;

	@Override
	public void onRegistrationComplete(Device device) {
		if (device != null) {
			addMessageToList("Registration successful");
			mChatApplication.mDevice = device;
			Utility.registerParseInstallation(device);
			// upload file to server

			// INFO: now user should be able to host and retrieve songs from
			// server send download instruction on the group and download it
			// locally

			Utility.receiveMediaFromServer(device, Utility.songObjectId, this);
			addMessageToList("receiving song... from server");
		} else {
			addMessageToList("Registration un-successful");
			// Registration failed
			// show a retry button on the UI to retry registration
			// User can not proceed further without this step only if he has
			// something to from/to server. Else all functionalities should be
			// accessible.
		}
	}

	@Override
	public void onDownloadSuccess(byte[] data) {
		if (data != null) {
			addMessageToList("song downloaded successfully");
			mChatApplication.mMediaPlayer = Utility.prepareMediaPlayer(PlayerActivity.this, mChatApplication.mMediaPlayer, data);
			mJoinButton.setEnabled(true);
		} else {
			addMessageToList("song download un-successful");
			// show retry button on UI
			// should not allow this guy to play the song as he can not
			// participate now
		}
	}

	/**
	 * Setting up Action Bar Items
	 * 
	 */

	private final class AnActionModeOfEpicProportions implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Used to put dark icons on light action bar
			boolean isLight = SampleList.THEME == R.style.Theme_Sherlock_Light;

			menu.add("Save").setIcon(isLight ? R.drawable.ic_compose_inverse : R.drawable.ic_compose)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			menu.add("Search").setIcon(isLight ? R.drawable.ic_search_inverse : R.drawable.ic_search)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			menu.add("Refresh").setIcon(isLight ? R.drawable.ic_refresh_inverse : R.drawable.ic_refresh)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			menu.add("Save").setIcon(isLight ? R.drawable.ic_compose_inverse : R.drawable.ic_compose)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			menu.add("Search").setIcon(isLight ? R.drawable.ic_search_inverse : R.drawable.ic_search)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			menu.add("Refresh").setIcon(isLight ? R.drawable.ic_refresh_inverse : R.drawable.ic_refresh)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Toast.makeText(PlayerActivity.this, "Got click: " + item, Toast.LENGTH_SHORT).show();
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// This uses the imported MenuItem from ActionBarSherlock
		Toast.makeText(this, "Got click: " + item.toString(), Toast.LENGTH_SHORT).show();
		mMode = startActionMode(new AnActionModeOfEpicProportions());
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}
}
