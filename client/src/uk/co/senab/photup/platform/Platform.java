package uk.co.senab.photup.platform;

import android.graphics.Canvas;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;

public class Platform {

	public static void disableHardwareAcceleration(View view) {
		if (isApiHighEnough(VERSION_CODES.HONEYCOMB)) {
			SDK11.disableHardwareAcceleration(view);
		}
	}

	public static boolean isCanvasHardwareAccelerated(Canvas canvas) {
		if (isApiHighEnough(VERSION_CODES.HONEYCOMB)) {
			return SDK11.isCanvasHardwareAccelerated(canvas);
		}
		return false;
	}

	static boolean isApiHighEnough(final int requiredApiLevel) {
		return VERSION.SDK_INT >= requiredApiLevel;
	}

}
