package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;

public class MediaStoreAsyncTask extends AsyncTask<Void, Void, List<PhotoSelection>> {

	public static interface MediaStoreResultListener {
		public void onPhotosLoaded(List<PhotoSelection> friends);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<MediaStoreResultListener> mListener;

	public MediaStoreAsyncTask(Context context, MediaStoreResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<MediaStoreResultListener>(listener);
	}

	@Override
	protected List<PhotoSelection> doInBackground(Void... params) {
		Context context = mContext.get();
		if (null != context) {

			Uri contentUri = Images.Media.EXTERNAL_CONTENT_URI;

			Cursor cursor = MediaStoreCursorHelper.openPhotosCursor(context, contentUri);
			if (cursor.getCount() == 0) {
				cursor.close();

				contentUri = Images.Media.INTERNAL_CONTENT_URI;
				cursor = MediaStoreCursorHelper.openPhotosCursor(context, contentUri);
			}

			List<PhotoSelection> selection = MediaStoreCursorHelper.photosCursorToSelectionList(contentUri, cursor);
			cursor.close();

			return selection;
		}
		return null;
	}

	@Override
	protected void onPostExecute(List<PhotoSelection> result) {
		super.onPostExecute(result);

		MediaStoreResultListener listener = mListener.get();
		if (null != listener) {
			listener.onPhotosLoaded(result);
		}
	}

}
