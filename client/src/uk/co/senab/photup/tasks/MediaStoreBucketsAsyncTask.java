package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import uk.co.senab.photup.model.MediaStoreBucket;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;

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
		Context context = mContext.get();
		if (null != context) {

			Uri contentUri = Images.Media.EXTERNAL_CONTENT_URI;

			Cursor cursor = MediaStoreCursorHelper.openPhotosCursor(context, contentUri);
			if (cursor.getCount() == 0) {
				cursor.close();

				contentUri = Images.Media.INTERNAL_CONTENT_URI;
				cursor = MediaStoreCursorHelper.openPhotosCursor(context, contentUri);
			}

			List<MediaStoreBucket> selection = MediaStoreCursorHelper.photosCursorToBucketList(cursor,
					MediaStoreBucket.getAllPhotosBucket(context));

			cursor.close();

			return selection;
		}
		return null;
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
