package com.requea.dysoweb.service.obr;




/**
 * An abstract wrapper around a progress monitor which,
 * unless overridden, forwards <code>IProgressMonitor</code>
 * and <code>IProgressMonitorWithBlocking</code> methods to the wrapped progress monitor.
 * <p>
 * This class can be used without OSGi running.
 * </p><p>
 * Clients may subclass.
 * </p>
 */
public abstract class ProgressMonitorWrapper implements IProgressMonitor {

	/** The wrapped progress monitor. */
	private IProgressMonitor progressMonitor;

	/** 
	 * Creates a new wrapper around the given monitor.
	 *
	 * @param monitor the progress monitor to forward to
	 */
	protected ProgressMonitorWrapper(IProgressMonitor monitor) {
		progressMonitor = monitor;
	}

	/** 
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#beginTask(String, int)
	 */
	public void beginTask(String name, int totalWork) {
		progressMonitor.beginTask(name, totalWork);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#done()
	 */
	public void done() {
		progressMonitor.done();
	}

	/**
	 * Returns the wrapped progress monitor.
	 *
	 * @return the wrapped progress monitor
	 */
	public IProgressMonitor getWrappedProgressMonitor() {
		return progressMonitor;
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#internalWorked(double)
	 */
	public void internalWorked(double work) {
		progressMonitor.internalWorked(work);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#isCanceled()
	 */
	public boolean isCanceled() {
		return progressMonitor.isCanceled();
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#setCanceled(boolean)
	 */
	public void setCanceled(boolean b) {
		progressMonitor.setCanceled(b);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#setTaskName(String)
	 */
	public void setTaskName(String name) {
		progressMonitor.setTaskName(name);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#subTask(String)
	 */
	public void subTask(String name) {
		progressMonitor.subTask(name);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#worked(int)
	 */
	public void worked(int work) {
		progressMonitor.worked(work);
	}
}
