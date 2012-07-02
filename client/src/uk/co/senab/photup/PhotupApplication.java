package uk.co.senab.photup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

public class PhotupApplication extends Application {

	static final int EXECUTOR_CORE_POOL_SIZE = 4;
	static final int EXECUTOR_MAX_POOL_SIZE = 10;

	private ExecutorService mExecutor;
	private BitmapLruCache mImageCache;

	private final PhotoSelectionController mPhotoController = new PhotoSelectionController();

	public static PhotupApplication getApplication(Context context) {
		return (PhotupApplication) context.getApplicationContext();
	}

	public ExecutorService getExecutorService() {
		if (null == mExecutor) {
			mExecutor = createExecutor();
		}
		return mExecutor;
	}
	
	public BitmapLruCache getImageCache() {
		if (null == mImageCache) {
			mImageCache = new BitmapLruCache(this);
		}
		return mImageCache;
	}

	public PhotoSelectionController getPhotoSelectionController() {
		return mPhotoController;
	}

	private static ExecutorService createExecutor() {
		return new ThreadPoolExecutor(EXECUTOR_CORE_POOL_SIZE, EXECUTOR_MAX_POOL_SIZE, 1L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}
	
	@SuppressWarnings("deprecation")
	public int getLargestScreenDimension() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return Math.max(display.getHeight(), display.getWidth());
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		
		if (null != mImageCache) {
			mImageCache.trimMemory();
		}
	}

}
