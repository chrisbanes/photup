package uk.co.senab.photup;

import uk.co.senab.photup.views.CropImageView;
import uk.co.senab.photup.views.HighlightView;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;

public class CropImageActivity extends Activity {

	private CropImageView mCropImageView;
	private Bitmap mBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCropImageView = new CropImageView(this, null);
		setContentView(mCropImageView);

		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		mCropImageView.setImageBitmap(mBitmap);

		makeHighlight();
	}

	private void makeHighlight() {
		HighlightView hv = new HighlightView(mCropImageView);
		int width = mBitmap.getWidth();
		int height = mBitmap.getHeight();
		Rect imageRect = new Rect(0, 0, width, height);

		// make the default size about 4/5 of the width or height
		int cropWidth = Math.min(width, height) * 4 / 5;
		int cropHeight = cropWidth;
		int x = (width - cropWidth) / 2;
		int y = (height - cropHeight) / 2;
		RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
		hv.setup(mCropImageView.getImageMatrix(), imageRect, cropRect, false);
		mCropImageView.setHighlight(hv);
	}
}
