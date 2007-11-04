package com.requea.dysoweb.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;

import com.requea.dysoweb.panel.utils.xml.XMLUtils;

public class Feature implements Serializable {

	private static final long serialVersionUID = 150939454759622369L;
	private String fID;
	private Element fImage;
	private List   fCategories;
	private String fName;
	private String fDescription;
	private String fBundleList;
	private String fVersion;
	private String fInfoURL;
	private String fBaseURL;
	private String fLongDesc;
	private String fSysId;
	private ArrayList fDependsOn;
	
	public static class Category {

		public String fId;
		public String fLabel;
		
	}
	
	public static Feature[] parse(Element el) {
		
		ArrayList lst = new ArrayList();
		
		Element elFeature = XMLUtils.getChild(el, "rqRepoFeature");
		while(elFeature != null) {
			
			Feature f = new Feature();
			f.fSysId = elFeature.getAttribute("sysId");
			f.fID = XMLUtils.getChildText(elFeature, "rqID");
			f.fName = XMLUtils.getChildText(elFeature, "rqName");
			f.fDescription = XMLUtils.getChildText(elFeature, "rqDescription");
			f.fLongDesc = XMLUtils.getChildText(elFeature, "rqLongDesc");
			f.fCategories = new ArrayList();
			Element elCategory = XMLUtils.getChild(elFeature, "rqCategory");
			while(elCategory != null) {
				Category c = new Category();
				c.fId = elCategory.getAttribute("sysId");
				c.fLabel = XMLUtils.getChildText(elCategory, "sysLabel");
				f.fCategories.add(c);
				elCategory = XMLUtils.getNextSibling(elCategory);
			}
			f.fImage = XMLUtils.getChild(elFeature, "rqImage");
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
		
		return (Feature[])lst.toArray(new Feature[lst.size()]);
	}

	public String getID() {
		return fID;
	}

	public List getCategories() {
		return fCategories;
	}

	public List getDependsOn() {
		return fDependsOn;
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

	public Element getImage() {
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
		return fLongDesc == null || fLongDesc.length() == 0 ? fDescription : fLongDesc;
	}

	public String getName() {
		return fName == null || fName.length() == 0 ? fID : fName;
	}

	public static Map buildCategories(Feature[] features) {
		
		TreeMap map = new TreeMap();
		
		for(int i=0; i<features.length; i++) {
			Feature f = features[i];
			for(int j=0; j<f.fCategories.size(); j++) {
				Feature.Category c = (Feature.Category)f.fCategories.get(j);
				if(!map.containsKey(c.fLabel)) {
					map.put(c.fLabel, c);
				}
			}
		}
		
		return map;
	}
	
}
