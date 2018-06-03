package org.vebqa.vebtal.seleneserestserver;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vmi.selenium.rollup.RollupRules;
import jp.vmi.selenium.selenese.AlertActionListener;
import jp.vmi.selenium.selenese.CollectionMap;
import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.Eval;
import jp.vmi.selenium.selenese.ModifierKeyState;
import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.VarsMap;
import jp.vmi.selenium.selenese.command.CommandFactory;
import jp.vmi.selenium.selenese.command.CommandListIterator;
import jp.vmi.selenium.selenese.javascript.JSLibrary;
import jp.vmi.selenium.selenese.locator.WebDriverElementFinder;
import jp.vmi.selenium.selenese.log.CookieFilter;
import jp.vmi.selenium.selenese.log.LogFilter;
import jp.vmi.selenium.selenese.log.PageInformation;
import jp.vmi.selenium.selenese.subcommand.SubCommandMap;
import jp.vmi.selenium.webdriver.WebDriverPreparator;

public class SeleneseContext implements Context {

	private static final Logger log = LoggerFactory.getLogger(SeleneseContext.class);
	
    private String initialWindowHandle = null;
	private VarsMap varsMap = new VarsMap();
	private final Deque<CommandListIterator> commandListIteratorStack = new ArrayDeque<>();
	private TestCase currentTestCase = null;
	private PageInformation latestPageInformation = PageInformation.EMPTY;
	private final ModifierKeyState modifierKeyState = new ModifierKeyState();
	private final CollectionMap collectionMap = new CollectionMap();
	private final WebDriverElementFinder elementFinder;
	private WebDriver driver = null;
	private WebDriverPreparator preparator = null;
	private CookieFilter cookieFilter = CookieFilter.ALL_PASS;
	private JSLibrary jsLibrary = new JSLibrary();
	private final SubCommandMap subCommandMap;
	private final Eval eval;
	private final CommandFactory commandFactory;
	
	private int timeout = 30 * 1000; /* ms */
	
	public SeleneseContext() {
		this.elementFinder = new WebDriverElementFinder();
        this.subCommandMap = new SubCommandMap();
        this.eval = new Eval();
        this.varsMap = new VarsMap();
        this.commandFactory = new CommandFactory(this);
    }
	
	public Deque<CommandListIterator> getCommandListIteratorStack() {
		return commandListIteratorStack;
	}

	public WebDriver getWrappedDriver() {
		return driver;
	}

    /**
     * Set WebDriver.
     *
     * @param driver WebDriver.
     */
    public void setDriver(WebDriver driver) {
        this.driver = driver;
        this.initialWindowHandle = driver.getWindowHandle();
        setDriverTimeout();
    }

    private void setDriverTimeout() {
        driver.manage().timeouts().pageLoadTimeout(timeout, TimeUnit.MILLISECONDS);
    }
    
	public void prepareWebDriver() {
		log.info("prepare");
		
	}

	public TestCase getCurrentTestCase() {
		return this.currentTestCase;
	}

	public void setCurrentTestCase(TestCase testCase) {
		this.currentTestCase = testCase;
	}

	public PrintStream getPrintStream() {
		return null;
	}

	public String getOverridingBaseURL() {
		return null;
	}

	public String getCurrentBaseURL() {
		return "http://www.google.de";
	}

	public CommandFactory getCommandFactory() {
		return commandFactory;
	}

	public CommandListIterator getCommandListIterator() {
		return commandListIteratorStack.peekFirst();
	}

	public void pushCommandListIterator(CommandListIterator commandListIterator) {
		commandListIteratorStack.push(commandListIterator);
	}

	public void popCommandListIterator() {
		 commandListIteratorStack.pop();
	}

	public VarsMap getVarsMap() {
		return this.varsMap;
	}

	public RollupRules getRollupRules() {
		return null;
	}

	public CollectionMap getCollectionMap() {
		return null;
	}

	public String getInitialWindowHandle() {
		return null;
	}

	public WebDriverElementFinder getElementFinder() {
		return this.elementFinder;
	}

	public Eval getEval() {
		return this.eval;
	}

	public boolean isTrue(String expr) {
		return false;
	}

	public SubCommandMap getSubCommandMap() {
		return subCommandMap;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
        this.timeout = timeout;
        if (driver != null)
            setDriverTimeout();
        log.info("Timeout: {} ms", timeout);
	}

	public void resetSpeed() {
	}

	public long getSpeed() {
		return 0;
	}

	public void setSpeed(long speed) {
	}

	public void waitSpeed() {
	}

	public PageInformation getLatestPageInformation() {
		 return latestPageInformation;
	}

	public void setLatestPageInformation(PageInformation pageInformation) {
		 this.latestPageInformation = pageInformation;
	}

	public CookieFilter getCookieFilter() {
		return this.cookieFilter;
	}

	public void setCookieFilter(CookieFilter cookieFilter) {
		this.cookieFilter = cookieFilter;
	}

	public JSLibrary getJSLibrary() {
		return this.jsLibrary;
	}

	public void setJSLibrary(JSLibrary jsLibrary) {
		this.jsLibrary = jsLibrary;
	}

	public ModifierKeyState getModifierKeyState() {
		return this.modifierKeyState;
	}

	public void resetState() {
        collectionMap.clear();
        modifierKeyState.reset();
	}

	public boolean isInteractive() {
		return false;
	}

    /**
     * Set WebDriverPreparator.
     *
     * @param preparator WebDriverPreparator.
     */
    public void setWebDriverPreparator(WebDriverPreparator preparator) {
        this.preparator = preparator;
    }

    /**
     * Return empty set of log filter.
     * 
     * @return	Enum of logfilter
     */
	@Override
	public EnumSet<LogFilter> getLogFilter() {
		return EnumSet.noneOf(LogFilter.class);
	}

	@Override
	public AlertActionListener getNextNativeAlertActionListener() {
		// TODO Auto-generated method stub
		return null;
	}
}
