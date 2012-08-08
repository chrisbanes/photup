package uk.co.senab.photup.tasks;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONException;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Account;
import android.content.Context;
import android.os.AsyncTask;

import com.facebook.android.FacebookError;

public class AccountsAsyncTask extends AsyncTask<Void, Void, List<Account>> {

	public static interface AccountsResultListener extends FacebookErrorListener {
		public void onAccountsLoaded(List<Account> friends);
	}

	private final WeakReference<Context> mContext;
	private final WeakReference<AccountsResultListener> mListener;

	public AccountsAsyncTask(Context context, AccountsResultListener listener) {
		mContext = new WeakReference<Context>(context);
		mListener = new WeakReference<AccountsResultListener>(listener);
	}

	@Override
	protected List<Account> doInBackground(Void... params) {
		Context context = mContext.get();
		if (null != context) {
			Account account = Account.getAccountFromSession(context);
			if (null != account) {
				try {
					FacebookRequester requester = new FacebookRequester(account);
					return requester.getAccounts();
				} catch (FacebookError e) {
					AccountsResultListener listener = mListener.get();
					if (null != listener) {
						listener.onFacebookError(e);
					} else {
						e.printStackTrace();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(List<Account> result) {
		super.onPostExecute(result);

		AccountsResultListener listener = mListener.get();
		if (null != result && null != listener) {
			listener.onAccountsLoaded(result);
		}
	}

}
