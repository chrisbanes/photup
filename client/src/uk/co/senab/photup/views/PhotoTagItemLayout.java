package uk.co.senab.photup.views;

import java.util.List;

import uk.co.senab.bitmapcache.R;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import uk.co.senab.photup.model.PhotoTag;
import uk.co.senab.photup.model.PhotoUpload;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;

@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class PhotoTagItemLayout extends FrameLayout implements MultiTouchImageView.OnMatrixChangedListener,
		OnPhotoTagsChangedListener, View.OnClickListener {

	static final String LOG_TAG = "PhotoTagItemLayout";

	private final MultiTouchImageView mImageView;
	private final AbsoluteLayout mTagLayout;

	private final PhotoUpload mUpload;

	public PhotoTagItemLayout(Context context, PhotoUpload upload) {
		super(context);

		mImageView = new MultiTouchImageView(context, true);
		mImageView.setMatrixChangeListener(this);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mTagLayout = new AbsoluteLayout(context);
		addView(mTagLayout, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mUpload = upload;
		mUpload.setTagChangedListener(this);

		addPhotoTags();
	}

	private void addPhotoTags() {
		mTagLayout.removeAllViews();

		final List<PhotoTag> tags = mUpload.getPhotoTags();
		if (null != tags && !tags.isEmpty()) {
			LayoutInflater layoutInflater = LayoutInflater.from(getContext());

			View tagLayout;
			for (PhotoTag tag : tags) {
				tagLayout = layoutInflater.inflate(R.layout.layout_photo_tag, mTagLayout, false);
				
				View removeBtn = tagLayout.findViewById(R.id.btn_remove_tag);
				removeBtn.setOnClickListener(this);
				removeBtn.setTag(tag);
				
				tagLayout.setTag(tag);
				tagLayout.setVisibility(View.GONE);
				mTagLayout.addView(tagLayout);
			}
		}
		
		layoutTags(mImageView.getDisplayRect());
	}

	public MultiTouchImageView getImageView() {
		return mImageView;
	}

	public void onMatrixChanged(RectF rect) {
		layoutTags(rect);
	}

	private void layoutTags(final RectF rect) {
		if (null == rect) {
			return;
		}

		AbsoluteLayout.LayoutParams lp;
		for (int i = 0, z = mTagLayout.getChildCount(); i < z; i++) {
			View tagLayout = mTagLayout.getChildAt(i);
			PhotoTag tag = (PhotoTag) tagLayout.getTag();

			lp = (AbsoluteLayout.LayoutParams) tagLayout.getLayoutParams();
			lp.x = Math.round((rect.width() * tag.getX() / 100f) + rect.left) - (tagLayout.getWidth() / 2);
			lp.y = Math.round((rect.height() * tag.getY() / 100f) + rect.top);
			tagLayout.setLayoutParams(lp);

			tagLayout.setVisibility(View.VISIBLE);
		}
	}

	public void onPhotoTagsChanged() {
		post(new Runnable() {
			public void run() {
				addPhotoTags();
			}
		});
	}

	public void onClick(View v) {
		PhotoTag tag = (PhotoTag) v.getTag();
		mUpload.removePhotoTag(tag);
	}
}
