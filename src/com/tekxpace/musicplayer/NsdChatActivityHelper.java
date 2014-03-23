package com.tekxpace.musicplayer;

import com.example.android.nsdchat.NsdChatActivity;

public class NsdChatActivityHelper {
	NsdChatActivity nsdChatActivity;
	public NsdChatActivityHelper(NsdChatActivity nsdChatActivity) {
		this.nsdChatActivity = nsdChatActivity;
	}

	public void register() {
		nsdChatActivity.clickAdvertise();
	}

	public void connect() {
		nsdChatActivity.clickConnect();
	}
}