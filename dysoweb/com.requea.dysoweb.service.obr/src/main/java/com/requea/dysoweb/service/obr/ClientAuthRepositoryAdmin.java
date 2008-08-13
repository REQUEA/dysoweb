package com.requea.dysoweb.service.obr;

import javax.net.ssl.SSLSocketFactory;

import org.osgi.service.obr.RepositoryAdmin;

public interface ClientAuthRepositoryAdmin extends RepositoryAdmin {

	public void setProxy(String proxhHost, int proxyPort, String proxyAuth);
	public void setSSLSocketFactory(SSLSocketFactory sf);
	
}
