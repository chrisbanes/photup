package uk.co.senab.footo.views;

import java.util.HashMap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.GridView;

public class MultiChoiceGridView extends GridView implements OnItemClickListener {

	private final HashMap<Long, Boolean> mCheckedMap = new HashMap<Long, Boolean>();

	public MultiChoiceGridView(Context context, AttributeSet attrs) {
		super(context, attrs);

		super.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> gridView, View view, int position, long id) {
		boolean newValue = !isItemIdChecked(id);
		mCheckedMap.put(id, newValue);
		((Checkable) view).setChecked(newValue);
	}

	public boolean isItemIdChecked(long id) {
		Boolean value = mCheckedMap.get(id);
		if (null != value) {
			return value.booleanValue();
		}
		return false;
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		// NO-OP for now
	}

}
