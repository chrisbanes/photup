package uk.co.senab.footo.adapters;

import java.lang.ref.WeakReference;

import uk.co.senab.footo.R;
import uk.co.senab.footo.views.RecycleableImageView;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Thumbnails;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ImageView;

public class PhotosAdapter extends ResourceCursorAdapter {

	public static class PhotoTag {

		private final int mPhotoId;

		public PhotoTag(int photoId) {
			mPhotoId = photoId;
		}

		public int getPhotoId() {
			return mPhotoId;
		}

	}

	private final ContentResolver mCr;

	public PhotosAdapter(Context context, int layout, Cursor c, boolean autoRequery) {
		super(context, layout, c, autoRequery);

		mCr = context.getContentResolver();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		RecycleableImageView iv = (RecycleableImageView) view.findViewById(R.id.iv_photo);

		int id = cursor.getInt(cursor.getColumnIndexOrThrow(ImageColumns._ID));

		new PhotoTask(mCr, iv).execute(id);

		view.setTag(new PhotoTag(id));
	}

	private static class PhotoTask extends AsyncTask<Integer, Void, Bitmap> {

		private final ContentResolver mCr;
		private final WeakReference<RecycleableImageView> mImageView;

		public PhotoTask(ContentResolver cr, RecycleableImageView imageView) {
			mImageView = new WeakReference<RecycleableImageView>(imageView);
			mCr = cr;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			RecycleableImageView iv = mImageView.get();
			if (null != iv) {
				iv.recycleBitmap();
			}
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			return Images.Thumbnails.getThumbnail(mCr, params[0], Thumbnails.MICRO_KIND, null);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);

			ImageView iv = mImageView.get();
			if (null != iv && null != result) {
				iv.setImageBitmap(result);
			}
		}
	}

}
