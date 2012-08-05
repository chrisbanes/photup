package uk.co.senab.photup.views;

import uk.co.senab.photup.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class UploadActionBarView extends RelativeLayout {

	private final Animation mCycleFadeAnimation;

	public UploadActionBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCycleFadeAnimation = AnimationUtils.loadAnimation(context, R.anim.cycle_fade);
	}

	public void animateBackground() {
		View animationBackground = getAnimationBackgroundView();
		if (null != animationBackground && animationBackground.getVisibility() != View.VISIBLE) {
			animationBackground.startAnimation(mCycleFadeAnimation);
			animationBackground.setVisibility(View.VISIBLE);
		}
	}

	public void stopAnimatingBackground() {
		View animationBackground = getAnimationBackgroundView();
		if (null != animationBackground && animationBackground.getVisibility() == View.VISIBLE) {
			animationBackground.clearAnimation();
			animationBackground.setVisibility(View.GONE);
		}
	}

	private View getAnimationBackgroundView() {
		return findViewById(R.id.v_action_upload_bg);
	}

}
