package uk.co.senab.photup.model;

import com.lightbox.android.photoprocessing.R;

public enum UploadQuality {

	LOW(640, 75), MEDIUM(1024, 80), HIGH(2048, 85);

	private final int mMaxDimension, mJpegQuality;

	private UploadQuality(int maxDimension, int jpegQuality) {
		mMaxDimension = maxDimension;
		mJpegQuality = jpegQuality;
	}

	public int getMaxDimension() {
		return mMaxDimension;
	}

	public int getJpegQuality() {
		return mJpegQuality;
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
