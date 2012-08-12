package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.tasks.MediaStoreAsyncTask;
import uk.co.senab.photup.tasks.MediaStoreAsyncTask.MediaStoreResultListener;
import android.content.Context;

public class UserPhotosViewPagerAdapter extends SelectedPhotosViewPagerAdapter implements MediaStoreResultListener {

	public UserPhotosViewPagerAdapter(Context context, OnSingleTapListener tapListener,
			OnPickFriendRequestListener friendRequestListener) {
		super(context, tapListener, friendRequestListener);
	}

	public void refresh() {
		new MediaStoreAsyncTask(getContext(), this).execute();
	}

	public void onPhotosLoaded(List<PhotoSelection> friends) {
		setData(friends);
	}

}
