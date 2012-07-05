package uk.co.senab.photup.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;

@SuppressWarnings("deprecation")
public class PhotoTagItemLayout extends FrameLayout {

	private final MultiTouchImageView mImageView;

	private final AbsoluteLayout mTagLayout;

	public PhotoTagItemLayout(Context context) {
		this(context, null);
	}

	public PhotoTagItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		mImageView = new MultiTouchImageView(context, true);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mTagLayout = new AbsoluteLayout(context);
		addView(mTagLayout, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
	}
	
	public MultiTouchImageView getImageView() {
		return mImageView;
	}

}
