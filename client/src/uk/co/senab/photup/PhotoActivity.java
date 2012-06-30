package uk.co.senab.photup;

import java.util.HashSet;

import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.listeners.PhotoListDisplayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PhotoActivity extends SherlockFragmentActivity implements OnPhotoSelectionChangedListener,
		BitmapCacheProvider, TabListener {

	static final int TAB_PHOTOS = 0;
	static final int TAB_SELECTED = 1;

	private HashSet<Long> mSelectedIds = new HashSet<Long>();
	private BitmapLruCache mCache;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Needs to be done before super.onCreate
		mCache = new BitmapLruCache(this);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));
		ab.addTab(ab.newTab().setText(getSelectedTabTitle()).setTag(TAB_SELECTED).setTabListener(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_photo_grid, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	public void onPhotoChosen(long id, boolean added) {
		if (added) {
			mSelectedIds.add(id);
		} else {
			mSelectedIds.remove(id);
		}

		getSupportActionBar().getTabAt(1).setText(getSelectedTabTitle());
	}

	private CharSequence getSelectedTabTitle() {
		return getString(R.string.tab_selected_photos, mSelectedIds.size());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mCache.evictAll();
	}

	public BitmapLruCache getBitmapCache() {
		return mCache;
	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		final int id = (Integer) tab.getTag();
		PhotoListDisplayer newFragment;

		switch (id) {
			case TAB_SELECTED:
				newFragment = new SelectedPhotosFragment();
				break;
			case TAB_PHOTOS:
			default:
				newFragment = new UserPhotosFragment();
				break;
		}

		newFragment.setSelectedPhotos(mSelectedIds);

		ft.replace(R.id.fl_photo_fragments, (Fragment) newFragment);
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

}
