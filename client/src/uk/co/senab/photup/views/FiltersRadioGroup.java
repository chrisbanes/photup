package uk.co.senab.photup.views;

import java.util.concurrent.ExecutorService;

import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.R;

public class FiltersRadioGroup extends RadioGroup implements AnimationListener {

	static final class FilterRunnable implements Runnable {

		// TODO Should make these WeakReferences
		private final Context mContext;
		private final RadioButton mButton;
		
		private final PhotoUpload mUpload;
		private final Filter mFilter;

		public FilterRunnable(Context context, PhotoUpload upload, Filter filter, RadioButton button) {
			mContext = context;
			mUpload = upload;
			mFilter = filter;
			mButton = button;
		}

		public void run() {
			Bitmap bitmap = mUpload.getThumbnail(mContext);
			final Bitmap filteredBitmap = PhotoProcessing.filterPhoto(bitmap, mFilter.getId());
			bitmap.recycle();
			
			if (Thread.currentThread().isInterrupted()) {
				filteredBitmap.recycle();
				return;
			}

			mButton.post(new Runnable() {
				public void run() {
					mButton.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), filteredBitmap));
				}
			});
		}
	};

	private final Animation mSlideInBottomAnim, mSlideOutBottomAnim;
	private final ExecutorService mExecutor;

	public FiltersRadioGroup(Context context, AttributeSet attrs) {
		super(context, attrs);

		mExecutor = PhotupApplication.getApplication(context).getSingleThreadExecutorService();

		mSlideInBottomAnim = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
		mSlideInBottomAnim.setAnimationListener(this);

		mSlideOutBottomAnim = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom);
		mSlideOutBottomAnim.setAnimationListener(this);

		addButtons(context);
	}

	private void addButtons(Context context) {
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		RadioButton button;
		for (Filter filter : Filter.FILTERS) {
			button = (RadioButton) layoutInflater.inflate(R.layout.layout_filters_item, this, false);
			button.setText(filter.getLabelId());
			button.setId(filter.getId());
			addView(button);
		}
	}

	public void setPhotoUpload(PhotoUpload upload) {
		for (final Filter filter : Filter.FILTERS) {
			final RadioButton button = (RadioButton) findViewById(filter.getId());

//			Drawable oldBg = button.getBackground();
//			button.setBackgroundDrawable(null);
//
//			if (oldBg instanceof BitmapDrawable) {
//				((BitmapDrawable) oldBg).getBitmap().recycle();
//			}

			mExecutor.submit(new FilterRunnable(getContext(), upload, filter, button));
		}
		
		if (upload.hasFilter()) {
			check(upload.getFilterUsed().getId());
		} else {
			clearCheck();
		}
	}

	public void show() {
		if (getVisibility() != View.VISIBLE) {
			setVisibility(View.VISIBLE);
			startAnimation(mSlideInBottomAnim);
		}
	}

	public void hide() {
		if (getVisibility() == View.VISIBLE) {
			startAnimation(mSlideOutBottomAnim);
		}
	}

	public void onAnimationEnd(Animation animation) {
		if (animation == mSlideOutBottomAnim) {
			setVisibility(View.GONE);
		}
	}

	public void onAnimationRepeat(Animation animation) {
		// NO-OP
	}

	public void onAnimationStart(Animation animation) {
		// NO-OP
	}

	@Override
	public void setVisibility(int visibility) {
		View parent = (View) getParent();
		if (null != parent) {
			parent.setVisibility(visibility);
		}

		super.setVisibility(visibility);
	}
}
