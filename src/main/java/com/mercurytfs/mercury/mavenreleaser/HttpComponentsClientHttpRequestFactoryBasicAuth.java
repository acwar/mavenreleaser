package com.mercurytfs.mercury.mavenreleaser;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.net.URI;

public class HttpComponentsClientHttpRequestFactoryBasicAuth extends HttpComponentsClientHttpRequestFactory
{
    HttpHost host;
    
    public HttpComponentsClientHttpRequestFactoryBasicAuth(final HttpHost host) {
        this.host = host;
    }

    @Override
    protected HttpContext createHttpContext(final HttpMethod httpMethod, final URI uri) {
        return this.createHttpContext();
    }
    
    private HttpContext createHttpContext() {
        final AuthCache authCache = new BasicAuthCache();
        final BasicScheme basicAuth = new BasicScheme();
        authCache.put(this.host, (AuthScheme)basicAuth);
        final BasicHttpContext localcontext = new BasicHttpContext();
        localcontext.setAttribute("http.auth.auth-cache", (Object)authCache);
        return localcontext;
    }
}
