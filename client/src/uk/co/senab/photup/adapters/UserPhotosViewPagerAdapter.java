package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Context;

public class UserPhotosViewPagerAdapter extends SelectedPhotosViewPagerAdapter {

	public UserPhotosViewPagerAdapter(Context context, OnSingleTapListener tapListener,
			OnPickFriendRequestListener friendRequestListener) {
		super(context, tapListener, friendRequestListener);
	}
	
	@Override
	public List<PhotoSelection> refreshData(Context context) {
		return PhotupApplication.getApplication(context).getMediaStorePhotos();
	}

}
