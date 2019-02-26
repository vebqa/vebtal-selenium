package org.vebqa.vebtal.seleneserestserver;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vmi.selenium.rollup.RollupRules;
import jp.vmi.selenium.selenese.AlertActionListener;
import jp.vmi.selenium.selenese.CollectionMap;
import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.Eval;
import jp.vmi.selenium.selenese.FlowControlState;
import jp.vmi.selenium.selenese.MaxTimeTimer;
import jp.vmi.selenium.selenese.ModifierKeyState;
import jp.vmi.selenium.selenese.SubCommandMapProvider;
import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.VarsMap;
import jp.vmi.selenium.selenese.command.CommandFactory;
import jp.vmi.selenium.selenese.command.CommandListIterator;
import jp.vmi.selenium.selenese.command.ICommand;
import jp.vmi.selenium.selenese.highlight.HighlightHandler;
import jp.vmi.selenium.selenese.highlight.HighlightStyle;
import jp.vmi.selenium.selenese.highlight.HighlightStyleBackup;
import jp.vmi.selenium.selenese.javascript.JSLibrary;
import jp.vmi.selenium.selenese.locator.Locator;
import jp.vmi.selenium.selenese.locator.WebDriverElementFinder;
import jp.vmi.selenium.selenese.log.CookieFilter;
import jp.vmi.selenium.selenese.log.LogFilter;
import jp.vmi.selenium.selenese.log.PageInformation;
import jp.vmi.selenium.selenese.subcommand.SubCommandMap;
import jp.vmi.selenium.selenese.utils.MouseUtils;
import jp.vmi.selenium.webdriver.WebDriverPreparator;

public class SeleneseContext implements Context, HighlightHandler {

	private static final Logger log = LoggerFactory.getLogger(SeleneseContext.class);

	private WebDriver driver = null;
	private WebDriverPreparator preparator = null;
    private String overridingBaseURL = null;
    private String initialWindowHandle = null;

    // private String screenshotDir = null;
    // private String screenshotAllDir = null;
    // private String screenshotOnFailDir = null;
    // private boolean isIgnoredScreenshotCommand = false;
    private boolean isHighlight = false;
    private boolean isInteractive = false;
    private Boolean isW3cAction = null;    
	private int timeout = 60 * 1000; /* ms */
    private int retries = 0;
    private int maxRetries = 0;
    private long initialSpeed = 0; /* ms */
    private long speed = 0; /* ms */
    // private int screenshotScrollTimeout = 100; /* ms */	
	
    private final Eval eval;
    private final SubCommandMap subCommandMap;
    private final WebDriverElementFinder elementFinder;
    private final CommandFactory commandFactory;
    private TestCase currentTestCase = null;
    private final Deque<CommandListIterator> commandListIteratorStack = new ArrayDeque<>();
	private VarsMap varsMap = new VarsMap();
	private final Map<ICommand, FlowControlState> flowControlMap = new IdentityHashMap<>();
	private final CollectionMap collectionMap = new CollectionMap();
    private RollupRules rollupRules = null; // lazy initialization
    private final Deque<HighlightStyleBackup> styleBackups;
    private final EnumSet<LogFilter> logFilter = LogFilter.all();
    private CookieFilter cookieFilter = CookieFilter.ALL_PASS;

    private JSLibrary jsLibrary = new JSLibrary();
    
	private PageInformation latestPageInformation = PageInformation.EMPTY;
	private final ModifierKeyState modifierKeyState = new ModifierKeyState(this);
	
    // private final JUnitResult jUnitResult = new JUnitResult();
    // private final HtmlResult htmlResult = new HtmlResult();

    private MaxTimeTimer maxTimeTimer = new MaxTimeTimer() {
    };	
	
