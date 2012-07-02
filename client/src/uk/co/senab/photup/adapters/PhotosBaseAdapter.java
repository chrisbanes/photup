package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.PhotupImageView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class PhotosBaseAdapter extends BaseAdapter {

	private List<PhotoUpload> mItems;

	private final BitmapLruCache mCache;
	private final Context mContext;
	private final LayoutInflater mLayoutInflater;
	private final PhotoSelectionController mController;

	public PhotosBaseAdapter(Context context) {
		mContext = context;
		mLayoutInflater = LayoutInflater.from(mContext);

		PhotupApplication app = PhotupApplication.getApplication(context);
		mCache = app.getImageCache();
		mController = app.getPhotoSelectionController();
		mItems = mController.getSelectedPhotoUploads();
	}

	public int getCount() {
		return null != mItems ? mItems.size() : 0;
	}

	public long getItemId(int position) {
		return position;
	}

	public PhotoUpload getItem(int position) {
		return mItems.get(position);
	}

	public View getView(int position, View view, ViewGroup parent) {
		if (null == view) {
			view = mLayoutInflater.inflate(R.layout.item_selected_photo, parent, false);
		}

		PhotupImageView iv = (PhotupImageView) view.findViewById(R.id.iv_photo);
		iv.requestThumbnail(getItem(position), mCache);

		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		mItems = mController.getSelectedPhotoUploads();
		super.notifyDataSetChanged();
	}

}
