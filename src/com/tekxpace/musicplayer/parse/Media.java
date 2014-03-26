package com.tekxpace.musicplayer.parse;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.tekxpace.musicplayer.utility.Utility;

@ParseClassName("Media")
public class Media extends ParseObject {
	public static final String TABLE = "Media";
	public static final String SONG_NAME = "songName";
	public static final String ARTIST_NAME = "artistName";
	public static final String ALBUM_NAME = "albumName";
	public static final String MEDIA_FILE = "mediaFile";

	public Media() {
		// A default constructor is required.
	}

	public void setArtistName(String artistName) {
		put(ARTIST_NAME, artistName);
	}

	public String getArtistName() {
		return getString(ARTIST_NAME);
	}

	public void setAlbumName(String albumName) {
		put(ALBUM_NAME, albumName);
	}

	public String getAlbumName() {
		return getString(ALBUM_NAME);
	}

	public void setSongName(String songName) {
		put(SONG_NAME, songName);
	}

	public String getSongName() {
		return getString(SONG_NAME);
	}

	public void setMediaFile(ParseFile parseFile) {
		put(MEDIA_FILE, parseFile);
	}

	public ParseFile getMediaFile() {
		return getParseFile(MEDIA_FILE);
	}

	public void setDeviceId(Device device) {
		put(Utility.USER_DEVICE, device);
	}

	public Device getDeviceId() {
		return (Device) getParseObject(Utility.USER_DEVICE);
	}
}
