/*******************************************************************************

 * Copyright notice                                                            *

 *                                                                             *

 * Copyright (c) 2005 Feed'n Read Development Team                             *

 * http://sourceforge.net/fnr                                                  *

 *                                                                             *

 * All rights reserved.                                                        *

 *                                                                             *

 * This program and the accompanying materials are made available under the    *

 * terms of the Common Public License v1.0 which accompanies this distribution,*

 * and is available at                                                         *

 * http://www.eclipse.org/legal/cpl-v10.html                                   *

 *                                                                             *

 * A copy is found in the file cpl-v10.html and important notices to the       *

 * license from the team is found in the textfile LICENSE.txt distributed      *

 * in this package.                                                            *

 *                                                                             *

 * This copyright notice MUST APPEAR in all copies of the file.                *

 *                                                                             *

 * Contributors:                                                               *

 *    Feed'n Read - initial API and implementation                             *

 *                  (smachhau@users.sourceforge.net)                           *

 *******************************************************************************/

package com.requea.dysoweb.panel.monitor;

import java.io.FilterInputStream;

import java.io.IOException;

import java.io.InputStream;

import java.io.InterruptedIOException;

/**
 * <p>
 * <code>InputStream</code> extension that is bindable to a
 * <code>IProgressMonitor</code> to report the progress of a read operation.
 * </p>
 * 
 * @author <a href="mailto:smachhau@users.sourceforge.net">Sebastian Machhausen</a>
 */

public class ProgressMonitorInputStream extends FilterInputStream {
	
	public final static String EMPTY_STRING = "";
	
	/**
	 * <p>
	 * The <code>IProgressMonitor</code> to report the reading status to
	 * </p>
	 */
	private IProgressMonitor monitor;

	/**
	 * <p>
	 * The number of bytes read
	 * </p>
	 */
	private int bytesRead;

	/**
	 * <p>
	 * The total number of bytes to be read
	 * </p>
	 */
	private int totalBytes;

	/**
	 * <p>
	 * Creates a new <code>ProgressMonitorInputStream</code> to monitor the
	 * progress of the specified <code>InputStream</code> and report the
	 * progress status to the specified <code>IProgressMonitor</code>.
	 * </p>
	 * 
	 * @param in
	 *            the <code>InputStream</code> to monitor
	 * @param monitor
	 *            the <code>IProgressMonitor</code> to report the
	 * progress status to
	 */
	public ProgressMonitorInputStream(InputStream in,
			IProgressMonitor monitor) {
		super(in);
		this.monitor = monitor;
		try {
			this.totalBytes = in.available();
			this.monitor.beginTask(EMPTY_STRING, this.totalBytes);
		}
		catch (IOException ioe) {
			this.totalBytes = 0;
		}
	} // end constructor ProgressMonitorInputStream(InputStream
	// IProgressMonitor)

	/**
	 * <p>
	 * Creates a new <code>ProgressMonitorInputStream</code> to monitor the
	 * progress of the specified <code>InputStream</code> and report the
	 * progress status to the specified <code>IProgressMonitor</code>.
	 * </p>
	 * 
	 * @param in
	 *            the <code>InputStream</code> to monitor
	 * @param monitor
	 *            the <code>IProgressMonitor</code> to report the
	 * progress status to
	 * @param size
	 *            the size in bytes of the <code>InputStream</code>
	 */

	public ProgressMonitorInputStream(InputStream in,
			IProgressMonitor monitor, long size) {
		super(in);
		this.monitor = monitor;
		this.totalBytes = (int) size;
		this.monitor.beginTask(EMPTY_STRING, this.totalBytes);
	} // end constructor ProgressMonitorInputStream(InputStream
	// IProgressMonitor, long)

