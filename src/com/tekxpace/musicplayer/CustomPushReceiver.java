package com.tekxpace.musicplayer;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.tekxpace.musicplayer.model.ConnectionModel;
import com.tekxpace.musicplayer.model.PayloadModel;
import com.tekxpace.musicplayer.utility.Utility;

public class CustomPushReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "PushReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "Notification received", Toast.LENGTH_SHORT).show();
		Log.d(LOG_TAG, "Notification received : CustomPushReceiver");
		try {
			String action = intent.getAction();
			String channel = intent.getExtras().getString("com.parse.Channel");
			JSONObject jsonObject = new JSONObject(intent.getExtras().getString("com.parse.Data"));

			// Log.d(LOG_TAG, "got action " + action + " on channel " + channel
			// + " with:");

			StringBuilder builder = new StringBuilder();

			Iterator itr = jsonObject.keys();
			while (itr.hasNext()) {
				String key = (String) itr.next();
				Log.d(LOG_TAG, "..." + key + " => " + jsonObject.getString(key));
				builder.append(jsonObject.getString(key) + " ");
			}

			if (action.equalsIgnoreCase(Utility.ACTION_UPDATE_STATUS)) {

				ConnectionModel connectionModel = ConnectionModel.fromJson(jsonObject.toString());

				if (MasterActivity.tvConnectionStatus != null) {
					MasterActivity.tvConnectionStatus.setText(connectionModel.senderDeviceName + " " + connectionModel.status);

					// send payload info using push to slave with objectId

					PayloadModel payloadModel = new PayloadModel();
					payloadModel.action = Utility.ACTION_PAYLOAD_INFO;
					payloadModel.senderDeviceId = MasterActivity.mDevice.getDeviceId();
					payloadModel.status = "Song info received";
					payloadModel.songObjectId = MasterActivity.songObjectId;

					String payloadJson = payloadModel.toJson();

					Utility.sendPushNotification(payloadJson, connectionModel.senderDeviceId);
				}
			} else if (action.equalsIgnoreCase(Utility.ACTION_PAYLOAD_INFO)) {

				PayloadModel payloadModel = PayloadModel.fromJson(jsonObject.toString());

				if (SlaveActivity.tvConnectionStatus != null) {
					SlaveActivity.tvConnectionStatus.setText(payloadModel.status);
				}
			}

		} catch (JSONException e) {
			Log.d(LOG_TAG, "JSONException: " + e.getMessage());
		}
	}
}