package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Place;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class PlacesAsyncTask extends AsyncTask<Void, Void, List<Place>> {

	public static interface PlacesResultListener extends FacebookErrorListener {
		void onPlacesLoaded(List<Place> albums);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<PlacesResultListener> mListener;

	private final Location mLocation;
	private final String mSearchQuery;

	public PlacesAsyncTask(Context context, PlacesResultListener listener, Location location, String searchQuery) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<PlacesResultListener>(listener);

		mLocation = location;
		mSearchQuery = searchQuery;
	}

	@Override
	protected List<Place> doInBackground(Void... params) {
		Context context = mContext.get();
		if (null != context) {
			FacebookRequester requester = new FacebookRequester(context);
			try {
				return requester.getPlaces(mLocation, mSearchQuery);
			} catch (FacebookError e) {
				PlacesResultListener listener = mListener.get();
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
	protected void onPostExecute(List<Place> result) {
		super.onPostExecute(result);

		PlacesResultListener listener = mListener.get();
		if (null != listener && null != result) {
			listener.onPlacesLoaded(result);
		}
	}

}