	/**
	 * <p>
	 * Overrides {@link java.io.FilterInputStream#read()} to update the
	 * <code>IProgressMonitor</code> after the read operation.
	 * </p>
	 * 
	 * @return the value read or <b>-1</b> if the end of the stream has been
	 * reached
	 * @throws IOException
	 *             if an io error occured during the read operation
	 */
	public int read() throws IOException {
		int c = this.in.read();
		if (c >= 0) {
			this.monitor.worked(1);
			this.bytesRead++;
		}
		if (this.monitor.isCanceled()) {
			InterruptedIOException exc = new InterruptedIOException(
				"Operation canceled");
			exc.bytesTransferred = this.bytesRead;
			throw (exc);
		}
		return (c);
	} // end method read()

	/**
	 * <p>
	 * Overrides {@link java.io.FilterInputStream#read(byte[])} to update the
	 * <code>IProgressMonitor</code> after the read operation.
	 * </p>
	 * 
	 * @param buffer
	 *            the buffer to read into
	 * @return the number of bytes read or -1 if the end of the stream has been
	 * reached
	 * @throws IOException
	 *             if an io error occured during the read operation
	 */
	public int read(byte buffer[]) throws IOException {
		int nr = this.in.read(buffer);
		if (nr > 0) {
			this.monitor.worked(nr);
			this.bytesRead += nr;
		}
		if (this.monitor.isCanceled()) {
			InterruptedIOException exc =
				new InterruptedIOException("progress");
			exc.bytesTransferred = this.bytesRead;
			throw (exc);
		}
		return (nr);
	} // end method read(byte[])

	/**
	 * <p>
	 * Overrides {@link java.io.FilterInputStream#read(byte[], int, int)} to
	 * update the <code>IProgressMonitor</code> after the read operation.
	 * </p>
	 * 
	 * @param buffer
	 *            the buffer to read into
	 * @param off
	 *            the offset at which to start storing byte values
	 * @param len
	 *            the maximum number of bytes to read
	 * @return the number of bytes read or -1 if the end of the stream has been
	 * reached
	 * @throws IOException
	 *             if an io error occured during the read operation
	 */
	public int read(byte buffer[],
			int off,
			int len) throws IOException {
		int nr = this.in.read(buffer, off, len);
		if (nr > 0) {
			this.monitor.worked(nr);
			this.bytesRead += nr;
		}
		if (this.monitor.isCanceled()) {
			InterruptedIOException exc =
				new InterruptedIOException("progress");
			exc.bytesTransferred = this.bytesRead;
			throw (exc);
		}
		return (nr);
	} // end method read(byte[], int, int)

	/**
	 * <p>
	 * Overrides {@link java.io.FilterInputStream#skip(long)} to update the
	 * progress monitor after the skip operation.
	 * </p>
	 * 
	 * @param n
	 *            the number of bytes to skip
	 * @return the number of bytes actually skipped
	 * @throws IllegalArgumentException
	 *             If <code>n</code> is negative.
	 * @throws IOException
	 *             if an io error occured during the skip operation
	 */
	public long skip(long n) throws IOException {
		long nr = this.in.skip(n);
		if (nr > 0) {
			this.monitor.worked((int) nr);
			this.bytesRead += nr;
		}
		return (nr);
	} // end method skip(long)

	/**
	 * <p>
	 * Overrides {@link java.io.FilterInputStream#reset()} to reset the
	 * <code>IProgressMonitor</code> as well as the <code>InputStream</code>.
	 * </p>
	 */
	public synchronized void reset() throws IOException {
		this.in.reset();
		this.bytesRead = this.totalBytes - this.in.available();
		this.monitor.beginTask(EMPTY_STRING, this.totalBytes);
		this.monitor.worked(this.bytesRead);
	} // end method reset()

	/**
	 * <p>
	 * Overrides {@link java.io.FilterInputStream#close()} to close the
	 * <code>IProgressMonitor</code> as well as the <code>InputStream</code>.
	 * </p>
	 * 
	 * @throws IOException
	 *             if an io error occured during the close operation
	 */
	public void close() throws IOException {
		this.in.close();
		this.monitor.done();
	} // end method close()
} // end class ProgressMonitorInputStream
