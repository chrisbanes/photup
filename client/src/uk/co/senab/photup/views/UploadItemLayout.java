package uk.co.senab.photup.views;

import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.PhotoUpload.OnUploadStateChanged;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class UploadItemLayout extends LinearLayout implements OnUploadStateChanged {

	private PhotoSelection mSelection;

	public UploadItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PhotupImageView getImageView() {
		return (PhotupImageView) findViewById(R.id.iv_photo);
	}

	public ImageView getResultImageView() {
		return (ImageView) findViewById(R.id.iv_upload_result);
	}

	public TextView getCaptionTextView() {
		return (TextView) findViewById(R.id.tv_photo_caption);
	}

	public TextView getTagTextView() {
		return (TextView) findViewById(R.id.tv_photo_tags);
	}

	public ProgressBar getProgressBar() {
		return (ProgressBar) findViewById(R.id.pb_upload_progress);
	}

	public void setPhotoSelection(PhotoSelection selection) {
		if (null != mSelection) {
			mSelection.removeUploadStateChangedListener(this);
			mSelection = null;
		}

		mSelection = selection;
		refreshUploadUi();

		mSelection.addUploadStateChangedListener(this);
	}

	public void refreshUploadUi() {
		if (null == mSelection) {
			return;
		}

		getImageView().requestThumbnail(mSelection, true);

		String caption = mSelection.getCaption();
		if (TextUtils.isEmpty(caption)) {
			getCaptionTextView().setText(R.string.untitled_photo);
		} else {
			getCaptionTextView().setText(mSelection.getCaption());
		}

		final int tagsCount = mSelection.getPhotoTagsCount();
		TextView tagsTv = getTagTextView();
		if (tagsCount > 0) {
			tagsTv.setText(getResources().getQuantityString(R.plurals.tag_summary_photo, tagsCount, tagsCount));
			tagsTv.setVisibility(View.VISIBLE);
		} else {
			tagsTv.setVisibility(View.GONE);
		}

		ProgressBar pb = getProgressBar();
		ImageView resultIv = getResultImageView();

		switch (mSelection.getState()) {
			case PhotoUpload.STATE_UPLOAD_COMPLETED:
				pb.setVisibility(View.GONE);

				resultIv.setImageResource(R.drawable.ic_success);
				resultIv.setVisibility(View.VISIBLE);
				break;

			case PhotoUpload.STATE_UPLOAD_IN_PROGRESS:
				final int progress = mSelection.getUploadProgress();
				if (progress <= 0) {
					pb.setIndeterminate(true);
				} else {
					pb.setIndeterminate(false);
					pb.setProgress(progress);
				}
				resultIv.setVisibility(View.GONE);

			case PhotoUpload.STATE_WAITING:
				pb.setVisibility(View.VISIBLE);
				resultIv.setVisibility(View.GONE);
				break;
		}
	}

	public void onUploadStateChanged(PhotoUpload upload, int state, int progress) {
		if (state == PhotoUpload.STATE_UPLOAD_COMPLETED) {
			upload.removeUploadStateChangedListener(this);
		}

		post(new Runnable() {
			public void run() {
				refreshUploadUi();
			}
		});
	}

}
