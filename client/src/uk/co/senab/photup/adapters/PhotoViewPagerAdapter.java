package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.MultiTouchImageView;
import uk.co.senab.photup.views.PhotoTagItemLayout;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class PhotoViewPagerAdapter extends PagerAdapter {

	private final Context mContext;
	private final PhotoSelectionController mController;
	private final OnSingleTapListener mTapListener;

	private List<PhotoUpload> mItems;

	public PhotoViewPagerAdapter(Context context, OnSingleTapListener tapListener) {
		mContext = context;
		mTapListener = tapListener;

		PhotupApplication app = PhotupApplication.getApplication(context);
		mController = app.getPhotoSelectionController();
		mItems = mController.getSelectedPhotoUploads();
	}

	@Override
	public void destroyItem(View container, int position, Object object) {
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public int getCount() {
		return null != mItems ? mItems.size() : 0;
	}

	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	public PhotoUpload getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public Object instantiateItem(View container, int position) {
		PhotoUpload upload = mItems.get(position);

		PhotoTagItemLayout view = new PhotoTagItemLayout(mContext, upload);

		MultiTouchImageView imageView = view.getImageView();
		imageView.requestFullSize(upload, true);
		imageView.setSingleTapListener(mTapListener);

		view.setTag(upload);
		((ViewPager) container).addView(view);
		return view;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

	@Override
	public void notifyDataSetChanged() {
		mItems = mController.getSelectedPhotoUploads();
		super.notifyDataSetChanged();
	}

}
