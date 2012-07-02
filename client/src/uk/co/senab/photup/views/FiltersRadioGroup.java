package uk.co.senab.photup.views;

import android.content.Context;
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

	private final Animation mSlideInBottomAnim, mSlideOutBottomAnim;

	public FiltersRadioGroup(Context context, AttributeSet attrs) {
		super(context, attrs);

		mSlideInBottomAnim = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
		mSlideInBottomAnim.setAnimationListener(this);

		mSlideOutBottomAnim = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom);
		mSlideOutBottomAnim.setAnimationListener(this);

		addButtons(context);
	}

	private void addButtons(Context context) {
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		RadioButton button;
		for (int filterResId : PhotoProcessing.FILTERS) {
			button = (RadioButton) layoutInflater.inflate(R.layout.layout_filters_item, this, false);
			button.setText(filterResId);

			addView(button);
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
