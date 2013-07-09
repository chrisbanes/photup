/*******************************************************************************
 * Copyright 2013 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.	
 *******************************************************************************/
package uk.co.senab.photup.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PreferenceConstants;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.util.Utils;

public class PhotoWatcherReceiver extends BroadcastReceiver {

    static final String KEY_LAST_UPLOADED = "last_uploaded_uri";
    static final String LOG_TAG = "PhotoWatcherReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Flags.DEBUG) {
            Log.d(LOG_TAG, "onReceive");
        }

        Uri uri = intent.getData();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Account account = Account.getAccountFromSession(context);

        if (null != account && null != uri && prefs
                .getBoolean(PreferenceConstants.PREF_INSTANT_UPLOAD_ENABLED, false)) {
            if (Flags.DEBUG) {
                Log.d(LOG_TAG, "Got Photo with URI: " + uri.toString());
            }

            final boolean uploadWhileRoaming = prefs
                    .getBoolean(PreferenceConstants.PREF_INSTANT_UPLOAD_IF_ROAMING,
                            false);
            if (ConnectivityReceiver.isConnectedViaCellularRoaming(context)
                    && !uploadWhileRoaming) {
                if (Flags.DEBUG) {
                    Log.d(LOG_TAG, "Instant Upload disabled because we're roaming.");
                }
                return;
            }

            final String albumId = prefs
                    .getString(PreferenceConstants.PREF_INSTANT_UPLOAD_ALBUM_ID, null);
            if (TextUtils.isEmpty(albumId)) {
                if (Flags.DEBUG) {
                    Log.d(LOG_TAG, "No album set!!!");
                }
                return;
            }

            final PhotoUpload upload = PhotoUpload.getSelection(uri);
            final String qualityId = prefs
                    .getString(PreferenceConstants.PREF_INSTANT_UPLOAD_QUALITY, null);
            final String filterId = prefs
                    .getString(PreferenceConstants.PREF_INSTANT_UPLOAD_FILTER, "1");

            upload.setUploadParams(account, albumId, UploadQuality.mapFromPreference(qualityId));
            upload.setFilterUsed(Filter.mapFromPref(filterId));

            PhotoUploadController controller = PhotoUploadController.getFromContext(context);

            if (controller.addUpload(upload)) {
                if (ConnectivityReceiver.isConnected(context)) {
                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Adding Upload for URI: " + uri.toString());
                    }
                    context.startService(Utils.getUploadAllIntent(context));
                }
            }
        }
    }
}
