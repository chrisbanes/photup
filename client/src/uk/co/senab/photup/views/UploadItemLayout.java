package uk.co.senab.photup.views;

import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.PhotoUpload.OnUploadStateChanged;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
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

	public TextView getCaptionTextView() {
		return (TextView) findViewById(R.id.tv_photo_caption);
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
		getCaptionTextView().setText(mSelection.getCaption());

		ProgressBar pb = getProgressBar();
		switch (mSelection.getState()) {
			case PhotoUpload.STATE_UPLOAD_COMPLETED:
				pb.setVisibility(View.GONE);
				break;
			case PhotoUpload.STATE_UPLOAD_IN_PROGRESS:
				final int progress = mSelection.getUploadProgress();
				if (progress <= 0) {
					pb.setIndeterminate(true);
				} else {
					pb.setIndeterminate(false);
					pb.setProgress(progress);
				}
			case PhotoUpload.STATE_WAITING:
				pb.setVisibility(View.VISIBLE);
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
