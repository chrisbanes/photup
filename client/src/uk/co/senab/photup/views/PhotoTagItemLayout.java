package uk.co.senab.photup.views;

import uk.co.senab.bitmapcache.R;
import uk.co.senab.photup.Constants;
import uk.co.senab.photup.listeners.OnFriendPickedListener;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import uk.co.senab.photup.listeners.OnPhotoTapListener;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoTag;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class PhotoTagItemLayout extends FrameLayout implements MultiTouchImageView.OnMatrixChangedListener,
		OnPhotoTagsChangedListener, View.OnClickListener, OnPhotoTapListener, OnFriendPickedListener {

	static final String LOG_TAG = "PhotoTagItemLayout";

	private PhotoTag mFriendRequestTag;
	private final MultiTouchImageView mImageView;

	private final LayoutInflater mLayoutInflater;

	private final Animation mPhotoTagInAnimation, mPhotoTagOutAnimation;

	private final OnPickFriendRequestListener mPickFriendListener;
	private final AbsoluteLayout mTagLayout;

	private final PhotoSelection mUpload;

	public PhotoTagItemLayout(Context context, PhotoSelection upload, OnPickFriendRequestListener friendRequestListener) {
		super(context);

		mPickFriendListener = friendRequestListener;
		mUpload = upload;
		mUpload.setTagChangedListener(this);

		mLayoutInflater = LayoutInflater.from(context);

		mImageView = new MultiTouchImageView(context, true);
		mImageView.setMatrixChangeListener(this);
		mImageView.setPhotoTapListener(this);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mTagLayout = new AbsoluteLayout(context);
		addView(mTagLayout, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

		mPhotoTagInAnimation = AnimationUtils.loadAnimation(context, R.anim.tag_fade_in);
		mPhotoTagOutAnimation = AnimationUtils.loadAnimation(context, R.anim.tag_fade_out);

		addPhotoTags();
	}

	public MultiTouchImageView getImageView() {
		return mImageView;
	}

	public void onClick(View v) {
		final PhotoTag tag = (PhotoTag) v.getTag();

		switch (v.getId()) {
			case R.id.btn_remove_tag:
				mUpload.removePhotoTag(tag);
				break;
			case R.id.tv_tag_label:
				mFriendRequestTag = tag;
				mPickFriendListener.onPickFriendRequested(this, mUpload.getTaggedFriends());
				break;
		}
	}

	public void onFriendPicked(FbUser friend) {
		mFriendRequestTag.setFriend(friend);

		View tagLayout = getTagLayout(mFriendRequestTag);
		TextView labelTv = (TextView) tagLayout.findViewById(R.id.tv_tag_label);
		labelTv.setText(friend.getName());

		mFriendRequestTag = null;

		layoutTags(mImageView.getDisplayRect());
	}

	public void onMatrixChanged(RectF rect) {
		layoutTags(rect);
	}

	public void onNewPhotoTagTap(PhotoTag newTag) {
		if (Constants.DEBUG) {
			Log.d(LOG_TAG, "onPhotoTap");
		}
		mUpload.addPhotoTag(newTag);
	}

	public void onPhotoTagsChanged(final PhotoTag tag, final boolean added) {
		post(new Runnable() {
			public void run() {
				onPhotoTagsChangedImp(tag, added);
			}
		});
	}

	void onPhotoTagsChangedImp(final PhotoTag tag, final boolean added) {
		if (added) {
			mTagLayout.addView(createPhotoTagLayout(tag));
			layoutTags(mImageView.getDisplayRect());
		} else {
			View view = getTagLayout(tag);
			view.startAnimation(mPhotoTagOutAnimation);
			mTagLayout.removeView(view);
		}
	}

	private void addPhotoTags() {
		mTagLayout.removeAllViews();
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
		FbUser friend = tag.getFriend();
		if (null != friend) {
			labelTv.setText(friend.getName());
		}
		labelTv.setOnClickListener(this);
		labelTv.setTag(tag);

		tagLayout.setTag(tag);
		tagLayout.setVisibility(View.GONE);

		return tagLayout;
	}

	private View getTagLayout(PhotoTag tag) {
		for (int i = 0, z = mTagLayout.getChildCount(); i < z; i++) {
			View tagLayout = mTagLayout.getChildAt(i);
			if (tag == tagLayout.getTag()) {
				return tagLayout;
			}
		}
		return null;
	}

	private void layoutTags(final RectF rect) {
		if (null == rect) {
			return;
		}

		Log.d(LOG_TAG, "layoutTags. Rect: " + rect.toString());

		AbsoluteLayout.LayoutParams lp;
		for (int i = 0, z = mTagLayout.getChildCount(); i < z; i++) {
			View tagLayout = mTagLayout.getChildAt(i);
			PhotoTag tag = (PhotoTag) tagLayout.getTag();

			// Measure View if we need to
			if (tagLayout.getWidth() == 0) {
				measureView(tagLayout);
			}

			lp = (AbsoluteLayout.LayoutParams) tagLayout.getLayoutParams();
			lp.x = Math.round((rect.width() * tag.getX() / 100f) + rect.left) - (tagLayout.getMeasuredWidth() / 2);
			lp.y = Math.round((rect.height() * tag.getY() / 100f) + rect.top);

			tagLayout.setLayoutParams(lp);

			if (tagLayout.getVisibility() != View.VISIBLE) {
				tagLayout.startAnimation(mPhotoTagInAnimation);
				tagLayout.setVisibility(View.VISIBLE);
			}
		}
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}
}
