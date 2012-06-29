package uk.co.senab.photup.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;

public class PhotoItemLayout extends CheckableFrameLayout {

	private final PhotupImageView mImageView;
	
	public PhotoItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		mImageView = new PhotupImageView(context);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		mImageView.setScaleType(ScaleType.CENTER_CROP);
	}

	public PhotupImageView getImageView() {
		return mImageView;
	}

}
