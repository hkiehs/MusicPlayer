package com.tekxpace.musicplayer;

import android.os.Bundle;

public class HomeActivity extends SlaveActivity {
	private static final String LOG_TAG = "HomeActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// master device should generate the channel
		// slave device should subscribe to that channel
	}
}