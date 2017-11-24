package org.vebqa.vebtal.seleneserestserver.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Liest ein Verzeichnis aus, identifiziert Webdriver Treiber und ordnet diese den Browsern zu.
 * @author doerges
 *
 */
public class DriverManager {

	private static final Logger logger = LoggerFactory.getLogger(DriverManager.class);
	private Map<String, String> drivers = new HashMap<>();
	private String standardDriverDir = "driver";
	
	
	public DriverManager() {
		drivers.clear();
	}
	
	public void scanDirectory() {
		logger.info("Scan for drivers..");
		File dir = new File(standardDriverDir);
		String[] directoryContents = dir.list();
		if (directoryContents == null) {
			logger.info("No webdriver found!");
			return;
		}
		for (String aFileName : directoryContents) {
			File temp = new File(String.valueOf(dir),aFileName);
			logger.info("Found driver: " + temp.getAbsolutePath());
			if (temp.getName().startsWith("chromedriver")) {
				drivers.put("Chrome", String.valueOf(temp.getAbsolutePath()));
				System.setProperty("webdriver.chrome.driver", temp.getAbsolutePath());
			}
			if (temp.getName().startsWith("geckodriver")) {
				drivers.put("Firefox", String.valueOf(temp.getAbsolutePath()));
				System.setProperty("webdriver.gecko.driver", temp.getAbsolutePath());
			}
			if (temp.getName().startsWith("IEDriverServer")) {
				drivers.put("IExplorer", String.valueOf(temp.getAbsolutePath()));
				System.setProperty("webdriver.ie.driver", temp.getAbsolutePath());
			}			
		}
	}
	
	public String getDriver(String aBrowser) {
		if (!drivers.containsKey(aBrowser)) {
			return null;
		}
		return drivers.get(aBrowser);
	}
	
	public Set<String> getDriverKeys() {
		return drivers.keySet();
	}
}
