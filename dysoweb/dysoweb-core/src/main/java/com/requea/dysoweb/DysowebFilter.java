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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.requea.webenv.IWebProcessor;

public class DysowebFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
		// starts the osgi platform
		String prefix = config.getInitParameter("RequestPrefix");
		DysowebServlet.startFelix(config.getServletContext(), prefix);
	}
	

	public void destroy() {
		DysowebServlet.stopFelix();
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		IWebProcessor processor = DysowebServlet.getActiveProcessor();
		if (processor != null) {
			// chain with the Request processor from the OSGI platform
			processor.process(request, response, chain);
		} else {
			// regular chain processing
			chain.doFilter(request, response);
		}
	}
}
