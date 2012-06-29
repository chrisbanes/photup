package uk.co.senab.footo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import uk.co.senab.photup.R;

import android.app.Application;
import android.content.Context;

public class FootoApplication extends Application {

	static final int EXECUTOR_CORE_POOL_SIZE = 4;
	static final int EXECUTOR_MAX_POOL_SIZE = 10;

	private ExecutorService mExecutor;

	@Override
	public void onCreate() {
		super.onCreate();
		
		mExecutor = createExecutor();
	}
	
	public static FootoApplication getApplication(Context context) {
		return (FootoApplication) context.getApplicationContext();
	}
	
	public ExecutorService getExecutorService() {
		return mExecutor;
	}

	private static ExecutorService createExecutor() {
		return new ThreadPoolExecutor(EXECUTOR_CORE_POOL_SIZE, EXECUTOR_MAX_POOL_SIZE, 1L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}
}
