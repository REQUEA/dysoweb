package com.requea.dysoweb.portlet;

public class DysowebPortletContext {
	
	private static IPortletProcessor fPortletProcessor;

	public static void setPortletProcessor(IPortletProcessor processor) {
		fPortletProcessor = processor;
	}
	
	public static IPortletProcessor getPortletProcessor() {
		return fPortletProcessor;
	}
}
