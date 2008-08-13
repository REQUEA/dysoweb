package com.requea.dysoweb.panel.tags;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.InstallServlet;
import com.requea.dysoweb.panel.SecurityFilter;
import com.requea.dysoweb.panel.utils.ISO8601DateTimeFormat;
import com.requea.dysoweb.util.xml.XMLException;
import com.requea.dysoweb.util.xml.XMLUtils;

public class InstalledFeaturesTag extends BodyTagSupport {

	private static final long serialVersionUID = -2314988786245575718L;
	private Element[] fFeatures;
	private int fIndex;
	
	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

        // retrieve all the apps installed
        File base = SecurityFilter.getConfigDir(pageContext.getServletContext()); 
        File dir = new File(base, "features");

        ArrayList lst = new ArrayList();
        
        // enumerate the xml files
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		});
		if(files == null) {
			return SKIP_BODY;
		}
		for(int i=0; i<files.length; i++) {
			// parse the file
			try {
				Document doc = XMLUtils.parse(new FileInputStream(files[i]));
				Element el = doc.getDocumentElement();
				lst.add(el);
			} catch (FileNotFoundException e) {
				// ignore
			} catch (XMLException e) {
				// ignore
			}
		}

		// get the array of documents elements
		Element[] features = (Element[])lst.toArray(new Element[lst.size()]);
		
		// sort the features by installation date
		Arrays.sort(features, new Comparator() {

			public int compare(Object arg0, Object arg1) {
				try {
					Element el0 = (Element)arg0;
					Element el1 = (Element)arg1;
					String strDate0 = el0.getAttribute("date");
					String strDate1 = el1.getAttribute("date");
					DateFormat df = new ISO8601DateTimeFormat();
					Date d0 = df.parse(strDate0);
					Date d1 = df.parse(strDate1);
					return d0.compareTo(d1);
				} catch(Exception e) {
					return 0;
				}
			}
		});
		
		fFeatures = features;
		fIndex = 0;
		if(fIndex < fFeatures.length) {
			request.setAttribute(InstallServlet.INSTALLEDFEATURE, fFeatures[fIndex]);
			return EVAL_BODY_INCLUDE;
		} else {			
			return SKIP_BODY;
		}
	}

	public int doAfterBody() throws JspException {

        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		fIndex ++;
		if(fIndex < fFeatures.length) {
			request.setAttribute(InstallServlet.INSTALLEDFEATURE, fFeatures[fIndex]);
			return EVAL_BODY_AGAIN;
		} else {			
			return SKIP_BODY;
		}
		
	}

	
}
