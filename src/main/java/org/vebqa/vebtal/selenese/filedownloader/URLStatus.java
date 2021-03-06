package org.vebqa.vebtal.selenese.filedownloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLStatus {
    private static final Logger logger = LoggerFactory.getLogger(URLStatus.class);
    private URI linkToCheck;
    private WebDriver driver;
    private boolean mimicWebDriverCookieState = true;
    private boolean followRedirects = false;
    private RequestMethod httpRequestMethod = RequestMethod.GET;
 
    public URLStatus(WebDriver driverObject) throws MalformedURLException, URISyntaxException {
        this.driver = driverObject;
    }
 
    /**
     * Specify a URL that you want to perform an HTTP Status Check upon
     *
     * @param 	linkToCheck	Link to check
     * @throws MalformedURLException	thrown if url is not well formed
     * @throws URISyntaxException	thrown if an URI syntax error occured
     */
    public void setURIToCheck(String linkToCheck) throws MalformedURLException, URISyntaxException {
        this.linkToCheck = new URI(linkToCheck);
    }
 
    /**
     * Specify a URL that you want to perform an HTTP Status Check upon
     *
     * @param	linkToCheck	Link to check
     * @throws MalformedURLException	thrown if url is not well formed
     */
    public void setURIToCheck(URI linkToCheck) throws MalformedURLException {
        this.linkToCheck = linkToCheck;
    }
 
    /**
     * Specify a URL that you want to perform an HTTP Status Check upon
     *
     * @param	linkToCheck	Link to check
     * @throws URISyntaxException Exception
     */
    public void setURIToCheck(URL linkToCheck) throws URISyntaxException {
        this.linkToCheck = linkToCheck.toURI();
    }
 
    /**
     * Set the HTTP Request Method (Defaults to 'GET')
     *
     * @param requestMethod request
     */
    public void setHTTPRequestMethod(RequestMethod requestMethod) {
        this.httpRequestMethod = requestMethod;
    }
 
    /**
     * Should redirects be followed before returning status code?
     * If set to true a 302 will not be returned, instead you will get the status code after the redirect has been followed
     * DEFAULT: false
     *
     * @param value boolean, set to true if follow redirects
     */
    public void followRedirects(Boolean value) {
        this.followRedirects = value;
    }
 
    /**
     * Perform an HTTP Status check and return the response code
     *
     * @return HTTP status code
     * @throws IOException there might be an exception during executing the client
     */
    public int getHTTPStatusCode() throws IOException {
 
        HttpClient client = new DefaultHttpClient();
        BasicHttpContext localContext = new BasicHttpContext();
 
        logger.info("Mimic WebDriver cookie state: {}", this.mimicWebDriverCookieState);
        if (this.mimicWebDriverCookieState) {
            localContext.setAttribute(ClientContext.COOKIE_STORE, mimicCookieState(this.driver.manage().getCookies()));
        }
        HttpRequestBase requestMethod = this.httpRequestMethod.getRequestMethod();
        requestMethod.setURI(this.linkToCheck);
        HttpParams httpRequestParameters = requestMethod.getParams();
        httpRequestParameters.setParameter(ClientPNames.HANDLE_REDIRECTS, this.followRedirects);
        requestMethod.setParams(httpRequestParameters);
 
        logger.info("Sending {} request for: {}", requestMethod.getMethod(), requestMethod.getURI());
        HttpResponse response = client.execute(requestMethod, localContext);
        logger.info("HTTP {} request status: {}", requestMethod.getMethod(), response.getStatusLine().getStatusCode());
 
        return response.getStatusLine().getStatusCode();
    }
 
    /**
     * Mimic the cookie state of WebDriver (Defaults to true)
     * This will enable you to access files that are only available when logged in.
     * If set to false the connection will be made as an anonymouse user
     *
     * @param value boolean, see documention
     */
    public void mimicWebDriverCookieState(boolean value) {
        this.mimicWebDriverCookieState = value;
    }
 
    /**
     * Load in all the cookies WebDriver currently knows about so that we can mimic the browser cookie state
     *
     * @param seleniumCookieSet set of all cookies we alreade know
     * @return BasicCookieStore
     */
    private BasicCookieStore mimicCookieState(Set<Cookie> seleniumCookieSet) {
        BasicCookieStore mimicWebDriverCookieStore = new BasicCookieStore();
        for (Cookie seleniumCookie : seleniumCookieSet) {
            BasicClientCookie duplicateCookie = new BasicClientCookie(seleniumCookie.getName(), seleniumCookie.getValue());
            duplicateCookie.setDomain(seleniumCookie.getDomain());
            duplicateCookie.setSecure(seleniumCookie.isSecure());
            duplicateCookie.setExpiryDate(seleniumCookie.getExpiry());
            duplicateCookie.setPath(seleniumCookie.getPath());
            mimicWebDriverCookieStore.addCookie(duplicateCookie);
        }
 
        return mimicWebDriverCookieStore;
    }
}
