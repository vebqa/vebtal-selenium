package org.vebqa.vebtal.selenese.filedownloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDownloader {
	private static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);
	
	private WebDriver driver;
	private boolean followRedirects = true;
	private boolean mimicWebDriverCookieState = true;
	private RequestMethod httpRequestMethod = RequestMethod.GET;
	private URI fileURI;

	public FileDownloader(WebDriver driverObject) {
		this.driver = driverObject;
	}

	/**
	 * Specify if the FileDownloader class should follow redirects when trying
	 * to download a file Default: true
	 *
	 * @param followRedirects boolean
	 */
	public void followRedirectsWhenDownloading(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	/**
	 * Mimic the cookie state of WebDriver (Defaults to true) This will enable
	 * you to access files that are only available when logged in. If set to
	 * false the connection will be made as an anonymouse user
	 *
	 * @param mimicWebDriverCookies boolean
	 */
	public void mimicWebDriverCookieState(boolean mimicWebDriverCookies) {
		mimicWebDriverCookieState = mimicWebDriverCookies;
	}

	/**
	 * Set the HTTP Request Method Default: GET
	 *
	 * @param requestType
	 *            RequestMethod
	 */
	public void setHTTPRequestMethod(RequestMethod requestType) {
		httpRequestMethod = requestType;
	}

	/**
	 * Specify a URL that you want to perform an HTTP Status Check upon/Download
	 * a file from
	 *
	 * @param linkToFile
	 *            String
	 * @throws MalformedURLException thrown if url is not well formed
	 * @throws URISyntaxException exception
	 */
	public void setURI(String linkToFile) throws MalformedURLException, URISyntaxException {
		fileURI = new URI(linkToFile);
	}

	/**
	 * Specify a URL that you want to perform an HTTP Status Check upon/Download
	 * a file from
	 *
	 * @param linkToFile
	 *            URI
	 * @throws MalformedURLException thrown if url is not well formed
	 */
	public void setURI(URI linkToFile) throws MalformedURLException {
		fileURI = linkToFile;
	}

	/**
	 * Specify a URL that you want to perform an HTTP Status Check upon/Download
	 * a file from
	 *
	 * @param linkToFile
	 *            URL
	 * @throws URISyntaxException in case of wrong URI
	 */
	public void setURI(URL linkToFile) throws URISyntaxException {
		fileURI = linkToFile.toURI();
	}

	/**
	 * Perform an HTTP Status Check upon/Download the file specified in the href
	 * attribute of a WebElement
	 *
	 * @param anchorElement
	 *            Selenium WebElement
	 * @throws Exception an exception
	 */
	public void setURISpecifiedInAnchorElement(WebElement anchorElement) throws Exception {
		if (("a").equals(anchorElement.getTagName())) {
			fileURI = new URI(anchorElement.getAttribute("href"));
		} else {
			throw new Exception("You have not specified an <a> element!");
		}
	}

	/**
	 * Perform an HTTP Status Check upon/Download the image specified in the src
	 * attribute of a WebElement
	 *
	 * @param imageElement
	 *            Selenium WebElement
	 * @throws Exception an exception
	 */
	public void setURISpecifiedInImageElement(WebElement imageElement) throws Exception {
		if (imageElement.getTagName().equals("img")) {
			fileURI = new URI(imageElement.getAttribute("src"));
		} else {
			throw new Exception("You have not specified an <img> element!");
		}
	}

	/**
	 * Load in all the cookies WebDriver currently knows about so that we can
	 * mimic the browser cookie state
	 *
	 * @param seleniumCookieSet
	 *            Set&lt;Cookie&gt;
	 */
	private BasicCookieStore mimicCookieState(Set<Cookie> seleniumCookieSet) {
		BasicCookieStore copyOfWebDriverCookieStore = new BasicCookieStore();
		for (Cookie seleniumCookie : seleniumCookieSet) {
			BasicClientCookie duplicateCookie = new BasicClientCookie(seleniumCookie.getName(),
					seleniumCookie.getValue());
			duplicateCookie.setDomain(seleniumCookie.getDomain());
			duplicateCookie.setSecure(seleniumCookie.isSecure());
			duplicateCookie.setExpiryDate(seleniumCookie.getExpiry());
			duplicateCookie.setPath(seleniumCookie.getPath());
			copyOfWebDriverCookieStore.addCookie(duplicateCookie);
		}

		return copyOfWebDriverCookieStore;
	}

	private HttpResponse getHTTPResponse() throws IOException, NullPointerException, KeyStoreException {
		if (fileURI == null)
			throw new NullPointerException("No file URI specified");

		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(new TrustStrategy(){
			    public boolean isTrusted(X509Certificate[] chain, String authType)
			            throws CertificateException {
			            return true;
			        }
			    }).build();
		} catch (KeyManagementException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		SSLContextBuilder builder = new SSLContextBuilder();
		SSLConnectionSocketFactory sslsf = null;
		HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
		
		try {
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HttpClient client = getNewHttpClient();

		BasicHttpContext localContext = new BasicHttpContext();

		// Clear down the local cookie store every time to make sure we don't
		// have any left over cookies influencing the test
		localContext.setAttribute(ClientContext.COOKIE_STORE, null);

		LOG.info("Mimic WebDriver cookie state: " + mimicWebDriverCookieState);
		if (mimicWebDriverCookieState) {
			localContext.setAttribute(ClientContext.COOKIE_STORE, mimicCookieState(driver.manage().getCookies()));
		}

		HttpRequestBase requestMethod = httpRequestMethod.getRequestMethod();
		requestMethod.setURI(fileURI);
		HttpParams httpRequestParameters = requestMethod.getParams();
		httpRequestParameters.setParameter(ClientPNames.HANDLE_REDIRECTS, followRedirects);
		requestMethod.setParams(httpRequestParameters);

		LOG.info("Sending " + httpRequestMethod.toString() + " request for: " + fileURI);
		return client.execute(requestMethod, localContext);
	}

	/**
	 * Gets the HTTP status code returned when trying to access the specified
	 * URI
	 *
	 * @return HTTP status code as integer
	 * @throws Exception an exception
	 */
	public int getLinkHTTPStatus() throws Exception {
		HttpResponse fileToDownload = getHTTPResponse();
		int httpStatusCode;
		try {
			httpStatusCode = fileToDownload.getStatusLine().getStatusCode();
		} finally {
			fileToDownload.getEntity().getContent().close();
		}

		return httpStatusCode;
	}

	/**
	 * Download a file from the specified URI
	 *
	 * @return File
	 * @throws Exception an exception
	 */
	public File downloadFile() throws Exception {
		doTrustToCertificates();

		File downloadedFile = File.createTempFile("download", ".tmp");
		HttpResponse fileToDownload = getHTTPResponse();
		try {
			FileUtils.copyInputStreamToFile(fileToDownload.getEntity().getContent(), downloadedFile);
		} finally {
			fileToDownload.getEntity().getContent().close();
		}

		return downloadedFile;
	}

	// trusting all certificate
	@SuppressWarnings("restriction")
	public void doTrustToCertificates() throws Exception {
		// Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
				return;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
				return;
			}
		} };

		// probieren wir es...
		trustEveryone();
		
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
//				if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
//					System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '"
//							+ session.getPeerHost() + "'.");
//				}
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
		LOG.info("Trust to all certificates! - Test only!");
	}
	
	public HttpClient getNewHttpClient() {
	    try {
	        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
	        trustStore.load(null, null);

	        TestSSLSocketFactory sf = new TestSSLSocketFactory(trustStore);
	        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	        HttpParams params = new BasicHttpParams();
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        registry.register(new Scheme("https", sf, 443));

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

	        return new DefaultHttpClient(ccm, params);
	    } catch (Exception e) {
	        return new DefaultHttpClient();
	    }
	}
	
	private void trustEveryone() { 
	    try { 
	            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){ 
	                    public boolean verify(String hostname, SSLSession session) { 
	                            return true; 
	                    }}); 
	            SSLContext context = SSLContext.getInstance("TLS"); 
	            context.init(null, new X509TrustManager[]{new X509TrustManager(){ 
	                    public void checkClientTrusted(X509Certificate[] chain, 
	                                    String authType) throws CertificateException {} 
	                    public void checkServerTrusted(X509Certificate[] chain, 
	                                    String authType) throws CertificateException {} 
	                    public X509Certificate[] getAcceptedIssuers() { 
	                            return new X509Certificate[0]; 
	                    }}}, new SecureRandom()); 
	            HttpsURLConnection.setDefaultSSLSocketFactory( 
	                            context.getSocketFactory()); 
	    } catch (Exception e) { // should never happen 
	            e.printStackTrace(); 
	    } 
	} 	
}
