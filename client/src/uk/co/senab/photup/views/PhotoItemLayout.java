package uk.co.senab.photup.views;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;

public class PhotoItemLayout extends CheckableFrameLayout implements View.OnClickListener {

	private final PhotupImageView mImageView;
	private final CheckableImageView mButton;
	private PhotoUpload mSelection;

	private boolean mAnimateCheck = true;

	private PhotoUploadController mController;

	public PhotoItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		mController = PhotupApplication.getApplication(context).getPhotoUploadController();

		mImageView = new PhotupImageView(context);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		mImageView.setScaleType(ScaleType.CENTER_CROP);

		mButton = new CheckableImageView(context);
		mButton.setScaleType(ScaleType.CENTER);
		mButton.setOnClickListener(this);
		mButton.setImageResource(R.drawable.btn_selection);

		int dimension = getResources().getDimensionPixelSize(R.dimen.spacing);
		addView(mButton, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP));
		mButton.setPadding(dimension, dimension, dimension, dimension);
	}

	public PhotupImageView getImageView() {
		return mImageView;
	}

	public void setAnimateWhenChecked(boolean animate) {
		mAnimateCheck = animate;
	}

	public void onClick(View v) {
		if (null != mSelection) {

			// Toggle check to show new state
			toggle();

			// Update the controller
			updateController();

			// Show animate if we've been set to
			if (mAnimateCheck) {
				Animation anim = AnimationUtils.loadAnimation(getContext(), isChecked() ? R.anim.photo_selection_added
						: R.anim.photo_selection_removed);
				v.startAnimation(anim);
			}
		}
	}

	@Override
	public void setChecked(final boolean b) {
		super.setChecked(b);
		mButton.setChecked(b);
	}

	public PhotoUpload getPhotoSelection() {
		return mSelection;
	}

	public void setPhotoSelection(PhotoUpload selection) {
		if (mSelection != selection) {
			mButton.clearAnimation();
			mSelection = selection;
		}
	}

	void updateController() {
		if (isChecked()) {
			mController.addSelection(mSelection);
		} else {
			mController.removeSelection(mSelection);
		}
	}

}
