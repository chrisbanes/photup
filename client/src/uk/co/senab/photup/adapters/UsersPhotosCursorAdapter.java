package uk.co.senab.photup.adapters;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Images;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.Checkable;

public class UsersPhotosCursorAdapter extends ResourceCursorAdapter {

	private final PhotoUploadController mController;

	public UsersPhotosCursorAdapter(Context context, Cursor c) {
		super(context, R.layout.item_grid_photo, c, 0);

		PhotupApplication app = PhotupApplication.getApplication(context);
		mController = app.getPhotoUploadController();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		PhotoItemLayout layout = (PhotoItemLayout) view;
		PhotupImageView iv = layout.getImageView();

		final PhotoSelection upload = MediaStoreCursorHelper.photosCursorToSelection(Images.Media.EXTERNAL_CONTENT_URI,
				cursor);

		if (null != upload) {
			iv.setFadeInDrawables(true);
			iv.requestThumbnail(upload, false);
			layout.setPhotoSelection(upload);

			if (null != mController) {
				((Checkable) view).setChecked(mController.isPhotoUploadSelected(upload));
			}
		}
	}

}
