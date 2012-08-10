package uk.co.senab.photup.platform;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.view.View;

@TargetApi(11)
public class SDK11 {

	public static void disableHardwareAcceleration(View view) {
		view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	public static boolean isCanvasHardwareAccelerated(Canvas canvas) {
		return canvas.isHardwareAccelerated();
	}

}
