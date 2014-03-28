package com.noextent.groupjams.model;

import com.google.gson.Gson;

public class PayloadModel {

	public String status;
	public String songObjectId;
	public String senderDeviceId;
	public String action;

	public static PayloadModel fromJson(String json) {
		return new Gson().fromJson(json, PayloadModel.class);
	}

	public String toJson() {
		return new Gson().toJson(this);
	}
}
