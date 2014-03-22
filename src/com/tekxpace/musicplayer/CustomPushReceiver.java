package com.tekxpace.musicplayer;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.tekxpace.musicplayer.model.ConnectionModel;

public class CustomPushReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "PushReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "Notification received", Toast.LENGTH_SHORT).show();
		Log.d(LOG_TAG, "Notification received : CustomPushReceiver");
		try {
			String action = intent.getAction();
			String channel = intent.getExtras().getString("com.parse.Channel");
			JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));

			Log.d(LOG_TAG, "got action " + action + " on channel " + channel + " with:");

			// StringBuilder builder = new StringBuilder();
			//
			// Iterator itr = json.keys();
			// while (itr.hasNext()) {
			// String key = (String) itr.next();
			// Log.d(LOG_TAG, "..." + key + " => " + json.getString(key));
			// builder.append(json.getString(key) + " ");
			// }

			ConnectionModel connectionModel = ConnectionModel.fromJson(json.toString());
			if (MasterActivity.tvConnectionStatus != null) {
				MasterActivity.tvConnectionStatus.setText(connectionModel.deviceName + " " + connectionModel.status);
				
				
				
				
			}

		} catch (JSONException e) {
			Log.d(LOG_TAG, "JSONException: " + e.getMessage());
		}
	}
}