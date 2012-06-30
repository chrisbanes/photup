package uk.co.senab.photup.views;

import java.util.Collection;
import java.util.HashSet;

import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.GridView;

public class MultiChoiceGridView extends GridView implements OnItemClickListener {

	public static interface OnItemCheckedListener {
		void onItemCheckChanged(View view, PhotoUpload upload, boolean checked);
	}

	private final HashSet<PhotoUpload> mCheckedItems = new HashSet<PhotoUpload>();

	private OnItemCheckedListener mCheckedListener;

	public MultiChoiceGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> gridView, View view, int position, long id) {
		PhotoUpload object = (PhotoUpload) view.getTag();
		setItemChecked(view, object, !isPhotoUploadChecked(object));
	}

	public void setItemChecked(View view, final PhotoUpload item, final boolean checked) {
		if (checked) {
			mCheckedItems.add(item);
		} else {
			mCheckedItems.remove(item);
		}

		((Checkable) view).setChecked(checked);

		if (null != mCheckedListener) {
			mCheckedListener.onItemCheckChanged(view, item, checked);
		}
	}

	public void setCheckedItems(Collection<PhotoUpload> items) {
		mCheckedItems.clear();
		mCheckedItems.addAll(items);
	}

	public boolean isPhotoUploadChecked(PhotoUpload item) {
		return mCheckedItems.contains(item);
	}

	public void setOnItemCheckedListener(OnItemCheckedListener l) {
		mCheckedListener = l;
	}

}
