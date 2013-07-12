/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup;

import com.crittercism.app.Crittercism;
import com.facebook.android.FacebookError;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.receivers.InstantUploadReceiver;
import uk.co.senab.photup.tasks.AccountsAsyncTask;
import uk.co.senab.photup.tasks.AccountsAsyncTask.AccountsResultListener;
import uk.co.senab.photup.tasks.FriendsAsyncTask;
import uk.co.senab.photup.tasks.FriendsAsyncTask.FriendsResultListener;
import uk.co.senab.photup.tasks.PhotupThreadFactory;
import uk.co.senab.photup.util.Utils;

public class PhotupApplication extends Application
        implements FriendsResultListener, AccountsResultListener {

    static final String LOG_TAG = "PhotupApplication";
    public static final String THREAD_FILTERS = "filters_thread";

    static final float EXECUTOR_POOL_SIZE_PER_CORE = 1.5f;

    private ExecutorService mMultiThreadExecutor, mSingleThreadExecutor, mDatabaseThreadExecutor;
    private BitmapLruCache mImageCache;

    private FriendsResultListener mFriendsListener;
    private ArrayList<FbUser> mFriends;

    private AccountsResultListener mAccountsListener;
    private ArrayList<Account> mAccounts;

    private boolean mIsFriendsLoaded, mIsAccountsLoaded;

    private PhotoUploadController mPhotoController;

    public static PhotupApplication getApplication(Context context) {
        return (PhotupApplication) context.getApplicationContext();
    }

    public ExecutorService getMultiThreadExecutorService() {
        if (null == mMultiThreadExecutor || mMultiThreadExecutor.isShutdown()) {
            final int numThreads = Math.round(Runtime.getRuntime().availableProcessors()
                    * EXECUTOR_POOL_SIZE_PER_CORE);
            mMultiThreadExecutor = Executors
                    .newFixedThreadPool(numThreads, new PhotupThreadFactory());

            if (Flags.DEBUG) {
                Log.d(LOG_TAG, "MultiThreadExecutor created with " + numThreads + " threads");
            }
        }
        return mMultiThreadExecutor;
    }

    public ExecutorService getPhotoFilterThreadExecutorService() {
        if (null == mSingleThreadExecutor || mSingleThreadExecutor.isShutdown()) {
            mSingleThreadExecutor = Executors
                    .newSingleThreadExecutor(new PhotupThreadFactory(THREAD_FILTERS));
        }
        return mSingleThreadExecutor;
    }

    public ExecutorService getDatabaseThreadExecutorService() {
        if (null == mDatabaseThreadExecutor || mDatabaseThreadExecutor.isShutdown()) {
            mDatabaseThreadExecutor = Executors.newSingleThreadExecutor(new PhotupThreadFactory());
        }
        return mDatabaseThreadExecutor;
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

    @SuppressWarnings("deprecation")
    public int getSmallestScreenDimension() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return Math.min(display.getHeight(), display.getWidth());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Flags.ENABLE_BUG_TRACKING) {
            Crittercism.init(this, Constants.CRITTERCISM_API_KEY);
        }

        checkInstantUploadReceiverState();

        mPhotoController = new PhotoUploadController(this);
        mFriends = new ArrayList<FbUser>();
        mAccounts = new ArrayList<Account>();

        // TODO Need to check for Facebook login
        Session session = Session.restore(this);
        if (null != session) {
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

            if (Flags.ENABLE_DB_PERSISTENCE) {
                HashMap<String, FbUser> friendsMap = new HashMap<String, FbUser>();
                for (FbUser friend : friends) {
                    friendsMap.put(friend.getId(), friend);
                }
                mPhotoController.populateDatabaseItemsFromFriends(friendsMap);
            }
        }

        setFriendsLoaded();
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
                        account.preload(this);
                        break;
                    }
                }
            }

            if (Flags.ENABLE_DB_PERSISTENCE) {
                HashMap<String, Account> accountsMap = new HashMap<String, Account>();
                for (Account account : accounts) {
                    accountsMap.put(account.getId(), account);
                }
                mPhotoController.populateDatabaseItemsFromAccounts(accountsMap);
            }
        }

        setAccountsLoaded();
    }

    private void setFriendsLoaded() {
        mIsFriendsLoaded = true;

        if (isDataLoaded()) {
            onDataLoaded();
        }
    }

    private void setAccountsLoaded() {
        mIsAccountsLoaded = true;

        if (isDataLoaded()) {
            onDataLoaded();
        }
    }

    private boolean isDataLoaded() {
        return mIsFriendsLoaded && mIsAccountsLoaded;
    }

    private void onDataLoaded() {
        if (mPhotoController.hasWaitingUploads()) {
            startService(Utils.getUploadAllIntent(this));
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
        final boolean enabled = prefs
                .getBoolean(PreferenceConstants.PREF_INSTANT_UPLOAD_ENABLED, false);

        final ComponentName component = new ComponentName(this, InstantUploadReceiver.class);
        final PackageManager pkgMgr = getPackageManager();

        switch (pkgMgr.getComponentEnabledSetting(component)) {
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                if (enabled) {
                    pkgMgr.setComponentEnabledSetting(component,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Enabled Instant Upload Receiver");
                    }
                }
                break;

            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                if (!enabled) {
                    pkgMgr.setComponentEnabledSetting(component,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Disabled Instant Upload Receiver");
                    }
                }
                break;
        }
    }

}
