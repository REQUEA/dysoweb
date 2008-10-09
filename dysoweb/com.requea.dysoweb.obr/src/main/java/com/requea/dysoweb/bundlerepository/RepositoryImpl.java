/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.requea.dysoweb.bundlerepository;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.osgi.service.obr.*;

import com.requea.dysoweb.bundlerepository.metadataparser.kxmlsax.KXml2SAXParser;
import com.requea.dysoweb.bundlerepository.metadataparser.XmlCommonHandler;

public class RepositoryImpl implements Repository
{
    private String m_name = null;
    private long m_lastmodified = 0;
    private URL m_url = null;
    private Resource[] m_resources = null;
    private int m_hopCount = 1;

    // Reusable comparator for sorting resources by name.
    private ResourceComparator m_nameComparator = new ResourceComparator();
	private SSLSocketFactory m_sslSocketFactory;
	private String m_proxyAuth;
	private Proxy m_proxy;

    public RepositoryImpl(URL url, Proxy proxy, String proxyAuth, SSLSocketFactory sslFactory) throws Exception
    {
        m_url = url;
        m_sslSocketFactory = sslFactory;
        m_proxy = proxy;
        m_proxyAuth = proxyAuth;
        
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    parseRepositoryFile(m_hopCount);
                    return null;
                }
            });
        } 
        catch (PrivilegedActionException ex) 
        {
            throw (Exception) ex.getCause();
        }
    }

    public URL getURL()
    {
        return m_url;
    }

    protected void setURL(URL url)
    {
        m_url = url;
    }

    public Resource[] getResources()
    {
        return m_resources;
    }

    public void addResource(Resource resource)
    {
        // Set resource's repository.
        ((ResourceImpl) resource).setRepository(this);

        // Add to resource array.
        if (m_resources == null)
        {
            m_resources = new Resource[] { resource };
        }
        else
        {
            Resource[] newResources = new Resource[m_resources.length + 1];
            System.arraycopy(m_resources, 0, newResources, 0, m_resources.length);
            newResources[m_resources.length] = resource;
            m_resources = newResources;
        }

        Arrays.sort(m_resources, m_nameComparator);
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        m_name = name;
    }

    public long getLastModified()
    {
        return m_lastmodified;
    }

    public void setLastmodified(String s)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss.SSS");
        try
        {
            m_lastmodified = format.parse(s).getTime();
        }
        catch (ParseException ex)
        {
        }
    }

    /**
     * Default setter method when setting parsed data from the XML file,
     * which currently ignores everything. 
    **/
    protected Object put(Object key, Object value)
    {
        // Ignore everything for now.
        return null;
    }

    private void parseRepositoryFile(int hopCount) throws Exception
    {
// TODO: OBR - Implement hop count.
        InputStream is = null;
        BufferedReader br = null;

        try
        {
            // Do it the manual way to have a chance to 
            // set request properties as proxy auth (EW).
            URLConnection conn = m_proxy == null ? m_url.openConnection() : m_url.openConnection(m_proxy); 

            // Support for http proxy authentication
            if ((m_proxyAuth != null) && (m_proxyAuth.length() > 0))
            {
                if ("http".equals(m_url.getProtocol()) ||
                    "https".equals(m_url.getProtocol()))
                {
                    String base64 = Util.base64Encode(m_proxyAuth);
                    conn.setRequestProperty(
                        "Proxy-Authorization", "Basic " + base64);
                }
            }
            // set the client certificate if any
            if(m_sslSocketFactory != null && conn instanceof HttpsURLConnection) {
            	((HttpsURLConnection)conn).setSSLSocketFactory(m_sslSocketFactory);
            }
            is = openStream(m_url, conn, "repository.xml");
            if (is != null)
            {
                // Create the parser Kxml
                XmlCommonHandler handler = new XmlCommonHandler();
                Object factory = new Object() {
                    public RepositoryImpl newInstance()
                    {
                        return RepositoryImpl.this;
                    }
                };

                // Get default setter method for Repository.
                Method repoSetter = RepositoryImpl.class.getDeclaredMethod(
                    "put", new Class[] { Object.class, Object.class });

                // Get default setter method for Resource.
                Method resSetter = ResourceImpl.class.getDeclaredMethod(
                    "put", new Class[] { Object.class, Object.class });

                // Map XML tags to types.
                handler.addType("repository", factory, Repository.class, repoSetter);
                handler.addType("resource", ResourceImpl.class, Resource.class, resSetter);
                handler.addType("category", CategoryImpl.class, null, null);
                handler.addType("require", RequirementImpl.class, Requirement.class, null);
                handler.addType("capability", CapabilityImpl.class, Capability.class, null);
                handler.addType("p", PropertyImpl.class, null, null);
                handler.setDefaultType(String.class, null, null);

                br = new BufferedReader(new InputStreamReader(is));
                KXml2SAXParser parser;
                parser = new KXml2SAXParser(br);
                parser.parseXML(handler);
            }
            else
            {
                // This should not happen.
                throw new Exception("Unable to get input stream for repository.");
            }
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                // Not much we can do.
            }
        }
    }

	public static Proxy getProxy(String host, int port) {
		return Proxy.NO_PROXY;
	}
	
	public static InputStream openStream(URL repoURL, URLConnection cnx, String path) throws IOException {
		if(repoURL.getPath().endsWith("zip")) {
			if(repoURL.getProtocol().equals("file")) {
				// use a zip file, since this is way faster
				File f = new File(repoURL.getFile());
				ZipFile zf = new ZipFile(f);
				ZipEntry ze = zf.getEntry(path);
				if(ze != null) {
					return zf.getInputStream(ze);
				} else {
					return null;
				}
			} else {
	            ZipInputStream zin = new ZipInputStream(cnx.getInputStream());
	            ZipEntry entry = zin.getNextEntry();
	            while (entry != null)
	            {
	                if (entry.getName().equalsIgnoreCase(path))
	                {
	                    return zin;
	                }
	                entry = zin.getNextEntry();
	            }
	            // nothing found
	            return null;
			}
		} else {
			return cnx.getInputStream();
		}
	}
	
}