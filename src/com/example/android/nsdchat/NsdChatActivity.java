/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.nsdchat;

import android.app.Activity;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NsdChatActivity extends Activity {
	public static final String TAG = "NsdChat";
	public static NsdChatActivity activity;

	private Handler mUpdateHandler;
	ChatConnection mConnection;
	NsdHelper mNsdHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;

		mUpdateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String chatLine = msg.getData().getString("msg");
				Log.i(TAG, "Received Msg [" + chatLine + "]");
			}
		};

		mConnection = new ChatConnection(mUpdateHandler);
		mNsdHelper = new NsdHelper(this);
		mNsdHelper.initializeNsd();
	}

	public void clickAdvertise() {
		// Register service
		if (mConnection.getLocalPort() > -1) {
			mNsdHelper.registerService(mConnection.getLocalPort());
		} else {
			Log.d(TAG, "ServerSocket isn't bound.");
		}
		Log.d(TAG, "Master registered");
	}

	public void clickDiscover() {
		mNsdHelper.discoverServices();
	}

	public void clickConnect() {
		NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
		if (service != null) {
			Log.d(TAG, "Connecting.");
			mConnection.connectToServer(service.getHost(), service.getPort());
		} else {
			Log.d(TAG, "No service to connect to!");
		}
	}

	public void clickSend(String message) {
		if (!message.isEmpty()) {
			mConnection.sendMessage(message);
		}
	}

	@Override
	protected void onPause() {
		if (mNsdHelper != null) {
			mNsdHelper.stopDiscovery();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mNsdHelper != null) {
			mNsdHelper.discoverServices();
			Log.d(TAG, "Master started discovery");
		}
	}

	@Override
	protected void onDestroy() {
		mNsdHelper.tearDown();
		mConnection.tearDown();
		super.onDestroy();
	}

}
