package com.requea.dysoweb.panel.monitor;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

import org.w3c.dom.Element;

import com.requea.dysoweb.service.obr.IProgressMonitor;
import com.requea.dysoweb.util.xml.XMLUtils;

/**
 * This progress monitor directs all task information to std.out.
 */
public class AjaxProgressMonitor implements IProgressMonitor, Serializable{
	private static final long serialVersionUID = 1L;
	private int _totalWork = 0;
	private int _workDoneSoFar = 0;
	private boolean _cancelRequested = false;
	private String fTaskName;

	private static NumberFormat format;
	static {
	    format = NumberFormat.getPercentInstance(Locale.ENGLISH);
	    format.setMaximumFractionDigits(2);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#beginTask(String, int)
	 */
	public void beginTask(String name, int totalWork) {
		_totalWork = totalWork;
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
		fTaskName = name;
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#subTask(String)
	 */
	public void subTask(String name) {
		fTaskName = name;
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#worked(int)
	 */
	public void worked(int work) {
		_workDoneSoFar += work;
	}

	public void renderProgress(Element el) {

		String strPercent;
		double dPercent = (double)_workDoneSoFar / (double)_totalWork;
		synchronized (format) {
			strPercent = format.format(dPercent);
		}
		
		Element elDiv = XMLUtils.addElement(el, "div");

		Element pg = XMLUtils.addElement(elDiv, "div");
		pg.setAttribute("class", "rqprogressBar");
		
		Element pb = XMLUtils.addElement(pg, "div");
		pb.setAttribute("class", "rqborder");
		
		Element pd = XMLUtils.addElement(pb, "div");
		pd.setAttribute("class", "rqbackground");
		pd.setAttribute("style", "width:"+strPercent);
		
		Element pf = XMLUtils.addElement(pd, "div");
		pf.setAttribute("class", "rqforeground");
		
		// and the text
		if(fTaskName != null) {
			Element elTask = XMLUtils.addElement(el, "div");
			elTask.setAttribute("class", "rqtask");
			String msg = fTaskName;  
			XMLUtils.setText(elTask, msg);
			XMLUtils.addElement(elTask, "br");
			XMLUtils.addElement(elTask, "div", strPercent).setAttribute("class", "rqpercent");
		}		
	}
}
