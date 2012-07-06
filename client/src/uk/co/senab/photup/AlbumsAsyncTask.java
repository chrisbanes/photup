package uk.co.senab.photup;

import java.lang.ref.WeakReference;
import java.util.List;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.model.Album;
import android.content.Context;
import android.os.AsyncTask;

public class AlbumsAsyncTask extends AsyncTask<Void, Void, List<Album>> {

	public static interface AlbumsResultListener {
		public void onAlbumsLoaded(List<Album> albums);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<AlbumsResultListener> mListener;

	public AlbumsAsyncTask(Context context, AlbumsResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<AlbumsResultListener>(listener);
	}

	@Override
	protected List<Album> doInBackground(Void... params) {
		Context context = mContext.get();
		if (null != context) {
			FacebookRequester requester = new FacebookRequester(context);
			return requester.getUploadableAlbums();
		}
		return null;
	}

	@Override
	protected void onPostExecute(List<Album> result) {
		super.onPostExecute(result);

		AlbumsResultListener listener = mListener.get();
		if (null != listener) {
			listener.onAlbumsLoaded(result);
		}
	}

}
