package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;

import uk.co.senab.photup.facebook.FacebookRequester;
import android.content.Context;
import android.os.AsyncTask;

public class NewAlbumAsyncTask extends AsyncTask<String, Void, String> {

	public static interface NewAlbumResultListener {
		public void onNewAlbumCreated(String albumId);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<NewAlbumResultListener> mListener;

	public NewAlbumAsyncTask(Context context, NewAlbumResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<NewAlbumResultListener>(listener);
	}

	@Override
	protected String doInBackground(String... params) {
		Context context = mContext.get();
		if (null != context) {
			FacebookRequester requester = new FacebookRequester(context);
			return requester.createNewAlbum(params[0], params[1], params[2]);
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);

		NewAlbumResultListener listener = mListener.get();
		if (null != listener) {
			listener.onNewAlbumCreated(result);
		}
	}

}
