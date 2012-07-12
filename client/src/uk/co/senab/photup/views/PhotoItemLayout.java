package uk.co.senab.photup.views;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class PhotoItemLayout extends CheckableFrameLayout implements View.OnClickListener {

	private final PhotupImageView mImageView;
	private final ImageView mButton;
	private PhotoSelection mSelection;

	private PhotoUploadController mController;

	public PhotoItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		mController = PhotupApplication.getApplication(context).getPhotoUploadController();

		mImageView = new PhotupImageView(context);
		addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		mImageView.setScaleType(ScaleType.CENTER_CROP);

		mButton = new ImageView(context);
		mButton.setScaleType(ScaleType.CENTER);
		mButton.setOnClickListener(this);
		mButton.setImageResource(R.drawable.ic_btn_selection_normal);

		int dimension = getResources().getDimensionPixelSize(R.dimen.button_selection_dimension);
		addView(mButton, new FrameLayout.LayoutParams(dimension, dimension, Gravity.RIGHT | Gravity.TOP));
	}

	public PhotupImageView getImageView() {
		return mImageView;
	}

	public void onClick(View v) {
		if (null != mSelection) {

			final boolean wasChecked = isChecked();

			// Toggle check to show new state
			toggle();

			Animation anim;
			if (wasChecked) {
				mController.removePhotoSelection(mSelection);
				anim = AnimationUtils.loadAnimation(getContext(), R.anim.photo_selection_removed);
			} else {
				mController.addPhotoSelection(mSelection);
				anim = AnimationUtils.loadAnimation(getContext(), R.anim.photo_selection_added);
			}

			v.startAnimation(anim);
		}
	}

	@Override
	public void setChecked(boolean b) {
		if (isChecked() != b) {
			super.setChecked(b);
			mButton.setImageResource(b ? R.drawable.ic_btn_selection_checked : R.drawable.ic_btn_selection_normal);
		}
	}

	public PhotoSelection getPhotoSelection() {
		return mSelection;
	}

	public void setPhotoSelection(PhotoSelection selection) {
		mSelection = selection;
	}

}
