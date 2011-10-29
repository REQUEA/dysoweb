package com.requea.dysoweb.service.obr;

import java.io.IOException;
import java.io.InputStream;

public interface HttpClientExecutor {

	public InputStream executeGet(String path) throws IOException;

}
