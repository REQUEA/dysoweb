package com.requea.dysoweb.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;

import com.requea.dysoweb.util.xml.XMLUtils;


public class Installable implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String fID;
	private String fType;
	private String fImage;
	private List   fCategories;
	private String fName;
	private String fDescription;
	private String fBundleList;
	private String fVersion;
	private String fInfoURL;
	private String fBaseURL;
	private String fLongDesc;
	private String fSysId;
	private boolean fRoot;
	private ArrayList fDependsOn;
	
	public static class Category implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		public String fId;
		public String fLabel;
	}
	
	public static Installable[] parse(Element el) {
		
		ArrayList lst = new ArrayList();
		
		Element elResource = XMLUtils.getChild(el, "resource");
		while(elResource != null) {
			Installable f = new Installable();
			f.fID = elResource.getAttribute("symbolicname");
			f.fName = elResource.getAttribute("presentationname");
			f.fVersion = elResource.getAttribute("version");
			f.parseCapabilities(elResource);
			
			f.fDescription = XMLUtils.getChildText(elResource, "description");
			f.fLongDesc = XMLUtils.getChildText(elResource, "documentation");

			f.fCategories = new ArrayList();
			Element elCategory = XMLUtils.getChild(elResource, "category");
			while(elCategory != null) {
				Category c = new Category();
				c.fId = elCategory.getAttribute("id");
				c.fLabel = c.fId;
				f.fCategories.add(c);
				elCategory = XMLUtils.getNextSibling(elCategory);
			}
			
			lst.add(f);

			elResource = XMLUtils.getNextSibling(elResource);
		}
		
		return (Installable[])lst.toArray(new Installable[lst.size()]);
	}

	private void parseCapabilities(Element elResource) {
		Element elCap = XMLUtils.getChild(elResource, "capability");
		while(elCap != null) {
			String name = elCap.getAttribute("name");
			if("bundle".equals(name)) {
				fType = "bundle";
			}
			if("product".equals(name)) {
				fType = "product";
			}
			if("bundle".equals(name) || "product".equals(name)) {
				Element elProp = XMLUtils.getChild(elCap, "p");
				while(elProp != null) {
					String propName = elProp.getAttribute("n");
					String propVal = elProp.getAttribute("v");
					if("rqroot".equals(propName) && "true".equals(propVal)) {
						// found a root resource
						fRoot = true;
					} else if("image".equals(propName)) {
						fImage = propVal;
					} else if("infoURL".equals(propName)) {
						fInfoURL = propVal;
					} else if("baseURL".equals(propName)) {
						fBaseURL = propVal;
					}
					// next one
					elProp = XMLUtils.getNextSibling(elProp);
				}
			}
			// check the next one
			elCap = XMLUtils.getNextSibling(elCap);
		}
	}
	
	public static Installable[] parseAsFeature(Element el) {
		
		ArrayList lst = new ArrayList();
		
		Element elFeature = XMLUtils.getChild(el, "rqRepoFeature");
		while(elFeature != null) {
			
			Installable f = new Installable();
			f.fSysId = elFeature.getAttribute("sysId");
			f.fID = "feature_"+XMLUtils.getChildText(elFeature, "rqID");
			f.fName = XMLUtils.getChildText(elFeature, "rqName");
			f.fDescription = XMLUtils.getChildText(elFeature, "rqDescription");
			f.fLongDesc = XMLUtils.getChildText(elFeature, "rqLongDesc");
			f.fCategories = new ArrayList();
			f.fType = "feature";
			f.fRoot = true;
			Element elCategory = XMLUtils.getChild(elFeature, "rqCategory");
			while(elCategory != null) {
				Category c = new Category();
				c.fId = elCategory.getAttribute("sysId");
				c.fLabel = XMLUtils.getChildText(elCategory, "sysLabel");
				f.fCategories.add(c);
				elCategory = XMLUtils.getNextSibling(elCategory);
			}
			Element elImage = XMLUtils.getChild(elFeature, "rqImage");
			if(elImage == null) {
				f.fImage = null;
			} else {
				f.fImage = elImage.getAttribute("url");
			}
			f.fBundleList = XMLUtils.getChildText(elFeature, "rqBundleList");
			f.fVersion = XMLUtils.getChildText(elFeature, "rqVersion");
			f.fInfoURL = XMLUtils.getChildText(elFeature, "rqInfoURL");
			f.fBaseURL = XMLUtils.getChildText(elFeature, "rqBaseURL");

			f.fDependsOn = new ArrayList();
			Element elDependent = XMLUtils.getChild(elFeature, "rqDependsOn");
			while(elDependent != null) {
				f.fDependsOn.add(elDependent.getAttribute("sysId"));
				elDependent = XMLUtils.getNextSibling(elDependent);
			}
			
			lst.add(f);
			elFeature = XMLUtils.getNextSibling(elFeature);
		}
		
		return (Installable[])lst.toArray(new Installable[lst.size()]);
	}
	

	public String getID() {
		return fID;
	}

	public List getCategories() {
		return fCategories;
	}

	public boolean isInCategory(String cat) {
		if(fCategories == null || cat == null) {
			return false;
		} else {
			for(int i=0; i<fCategories.size(); i++) {
				Category c = (Category)fCategories.get(i);
				if(cat.equals(c.fId)) {
					return true;
				}
			}
			// nothing found
			return false;
		}
	}
	
	public String getSysId() {
		return fSysId;
	}

	public String getVersion() {
		return fVersion;
	}

	public String getBaseURL() {
		return fBaseURL;
	}

	public String getImage() {
		return fImage;
	}

	public String getBundleList() {
		return fBundleList;
	}

	public String getInfoURL() {
		return fInfoURL;
	}

	public String getDescription() {
		return fDescription;
	}

	public String getLongDesc() {
		return fLongDesc;
	}

	public String getName() {
		return fName == null || fName.length() == 0 ? fID : fName;
	}

	public static Map buildCategories(Installable[] features) {
		
		TreeMap map = new TreeMap();
		
		for(int i=0; i<features.length; i++) {
			Installable f = features[i];
			for(int j=0; j<f.fCategories.size(); j++) {
				Installable.Category c = (Installable.Category)f.fCategories.get(j);
				if(!map.containsKey(c.fLabel)) {
					map.put(c.fLabel, c);
				}
			}
		}
		
		return map;
	}

	public boolean isRoot() {
		return fRoot;
	}

	public String getType() {
		return fType;
	}

	public List getDependsOn() {
		return fDependsOn;
	}
}
