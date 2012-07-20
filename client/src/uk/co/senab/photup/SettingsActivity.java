package uk.co.senab.photup;

import java.util.List;

import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import android.os.Bundle;
import android.preference.ListPreference;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.facebook.android.FacebookError;

@SuppressWarnings("deprecation")
public class SettingsActivity extends SherlockPreferenceActivity implements AlbumsResultListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_prefs);

		PhotupApplication.getApplication(this).getAlbums(this, false);
	}

	public void onFacebookError(FacebookError e) {
		// NO-OP
	}

	public void onAlbumsLoaded(List<Album> albums) {
		String[] entries = new String[albums.size()];
		String[] entryValues = new String[albums.size()];

		for (int i = 0, z = albums.size(); i < z; i++) {
			final Album album = albums.get(i);
			entries[i] = album.getName();
			entryValues[i] = album.getId();
		}

		ListPreference albumsPref = (ListPreference) findPreference(PreferenceConstants.PREF_INSTANT_UPLOAD_ALBUM_ID);

		albumsPref.setEntries(entries);
		albumsPref.setEntryValues(entryValues);
		albumsPref.setEnabled(true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		PhotupApplication.getApplication(this).checkInstantUploadReceiverState();
	}

}
