package uk.co.senab.photup;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.FloatMath;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;

public class Utils {

	public static Bitmap drawViewOntoBitmap(View view) {
		Bitmap image = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(image);
		view.draw(canvas);
		return image;
	}

	public static Animation createScaleAnimation(View view, int parentWidth, int parentHeight, int toX, int toY) {
		// Difference in X and Y
		final int diffX = toX - view.getLeft();
		final int diffY = toY - view.getTop();

		// Calculate actual distance using pythagors
		float diffDistance = FloatMath.sqrt((toX * toX) + (toY * toY));
		float parentDistance = FloatMath.sqrt((parentWidth * parentWidth) + (parentHeight * parentHeight));

		ScaleAnimation scaleAnimation = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.ABSOLUTE, diffX,
				Animation.ABSOLUTE, diffY);
		scaleAnimation.setFillAfter(true);
		scaleAnimation.setInterpolator(new DecelerateInterpolator());
		scaleAnimation.setDuration(Math.round(diffDistance / parentDistance
				* Constants.SCALE_ANIMATION_DURATION_FULL_DISTANCE));

		return scaleAnimation;
	}

}
