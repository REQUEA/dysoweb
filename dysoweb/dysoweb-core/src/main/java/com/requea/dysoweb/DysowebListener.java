// ========================================================================
// Copyright 2007 Requea.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package com.requea.dysoweb;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.requea.webenv.IWebProcessor;
import com.requea.webenv.WebContext;

public class DysowebListener implements ServletContextListener, ServletContextAttributeListener, HttpSessionListener, HttpSessionAttributeListener {

	public void contextDestroyed(ServletContextEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processContextEvent("contextDestroyed", event);
	}

	public void contextInitialized(ServletContextEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processContextEvent("contextInitialized", event);
	}

	public void attributeAdded(ServletContextAttributeEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processContextAttributeEvent("attributeAdded", event);
	}

	public void attributeRemoved(ServletContextAttributeEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processContextAttributeEvent("attributeRemoved", event);
	}

	public void attributeReplaced(ServletContextAttributeEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processContextAttributeEvent("attributeReplaced", event);
	}

	public void sessionCreated(HttpSessionEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processSessionEvent("sessionCreated", event);
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processSessionEvent("sessionDestroyed", event);
	}

	public void attributeAdded(HttpSessionBindingEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processSessionAttributeEvent("attributeAdded", event);
	}

	public void attributeRemoved(HttpSessionBindingEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processSessionAttributeEvent("attributeRemoved", event);
	}

	public void attributeReplaced(HttpSessionBindingEvent event) {
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null)
			processor.processSessionAttributeEvent("attributeReplaced", event);
	}



}
