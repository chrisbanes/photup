package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Event;
import android.content.Context;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class EventsAsyncTask extends AsyncTask<Void, Void, List<Event>> {

	public static interface EventsResultListener extends FacebookErrorListener {
		void onEventsLoaded(Account account, List<Event> events);
	}

	private final Account mAccount;
	private final WeakReference<Context> mContext;
	private final WeakReference<EventsResultListener> mListener;

	public EventsAsyncTask(Context context, Account account, EventsResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mAccount = account;
		mListener = new WeakReference<EventsResultListener>(listener);
	}

	@Override
	protected List<Event> doInBackground(Void... params) {

		FacebookRequester requester = new FacebookRequester(mAccount);
		try {
			return requester.getEvents();
		} catch (FacebookError e) {
			EventsResultListener listener = mListener.get();
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
	protected void onPostExecute(List<Event> result) {
		super.onPostExecute(result);
		
		Context context = mContext.get();
		if (null != context) {
			if (null != result) {
				Event.saveToDatabase(context, result, mAccount);
			} else {
				result = Event.getFromDatabase(context, mAccount);
			}
		}

		EventsResultListener listener = mListener.get();
		if (null != listener && null != result) {
			listener.onEventsLoaded(mAccount, result);
		}
	}

}
