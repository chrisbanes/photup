package uk.co.senab.footo.adapters;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import uk.co.senab.footo.views.MultiChoiceGridView;
import uk.co.senab.footo.views.PhotupImageView;
import uk.co.senab.photup.R;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.Checkable;

public class PhotosAdapter extends ResourceCursorAdapter {

	private final HashSet<WeakReference<PhotupImageView>> mImageViews;
	
	private MultiChoiceGridView mParent;

	public PhotosAdapter(Context context, int layout, Cursor c, boolean autoRequery) {
		super(context, layout, c, autoRequery);
		mImageViews = new HashSet<WeakReference<PhotupImageView>>();
	}

	public void setParentView(MultiChoiceGridView gridView) {
		mParent = gridView;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		PhotupImageView iv = (PhotupImageView) view.findViewById(R.id.iv_photo);

		long id = cursor.getInt(cursor.getColumnIndexOrThrow(ImageColumns._ID));
		iv.requestThumbnailId(id);

		if (null != mParent) {
			((Checkable) view).setChecked(mParent.isItemIdChecked(id));
		}
		
		mImageViews.add(new WeakReference<PhotupImageView>(iv));
	}

	public void cleanup() {
		for (WeakReference<PhotupImageView> ref : mImageViews) {
			PhotupImageView iv = ref.get();
			if (null != iv) {
				iv.recycleBitmap();
			}	 
		}
		
		mImageViews.clear();
	}

}
