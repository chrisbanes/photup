package uk.co.senab.photup.views;

import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import uk.co.senab.photup.listeners.OnPhotoTagTapListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoTag;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class MultiTouchImageView extends PhotupImageView implements PhotoViewAttacher.OnPhotoTapListener {

	private final PhotoViewAttacher mAttacher;

	private OnSingleTapListener mSingleTapListener;
	private OnPhotoTagTapListener mTagTapListener;

	public MultiTouchImageView(Context context) {
		this(context, null);
	}

	public MultiTouchImageView(Context context, AttributeSet attr) {
		super(context, attr);
		mAttacher = new PhotoViewAttacher(this);
	}

	/**
	 * Gets the Display Rectangle of the currently displayed Drawable. The
	 * Rectangle is relative to this View and includes all scaling and
	 * translations.
	 * 
	 * @return - RectF of Displayed Drawable
	 */
	public RectF getDisplayRect() {
		return mAttacher.getDisplayRect();
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		mAttacher.update();
	}

	/**
	 * Register a callback to be invoked when the Matrix has changed for this
	 * View. An example would be the user panning or scaling the Photo.
	 * 
	 * @param listener
	 *            - Listener to be registered.
	 */
	public void setOnMatrixChangeListener(OnMatrixChangedListener listener) {
		mAttacher.setOnMatrixChangeListener(listener);
	}

	/**
	 * Register a callback to be invoked when the Photo displayed by this View
	 * is tapped with a single tap.
	 * 
	 * @param listener
	 *            - Listener to be registered.
	 */
	public void setPhotoTapListener(OnPhotoTagTapListener listener) {
		mTagTapListener = listener;
		mAttacher.setOnPhotoTapListener(this);
	}

	public void setSingleTapListener(OnSingleTapListener listener) {
		mSingleTapListener = listener;
		mAttacher.setOnPhotoTapListener(this);
	}

	public final void onPhotoTap(View view, float x, float y) {
		if (null != mSingleTapListener && mSingleTapListener.onSingleTap()) {
			return;
		}

		if (null != mTagTapListener) {
			mTagTapListener.onNewPhotoTagTap(new PhotoTag(x * 100f, y * 100f));
			return;
		}
	}

	/**
	 * Allows you to enable/disable the zoom functionality on the ImageView.
	 * When disable the ImageView reverts to using the FIT_CENTER matrix.
	 * 
	 * @param zoomable
	 *            - Whether the zoom functionality is enabled.
	 */
	public void setZoomable(boolean zoomable) {
		mAttacher.setZoomable(zoomable);
	}

	@Override
	protected void onDetachedFromWindow() {
		mAttacher.cleanup();
		super.onDetachedFromWindow();
	}
}
