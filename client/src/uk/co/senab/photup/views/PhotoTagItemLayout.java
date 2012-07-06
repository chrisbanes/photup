package uk.co.senab.photup.views;

import uk.co.senab.bitmapcache.R;
import uk.co.senab.photup.Constants;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import uk.co.senab.photup.listeners.OnPhotoTapListener;
import uk.co.senab.photup.model.Friend;
import uk.co.senab.photup.model.PhotoTag;
import uk.co.senab.photup.model.PhotoUpload;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class PhotoTagItemLayout extends FrameLayout implements MultiTouchImageView.OnMatrixChangedListener,
		OnPhotoTagsChangedListener, View.OnClickListener, OnPhotoTapListener {

	static final String LOG_TAG = "PhotoTagItemLayout";

	private final MultiTouchImageView mImageView;
	private final LayoutInflater mLayoutInflater;

	private final Animation mPhotoTagInAnimation, mPhotoTagOutAnimation;

	private final AbsoluteLayout mTagLayout;

	private final PhotoUpload mUpload;

	public PhotoTagItemLayout(Context context, PhotoUpload upload) {
		super(context);

		mLayoutInflater = LayoutInflater.from(context);

		mImageView = new MultiTouchImageView(context, true);
		mImageView.setMatrixChangeListener(this);
		mImageView.setPhotoTapListener(this);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mTagLayout = new AbsoluteLayout(context);
		addView(mTagLayout, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mUpload = upload;
		mUpload.setTagChangedListener(this);

		mPhotoTagInAnimation = AnimationUtils.loadAnimation(context, R.anim.tag_fade_in);
		mPhotoTagOutAnimation = AnimationUtils.loadAnimation(context, R.anim.tag_fade_out);

		addPhotoTags();
	}

	public MultiTouchImageView getImageView() {
		return mImageView;
	}

	public void onClick(View v) {
		PhotoTag tag = (PhotoTag) v.getTag();
		mUpload.removePhotoTag(tag);
	}

	public void onMatrixChanged(RectF rect) {
		layoutTags(rect);
	}

	public void onPhotoTagsChanged(final PhotoTag tag, final boolean added) {
		post(new Runnable() {
			public void run() {
				onPhotoTagsChangedImp(tag, added);
			}
		});
	}

	public void onNewPhotoTagTap(PhotoTag newTag) {
		if (Constants.DEBUG) {
			Log.d(LOG_TAG, "onPhotoTap");
		}
		onPhotoTagsChangedImp(newTag, true);
	}

	void onPhotoTagsChangedImp(final PhotoTag tag, final boolean added) {
		if (added) {
			mTagLayout.addView(createPhotoTagLayout(tag));
			layoutTags(mImageView.getDisplayRect());

		} else {
			for (int i = 0, z = mTagLayout.getChildCount(); i < z; i++) {
				View tagLayout = mTagLayout.getChildAt(i);
				if (tag == tagLayout.getTag()) {
					tagLayout.startAnimation(mPhotoTagOutAnimation);
					mTagLayout.removeView(tagLayout);
					break;
				}
			}
		}
	}

	private void addPhotoTags() {
		for (PhotoTag tag : mUpload.getPhotoTags()) {
			mTagLayout.addView(createPhotoTagLayout(tag));
		}
		layoutTags(mImageView.getDisplayRect());
	}

	private View createPhotoTagLayout(PhotoTag tag) {
		View tagLayout = mLayoutInflater.inflate(R.layout.layout_photo_tag, mTagLayout, false);

		View removeBtn = tagLayout.findViewById(R.id.btn_remove_tag);
		removeBtn.setOnClickListener(this);
		removeBtn.setTag(tag);

		TextView labelTv = (TextView) tagLayout.findViewById(R.id.tv_tag_label);
		Friend friend = tag.getFriend();
		if (null != friend) {
			labelTv.setText(friend.getName());
		}

		tagLayout.setTag(tag);
		tagLayout.setVisibility(View.GONE);

		return tagLayout;
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

			if (tagLayout.getVisibility() != View.VISIBLE) {
				tagLayout.startAnimation(mPhotoTagInAnimation);
				tagLayout.setVisibility(View.VISIBLE);
			}
		}
	}
}
