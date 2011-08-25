package com.requea.dysoweb.service.obr;


import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.osgi.service.obr.RepositoryAdmin;

public interface ClientAuthRepositoryAdmin extends RepositoryAdmin {

	void setHttp(HttpClient m_httpClient, HttpHost m_targetHost);
}
