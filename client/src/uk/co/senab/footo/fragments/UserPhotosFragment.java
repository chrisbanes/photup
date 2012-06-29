package uk.co.senab.footo.fragments;

import uk.co.senab.footo.adapters.PhotosAdapter;
import uk.co.senab.footo.views.MultiChoiceGridView;
import uk.co.senab.photup.R;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

public class UserPhotosFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	static final int LOADER_USER_PHOTOS = 0x01;

	private MultiChoiceGridView mPhotoGrid;
	private PhotosAdapter mAdapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(LOADER_USER_PHOTOS, null, this);
		mAdapter = new PhotosAdapter(getActivity(), R.layout.item_user_photo, null, true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mPhotoGrid = (MultiChoiceGridView) view.findViewById(R.id.gv_users_photos);
		mPhotoGrid.setAdapter(mAdapter);
		mAdapter.setParentView(mPhotoGrid);

		return view;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		String[] projection = { ImageColumns._ID };

		CursorLoader cursorLoader = new CursorLoader(getActivity(), Images.Media.EXTERNAL_CONTENT_URI, projection,
				null, null, Images.Media.DATE_ADDED + " desc");

		return cursorLoader;

	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

}
