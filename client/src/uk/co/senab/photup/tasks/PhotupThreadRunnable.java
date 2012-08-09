package uk.co.senab.photup.tasks;

public abstract class PhotupThreadRunnable implements Runnable {

	protected boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}

}
