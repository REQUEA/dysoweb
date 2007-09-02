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

package com.requea.dysoweb.processor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.requea.dysoweb.processor.IFilterDefinition;
import com.requea.dysoweb.processor.RequestProcessor.EntryInfo;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.org.mortbay.jetty.servlet.PathMap;
import com.requea.dysoweb.servlet.wrapper.FilterMappingDefinition;
import com.requea.dysoweb.servlet.wrapper.ServletMappingDefinition;
import com.requea.dysoweb.servlet.wrapper.ServletWrapper;

/**
 * The object that maps Servlet request patterns to ApplicationChain form processing
 * @author Pierre Dubois
 *
 */
public class RequestMapper {

    private static Log fLog = LogFactory.getLog(RequestMapper.class);
	
	private PathMap fFiltersPathMap;
	private PathMap fServletsPathMap;
	private boolean fInitialized = false;


	private RequestProcessor fRequestProcessor;

	public RequestMapper(RequestProcessor processor) {
		fRequestProcessor = processor;
	}
	
	public RequestMapper(RequestProcessor processor, RequestMapper src) {
		fRequestProcessor = processor;
	}

	
	public IFilterDefinition[] getFilters(String uri) {
		List lst = fFiltersPathMap.getMatches(uri);
		
		IFilterDefinition[] filters = new IFilterDefinition[lst.size()];
		for(int i=0; i<lst.size(); i++) {
			Map.Entry entry = (Map.Entry)lst.get(i);
			filters[i] = (IFilterDefinition)entry.getValue();
		}
		
		return filters;
	}
	
	public ServletWrapper getServletWrapper(String uri) throws IOException {
		if(!fInitialized) {
			return null;
		}
		
		if(uri.endsWith(".jsp") || uri.endsWith(".jspx")) {
			// jasper wrappers
			EntryInfo ei = fRequestProcessor.getEntryInfo(uri);
			return ei == null ? null : fRequestProcessor.getJapserWrapper(new Long(ei.getBundleId()));
		} else {
			// regular wrapper
			return (ServletWrapper)fServletsPathMap.match(uri);
		}
	}

	/*
	 * Initialize the mapper: load the servlets, filters and create the Servlet wrappers for the patterns
	 * based on the mapping definitions
	 */
	public synchronized void init(List definitions) throws WebAppException, ServletException {

		// already initialized?
		if(fInitialized) {
			return;
		}
		
		// clear the previous maps (note that there shoiuld not be)
		fServletsPathMap = new PathMap();
		fFiltersPathMap = new PathMap();
		
		// Phase3: map the servlet wrappers and the filters that are mapped to a servlet
		for(int i=0; i<definitions.size(); i++) {
			Object item = definitions.get(i); 
			if(item instanceof FilterMappingDefinition) {
				FilterMappingDefinition def = (FilterMappingDefinition)item;
				if(def.getURLPattern() != null) {
					// filter mapped by URL
					IFilterDefinition flt = fRequestProcessor.getFilterByName(def.getFilterName());
					if(flt == null) {
						fLog.equals("Incorrect filter mapping definition. Unable to find filter "+def.getFilterName() + " for servlet " + def.getServletName());
					} else {
						fFiltersPathMap.put(def.getURLPattern(), flt);
					}
				}
			}
		}
		
		// Phase4: create mapping for the servlets based on the servlet mappings
		for(int i=0; i<definitions.size(); i++) {
			Object item = definitions.get(i); 
			if(item instanceof ServletMappingDefinition) {
				ServletMappingDefinition def = (ServletMappingDefinition)item;
				String servletName = def.getServletName();
				ServletWrapper w = fRequestProcessor.getServletWrapper(servletName);
				String pattern = def.getPattern();
				fServletsPathMap.put(pattern, w);
			}
		}
		
		fInitialized = true;
	}
}
