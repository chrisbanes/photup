package uk.co.senab.photup.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropImageView extends MultiTouchImageView implements View.OnTouchListener {

	private float mLastX, mLastY;
	private int mMotionEdge;

	private HighlightView mCrop = null;
	private HighlightView mMotionHighlightView = null;

	public CropImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setZoomable(true);

		// Override the touch listener so that only we get the touch events
		setOnTouchListener(this);
	}

	public boolean onTouch(View view, MotionEvent event) {
		if (mCrop == null) {
			return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				int edge = mCrop.getHit(event.getX(), event.getY());
				if (edge != HighlightView.GROW_NONE) {
					mMotionEdge = edge;
					mMotionHighlightView = mCrop;
					mLastX = event.getX();
					mLastY = event.getY();
					mMotionHighlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move
							: HighlightView.ModifyMode.Grow);
				}
				break;

			case MotionEvent.ACTION_UP:
				if (mMotionHighlightView != null) {
					mMotionHighlightView.setMode(HighlightView.ModifyMode.None);
				}
				mMotionHighlightView = null;
				break;

			case MotionEvent.ACTION_MOVE:
				if (mMotionHighlightView != null) {
					mMotionHighlightView.handleMotion(mMotionEdge, event.getX() - mLastX, event.getY() - mLastY);
					mLastX = event.getX();
					mLastY = event.getY();
				}
				break;
		}

		return true;
	}

	public void setHighlight(HighlightView hv) {
		mCrop = hv;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mCrop != null) {
			mCrop.draw(canvas);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (getCachedBitmapWrapper() != null && getCachedBitmapWrapper().getBitmap() != null) {
			if (mCrop != null) {
				mCrop.mMatrix.set(getImageMatrix());
				mCrop.invalidate();
			}
		}
	}

}