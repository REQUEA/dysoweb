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

package com.requea.webenv;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;


public interface IWebProcessor {

	public void process(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException;
	
	// listeners events
	public void processContextEvent(String eventType, ServletContextEvent event);
	public void processContextAttributeEvent(String eventType, ServletContextAttributeEvent event);
	public void processSessionEvent(String eventType, HttpSessionEvent event);
	public void processSessionAttributeEvent(String eventType, HttpSessionBindingEvent event);
}
