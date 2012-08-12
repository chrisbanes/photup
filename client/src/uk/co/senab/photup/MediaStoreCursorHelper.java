package uk.co.senab.photup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import uk.co.senab.photup.model.MediaStoreBucket;
import uk.co.senab.photup.model.MediaStorePhotoUpload;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

public class MediaStoreCursorHelper {

	public static final String[] PHOTOS_PROJECTION = { Images.Media._ID, Images.Media.MINI_THUMB_MAGIC,
			Images.Media.DATA, Images.Media.BUCKET_DISPLAY_NAME, Images.Media.BUCKET_ID };
	public static final String PHOTOS_ORDER_BY = Images.Media.DATE_ADDED + " desc";

	public static ArrayList<PhotoSelection> photosCursorToSelectionList(Uri contentUri, Cursor cursor) {
		ArrayList<PhotoSelection> items = new ArrayList<PhotoSelection>(cursor.getCount());

		PhotoSelection item;
		while (cursor.moveToNext()) {
			try {
				item = photosCursorToSelection(contentUri, cursor);
				if (null != item) {
					items.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return items;
	}

	public static PhotoSelection photosCursorToSelection(Uri contentUri, Cursor cursor) {
		PhotoSelection item = null;

		try {
			File file = new File(cursor.getString(cursor.getColumnIndexOrThrow(ImageColumns.DATA)));
			if (file.exists()) {
				item = new MediaStorePhotoUpload(contentUri, cursor.getInt(cursor
						.getColumnIndexOrThrow(ImageColumns._ID)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return item;
	}

	public static ArrayList<MediaStoreBucket> photosCursorToBucketList(Cursor cursor, MediaStoreBucket firstBucket) {
		ArrayList<MediaStoreBucket> items = new ArrayList<MediaStoreBucket>(cursor.getCount());
		items.add(firstBucket);

		HashSet<String> bucketIds = new HashSet<String>();

		MediaStoreBucket item = null;
		while (cursor.moveToNext()) {
			try {
				item = null;

				final int idColumn = cursor.getColumnIndexOrThrow(ImageColumns.BUCKET_ID);
				final int nameColumn = cursor.getColumnIndexOrThrow(ImageColumns.BUCKET_DISPLAY_NAME);

				final String bucketId = cursor.getString(idColumn);

				if (bucketIds.add(bucketId)) {
					item = new MediaStoreBucket(bucketId, cursor.getString(nameColumn));
					items.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return items;
	}

	public static Cursor openPhotosCursor(Context context, Uri contentUri) {
		return context.getContentResolver().query(contentUri, PHOTOS_PROJECTION, null, null, PHOTOS_ORDER_BY);
	}

}
