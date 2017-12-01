package org.vebqa.vebtal.seleneserestserver;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.model.Command;
import org.vebqa.vebtal.model.Response;

import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.command.ICommandFactory;
import jp.vmi.selenium.selenese.config.DefaultConfig;
import jp.vmi.selenium.selenese.config.IConfig;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.webdriver.DriverOptions;
import jp.vmi.selenium.webdriver.WebDriverManager;


@Path("selenese")
public class SeleneseResource {

	private static final Logger logger = LoggerFactory.getLogger(SeleneseResource.class);
	
	private static SeleneseContext seleneseContext = new SeleneseContext();
	
	private static WebDriverManager manager = null;
	
	@POST
	@Path("execute")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response executeSelenese(Command cmd) {
		SeleneseTestAdaptionPlugin.addCommandToList(cmd);
		
		// Default Config laden
		// @ToDo: RemoteDriver durchschleifen
		IConfig config = new DefaultConfig();
		DriverOptions driverOptions = new DriverOptions(config);
		
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
			
			// Disable Auswahl bis zum Restart oder schliessen des Browsers.
			SeleneseTestAdaptionPlugin.disableComboBox();
			
			manager.setDriverOptions(driverOptions);
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
            logger.error("Error loading user defined command factory: " + factoryName, e);
            throw new IllegalArgumentException("invalid user defined command factory: " + factoryName);
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
        
		TestCase tCase = new TestCase();
		tCase.addCommand(seleneseContext.getCommandFactory(), cmd.getCommand() , cmd.getTarget(), cmd.getValue());
		seleneseContext.setCurrentTestCase(tCase);
		Result result = tCase.execute(null, seleneseContext);
		
		Response tResponse = new Response();
		tResponse.setCode(String.valueOf(result.getLevel().value));
		String tMessage = "";
		if (result.isSuccess()) {
			// Wenn Command ein Store Befehl war das Ziel an Tosca senden
			if (cmd.getCommand().startsWith("store")) {
				tResponse.setStoredKey(cmd.getValue());
				tResponse.setStoredValue((String)seleneseContext.getVarsMap().get(cmd.getValue()));
			}

			tResponse.setMessage("Successfully processed command: " + cmd.getCommand() + " with target: " + cmd.getTarget() + " and value: " + cmd.getValue());
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

	@POST
	@Path("close")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response tearDownBrowser() {
		Response tResponse = new Response();
		
		try {
			seleneseContext.getWrappedDriver().close();
			manager.quitDriver();
			manager = null;
		} catch (Exception e) {
			tResponse.setCode("1");
			tResponse.setMessage("Something went wrong while closing the webdriver! " + e.getMessage());
			return tResponse;
		}
		
		tResponse.setCode("0");
		tResponse.setMessage("Webdriver closed successfully!");
		
		// Browser Auswahlbox kann wieder aktiviert werden.
		SeleneseTestAdaptionPlugin.enableCombobox();
	
		return tResponse;
	}
}
