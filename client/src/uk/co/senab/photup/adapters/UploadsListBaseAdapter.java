package uk.co.senab.photup.adapters;

import java.util.List;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.views.UploadItemLayout;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class UploadsListBaseAdapter extends BaseAdapter {

	private List<PhotoSelection> mItems;

	private final Context mContext;
	private final LayoutInflater mLayoutInflater;
	private final PhotoUploadController mController;

	public UploadsListBaseAdapter(Context context) {
		mContext = context;
		mLayoutInflater = LayoutInflater.from(mContext);

		PhotupApplication app = PhotupApplication.getApplication(context);
		mController = app.getPhotoUploadController();
		mItems = mController.getUploadingPhotoUploads();
	}

	public int getCount() {
		return null != mItems ? mItems.size() : 0;
	}

	public long getItemId(int position) {
		return position;
	}

	public PhotoSelection getItem(int position) {
		return mItems.get(position);
	}

	public View getView(int position, View view, ViewGroup parent) {
		if (null == view) {
			view = mLayoutInflater.inflate(R.layout.item_list_upload, parent, false);
		}

		UploadItemLayout layout = (UploadItemLayout) view;
		layout.setPhotoSelection(getItem(position));

		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		mItems = mController.getUploadingPhotoUploads();
		super.notifyDataSetChanged();
	}

}
