package com.noextent.groupjams.model;

import com.google.gson.Gson;

public class MediaModel {

	public String action;
	public int playBackPosition;

	public static MediaModel fromJson(String json) {
		return new Gson().fromJson(json, MediaModel.class);
	}

	public String toJson() {
		return new Gson().toJson(this);
	}
}
