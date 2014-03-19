package com.tekxpace.musicplayer.parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/*
 * An extension of ParseObject that makes
 * it more convenient to access information
 * about a given Device 
 */

@ParseClassName("Group")
public class Group extends ParseObject {

	public Group() {
		// A default constructor is required.
	}

	public void setSlaveDevice(Device device) {
		put("slaveDevice", device);
	}

	public Device getSlaveDevice() {
		return (Device) getParseObject("slaveDevice");
	}

	public void setMasterDevice(Device device) {
		put("masterDevice", device);
	}

	public Device getMasterDevice() {
		return (Device) getParseObject("masterDevice");
	}
}
