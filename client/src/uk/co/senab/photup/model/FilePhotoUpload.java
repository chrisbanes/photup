package uk.co.senab.photup.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.utils.BitmapUtils;
import com.lightbox.android.photoprocessing.utils.BitmapUtils.BitmapSize;
import com.lightbox.android.photoprocessing.utils.FileUtils;

public class FilePhotoUpload extends PhotoSelection {

	static final int MINI_THUMBNAIL_SIZE = 300;
	static final int MICRO_THUMBNAIL_SIZE = 96;

	private final Uri mFullUri;

	public FilePhotoUpload(Uri uri) {
		mFullUri = uri;
	}

	public Uri getOriginalPhotoUri() {
		return mFullUri;
	}

	public Bitmap getThumbnailImage(Context context) {
		Resources res = context.getResources();

		int size = res.getBoolean(R.bool.load_mini_thumbnails) ? MINI_THUMBNAIL_SIZE : MICRO_THUMBNAIL_SIZE;
		if (size == MINI_THUMBNAIL_SIZE && res.getBoolean(R.bool.sample_mini_thumbnails)) {
			size /= 2;
		}

		try {
			Bitmap bitmap = Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size);
			bitmap = Utils.rotate(bitmap, getExifRotation(context));
			return bitmap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap getDisplayImage(Context context) {
		try {
			final int size = PhotupApplication.getApplication(context).getSmallestScreenDimension();
			Bitmap bitmap = Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size);
			bitmap = Utils.rotate(bitmap, getExifRotation(context));
			return bitmap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int getExifRotation(Context context) {
		return Utils.getOrientationFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
	}

	@Override
	public Bitmap getUploadImage(Context context, final UploadQuality quality) {
		Utils.checkPhotoProcessingThread();
		return getUploadImageNative(context, quality);
	}

	private Bitmap getUploadImageNative(final Context context, final UploadQuality quality) {
		try {
			String path = Utils.getPathFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
			if (null != path) {
				BitmapSize size = BitmapUtils.getBitmapSize(path);

				byte[] jpegData = FileUtils.readFileToByteArray(new File(path));
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Read file to RAM!");
				}

				final float resizeRatio = Math.max(size.width, size.height) / (float) quality.getMaxDimension();
				size = new BitmapSize(Math.round(size.width / resizeRatio), Math.round(size.height / resizeRatio));

				PhotoProcessing.nativeInitBitmap(size.width, size.height);
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Init " + size.width + "x" + size.height);
				}

				final int decodeResult = PhotoProcessing.nativeLoadResizedJpegBitmap(jpegData, jpegData.length,
						size.width * size.height);

				// Free the byte[]
				jpegData = null;

				if (decodeResult == 0) {
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native decode complete!");
					}
				} else {
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native decode failed. Trying Android decode");
					}

					// Just in case
					PhotoProcessing.nativeDeleteBitmap();

					// Decode in Android and send to native
					Bitmap bitmap = Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(),
							quality.getMaxDimension());
					PhotoProcessing.sendBitmapToNative(bitmap);
					bitmap.recycle();

					// Do resize
					PhotoProcessing.nativeResizeBitmap(size.width, size.height);
				}

				/**
				 * Apply filter if needed
				 */
				if (requiresProcessing()) {
					PhotoProcessing.filterPhoto(getFilterUsed().getId());
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native filter complete!");
					}
				}

				/**
				 * Rotate if needed
				 */
				final int rotation = getTotalRotation(context);
				switch (rotation) {
					case 90:
						PhotoProcessing.nativeRotate90();
						break;
					case 180:
						PhotoProcessing.nativeRotate180();
						break;
					case 270:
						PhotoProcessing.nativeRotate180();
						PhotoProcessing.nativeRotate90();
						break;
				}
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. " + rotation + " degree rotation complete!");
				}

				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Native worked!");
				}

				return PhotoProcessing.getBitmapFromNative(null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Just in case...
			PhotoProcessing.nativeDeleteBitmap();
		}

		return null;
	}

}
