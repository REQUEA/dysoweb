package com.requea.dysoweb.portlet.processor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.portlet.Portlet;
import javax.portlet.PortletException;

import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.portlet.IPortletProcessor;
import com.requea.dysoweb.portlet.IPortletProcessorListener;
import com.requea.dysoweb.portlet.util.XMLException;
import com.requea.dysoweb.portlet.util.XMLUtils;

public class PortletProcessor implements IPortletProcessor {

	private Map fDefinitions = new HashMap();
	
	public Portlet createPortlet(IPortletProcessorListener listener, String portletName) throws PortletException {
		synchronized (fDefinitions) {
			// get the portlet definitions
			PortletDefinition def = (PortletDefinition)fDefinitions.get(portletName);
			if(def == null) {
				return null;
			}
			if(def.fPortlet != null) {
				return def.fPortlet;
			} else {
				// instantiate the portlet
				try {
					Class cls = def.fBundle.loadClass(def.fClassName);
					Portlet portlet = (Portlet)cls.newInstance();
					def.fPortlet = portlet;
					def.fListener = listener;
					return portlet;
				} catch(Exception e) {
					throw new PortletException(e); 
				}
			}
		}
	}
	public void destroyPortlet(IPortletProcessorListener listener, String portletName) {
		synchronized (fDefinitions) {
			// get the portlet definitions
			PortletDefinition def = (PortletDefinition)fDefinitions.get(portletName);
			if(def == null) {
				// nothing to destroy
				return;
			}
			def.fListener = null;
			def.fPortlet = null;
		}
	}

	public void deploy(Bundle bundle) throws WebAppException {
		if(bundle == null) {
			// do nothing
			return;
		}
		
		// check if there is a portlet.xml descriptor
		URL descURL = bundle.getResource("/webapp/WEB-INF/portlet.xml");
		if(descURL != null) {
			// this dysoweb application contains portlets
			// parse the descriptor and add the definitions to the list of portlets definitions
			parsePortletDescriptor(bundle, descURL);
		}
	}


	public void undeploy(Bundle bundle) throws WebAppException {
		synchronized (fDefinitions) {
			// check if some portlet definitions have portlet associated 
			Iterator iter = fDefinitions.values().iterator();
			List portletNames = new ArrayList();
			while(iter.hasNext()) {
				PortletDefinition def = (PortletDefinition)iter.next();
				if(def.fBundleId == bundle.getBundleId()) {
					// this portlet should be undeployed
					if(def.fListener != null) {
						def.fListener.onProxyUnavailable();
					}
					def.fPortlet = null;
					def.fListener = null;
					portletNames.add(def.fPortletName);
				}
			}
			// remove all the portlets
			for(int i=0; i<portletNames.size(); i++) {
				fDefinitions.remove(portletNames.get(i));
			}
		}
	}
	
	private void parsePortletDescriptor(Bundle bundle, URL url) throws WebAppException {
		try {
			InputStream is = url.openStream();
			Document doc = XMLUtils.parse(is);
			is.close();

			synchronized (fDefinitions) {
				// scan the xml content and load the context descriptor
				Element elPortlet = XMLUtils.getChild(doc.getDocumentElement(), "portlet");
				while(elPortlet != null) {
					String portletName = XMLUtils.getChildText(elPortlet, "portlet-name");
					String portletClass = XMLUtils.getChildText(elPortlet, "portlet-class");
					
					PortletDefinition def = new PortletDefinition();
					def.fPortletName = portletName;
					def.fClassName = portletClass;
					def.fBundle = bundle;
					def.fBundleId = bundle.getBundleId();
					// add the definition to the definitions
					fDefinitions.put(portletName, def);

					// next one
					elPortlet = XMLUtils.getNextSibling(elPortlet);
				}
			}
		} catch(IOException e) {
			throw new WebAppException(e);
		} catch (XMLException e) {
			throw new WebAppException(e);
		}
	}
	
	private class PortletDefinition {
		public String fPortletName;
		public Bundle fBundle;
		public long   fBundleId;
		public IPortletProcessorListener fListener;
		public Portlet fPortlet;
		public String fClassName; 
		
	}
}
