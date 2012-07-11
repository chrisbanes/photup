package uk.co.senab.photup.adapters;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.model.MediaStorePhotoUpload;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.Checkable;

public class PhotosCursorAdapter extends ResourceCursorAdapter {

	private final PhotoUploadController mController;

	public PhotosCursorAdapter(Context context, int layout, Cursor c, boolean autoRequery) {
		super(context, layout, c, autoRequery);

		PhotupApplication app = PhotupApplication.getApplication(context);
		mController = app.getPhotoUploadController();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		PhotoItemLayout layout = (PhotoItemLayout) view;
		PhotupImageView iv = layout.getImageView();

		long id = cursor.getInt(cursor.getColumnIndexOrThrow(ImageColumns._ID));
		final PhotoSelection upload = new MediaStorePhotoUpload(id);
		iv.requestThumbnail(upload,false);

		view.setTag(upload);

		if (null != mController) {
			((Checkable) view).setChecked(mController.isPhotoUploadSelected(upload));
		}
	}

}
