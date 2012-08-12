package uk.co.senab.photup.model;

import uk.co.senab.photup.R;
import uk.co.senab.photup.util.Utils;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Images.Thumbnails;

public class MediaStorePhotoUpload extends FilePhotoUpload {

	private final long mId;

	public MediaStorePhotoUpload(Uri contentUri, long id) {
		super(Uri.withAppendedPath(contentUri, String.valueOf(id)));
		mId = id;
	}

	public MediaStorePhotoUpload(Uri uri) {
		super(uri);
		mId = Long.parseLong(uri.getLastPathSegment());
	}

	public Bitmap getThumbnailImage(Context context) {
		Resources res = context.getResources();

		final int kind = res.getBoolean(R.bool.load_mini_thumbnails) ? Thumbnails.MINI_KIND : Thumbnails.MICRO_KIND;

		BitmapFactory.Options opts = null;
		if (kind == Thumbnails.MINI_KIND && res.getBoolean(R.bool.sample_mini_thumbnails)) {
			opts = new BitmapFactory.Options();
			opts.inSampleSize = 2;
		}

		try {
			ContentResolver cr = context.getContentResolver();
			Bitmap bitmap = Thumbnails.getThumbnail(cr, mId, kind, opts);
			bitmap = Utils.rotate(bitmap, getExifRotation(context));
			return bitmap;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
