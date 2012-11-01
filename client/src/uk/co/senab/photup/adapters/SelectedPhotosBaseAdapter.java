package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class SelectedPhotosBaseAdapter extends BaseAdapter {

	private List<PhotoUpload> mItems;

	private final Context mContext;
	private final LayoutInflater mLayoutInflater;
	private final PhotoUploadController mController;

	public SelectedPhotosBaseAdapter(Context context) {
		mContext = context;
		mLayoutInflater = LayoutInflater.from(mContext);

		PhotupApplication app = PhotupApplication.getApplication(context);
		mController = app.getPhotoUploadController();
		mItems = mController.getSelected();
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
			view = mLayoutInflater.inflate(R.layout.item_grid_photo_selected, parent, false);
		}

		PhotoItemLayout layout = (PhotoItemLayout) view;
		PhotupImageView iv = layout.getImageView();

		final PhotoUpload upload = getItem(position);

		iv.requestThumbnail(upload, true);
		layout.setShowCaption(true);
		layout.setAnimateWhenChecked(false);
		layout.setPhotoSelection(upload);
		layout.setChecked(true);

		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		mItems = mController.getSelected();
		super.notifyDataSetChanged();
	}

}
