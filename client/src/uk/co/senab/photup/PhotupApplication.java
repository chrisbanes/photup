package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.receivers.PhotoWatcherReceiver;
import uk.co.senab.photup.tasks.AccountsAsyncTask;
import uk.co.senab.photup.tasks.AccountsAsyncTask.AccountsResultListener;
import uk.co.senab.photup.tasks.FriendsAsyncTask;
import uk.co.senab.photup.tasks.FriendsAsyncTask.FriendsResultListener;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.facebook.android.FacebookError;

@ReportsCrashes(formKey = Constants.ACRA_GOOGLE_DOC_ID, mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast)
public class PhotupApplication extends Application implements FriendsResultListener,
		AccountsResultListener {

	static final String LOG_TAG = "PhotupApplication";
	public static final String THREAD_FILTERS = "filters_thread";

	static final int EXECUTOR_CORE_POOL_SIZE_PER_CORE = 2;
	static final int EXECUTOR_MAX_POOL_SIZE_PER_CORE = 5;

	private ExecutorService mMultiThreadExecutor, mSingleThreadExecutor;
	private BitmapLruCache mImageCache;

	private FriendsResultListener mFriendsListener;
	private ArrayList<FbUser> mFriends;

	private AccountsResultListener mAccountsListener;
	private ArrayList<Account> mAccounts;

	private final PhotoUploadController mPhotoController = new PhotoUploadController();

	public static PhotupApplication getApplication(Context context) {
		return (PhotupApplication) context.getApplicationContext();
	}

	public ExecutorService getMultiThreadExecutorService() {
		if (null == mMultiThreadExecutor) {
			mMultiThreadExecutor = createExecutor();
		}
		return mMultiThreadExecutor;
	}

	public ExecutorService getSingleThreadExecutorService() {
		if (null == mSingleThreadExecutor) {
			mSingleThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {

				public Thread newThread(Runnable r) {
					return new Thread(r, THREAD_FILTERS);
				}
			});
		}
		return mSingleThreadExecutor;
	}

	public BitmapLruCache getImageCache() {
		if (null == mImageCache) {
			mImageCache = new BitmapLruCache(this, Constants.IMAGE_CACHE_HEAP_PERCENTAGE);
		}
		return mImageCache;
	}

	public PhotoUploadController getPhotoUploadController() {
		return mPhotoController;
	}

	private static ExecutorService createExecutor() {
		final int numCores = Runtime.getRuntime().availableProcessors();

		return new ThreadPoolExecutor(numCores * EXECUTOR_CORE_POOL_SIZE_PER_CORE, numCores
				* EXECUTOR_MAX_POOL_SIZE_PER_CORE, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	@SuppressWarnings("deprecation")
	public int getSmallestScreenDimension() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return Math.min(display.getHeight(), display.getWidth());
	}

	@Override
	public void onCreate() {
		if (Constants.ENABLE_ACRA) {
			ACRA.init(this);
		}

		super.onCreate();

		checkInstantUploadReceiverState();

		mFriends = new ArrayList<FbUser>();
		mAccounts = new ArrayList<Account>();

		// TODO Need to check for Facebook login
		Session session = Session.restore(this);
		if (null != session) {
			mPhotoController.populateFromDatabase(this);
			
			getAccounts(null, false);
			getFriends(null);
		}
	}

	public void getFriends(FriendsResultListener listener) {
		if (mFriends.isEmpty()) {
			mFriendsListener = listener;
			new FriendsAsyncTask(this, this).execute();
		} else {
			listener.onFriendsLoaded(mFriends);
		}
	}
	
	public Account getMainAccount() {
		for (Account account : mAccounts) {
			if (account.isMainAccount()) {
				return account;
			}
		}
		return null;
	}

	public void getAccounts(AccountsResultListener listener, boolean forceRefresh) {
		if (forceRefresh || mAccounts.isEmpty()) {
			mAccountsListener = listener;
			new AccountsAsyncTask(this, this).execute();
		} else {
			listener.onAccountsLoaded(mAccounts);
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();

		if (null != mImageCache) {
			mImageCache.trimMemory();
		}
	}

	public void onFriendsLoaded(List<FbUser> friends) {
		mFriends.clear();

		if (null != friends) {
			mFriends.addAll(friends);

			if (null != mFriendsListener && mFriendsListener != this) {
				mFriendsListener.onFriendsLoaded(mFriends);
				mFriendsListener = null;
			}
			
			HashMap<String, FbUser> friendsMap = new HashMap<String, FbUser>();
			for (FbUser friend : friends) {
				friendsMap.put(friend.getId(), friend);
			}
			mPhotoController.populateDatabaseItemsFromFriends(friendsMap);
		}
	}

	public void onAccountsLoaded(List<Account> accounts) {
		mAccounts.clear();

		if (null != accounts && !accounts.isEmpty()) {
			mAccounts.addAll(accounts);
			if (null != mAccountsListener && mAccountsListener != this) {
				mAccountsListener.onAccountsLoaded(mAccounts);
				mAccountsListener = null;

			} else if (!mAccounts.isEmpty()) {
				// PRELOAD Main Account's Data
				for (Account account : mAccounts) {
					if (account.isMainAccount()) {
						account.getAlbums(null, false);
						account.getGroups(null, false);
						account.getEvents(null, false);
						break;
					}
				}
			}
			
			HashMap<String,Account> accountsMap = new HashMap<String, Account>();
			for (Account account : accounts) {
				accountsMap.put(account.getId(), account);
			}
			mPhotoController.populateDatabaseItemsFromAccounts(accountsMap);
		}
	}

	public void onFacebookError(FacebookError e) {
		Log.e("PhotupApplication", "FacebookError");
		e.printStackTrace();

		Session.clearSavedSession(this);
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void checkInstantUploadReceiverState() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean enabled = prefs.getBoolean(PreferenceConstants.PREF_INSTANT_UPLOAD_ENABLED, false);

		final ComponentName component = new ComponentName(this, PhotoWatcherReceiver.class);
		final PackageManager pkgMgr = getPackageManager();

		switch (pkgMgr.getComponentEnabledSetting(component)) {
			case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
				if (enabled) {
					pkgMgr.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
							PackageManager.DONT_KILL_APP);
					if (Constants.DEBUG) {
						Log.d(LOG_TAG, "Enabled Instant Upload Receiver");
					}
				}
				break;

			case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
			case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
				if (!enabled) {
					pkgMgr.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
							PackageManager.DONT_KILL_APP);
					if (Constants.DEBUG) {
						Log.d(LOG_TAG, "Disabled Instant Upload Receiver");
					}
				}
				break;
		}
	}

}
