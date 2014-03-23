package com.tekxpace.musicplayer.parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/*
 * An extension of ParseObject that makes
 * it more convenient to access information
 * about a given Device 
 */

@ParseClassName("Device")
public class Device extends ParseObject {
	public static final String DEVICE_ID = "deviceId";

	public Device() {
		// A default constructor is required.
	}

	public String getDeviceName() {
		return getString("deviceName");
	}

	public void setDeviceName(String title) {
		put("deviceName", title);
	}

	public String getDeviceId() {
		return getString("deviceId");
	}

	public void setDeviceId(String deviceId) {
		put("deviceId", deviceId);
	}
}
