package com.requea.dysoweb.portlet;

import javax.portlet.Portlet;
import javax.portlet.PortletException;

public interface IPortletProcessor {

	public Portlet createPortlet(IPortletProcessorListener listener, String portletName)  throws PortletException;
	public void destroyPortlet(IPortletProcessorListener listener, String portletName);
}
