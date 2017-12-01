package org.vebqa.vebtal.selenese.junit;

import static jp.vmi.selenium.selenese.config.DefaultConfig.DEFAULT_TIMEOUT_MILLISEC_N;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jp.vmi.selenium.selenese.Runner;
import jp.vmi.selenium.selenese.command.ICommandFactory;
import jp.vmi.selenium.selenese.config.DefaultConfig;
import jp.vmi.selenium.selenese.config.IConfig;
import jp.vmi.selenium.selenese.log.CookieFilter;
import jp.vmi.selenium.selenese.log.CookieFilter.FilterType;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.utils.LoggerUtils;
import jp.vmi.selenium.webdriver.DriverOptions;
import jp.vmi.selenium.webdriver.DriverOptions.DriverOption;
import jp.vmi.selenium.webdriver.WebDriverManager;

/**
 * Provide command line interface.
 */
public class CustomSeleneseStarter {

    private static final Logger logger = LoggerFactory.getLogger(CustomSeleneseStarter.class);

    private static final String PROG_TITLE = "Selenese Runner";

    private boolean noExit = false;
    private boolean exitStrictly = false;
    private Integer exitCode = null;
    
    WebDriverManager manager;

    /**
     * Get version of Selenese Runner.
     * <p>
     * This information is provided by maven generated property file.
     * </p>
     * @return version string.
     */
    public String getVersion() {
        try (InputStream is = getClass().getResourceAsStream("/META-INF/maven/jp.vmi/selenese-runner-java/pom.properties")) {
            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                return "DEV package based on: " + prop.getProperty("version");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "(missing version information)";
    }

    private void help(String... msgs) {
        String progName = System.getenv("PROG_NAME");
        if (StringUtils.isBlank(progName))
            progName = "java -jar selenese-runner.jar";
        new DefaultConfig().showHelp(new PrintWriter(System.out), PROG_TITLE, getVersion(), progName, msgs);
        exit(1);
    }

    /**
     * Start Selenese Runner.
     *
     * @param args command line arguments.
     */
    public void run(String[] args) {
    	// HTML Ausgabeverzeichnis
        Calendar tCal = Calendar.getInstance();
        Date tNow = tCal.getTime();        
        String resultDir = System.getProperty("test.result") + "/" + tNow.getTime();
    	
        int exitCode = 1;
        Result totalResult = null;
        String aTestSpec = null;
        try {
            IConfig config = new DefaultConfig(args);
            String[] filenames = config.getArgs();
            if (filenames.length == 0)
                help();
            // wir beruecksichtigen nur das erste!
            aTestSpec = new File(filenames[0]).getName();
            
            logger.info("Start: {} {}", PROG_TITLE, getVersion());
            Runner runner = new Runner();
            // runner.setCommandLineArgs(args);
            runner.setHtmlResultDir(resultDir);
            runner.setJUnitResultDir(resultDir);
            
            setupRunner(runner, config, filenames);
            totalResult = runner.run(filenames);
            runner.finish();
            if (exitStrictly) {
                exitCode = totalResult.getLevel().strictExitCode;
            } else {
                exitCode = totalResult.getLevel().exitCode;
            }
        } catch (IllegalArgumentException e) {
            help("Error: " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        
        // Exit schreiben

        String exportFile = System.getProperty("test.export");
        logger.info("Try to save export to: {}", exportFile);
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File varFile = new File(exportFile);
		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("exchange");
			doc.appendChild(rootElement);
			
			// Documentation path -> "documentation"
			Element element = doc.createElement("documentation");
			if (resultDir != null) {
				element.appendChild(doc.createTextNode(resultDir + "\\TEST-" + aTestSpec));
			} else {
				element.appendChild(doc.createTextNode("Error retrieving documentation path!"));
			}
			rootElement.appendChild(element);
			
			Element eleResult = doc.createElement("result");
			if (totalResult.isSuccess()) {
				eleResult.appendChild(doc.createTextNode("success"));
			} else if (totalResult.isAborted()) {
				eleResult.appendChild(doc.createTextNode("aborted"));
			} else if (totalResult.isFailed()) {
				eleResult.appendChild(doc.createTextNode("failed"));
			}
			rootElement.appendChild(eleResult);
			
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult sResult = new StreamResult(varFile);

			transformer.transform(source, sResult);

			logger.info("File saved to: {}", exportFile);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}		
		
        exit(exitCode);
    }

    /**
     * Setup Runner by configuration.
     *
     * @param runner Runner object.
     * @param config configuration.
     * @param filenames filenames of test-suites/test-cases.
     */
    public void setupRunner(Runner runner, IConfig config, String... filenames) {
        if (config.getMaxTime() != null) {
            long maxTime = NumberUtils.toLong(config.getMaxTime(), 0);
            if (maxTime <= 0)
                throw new IllegalArgumentException("Invalid max time value. (" + config.getMaxTime() + ")");
            // runner.setupMaxTimeTimer(maxTime * 1000);
        }
        String driverName = config.getDriver();
        DriverOptions driverOptions = new DriverOptions(config);
        if (driverName == null) {
            if (driverOptions.has(DriverOption.FIREFOX))
                driverName = WebDriverManager.FIREFOX;
            else if (driverOptions.has(DriverOption.CHROMEDRIVER))
                driverName = WebDriverManager.CHROME;
            else if (driverOptions.has(DriverOption.IEDRIVER))
                driverName = WebDriverManager.IE;
            else if (driverOptions.has(DriverOption.PHANTOMJS))
                driverName = WebDriverManager.PHANTOMJS;
        }
        manager = WebDriverManager.newInstance();
        manager.setWebDriverFactory(driverName);
        manager.setDriverOptions(driverOptions);
        if (config.getCommandFactory() != null) {
            String factoryName = config.getCommandFactory();
            ICommandFactory factory;
            try {
                Class<?> factoryClass = Class.forName(factoryName);
                factory = (ICommandFactory) factoryClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException("invalid user defined command factory: " + factoryName);
            }
            runner.getCommandFactory().registerCommandFactory(factory);
            logger.info("Registered: {}", factoryName);
        }
        runner.setDriver(manager.get());
        runner.setWebDriverPreparator(manager);
        if (config.isHighlight())
            runner.setHighlight(true);
        if (config.isInteractive())
            runner.setInteractive(true);
        if (config.getScreenshotDir() != null)
            runner.setScreenshotDir(config.getScreenshotDir());
        if (config.getScreenshotAll() != null)
            runner.setScreenshotAllDir(config.getScreenshotAll());
        if (config.getScreenshotOnFail() != null)
            runner.setScreenshotOnFailDir(config.getScreenshotOnFail());
        if (config.getBaseurl() != null)
            runner.setOverridingBaseURL(config.getBaseurl());
        if (config.isIgnoreScreenshotCommand())
            runner.setIgnoredScreenshotCommand(true);
        if (config.getRollup() != null) {
            String[] rollups = config.getRollup();
            for (String rollup : rollups)
                runner.getRollupRules().load(rollup);
        }
        if (config.getCookieFilter() != null) {
            String cookieFilter = config.getCookieFilter();
            if (cookieFilter.length() < 2)
                throw new IllegalArgumentException("invalid cookie filter format: " + cookieFilter);
            FilterType filterType;
            switch (cookieFilter.charAt(0)) {
            case '+':
                filterType = FilterType.PASS;
                break;
            case '-':
                filterType = FilterType.SKIP;
                break;
            default:
                throw new IllegalArgumentException("invalid cookie filter format: " + cookieFilter);
            }
            String pattern = cookieFilter.substring(1);
            runner.setCookieFilter(new CookieFilter(filterType, pattern));
        }
        if (config.getXmlResult() != null)
            runner.setJUnitResultDir(config.getXmlResult());
        if (config.getHtmlResult() != null)
            runner.setHtmlResultDir(config.getHtmlResult());
        int timeout = NumberUtils.toInt(config.getTimeout(), DEFAULT_TIMEOUT_MILLISEC_N);
        if (timeout <= 0)
            throw new IllegalArgumentException("Invalid timeout value. (" + config.getTimeout() + ")");
        runner.setTimeout(timeout);
        int speed = NumberUtils.toInt(config.getSetSpeed(), 0);
        if (speed < 0)
            throw new IllegalArgumentException("Invalid speed value. (" + config.getSetSpeed() + ")");
        runner.setInitialSpeed(speed);
        if (config.isNoExit())
            noExit = true;
        if (config.isStrictExitCode())
            exitStrictly = true;
        runner.setPrintStream(System.out);
    }

    protected void exit(int exitCode) {
        this.exitCode = exitCode;
        logger.info("Exit code: {}", exitCode);
        
        WebDriverManager.quitDriversOnAllManagers();
        if (!noExit)
            System.exit(exitCode);
    }

    /**
     * Get exit code.
     *
     * @return exit code, or null if don't end.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Selenese Runner main.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        LoggerUtils.initLogger();
        try {
        	new CustomSeleneseStarter().run(args);
        } catch (Exception e) {
        	logger.info("Error while starting selenese runner!", e);
        }
        try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			logger.error("This gets interrupted...", e);
		}
    }
}