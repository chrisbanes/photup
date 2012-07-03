package uk.co.senab.photup;

import java.io.FileNotFoundException;

import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.utils.MediaUtils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.FloatMath;
import android.util.Log;
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

	// And to convert the image URI to the direct file system path of the image
	// file
	public static String getPathFromContentUri(ContentResolver cr, Uri contentUri) {
		if (Constants.DEBUG) {
			Log.d("Utils", "Getting file path for Uri: " + contentUri);
		}

		String returnValue = null;

		if (ContentResolver.SCHEME_CONTENT.equals(contentUri.getScheme())) {
			// can post image
			String[] proj = { MediaStore.Images.Media.DATA };
			Cursor cursor = cr.query(contentUri, proj, null, null, null);

			if (null != cursor) {
				final int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

				if (cursor.moveToFirst()) {
					returnValue = cursor.getString(column_index);
				}
				cursor.close();
			}
		} else if (ContentResolver.SCHEME_FILE.equals(contentUri.getScheme())) {
			returnValue = contentUri.getPath();
		}

		return returnValue;
	}

	public static Bitmap resizeBitmap(final ContentResolver resolver, final Uri uri, final int maxDimension,
			final boolean fineResize) throws FileNotFoundException {

		// Get original dimensions
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		try {
			BitmapFactory.decodeStream(resolver.openInputStream(uri), null, o);
		} catch (SecurityException se) {
			se.printStackTrace();
			return null;
		}

		final int origWidth = o.outWidth;
		final int origHeight = o.outHeight;

		// Holds returned bitmap
		Bitmap bitmap;

		o.inJustDecodeBounds = false;
		o.inDither = false;
		o.inScaled = false;
		o.inPurgeable = true;
		o.inInputShareable = true;

		if (origWidth > maxDimension || origHeight > maxDimension) {
			int k = 1;
			int tmpHeight = origHeight, tmpWidth = origWidth;
			while ((tmpWidth / 2) >= maxDimension || (tmpHeight / 2) >= maxDimension) {
				tmpWidth /= 2;
				tmpHeight /= 2;
				k *= 2;
			}
			o.inSampleSize = k;

			bitmap = BitmapFactory.decodeStream(resolver.openInputStream(uri), null, o);
		} else {
			bitmap = BitmapFactory.decodeStream(resolver.openInputStream(uri), null, o);
		}

		if (null != bitmap) {
			// Do fine resize if needed
			if (fineResize) {
				bitmap = fineResizePhoto(bitmap, maxDimension);
			}

			if (Constants.DEBUG) {
				Log.d("Utils", "Resized bitmap to: " + bitmap.getWidth() + "x" + bitmap.getHeight());
			}

			final String filePath = getPathFromContentUri(resolver, uri);
			if (null != filePath) {
				final int angle = MediaUtils.getExifOrientation(filePath);
				if (angle != 0) {
					if (Constants.DEBUG) {
						Log.d("Utils", "Rotating bitmap by: " + angle);
					}
					bitmap = PhotoProcessing.rotate(bitmap, angle);
				}
			}
		}

		return bitmap;
	}

	public static Bitmap fineResizePhoto(final Bitmap bitmap, final int maxDimension) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		final int biggestDimension = Math.max(width, height);

		if (biggestDimension <= maxDimension) {
			return bitmap;
		}

		final float ratio = maxDimension / (float) biggestDimension;
		Bitmap resized = PhotoProcessing.resize(bitmap, Math.round(width * ratio), Math.round(height * ratio));
		if (Constants.DEBUG) {
			Log.d("PhotoUpload", "Finely resized to: " + resized.getWidth() + "x" + resized.getHeight());
		}

		return resized;
	}

}
