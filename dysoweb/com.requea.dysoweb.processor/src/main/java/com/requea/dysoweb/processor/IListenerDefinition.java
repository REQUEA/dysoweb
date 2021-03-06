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

import org.osgi.framework.Bundle;

import com.requea.dysoweb.WebAppException;

public interface IListenerDefinition {

	public boolean isContextNotified();

	public long getBundleId();
	public void loadClass(Bundle bundle) throws WebAppException;
	public ClassLoader getLoader();
	
	public void load() throws WebAppException;
	public void unload();
	
	public Object getInstance();

	public String getClassName();
	
	public void setContextNotified(boolean b);
	
}
