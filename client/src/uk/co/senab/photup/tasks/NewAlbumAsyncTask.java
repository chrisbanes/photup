package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.model.Account;
import android.os.AsyncTask;

public class NewAlbumAsyncTask extends AsyncTask<String, Void, String> {

	public static interface NewAlbumResultListener {
		public void onNewAlbumCreated(String albumId);
	}

	private final Account mAccount;
	private final WeakReference<NewAlbumResultListener> mListener;

	public NewAlbumAsyncTask(Account account, NewAlbumResultListener listener) {
		mAccount = account;
		mListener = new WeakReference<NewAlbumResultListener>(listener);
	}

	@Override
	protected String doInBackground(String... params) {
		FacebookRequester requester = new FacebookRequester(mAccount);
		return requester.createNewAlbum(params[0], params[1], params[2]);
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
