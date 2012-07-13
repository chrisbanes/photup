package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Album;
import android.content.Context;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class AlbumsAsyncTask extends AsyncTask<Void, Void, List<Album>> {

	public static interface AlbumsResultListener extends FacebookErrorListener {
		void onAlbumsLoaded(List<Album> albums);
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
			try {
				return requester.getUploadableAlbums();
			} catch (FacebookError e) {
				AlbumsResultListener listener = mListener.get();
				if (null != listener) {
					listener.onFacebookError(e);
				} else {
					e.printStackTrace();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(List<Album> result) {
		super.onPostExecute(result);

		AlbumsResultListener listener = mListener.get();
		if (null != listener && null != result) {
			listener.onAlbumsLoaded(result);
		}
	}

}
