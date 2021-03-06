package org.vebqa.vebtal.seleneserestserver;

import java.util.Set;

import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.AbstractTestAdaptionResource;
import org.vebqa.vebtal.GuiManager;
import org.vebqa.vebtal.TestAdaptionResource;
import org.vebqa.vebtal.model.Command;
import org.vebqa.vebtal.model.CommandType;
import org.vebqa.vebtal.model.Response;
import org.vebqa.vebtal.sut.SutStatus;

import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.command.ICommandFactory;
import jp.vmi.selenium.selenese.config.DefaultConfig;
import jp.vmi.selenium.selenese.config.IConfig;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.webdriver.DriverOptions;
import jp.vmi.selenium.webdriver.DriverOptions.DriverOption;
import jp.vmi.selenium.webdriver.WebDriverManager;

public class SeleneseResource extends AbstractTestAdaptionResource implements TestAdaptionResource {

	private static final Logger logger = LoggerFactory.getLogger(SeleneseResource.class);

	private static SeleneseContext seleneseContext = new SeleneseContext();

	private static WebDriverManager manager = null;

	public Response execute(Command cmd) {

		// switch command type
		if (cmd.getCommand().startsWith("verify")) {
			SeleneseTestAdaptionPlugin.addCommandToList(cmd, CommandType.ASSERTION);
		} else if (cmd.getCommand().startsWith("assert")) {
			SeleneseTestAdaptionPlugin.addCommandToList(cmd, CommandType.ASSERTION);
		} else if (cmd.getCommand().startsWith("store")) {
			SeleneseTestAdaptionPlugin.addCommandToList(cmd, CommandType.ACCESSOR);
		} else if (cmd.getCommand().startsWith("take")) {
			SeleneseTestAdaptionPlugin.addCommandToList(cmd, CommandType.ACCESSOR);
		} else {
			SeleneseTestAdaptionPlugin.addCommandToList(cmd, CommandType.ACTION);
		}

		// Default Config laden
		// @ToDo: RemoteDriver durchschleifen
		IConfig config = new DefaultConfig("--alerts-policy", "accept");

		logger.debug("alert policy: " + config.getAlertsPolicy());
		DriverOptions driverOptions = new DriverOptions(config);

		String tSelectedProxy = SeleneseTestAdaptionPlugin.getSelectedProxy();

		switch (tSelectedProxy) {
		case "NoProxy":
			logger.info("Browser will use direct connection.");
			break;
		case "ZAP":
			String tZapHost = GuiManager.getinstance().getConfig().getString("zap.host");
			String tZapPort = GuiManager.getinstance().getConfig().getString("zap.port");
			driverOptions.set(DriverOption.PROXY, tZapHost + ":" + tZapPort);
			// ClientApi api = new ClientApi(tZapHost, Integer.parseInt(tZapPort), null,
			// false);
			logger.info("Proxy settings for ZAP inserted to driver options.");
			break;
		default:
			String tProxyHost = GuiManager.getinstance().getConfig().getString("browser.proxy.host");
			driverOptions.set(DriverOption.PROXY, tProxyHost + ":" + tSelectedProxy);
			logger.info("Proxy settings inserted to driver options.");
			break;
		}

		// Manager Instanziieren wenn noch nicht geschehen
		if (manager == null) {

			manager = WebDriverManager.newInstance();

			if ("chrome".equalsIgnoreCase(SeleneseTestAdaptionPlugin.getSelectedDriver())) {
				manager.setWebDriverFactory(WebDriverManager.CHROME);
				driverOptions.set(DriverOption.CHROME_EXPERIMENTAL_OPTIONS,
						GuiManager.getinstance().getConfig().getProperty("browser.options.json"));
			} else if ("firefox".equalsIgnoreCase(SeleneseTestAdaptionPlugin.getSelectedDriver())) {
				manager.setWebDriverFactory(WebDriverManager.FIREFOX);
			} else if ("iexplorer".equalsIgnoreCase(SeleneseTestAdaptionPlugin.getSelectedDriver())) {
				manager.setWebDriverFactory(WebDriverManager.IE);
			}
			manager.setDriverOptions(driverOptions);
		}

		// activate highlight
		boolean tSelectedHighlight = SeleneseTestAdaptionPlugin.getIsSelectedHighlight();
		if (tSelectedHighlight) {
			seleneseContext.setHighlight(true);
		} else {
			seleneseContext.setHighlight(false);
		}
		
		boolean tCreateNoScreenshots = SeleneseTestAdaptionPlugin.getIsNoScreenshot();
		if (tCreateNoScreenshots) {
		    seleneseContext.setScreenshotAllDir(null);
		    seleneseContext.setScreenshotOnFailDir(null);
		} 
		
		boolean tCreateScreenshotsAll = SeleneseTestAdaptionPlugin.getIsScreenshotAll();
		if (tCreateScreenshotsAll){
			seleneseContext.setScreenshotAllDir(GuiManager.getinstance().getConfig().getString("browser.screenshot.path"));
			seleneseContext.setScreenshotScrollTimeout(100);
		}
		
		boolean tCreateScreenshotOnFail = SeleneseTestAdaptionPlugin.getIsScreenshotOnFail();
		if (tCreateScreenshotOnFail) {
			seleneseContext.setScreenshotOnFailDir(GuiManager.getinstance().getConfig().getString("browser.screenshot.path"));
			seleneseContext.setScreenshotScrollTimeout(100);
		}
		
		if (cmd.getCommand().contains("open")) {
			// Disable Auswahl bis zum Restart oder schliessen des Browsers.
			SeleneseTestAdaptionPlugin.disableComboBox();
			GuiManager.getinstance().setTabStatus(SeleneseTestAdaptionPlugin.ID, SutStatus.CONNECTED);
		}

		// Manager uebergeben an den Context
		// wrap webdriver with eventfiring for taaking screenshots
		seleneseContext.setDriver(new EventFiringWebDriver(manager.get()));
		seleneseContext.setWebDriverPreparator(manager);

		// TODO: refactor to dynamic loading
		String factoryName = "org.vebqa.vebtal.selenese.command.AdditionalSeleneseExtensions";
		ICommandFactory factory;
		try {
			Class<?> factoryClass = Class.forName(factoryName);
			factory = (ICommandFactory) factoryClass.newInstance();
		} catch (Exception e) {
			logger.error("Error loading user defined command factory: {}", factoryName, e);
			throw new IllegalArgumentException("invalid user defined command factory: " + factoryName, e);
		}
		seleneseContext.getCommandFactory().registerCommandFactory(factory);
		logger.info("Registered command factory: " + factory);

		factoryName = "org.vebqa.vebtal.selenese.command.GalenSeleneseExtensions";
		try {
			Class<?> factoryClass = Class.forName(factoryName);
			factory = (ICommandFactory) factoryClass.newInstance();
		} catch (Exception e) {
			logger.error("Error loading user defined command factory: " + factoryName, e);
			throw new IllegalArgumentException("invalid user defined command factory: " + factoryName);
		}
		seleneseContext.getCommandFactory().registerCommandFactory(factory);
		logger.info("Registered command factory: {}", factory);

		factoryName = "org.vebqa.vebtal.selenese.command.ZAPSeleneseExtensions";
		try {
			Class<?> factoryClass = Class.forName(factoryName);
			factory = (ICommandFactory) factoryClass.newInstance();
		} catch (Exception e) {
			logger.error("Error loading user defined command factory: " + factoryName, e);
			throw new IllegalArgumentException("invalid user defined command factory: " + factoryName);
		}
		seleneseContext.getCommandFactory().registerCommandFactory(factory);
		logger.info("Registered command factory: {}", factory);

		Set<String> allCapsNames = manager.getDriverOptions().getCapabilities().getCapabilityNames();
		logger.info("Count defined capabilities: " + allCapsNames.size());
		for (String name : allCapsNames) {
			logger.info("Capability: " + name);
		}

		TestCase tCase = new TestCase();
		tCase.addCommand(seleneseContext.getCommandFactory(), cmd.getCommand(), cmd.getTarget(), cmd.getValue());
		seleneseContext.setCurrentTestCase(tCase);

		Result result = null;
		seleneseContext.maxTimeTimer.start();
		setStart();
		try {
			result = tCase.execute(null, seleneseContext);
		} catch (UnhandledAlertException e) {
			logger.error("I am struggling of unhandled alert exception!", e);
		}
		setFinished();
		seleneseContext.maxTimeTimer.stop();

		Response tResponse = new Response();
		tResponse.setCode(String.valueOf(result.getLevel().value));
		String tMessage = "";
		if (result.isSuccess()) {
			// Wenn Command ein Store Befehl war das Ziel zuruecksenden
			if (cmd.getCommand().startsWith("store")) {
				tResponse.setStoredKey(cmd.getValue());
				
				Object varValue = seleneseContext.getVarsMap().get(cmd.getValue());
				if (varValue instanceof Integer) {
					tResponse.setStoredValue(String.valueOf(varValue));
				} else {
					tResponse.setStoredValue((String) varValue);
				}
			}

			tResponse.setMessage("Successfully processed command: " + cmd.getCommand() + " with target: "
					+ cmd.getTarget() + " and value: " + cmd.getValue());
			tMessage = "Successfully processed command";
		} else if (result.isFailed()) {
			tResponse.setMessage("Command failed: " + result.getMessage());
			tMessage = result.getMessage();
		} else if (result.isAborted()) {
			tResponse.setMessage("Command aborted! ");
			tMessage = "Command aborted";
		}

		SeleneseTestAdaptionPlugin.setLatestResult(result.isSuccess(), tMessage);

		return tResponse;
	}

	public Response setupBrowser() {
		return new Response();
	}

	public static WebDriverManager getManager() {
		return SeleneseResource.manager;
	}

	public static void destroyManager() {
		SeleneseResource.manager = null;
	}
}
