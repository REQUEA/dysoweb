package com.requea.dysoweb.panel.monitor;

import java.text.DecimalFormat;

import com.requea.dysoweb.service.obr.IProgressMonitor;

/**
 * This progress monitor directs all task information to std.out.
 */
public class ConsoleProgressMonitor implements IProgressMonitor {
	private int _totalWork = 0;
	private int _workDoneSoFar = 0;
	private boolean _cancelRequested = false;
	
	private static DecimalFormat format = new DecimalFormat("0.00%");

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#beginTask(String, int)
	 */
	public void beginTask(String name, int totalWork) {
		_totalWork = totalWork;
		report(name);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#done()
	 */
	public void done() {
		int workRemaining = _totalWork - _workDoneSoFar;
		worked(workRemaining);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#internalWorked(double)
	 */
	public void internalWorked(double work) {
		_workDoneSoFar += work;
		double dPercent = (double)_workDoneSoFar / (double)_totalWork;
		synchronized (format) {
			System.out.println(" Result:" + format.format(dPercent));
		}
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#isCanceled()
	 */
	public boolean isCanceled() {
		return _cancelRequested;
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#setCanceled(boolean)
	 */
	public void setCanceled(boolean value) {
		_cancelRequested = value;
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#setTaskName(String)
	 */
	public void setTaskName(String name) {
		report(name);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#subTask(String)
	 */
	public void subTask(String name) {
		report(name);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#worked(int)
	 */
	public void worked(int work) {
		_workDoneSoFar += work;
	}

	private void report(String message) {
		System.out.println(message);
	}
}
