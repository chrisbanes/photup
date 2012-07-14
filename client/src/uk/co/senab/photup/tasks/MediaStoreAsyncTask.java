package uk.co.senab.photup.tasks;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.model.MediaStorePhotoUpload;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

public class MediaStoreAsyncTask extends AsyncTask<Void, Void, List<PhotoSelection>> {

	final static String[] PROJECTION = { Images.Media._ID, Images.Media.MINI_THUMB_MAGIC, Images.Media.DATA };

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

			Cursor cursor = openCursor(context, contentUri);
			if (cursor.getCount() == 0) {
				cursor.close();

				contentUri = Images.Media.INTERNAL_CONTENT_URI;
				cursor = openCursor(context, contentUri);
			}

			List<PhotoSelection> selection = cursorToSelectionList(contentUri, cursor);
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

	static Cursor openCursor(Context context, Uri contentUri) {
		return context.getContentResolver()
				.query(contentUri, PROJECTION, null, null, Images.Media.DATE_ADDED + " desc");
	}

	static ArrayList<PhotoSelection> cursorToSelectionList(Uri contentUri, Cursor cursor) {
		ArrayList<PhotoSelection> items = new ArrayList<PhotoSelection>(cursor.getCount());

		PhotoSelection item;
		File file;
		while (cursor.moveToNext()) {
			try {
				file = new File(cursor.getString(cursor.getColumnIndexOrThrow(ImageColumns.DATA)));
				if (file.exists()) {
					item = new MediaStorePhotoUpload(contentUri, cursor.getInt(cursor
							.getColumnIndexOrThrow(ImageColumns._ID)));
					items.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return items;
	}

}
