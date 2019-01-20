package org.vebqa.vebtal.seleneserestserver;

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
		IConfig config = new DefaultConfig();
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
			// ClientApi api = new ClientApi(tZapHost, Integer.parseInt(tZapPort), null, false);
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
			} else if ("firefox".equalsIgnoreCase(SeleneseTestAdaptionPlugin.getSelectedDriver())) {
				manager.setWebDriverFactory(WebDriverManager.FIREFOX);
			} else if ("iexplorer".equalsIgnoreCase(SeleneseTestAdaptionPlugin.getSelectedDriver())) {
				manager.setWebDriverFactory(WebDriverManager.IE);
			}
			manager.setDriverOptions(driverOptions);
		}

		if (cmd.getCommand().contains("open")) {
			// Disable Auswahl bis zum Restart oder schliessen des Browsers.
			SeleneseTestAdaptionPlugin.disableComboBox();
			GuiManager.getinstance().setTabStatus(SeleneseTestAdaptionPlugin.ID, SutStatus.CONNECTED);
		}

		// Manager uebergeben an den Context
		seleneseContext.setDriver(manager.get());
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
		
		TestCase tCase = new TestCase();
		tCase.addCommand(seleneseContext.getCommandFactory(), cmd.getCommand(), cmd.getTarget(), cmd.getValue());
		seleneseContext.setCurrentTestCase(tCase);

		setStart();
		Result result = tCase.execute(null, seleneseContext);
		setFinished();

		Response tResponse = new Response();
		tResponse.setCode(String.valueOf(result.getLevel().value));
		String tMessage = "";
		if (result.isSuccess()) {
			// Wenn Command ein Store Befehl war das Ziel an Tosca senden
			if (cmd.getCommand().startsWith("store")) {
				tResponse.setStoredKey(cmd.getValue());
				tResponse.setStoredValue((String) seleneseContext.getVarsMap().get(cmd.getValue()));
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
