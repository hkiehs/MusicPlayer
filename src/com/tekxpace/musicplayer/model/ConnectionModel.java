package com.tekxpace.musicplayer.model;

import com.google.gson.Gson;

public class ConnectionModel {

	public String status;
	public String senderDeviceName;
	public String senderDeviceId;
	public String action;

	public static ConnectionModel fromJson(String json) {
		return new Gson().fromJson(json, ConnectionModel.class);
	}

	public String toJson() {
		return new Gson().toJson(this);
	}
}
