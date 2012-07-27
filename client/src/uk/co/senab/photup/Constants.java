package uk.co.senab.photup;

public class Constants {

	public static final float IMAGE_CACHE_HEAP_PERCENTAGE = 1f / 6f;

	public static final boolean DEBUG = BuildConfig.DEBUG;
	public static final boolean ENABLE_ACRA = false;

	public static final long SCALE_ANIMATION_DURATION_FULL_DISTANCE = 800;

	public static final int DISPLAY_PHOTO_SIZE = 640;

	public static final String FACEBOOK_APP_ID = "134669876670695";

	public static final String[] FACEBOOK_PERMISSIONS = { "publish_stream", "user_photos", "manage_pages" };

	public static final int FACE_DETECTOR_MAX_FACES = 8;

	public static final String ACRA_GOOGLE_DOC_ID = "dHlELWNlMndaVktHanhsYTl1ZEQtYUE6MQ";

	public static final String INTENT_SERVICE_UPLOAD_ALL = "photup.intent.action.UPLOAD_ALL";
	public static final String INTENT_PHOTO_TAKEN = "photup.intent.action.PHOTO_TAKEN";

}
