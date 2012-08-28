package uk.co.senab.photup;

public final class Constants {

	public static final float IMAGE_CACHE_HEAP_PERCENTAGE = 1f / 6f;

	public static final int DISPLAY_PHOTO_SIZE = 640;

	public static final long SCALE_ANIMATION_DURATION_FULL_DISTANCE = 800;

	public static final String FACEBOOK_APP_ID = "134669876670695";

	public static final String[] FACEBOOK_PERMISSIONS = { "publish_stream", "user_photos", "manage_pages",
			"user_groups", "user_events" };

	public static final int FACE_DETECTOR_MAX_FACES = 8;

	public static final String ACRA_GOOGLE_DOC_ID = "dHlELWNlMndaVktHanhsYTl1ZEQtYUE6MQ";

	public static final String INTENT_SERVICE_UPLOAD_ALL = "photup.intent.action.UPLOAD_ALL";
	public static final String INTENT_PHOTO_TAKEN = "photup.intent.action.PHOTO_TAKEN";
	public static final String INTENT_NEW_PERMISSIONS = "photup.intent.action.NEW_PERMISSIONS";
	public static final String INTENT_LOGOUT = "photup.intent.action.LOGOUT";

	public static final String PROMO_POST_URL = "https://play.google.com/store/apps/details?id=uk.co.senab.photup";
	public static final String PROMO_IMAGE_URL = "http://www.senab.co.uk/wp-content/uploads/2012/08/photup_logo.png";
}
