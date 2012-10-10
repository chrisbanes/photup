package uk.co.senab.photup.tasks;

import java.util.concurrent.ThreadFactory;

public class PhotupThreadFactory implements ThreadFactory {

	private final String mThreadName;

	public PhotupThreadFactory(String threadName) {
		mThreadName = threadName;
	}

	public PhotupThreadFactory() {
		this(null);
	}

	public Thread newThread(final Runnable r) {
		if (null != mThreadName) {
			return new Thread(r, mThreadName);
		} else {
			return new Thread(r);
		}
	}
}