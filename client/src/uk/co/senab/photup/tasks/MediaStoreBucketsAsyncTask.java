package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.model.MediaStoreBucket;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

public class MediaStoreBucketsAsyncTask extends AsyncTask<Void, Void, List<MediaStoreBucket>> {

	public static interface MediaStoreBucketsResultListener {
		public void onBucketsLoaded(List<MediaStoreBucket> buckets);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<MediaStoreBucketsResultListener> mListener;

	public MediaStoreBucketsAsyncTask(Context context, MediaStoreBucketsResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<MediaStoreBucketsResultListener>(listener);
	}

	@Override
	protected List<MediaStoreBucket> doInBackground(Void... params) {
		ArrayList<MediaStoreBucket> result = null;
		Context context = mContext.get();

		if (null != context) {
			// Add 'All Photos' item
			result = new ArrayList<MediaStoreBucket>();
			result.add(MediaStoreBucket.getAllPhotosBucket(context));

			Cursor cursor = MediaStoreCursorHelper.openPhotosCursor(context,
					MediaStoreCursorHelper.MEDIA_STORE_CONTENT_URI);

			if (null != cursor) {
				MediaStoreCursorHelper.photosCursorToBucketList(cursor, result);
				cursor.close();
			}
		}

		return result;
	}

	@Override
	protected void onPostExecute(List<MediaStoreBucket> result) {
		super.onPostExecute(result);

		MediaStoreBucketsResultListener listener = mListener.get();
		if (null != listener) {
			listener.onBucketsLoaded(result);
		}
	}

}
