package uk.co.senab.footo.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RecycleableImageView extends ImageView {

	public RecycleableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		recycleBitmap();
		
		super.setImageBitmap(bm);
	}

	public void recycleBitmap() {
		Bitmap currentBitmap = getCurrentBitmap();

		if (null != currentBitmap) {
			setImageDrawable(null);
			
			currentBitmap.recycle();
		}
	}

	private Bitmap getCurrentBitmap() {
		Drawable d = getDrawable();

		if (d instanceof BitmapDrawable) {
			return ((BitmapDrawable) d).getBitmap();
		}

		return null;
	}
}
