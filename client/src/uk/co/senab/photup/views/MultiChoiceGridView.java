package uk.co.senab.photup.views;

import java.util.Collection;
import java.util.HashMap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.GridView;

public class MultiChoiceGridView extends GridView implements OnItemClickListener {

	public static interface OnItemCheckedListener {
		void onItemCheckChanged(AdapterView<?> parent, View view, int position, long id, boolean checked);
	}

	private final HashMap<Long, Boolean> mCheckedMap = new HashMap<Long, Boolean>();

	private OnItemCheckedListener mCheckedListener;

	public MultiChoiceGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> gridView, View view, int position, long id) {
		boolean newValue = !isItemIdChecked(id);

		if (newValue) {
			mCheckedMap.put(id, true);
		} else {
			mCheckedMap.remove(id);
		}
		
		((Checkable) view).setChecked(newValue);

		if (null != mCheckedListener) {
			mCheckedListener.onItemCheckChanged(gridView, view, position, id, newValue);
		}
	}

	public void setCheckedItems(Collection<Long> selectedIds) {
		for (Long id : selectedIds) {
			mCheckedMap.put(id, true);
		}
	}

	public Collection<Long> getSelectedIds() {
		return mCheckedMap.keySet();
	}

	public boolean isItemIdChecked(long id) {
		Boolean value = mCheckedMap.get(id);
		if (null != value) {
			return value.booleanValue();
		}
		return false;
	}

	public void setOnItemCheckedListener(OnItemCheckedListener l) {
		mCheckedListener = l;
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		// NO-OP for now
	}

}
