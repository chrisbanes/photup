package uk.co.senab.photup.adapters;

import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.views.PhotoItemLayout;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class CameraBaseAdapter extends BaseAdapter {

	private final LayoutInflater mLayoutInflater;

	public CameraBaseAdapter(Context context) {
		mLayoutInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return 1;
	}

	public long getItemId(int position) {
		return position;
	}

	public PhotoSelection getItem(int position) {
		return null;
	}

	public View getView(int position, View view, ViewGroup parent) {
		if (null == view) {
			view = mLayoutInflater.inflate(R.layout.item_grid_photo, parent, false);
		}

		PhotoItemLayout layout = (PhotoItemLayout) view;
		
		ImageView iv = layout.getImageView();
		iv.setImageResource(R.drawable.ic_menu_camera);
		iv.setScaleType(ScaleType.CENTER_INSIDE);

		return view;
	}

}
