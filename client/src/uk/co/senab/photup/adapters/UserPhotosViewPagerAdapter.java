package uk.co.senab.photup.adapters;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.util.CursorPagerAdapter;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import uk.co.senab.photup.views.MultiTouchImageView;
import uk.co.senab.photup.views.PhotoTagItemLayout;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

public class UserPhotosViewPagerAdapter extends CursorPagerAdapter {

	private final PhotoUploadController mController;
	private final OnSingleTapListener mTapListener;
	private final OnPickFriendRequestListener mFriendPickRequestListener;

	public UserPhotosViewPagerAdapter(Context context, OnSingleTapListener tapListener,
			OnPickFriendRequestListener friendRequestListener) {
		super(context, null, 0);
		mTapListener = tapListener;
		mFriendPickRequestListener = friendRequestListener;

		PhotupApplication app = PhotupApplication.getApplication(context);
		mController = app.getPhotoUploadController();
	}

	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {	
		final PhotoSelection upload = MediaStoreCursorHelper.photosCursorToSelection(MediaStoreCursorHelper.MEDIA_STORE_CONTENT_URI,
				cursor);

		PhotoTagItemLayout view = null;

		if (null != upload) {
			view = new PhotoTagItemLayout(mContext, mController, upload, mFriendPickRequestListener);
			view.setPosition(cursor.getPosition());

			upload.setFaceDetectionListener(view);

			MultiTouchImageView imageView = view.getImageView();
			imageView.requestFullSize(upload, true, null);
			imageView.setSingleTapListener(mTapListener);
		}

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// NO-OP
	}

	@Override
	public void destroyItem(View container, int position, Object object) {
		PhotoTagItemLayout view = (PhotoTagItemLayout) object;

		MultiTouchImageView imageView = view.getImageView();
		imageView.cancelRequest();

		((ViewPager) container).removeView(view);
	}

}
