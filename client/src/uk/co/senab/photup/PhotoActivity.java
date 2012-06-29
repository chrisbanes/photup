package uk.co.senab.photup;

import java.util.HashSet;

import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PhotoActivity extends SherlockFragmentActivity implements OnPhotoSelectionChangedListener {

	private HashSet<Long> mSelectedIds = new HashSet<Long>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_photo_grid, menu);

		MenuItem item = menu.findItem(R.id.menu_num_items);
		item.setTitle(String.valueOf(mSelectedIds.size()));

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
		
		supportInvalidateOptionsMenu();
	}

}
