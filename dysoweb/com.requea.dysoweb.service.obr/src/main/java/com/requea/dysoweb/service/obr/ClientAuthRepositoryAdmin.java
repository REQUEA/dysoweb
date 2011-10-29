package com.requea.dysoweb.service.obr;


import org.osgi.service.obr.RepositoryAdmin;

public interface ClientAuthRepositoryAdmin extends RepositoryAdmin {

	void setHttp(HttpClientExecutor executor);
}
