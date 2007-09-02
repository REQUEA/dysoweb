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

package com.requea.dysoweb.servlet.wrapper;

/**
 * The definition for a filter mapping
 * @author Pierre Dubois
 *
 */
public class FilterMappingDefinition {

	private long   fBundleId;
	private String fFilterName;
	private String fServletName;
	private String fURLPattern;

	public FilterMappingDefinition(long bundleId, String filterName, String servletName, String urlPattern) {
		fBundleId = bundleId;
		fFilterName = filterName;
		fServletName = servletName;
		fURLPattern = urlPattern;
	}

	public String getFilterName() {
		return fFilterName;
	}

	public String getServletName() {
		return fServletName;
	}

	public long getBundleId() {
		return fBundleId;
	}

	public String getURLPattern() {
		return fURLPattern;
	}
	
}
