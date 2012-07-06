package uk.co.senab.photup.views;

import uk.co.senab.photup.listeners.OnPhotoTapListener;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class MultiTouchImageView extends PhotupImageView implements VersionedGestureDetector.OnGestureListener,
		GestureDetector.OnDoubleTapListener {

	static final String LOG_TAG = "MultiTouchImageView";

	static interface OnMatrixChangedListener {
		void onMatrixChanged(RectF rect);
	}

	static final long ANIMATION_DURATION = 180;

	private class AnimatedZoomRunnable implements Runnable {

		private final long mStartTime;
		private final float mIncrementPerMs;
		private final float mStartScale;
		private final float mFocalX, mFocalY;

		public AnimatedZoomRunnable(final float zoomLevel, final float focalX, final float focalY) {
			mStartScale = getScale();
			mStartTime = System.currentTimeMillis();
			mIncrementPerMs = (zoomLevel - mStartScale) / ANIMATION_DURATION;
			mFocalX = focalX;
			mFocalY = focalY;
		}

		public void run() {
			float currentMs = Math.min(ANIMATION_DURATION, System.currentTimeMillis() - mStartTime);
			float target = mStartScale + (mIncrementPerMs * currentMs);

			mSuppMatrix.setScale(target, target, mFocalX, mFocalY);
			centerAndDisplayMatrix();

			if (currentMs < ANIMATION_DURATION) {
				postDelayed(this, 17);
			}
		}
	}

	static final int EDGE_NONE = -1;
	static final int EDGE_LEFT = 0;
	static final int EDGE_RIGHT = 1;
	static final int EDGE_BOTH = 2;

	private static final float MAX_ZOOM = 3.0f;
	private static final float MID_ZOOM = 1.75f;
	private static final float MIN_ZOOM = 1.0f;

	private GestureDetector gestureScanner;
	private VersionedGestureDetector mScaleDetector;

	private final Matrix mBaseMatrix = new Matrix();
	private final Matrix mDrawMatrix = new Matrix();
	private final Matrix mSuppMatrix = new Matrix();

	private final float[] mMatrixValues = new float[9];

	private int mScrollEdge = EDGE_BOTH;

	private OnMatrixChangedListener mMatrixChangeListener;
	private OnPhotoTapListener mPhotoTapListener;

	private boolean mZoomEnabled = false;

	public MultiTouchImageView(Context context, AttributeSet attr) {
		super(context, attr);
		setZoomable(false);
	}

	public MultiTouchImageView(Context context, boolean zoomable) {
		super(context);
		setZoomable(zoomable);
	}

	public Matrix getDisplayMatrix() {
		mDrawMatrix.set(mBaseMatrix);
		mDrawMatrix.postConcat(mSuppMatrix);
		return mDrawMatrix;
	}

	public boolean isZoomable() {
		return mZoomEnabled;
	}

	public void onDrag(float dx, float dy) {
		mSuppMatrix.postTranslate(dx, dy);
		centerAndDisplayMatrix();
	}

	public void onScale(float scaleFactor, float focusX, float focusY) {
		mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
		centerAndDisplayMatrix();
	}

	public boolean onDoubleTap(MotionEvent e) {
		try {
			float scale = getScale();
			float x = e.getX();
			float y = e.getY();

			if (scale < MID_ZOOM) {
				post(new AnimatedZoomRunnable(MID_ZOOM, x, y));
			} else if (scale >= MID_ZOOM && scale < MAX_ZOOM) {
				post(new AnimatedZoomRunnable(MAX_ZOOM, x, y));
			} else {
				post(new AnimatedZoomRunnable(MIN_ZOOM, x, y));
			}
		} catch (java.lang.ArrayIndexOutOfBoundsException ae) {
		}

		return true;
	}

	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (null != mPhotoTapListener) {
			// Check whether e is in photo region

			mPhotoTapListener.onPhotoTap();
			return true;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mZoomEnabled) {

			getParent().requestDisallowInterceptTouchEvent(true);

			if (null != gestureScanner && gestureScanner.onTouchEvent(ev)) {
				return true;
			}

			// Let the ScaleGestureDetector inspect all events.
			if (null != mScaleDetector && mScaleDetector.onTouchEvent(ev)) {

				if (mScrollEdge != EDGE_NONE) {
					getParent().requestDisallowInterceptTouchEvent(false);
				}

				// Reset Scale back to 1.0f if we're below 0.9f;
				switch (ev.getAction()) {
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						if (getScale() < MIN_ZOOM) {
							post(new AnimatedZoomRunnable(MIN_ZOOM, 0f, 0f));
						}
						break;
				}

				return true;
			}

			getParent().requestDisallowInterceptTouchEvent(false);
		}

		return super.onTouchEvent(ev);
	}

	public void resetScalePan() {
		mSuppMatrix.reset();
		setImageMatrix(getDisplayMatrix());
		mScrollEdge = EDGE_BOTH;
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		updateBaseMatrix(drawable);
	}

	public void setZoomable(boolean zoomable) {
		mZoomEnabled = zoomable;
		if (mZoomEnabled) {

			setScaleType(ScaleType.MATRIX);

			if (null == mScaleDetector) {
				mScaleDetector = VersionedGestureDetector.newInstance(getContext(), this);
			}

			if (null == gestureScanner) {
				gestureScanner = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener());
				gestureScanner.setOnDoubleTapListener(this);
			}
		} else {
			if (getScaleType() == ScaleType.MATRIX) {
				resetScalePan();
			}
		}
	}

	public void setMatrixChangeListener(OnMatrixChangedListener listener) {
		mMatrixChangeListener = listener;
	}

	public void setPhotoTapListener(OnPhotoTapListener listener) {
		mPhotoTapListener = listener;
	}

	@Override
	protected boolean setFrame(int l, int t, int r, int b) {
		if (super.setFrame(l, t, r, b)) {
			updateBaseMatrix(getDrawable());
			return true;
		}
		return false;
	}

	private void centerMatrix() {
		Drawable d = getDrawable();
		if (null == d) {
			return;
		}

		RectF rect = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		getDisplayMatrix().mapRect(rect);

		final float height = rect.height(), width = rect.width();
		float deltaX = 0, deltaY = 0;

		final int viewHeight = getHeight();
		if (height < viewHeight) {
			deltaY = (viewHeight - height) / 2 - rect.top;
		} else if (rect.top > 0) {
			deltaY = -rect.top;
		} else if (rect.bottom < viewHeight) {
			deltaY = viewHeight - rect.bottom;
		}

		final int viewWidth = getWidth();
		if (width < viewWidth) {
			deltaX = (viewWidth - width) / 2 - rect.left;
			mScrollEdge = EDGE_BOTH;
		} else if (rect.left > 0) {
			mScrollEdge = EDGE_LEFT;
			deltaX = -rect.left;
		} else if (rect.right < viewWidth) {
			deltaX = viewWidth - rect.right;
			mScrollEdge = EDGE_RIGHT;
		} else {
			mScrollEdge = EDGE_NONE;
		}

		mSuppMatrix.postTranslate(deltaX, deltaY);
	}

	private void centerAndDisplayMatrix() {
		centerMatrix();
		setImageMatrix(getDisplayMatrix());
	}

	@Override
	public void setImageMatrix(Matrix matrix) {
		super.setImageMatrix(matrix);

		if (null != mMatrixChangeListener) {
			RectF displayRect = getDisplayRect(matrix);
			if (null != displayRect) {
				mMatrixChangeListener.onMatrixChanged(displayRect);
			}
		}
	}

	public RectF getDisplayRect() {
		centerMatrix();
		return getDisplayRect(getDisplayMatrix());
	}

	private RectF getDisplayRect(Matrix matrix) {
		Drawable d = getDrawable();
		if (null != d) {
			RectF rect = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(rect);
			return rect;
		}
		return null;
	}

	private float getScale() {
		return getValue(mSuppMatrix, Matrix.MSCALE_X);
	}

	private float getValue(Matrix matrix, int whichValue) {
		matrix.getValues(mMatrixValues);
		return mMatrixValues[whichValue];
	}

	/**
	 * Calculate Matrix for FIT_CENTER
	 * 
	 * @param d
	 */
	private void updateBaseMatrix(Drawable d) {
		if (null == d) {
			return;
		}

		float viewWidth = getWidth();
		float viewHeight = getHeight();

		int w = d.getIntrinsicWidth();
		int h = d.getIntrinsicHeight();
		mBaseMatrix.reset();

		// We limit up-scaling to 2x otherwise the result may look bad if it's
		// a small icon.
		float widthScale = viewWidth / w;
		float heightScale = viewHeight / h;
		float scale = Math.min(widthScale, heightScale);

		mBaseMatrix.postScale(scale, scale);
		mBaseMatrix.postTranslate((viewWidth - w * scale) / 2F, (viewHeight - h * scale) / 2F);

		resetScalePan();
	}
}
