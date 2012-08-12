package uk.co.senab.photup.util;

import uk.co.senab.photup.Constants;
import android.content.Context;

import com.flurry.android.FlurryAgent;

public class Analytics {

	static final String FLURRY_API_KEY = "6RCVY5ZZ347B6R99QM7J";
	
	public static final String EVENT_PHOTO_RESET = "photo_viewer_reset";
	public static final String EVENT_PHOTO_FILTERS = "photo_viewer_filters";
	public static final String EVENT_PHOTO_CROP = "photo_viewer_crop";
	public static final String EVENT_PHOTO_ROTATE = "photo_viewer_rotate";
	public static final String EVENT_PHOTO_CAPTION = "photo_viewer_caption";
	public static final String EVENT_PHOTO_PLACE = "photo_viewer_place";

	static {
		FlurryAgent.setLogEnabled(Constants.DEBUG);
		FlurryAgent.setCaptureUncaughtExceptions(false);
		FlurryAgent.setReportLocation(false);
	}

	public static void onStartSession(Context context) {
		FlurryAgent.onStartSession(context, FLURRY_API_KEY);
	}

	public static void onEndSession(Context context) {
		FlurryAgent.onEndSession(context);
	}
	
	public static void logEvent(final String event) {
		FlurryAgent.logEvent(event);
	}

}
