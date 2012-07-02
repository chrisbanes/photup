package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.MultiTouchImageView;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

public class PhotoViewPagerAdapter extends PagerAdapter {

	private final Context mContext;
	private final PhotoSelectionController mController;
	private final LayoutInflater mLayoutInflater;
	private final OnTouchListener mOnTouchListener;

	private List<PhotoUpload> mItems;

	public PhotoViewPagerAdapter(Context context, OnTouchListener touchListener) {
		mContext = context;
		mLayoutInflater = LayoutInflater.from(mContext);
		mOnTouchListener = touchListener;

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
		View view = mLayoutInflater.inflate(R.layout.item_photo_viewer, (ViewGroup) container, false);
		
		PhotoUpload upload = mItems.get(position);

		MultiTouchImageView imageView = (MultiTouchImageView) view.findViewById(R.id.iv_photo);
		imageView.requestFullSize(upload);
		imageView.setZoomable(true);
		
		view.setTag(upload);
		view.setOnTouchListener(mOnTouchListener);
		((ViewPager) container).addView(view);
		return imageView;
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
