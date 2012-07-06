package uk.co.senab.photup.model;

import uk.co.senab.photup.Constants;

import com.lightbox.android.photoprocessing.R;

public enum UploadQuality {

	LOW(640), MEDIUM(1024), HIGH(Constants.FACEBOOK_MAX_PHOTO_SIZE);

	private final int mMaxDimension;

	private UploadQuality(int maxDimension) {
		mMaxDimension = maxDimension;
	}

	public int getMaxDimension() {
		return mMaxDimension;
	}

	public static UploadQuality mapFromButtonId(int buttonId) {
		switch (buttonId) {
			case R.id.rb_quality_low:
				return UploadQuality.LOW;
			case R.id.rb_quality_medium:
				return UploadQuality.MEDIUM;
			default:
			case R.id.rb_quality_high:
				return UploadQuality.HIGH;

		}
	}
}
