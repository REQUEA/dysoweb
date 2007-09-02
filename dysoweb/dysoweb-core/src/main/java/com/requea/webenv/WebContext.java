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

import java.io.File;

import javax.servlet.ServletContext;


public class WebContext {

	private static ServletContext fContext;
	private static IWebProcessor fProcessor;
	private static File fBaseDir;
	private static File fScratchDir;
	private static String fRequestPrefix;

	public static void setServletContext(ServletContext ctx) {
		fContext = ctx;
	}
	public static ServletContext getServletContext() {
		return fContext;
	}
	public static void setProcessor(IWebProcessor processor) {
		fProcessor = processor;
	}
	public static IWebProcessor getProcessor() {
		return fProcessor;
	}
	
	public static File getBaseDir() {
		return fBaseDir;
	}
	public static File getScratchDir() {
		return fScratchDir;
	}
	public static void setBaseDir(File baseDir) {
		fBaseDir = baseDir;
	}
	public static void setScratchDir(File scratchDir) {
		fScratchDir = scratchDir;
	}
	public static void setRequestPrefix(String path) {
		fRequestPrefix = path;
	}
	public static String getRequestPrefix() {
		return fRequestPrefix;
	}
	
}
