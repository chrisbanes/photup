package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class AlbumsAsyncTask extends AsyncTask<Void, Void, List<Album>> {

	public static interface AlbumsResultListener extends FacebookErrorListener {
		void onAlbumsLoaded(Account account, List<Album> albums);
	}

	private final Account mAccount;
	private final WeakReference<AlbumsResultListener> mListener;

	public AlbumsAsyncTask(Account account, AlbumsResultListener listener) {
		mAccount = account;
		mListener = new WeakReference<AlbumsResultListener>(listener);
	}

	@Override
	protected List<Album> doInBackground(Void... params) {

		FacebookRequester requester = new FacebookRequester(mAccount);
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

		return null;
	}

	@Override
	protected void onPostExecute(List<Album> result) {
		super.onPostExecute(result);

		AlbumsResultListener listener = mListener.get();
		if (null != listener && null != result) {
			listener.onAlbumsLoaded(mAccount, result);
		}
	}

}
