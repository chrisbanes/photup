package uk.co.senab.photup;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.FbUser;
import android.content.Context;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class FriendsAsyncTask extends AsyncTask<Void, Void, List<FbUser>> {

	public static interface FriendsResultListener extends FacebookErrorListener {
		public void onFriendsLoaded(List<FbUser> friends);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<FriendsResultListener> mListener;

	public FriendsAsyncTask(Context context, FriendsResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<FriendsResultListener>(listener);
	}

	@Override
	protected List<FbUser> doInBackground(Void... params) {
		Context context = mContext.get();
		if (null != context) {
			FacebookRequester requester = new FacebookRequester(context);
			try {
				return requester.getFriends();
			} catch (FacebookError e) {
				FriendsResultListener listener = mListener.get();
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
	protected void onPostExecute(List<FbUser> result) {
		super.onPostExecute(result);

		FriendsResultListener listener = mListener.get();
		if (null != result && null != listener) {
			listener.onFriendsLoaded(result);
		}
	}

}
