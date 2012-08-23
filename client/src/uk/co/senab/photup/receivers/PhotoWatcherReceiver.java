package uk.co.senab.photup.receivers;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PreferenceConstants;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class PhotoWatcherReceiver extends BroadcastReceiver {

	static final String KEY_LAST_UPLOADED = "last_uploaded_uri";
	static final String LOG_TAG = "PhotoWatcherReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Constants.DEBUG) {
			Log.d(LOG_TAG, "onReceive");
		}

		Uri uri = intent.getData();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (null != uri && prefs.getBoolean(PreferenceConstants.PREF_INSTANT_UPLOAD_ENABLED, false)) {

			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "Got Photo with URI: " + uri.toString());
			}

			final String albumId = prefs.getString(PreferenceConstants.PREF_INSTANT_UPLOAD_ALBUM_ID, null);
			if (TextUtils.isEmpty(albumId)) {
				if (Constants.DEBUG) {
					Log.d(LOG_TAG, "No album set!!!");
				}
				return;
			}

			final PhotoUpload upload = PhotoUpload.getSelection(uri);
			final String qualityId = prefs.getString(PreferenceConstants.PREF_INSTANT_UPLOAD_QUALITY, null);
			final String filterId = prefs.getString(PreferenceConstants.PREF_INSTANT_UPLOAD_FILTER, "0");

			upload.setUploadParams(Account.getAccountFromSession(context), albumId,
					UploadQuality.mapFromPreference(qualityId));
			upload.setFilterUsed(Filter.mapFromPref(filterId));

			PhotoUploadController controller = PhotoUploadController.getFromContext(context);

			if (controller.addPhotoToUploads(upload)) {
				if (Constants.DEBUG) {
					Log.d(LOG_TAG, "Starting Upload for URI: " + uri.toString());
				}
				context.startService(Utils.getUploadAllIntent(context));
			}

		}
	}
}
