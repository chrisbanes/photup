package uk.co.senab.footo.views;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

public class CheckableImageView extends RecycleableImageView implements Checkable {

	private static final int[] CheckedStateSet = { R.attr.state_checked };

	private boolean mChecked = false;

	public CheckableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean isChecked() {
		return mChecked;
	}

	public void setChecked(boolean b) {
		if (b != mChecked) {
			mChecked = b;
			refreshDrawableState();
		}
	}

	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CheckedStateSet);
		}
		return drawableState;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		invalidate();
	}

}
