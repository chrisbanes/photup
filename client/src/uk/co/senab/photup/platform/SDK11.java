package uk.co.senab.photup.platform;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.view.View;

@SuppressLint("NewApi")
public class SDK11 {

	public static void disableHardwareAcceleration(View view) {
		view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	public static boolean isCanvasHardwareAccelerated(Canvas canvas) {
		return canvas.isHardwareAccelerated();
	}

}
