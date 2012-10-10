package uk.co.senab.photup.tasks;

import android.os.Process;

public abstract class PhotupThreadRunnable implements Runnable {
	
	public final void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		runImpl();
	}
	
	public abstract void runImpl();

	protected boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}

}
