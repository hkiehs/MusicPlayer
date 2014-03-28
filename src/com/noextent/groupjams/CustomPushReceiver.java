package com.noextent.groupjams;
//package com.noextent.groupjams;
//
//import java.util.Iterator;
//import java.util.List;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.parse.FindCallback;
//import com.parse.GetDataCallback;
//import com.parse.ParseException;
//import com.parse.ParseFile;
//import com.parse.ParseObject;
//import com.parse.ParseQuery;
//import com.noextent.groupjams.model.ConnectionModel;
//import com.noextent.groupjams.model.PayloadModel;
//import com.noextent.groupjams.utility.Utility;
//
//public class CustomPushReceiver extends BroadcastReceiver {
//	private static final String LOG_TAG = "PushReceiver";
//
//	@Override
//	public void onReceive(Context context, Intent intent) {
//		Log.d(LOG_TAG, "End Time : " + System.currentTimeMillis());
//		final Context mContext = context;
//		Log.d(LOG_TAG, "Notification received : CustomPushReceiver");
//		try {
//			String action = intent.getAction();
//			String channel = intent.getExtras().getString("com.parse.Channel");
//			JSONObject jsonObject = new JSONObject(intent.getExtras().getString("com.parse.Data"));
//
//			// Log.d(LOG_TAG, "got action " + action + " on channel " + channel
//			// + " with:");
//
//			StringBuilder builder = new StringBuilder();
//			Iterator itr = jsonObject.keys();
//			while (itr.hasNext()) {
//				String key = (String) itr.next();
//				Log.d(LOG_TAG, "..." + key + " => " + jsonObject.getString(key));
//				builder.append(jsonObject.getString(key) + " ");
//			}
//
//			if (action.equalsIgnoreCase(Utility.ACTION_UPDATE_STATUS)) {
//				ConnectionModel connectionModel = ConnectionModel.fromJson(jsonObject.toString());
//
//				if (connectionModel.status.equalsIgnoreCase(Utility.STATUS_CONNECTED)) {
//					Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
//					if (MasterActivity.tvConnectionStatus != null) {
//						MasterActivity.tvConnectionStatus.setText(connectionModel.senderDeviceName + " " + connectionModel.status);
//
//						// send payload info using push to slave with objectId
//						PayloadModel payloadModel = new PayloadModel();
//						payloadModel.action = Utility.ACTION_PAYLOAD_INFO;
//						payloadModel.senderDeviceId = MasterActivity.mDevice.getDeviceId();
//						payloadModel.status = "Song info received";
//						payloadModel.songObjectId = MasterActivity.songObjectId;
//
//						String payloadJson = payloadModel.toJson();
//						Utility.sendPushNotification(payloadJson, connectionModel.senderDeviceId);
//					}
//				} else if (connectionModel.status.equalsIgnoreCase(Utility.STATUS_READY)) {
//					Toast.makeText(context, "Slave device Ready", Toast.LENGTH_SHORT).show();
//					MasterActivity.slaveDeviceId = connectionModel.senderDeviceId;
//					// enable master to send play request
//					MasterActivity.btPlayPause.setEnabled(true);
//				} else if (connectionModel.status.equalsIgnoreCase(Utility.STATUS_PLAY)) {
//					// ask devices to play
//					Utility.playMedia(SlaveActivity.mediaPlayer, connectionModel.playBackPosition);
//					// connectionUpdate(connectionModel);
//
//				} else if (connectionModel.status.equalsIgnoreCase(Utility.STATUS_PAUSE)) {
//					// ask devices to pause
//					Utility.pauseMedia(SlaveActivity.mediaPlayer, connectionModel.playBackPosition);
//					// connectionUpdate(connectionModel);
//
//				}
//			} else if (action.equalsIgnoreCase(Utility.ACTION_PAYLOAD_INFO)) {
//				PayloadModel payloadModel = PayloadModel.fromJson(jsonObject.toString());
//				Log.d(LOG_TAG, payloadModel.toJson());
//
//				if (SlaveActivity.tvConnectionStatus != null) {
//					SlaveActivity.tvConnectionStatus.setText(payloadModel.status);
//					receiveMediaFromServer(mContext, payloadModel.songObjectId, payloadModel.senderDeviceId);
//				}
//			}
//
//		} catch (JSONException e) {
//			Log.d(LOG_TAG, "JSONException: " + e.getMessage());
//		}
//	}
//
//	private void connectionUpdate(ConnectionModel connectionModel) {
//		ConnectionModel payloadModel = new ConnectionModel();
//		payloadModel.senderDeviceId = SlaveActivity.mDevice.getDeviceId();
//		payloadModel.action = Utility.ACTION_UPDATE_STATUS;
//		payloadModel.status = "Measure Round Trip Time [ " + System.currentTimeMillis() + "]";
//		Utility.sendPushNotification(payloadModel.toJson(), connectionModel.senderDeviceId);
//	}
//
//	private void receiveMediaFromServer(final Context context, String objectId, final String senderDeviceId) {
//		// request to receive file from server
//		ParseQuery<ParseObject> query = ParseQuery.getQuery("Media");
//		query.whereEqualTo(Utility.OBJECT_ID, objectId);
//		query.setTrace(true);
//		// query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
//		query.findInBackground(new FindCallback<ParseObject>() {
//			public void done(List<ParseObject> users, ParseException e) {
//				if (e == null) {
//					Log.d(LOG_TAG, "received " + users.size() + "song file");
//					ParseFile mediaFile = (ParseFile) users.get(0).get("mediaFile");
//					mediaFile.getDataInBackground(new GetDataCallback() {
//						public void done(byte[] data, ParseException e) {
//							if (e == null) {
//								Log.d(LOG_TAG, "song downloaded from server");
//								SlaveActivity.mediaPlayer = Utility.prepareMediaPlayer(context, SlaveActivity.mediaPlayer, data);
//
//								// notify master device that the slave is ready
//								// to play song
//								ConnectionModel ready = new ConnectionModel();
//								ready.status = Utility.STATUS_READY;
//								ready.action = Utility.ACTION_UPDATE_STATUS;
//								ready.senderDeviceId = SlaveActivity.mDevice.getDeviceId();
//								ready.senderDeviceName = SlaveActivity.mDevice.getDeviceName();
//
//								// notifying master for slave ready
//								Utility.sendPushNotification(ready.toJson(), senderDeviceId);
//							} else {
//								e.printStackTrace();
//							}
//						}
//					});
//				} else {
//					Log.d(LOG_TAG, "Error: " + e.getMessage());
//				}
//			}
//		});
//	}
//}