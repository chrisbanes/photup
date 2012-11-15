package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Group;
import android.content.Context;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class GroupsAsyncTask extends AsyncTask<Void, Void, List<Group>> {

	public static interface GroupsResultListener extends FacebookErrorListener {
		void onGroupsLoaded(Account account, List<Group> groups);
	}

	private final Account mAccount;
	private final WeakReference<Context> mContext;
	private final WeakReference<GroupsResultListener> mListener;

	public GroupsAsyncTask(Context context, Account account, GroupsResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mAccount = account;
		mListener = new WeakReference<GroupsResultListener>(listener);
	}

	@Override
	protected List<Group> doInBackground(Void... params) {

		FacebookRequester requester = new FacebookRequester(mAccount);
		try {
			return requester.getGroups();
		} catch (FacebookError e) {
			GroupsResultListener listener = mListener.get();
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
	protected void onPostExecute(List<Group> result) {
		super.onPostExecute(result);
		
		Context context = mContext.get();
		if (null != context) {
			if (null != result) {
				Group.saveToDatabase(context, result, mAccount);
			} else {
				result = Group.getFromDatabase(context, mAccount);
			}
		}

		GroupsResultListener listener = mListener.get();
		if (null != listener && null != result) {
			listener.onGroupsLoaded(mAccount, result);
		}
	}

}