    private final AlertActionListener alertActionListener = new AlertActionListener() {

        private boolean accept = true;
        private String answer = null;

        @Override
        public void setAccept(boolean accept) {
            this.accept = accept;
        }

        @Override
        public void setAnswer(String answer) {
            this.answer = answer;
        }

        @Override
        public void actionPerformed(Alert alert) {
            if (answer != null)
                alert.sendKeys(answer);
            if (accept)
                alert.accept();
            else
                alert.dismiss();
            // reset the behavior
            this.answer = null;
            this.accept = true;
        }
    };

    
	public SeleneseContext() {
		this.elementFinder = new WebDriverElementFinder();
        this.subCommandMap = new SubCommandMap();
        this.eval = new Eval();
        this.varsMap = new VarsMap();
        // this.commandFactory = new CommandFactory(this);
        this.commandFactory = new CommandFactory((SubCommandMapProvider) this);
        this.styleBackups = new ArrayDeque<>();
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
        if (preparator == null)
            return;
        setDriver(preparator.get());		
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

    public void setOverridingBaseURL(String overridingBaseURL) {
        this.overridingBaseURL = overridingBaseURL;
        log.info("Override base URL: {}", overridingBaseURL);
    }
    
	public String getOverridingBaseURL() {
		return overridingBaseURL;
	}

	public String getCurrentBaseURL() {
		return StringUtils.defaultIfBlank(overridingBaseURL, currentTestCase.getBaseURL());
	}

    @Override
    public boolean isHighlight() {
        return isHighlight;
    }

    /**
     * Set locator highlighting.
     *
     * @param isHighlight true if use locator highlighting.
     */
    public void setHighlight(boolean isHighlight) {
        this.isHighlight = isHighlight;
        log.info("Highlight mode: {}", isHighlight ? "enabled" : "disabled");
    }

    @Override
    public boolean isInteractive() {
        return isInteractive;
    }

    @Override
    public boolean isW3cAction() {
        return isW3cAction != null ? isW3cAction : MouseUtils.isW3cAction(getBrowserName());
    }

    /**
     * Set W3C action compatibility.
     *
     * @param isW3cAction true if Action command is W3C compatible.
     */
    public void setW3cAction(Boolean isW3cAction) {
        this.isW3cAction = isW3cAction;
    }
	
    class AlertActionImpl implements AlertActionListener {
        boolean accept = true;
        String answer = null;

        @Override
        public void setAccept(boolean accept) {
            this.accept = accept;
        }

        @Override
        public void setAnswer(String answer) {
            this.answer = answer;
        }

        @Override
        public void actionPerformed(Alert alert) {
            if (answer != null) {
                alert.sendKeys(answer);
            }
            if (accept) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            // reset the behavior
            this.answer = null;
            this.accept = true;
        }
    }

    @Override
    public AlertActionListener getNextNativeAlertActionListener() {
        return this.alertActionListener;
    }

    /**
     * Set interactive.
     *
     * @param isInteractive true if Runner executes test step-by-step upon user key stroke.
     */
    public void setInteractive(boolean isInteractive) {
        this.isInteractive = isInteractive;
        log.info("Interactive mode: {}", isInteractive ? "enabled" : "disabled");
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

    public void setVarsMap(VarsMap varsMap) {
        this.varsMap = varsMap;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends FlowControlState> T getFlowControlState(ICommand command) {
        return (T) flowControlMap.get(command);
    }

    @Override
    public <T extends FlowControlState> void setFlowControlState(ICommand command, T state) {
        flowControlMap.put(command, state);
    }    
    
	public RollupRules getRollupRules() {
        if (rollupRules == null)
            rollupRules = new RollupRules();
        return rollupRules;
	}

	public CollectionMap getCollectionMap() {
		return collectionMap;
	}

	public String getInitialWindowHandle() {
		return initialWindowHandle;
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

    @Override
    public void resetRetries() {
        retries = 1;
    }

    @Override
    public void incrementRetries() {
        retries++;
    }

    @Override
    public boolean hasReachedMaxRetries() {
        return retries >= maxRetries;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        log.info("Max retries: {}", maxRetries);
    }
	
    public long getInitialSpeed() {
        return initialSpeed;
    }

    /**
     * Set initial speed at starting test-suite. (ms)
     *
     * @param initialSpeed initial speed.
     */
    public void setInitialSpeed(long initialSpeed) {
        this.initialSpeed = initialSpeed;
    }

    @Override
    public void resetSpeed() {
        speed = initialSpeed;
        log.info("Current speed: {} ms/command", speed);
    }

    @Override
    public long getSpeed() {
        return speed;
    }

    @Override
    public void setSpeed(long speed) {
        this.speed = speed;
        log.info("Current speed: {} ms/command", speed);
    }

    @Override
    public void waitSpeed() {
        if (speed > 0) {
            try {
                Thread.sleep(speed);
            } catch (InterruptedException e) {
                // ignore it.
            }
        }
    }
	public PageInformation getLatestPageInformation() {
		 return latestPageInformation;
	}

	public void setLatestPageInformation(PageInformation pageInformation) {
		 this.latestPageInformation = pageInformation;
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
		// return EnumSet.noneOf(LogFilter.class);
		return this.logFilter;
	}
	
    @Override
    public CookieFilter getCookieFilter() {
        return cookieFilter;
    }

    @Override
    public void setCookieFilter(CookieFilter cookieFilter) {
        this.cookieFilter = cookieFilter;
    }
    
    @Override
    public void highlight(Locator ploc, HighlightStyle highlightStyle) {
        List<Locator> selectedFrameLocators = elementFinder.getCurrentFrameLocators();
        Map<String, String> prevStyles = highlightStyle.doHighlight(driver, elementFinder, ploc, selectedFrameLocators);
        if (prevStyles == null)
            return;
        HighlightStyleBackup backup = new HighlightStyleBackup(prevStyles, ploc, selectedFrameLocators);
        styleBackups.push(backup);
    }

    @Override
    public void unhighlight() {
        while (!styleBackups.isEmpty()) {
            HighlightStyleBackup backup = styleBackups.pop();
            backup.restore(driver, elementFinder);
        }
    }
    
}
