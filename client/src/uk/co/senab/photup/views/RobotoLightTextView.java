package uk.co.senab.photup.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class RobotoLightTextView extends TextView {

	private Typeface mRobotoLightTypeface;

	public RobotoLightTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mRobotoLightTypeface = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
		setTypeface(mRobotoLightTypeface);
	}

}
