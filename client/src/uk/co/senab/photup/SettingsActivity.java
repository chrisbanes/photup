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

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.facebook.android.FacebookError;

import android.os.Bundle;
import android.preference.ListPreference;

import java.util.List;

import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;

@SuppressWarnings("deprecation")
public class SettingsActivity extends SherlockPreferenceActivity implements AlbumsResultListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_prefs);

        Account mainAccount = PhotupApplication.getApplication(this).getMainAccount();
        if (null != mainAccount) {
            mainAccount.getAlbums(getApplicationContext(), this, false);
        }

        populateFiltersPref();
    }

    public void onFacebookError(FacebookError e) {
        // NO-OP
    }

    public void onAlbumsLoaded(Account account, List<Album> albums) {
        String[] entries = new String[albums.size()];
        String[] entryValues = new String[albums.size()];

        for (int i = 0, z = albums.size(); i < z; i++) {
            final Album album = albums.get(i);
            entries[i] = album.getName();
            entryValues[i] = album.getId();
        }

        ListPreference albumsPref = (ListPreference) findPreference(
                PreferenceConstants.PREF_INSTANT_UPLOAD_ALBUM_ID);

        albumsPref.setEntries(entries);
        albumsPref.setEntryValues(entryValues);
        albumsPref.setEnabled(true);
    }

    private void populateFiltersPref() {
        ListPreference filtersPref = (ListPreference) findPreference(
                PreferenceConstants.PREF_INSTANT_UPLOAD_FILTER);
        Filter[] filters = Filter.values();

        String[] entries = new String[filters.length];
        String[] entryValues = new String[filters.length];

        for (int i = 0, z = filters.length; i < z; i++) {
            Filter filter = filters[i];
            entries[i] = getString(filter.getLabelId());
            entryValues[i] = filter.mapToPref();
        }

        filtersPref.setEntries(entries);
        filtersPref.setEntryValues(entryValues);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PhotupApplication.getApplication(this).checkInstantUploadReceiverState();
    }

}
