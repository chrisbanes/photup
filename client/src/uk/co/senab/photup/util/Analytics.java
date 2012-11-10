package uk.co.senab.photup.util;

import android.content.Context;

public class Analytics {

	public static final String EVENT_PHOTO_RESET = "photo_viewer_reset";
	public static final String EVENT_PHOTO_FILTERS = "photo_viewer_filters";
	public static final String EVENT_PHOTO_CROP = "photo_viewer_crop";
	public static final String EVENT_PHOTO_ROTATE = "photo_viewer_rotate";
	public static final String EVENT_PHOTO_CAPTION = "photo_viewer_caption";
	public static final String EVENT_PHOTO_PLACE = "photo_viewer_place";

	public static void onStartSession(Context context) {
		//
	}

	public static void onEndSession(Context context) {
		//
	}

	public static void logEvent(final String event) {
		//
	}

}
