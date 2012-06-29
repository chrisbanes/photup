package uk.co.senab.photup.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.co.senab.photup.R;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.views.MultiChoiceGridView;
import uk.co.senab.photup.views.PhotupImageView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;

public class PhotosBaseAdapter extends BaseAdapter {

	private List<Long> mItems;
	private MultiChoiceGridView mParent;

	private final BitmapLruCache mCache;
	private final Context mContext;
	private final LayoutInflater mLayoutInflater;

	public PhotosBaseAdapter(Context context, BitmapLruCache cache) {
		mContext = context;
		mCache = cache;
		mItems = new ArrayList<Long>();
		mLayoutInflater = LayoutInflater.from(mContext);
	}

	public void setParentView(MultiChoiceGridView gridView) {
		mParent = gridView;
	}

	public int getCount() {
		return mItems.size();
	}

	public long getItemId(int position) {
		return getItem(position);
	}

	public Long getItem(int position) {
		return mItems.get(position);
	}
	
	public void setItems(Collection<Long> items) {
		mItems.clear();
		mItems.addAll(items);
		notifyDataSetChanged();
	}

	public View getView(int position, View view, ViewGroup parent) {
		if (null == view) {
			view = mLayoutInflater.inflate(R.layout.item_user_photo, parent, false);
		}

		PhotupImageView iv = (PhotupImageView) view.findViewById(R.id.iv_photo);

		long id = getItem(position);
		iv.requestThumbnailId(id, mCache);

		if (null != mParent) {
			((Checkable) view).setChecked(mParent.isItemIdChecked(id));
		}

		return view;
	}

}
