package org.vebqa.vebtal.seleneserestserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.utils.web.ScrollStrategy;

import jp.vmi.selenium.rollup.RollupRules;
import jp.vmi.selenium.selenese.AlertActionListener;
import jp.vmi.selenium.selenese.CollectionMap;
import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.Eval;
import jp.vmi.selenium.selenese.FlowControlState;
import jp.vmi.selenium.selenese.MaxTimeTimer;
import jp.vmi.selenium.selenese.ModifierKeyState;
import jp.vmi.selenium.selenese.ScreenshotHandler;
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
import jp.vmi.selenium.selenese.utils.PathUtils;
import jp.vmi.selenium.webdriver.WebDriverPreparator;

public class SeleneseContext implements Context, HighlightHandler, ScreenshotHandler {

	private static final Logger log = LoggerFactory.getLogger(SeleneseContext.class);

	private static final DateTimeFormatter FILE_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

	private static PrintStream DEFAULT_PRINT_STREAM = new PrintStream(new NullOutputStream());

	private WebDriver driver = null;
	private PrintStream ps;
	private WebDriverPreparator preparator = null;
	private String overridingBaseURL = null;
	private String initialWindowHandle = null;

	private String screenshotDir = null;
	private String screenshotAllDir = null;
	private String screenshotOnFailDir = null;
	private boolean isIgnoredScreenshotCommand = false;

	private boolean isHighlight = false;
	private boolean isInteractive = false;
	private Boolean isW3cAction = null;
	private int timeout = 60 * 1000; /* ms */
	private int retries = 0;
	private int maxRetries = 0;
	private long initialSpeed = 0; /* ms */
	private long speed = 0; /* ms */

	private int screenshotScrollTimeout = 100; /* ms */

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

	public MaxTimeTimer maxTimeTimer = new MaxTimeTimer() {
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
		this.ps = DEFAULT_PRINT_STREAM;
		this.elementFinder = new WebDriverElementFinder();
		this.subCommandMap = new SubCommandMap();
		this.eval = new Eval();
		this.varsMap = new VarsMap();
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
		if (this.isW3cAction == null) {
			this.isW3cAction = MouseUtils.isW3cAction(driver);
		}
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

	public void setPrintStream(PrintStream ps) {
		this.ps = ps;
	}

	public PrintStream getPrintStream() {
		return this.ps;
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

	private static void mkdirsForScreenshot(String dirStr, String msg) {
		if (dirStr == null)
			return;
		File dir = new File(dirStr);
		if (dir.exists()) {
			if (dir.isDirectory())
				return;
			else
				throw new IllegalArgumentException(dirStr + " is not directory.");
		}
		dir.mkdirs();
		log.info("Make the directory for {}: {}", msg, dirStr);
	}

	/**
	 * Set directory for storing screenshots.
	 *
	 * @param screenshotDir directory.
	 * @exception IllegalArgumentException throws if screenshotDir is not directory.
	 */
	public void setScreenshotDir(String screenshotDir) throws IllegalArgumentException {
		mkdirsForScreenshot(screenshotDir, "screenshot");
		this.screenshotDir = screenshotDir;
		log.info("Screenshot directory: {}", Objects.toString(screenshotDir, "-"));
	}

	/**
	 * Set directory for storing screenshots at all commands.
	 *
	 * @param screenshotAllDir directory.
	 * @exception IllegalArgumentException throws if screenshotAllDir is not
	 *                                     directory.
	 */
	public void setScreenshotAllDir(String screenshotAllDir) throws IllegalArgumentException {
		mkdirsForScreenshot(screenshotAllDir, "screenshot-all");
		this.screenshotAllDir = screenshotAllDir;
		log.info("Screenshot for all commands directory: {}", Objects.toString(screenshotAllDir, "-"));
	}

	/**
	 * Set directory for storing screenshot on fail.
	 *
	 * @param screenshotOnFailDir directory.
	 */
	public void setScreenshotOnFailDir(String screenshotOnFailDir) {
		mkdirsForScreenshot(screenshotOnFailDir, "screenshot-on-fail");
		this.screenshotOnFailDir = screenshotOnFailDir;
		log.info("Screenshot on fail directory: {}", Objects.toString(screenshotOnFailDir, "-"));
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
		return isW3cAction != null ? isW3cAction : false;
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
	 * @param isInteractive true if Runner executes test step-by-step upon user key
	 *                      stroke.
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
	 * @return Enum of logfilter
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

	public void setScreenshotScrollTimeout(int timeout) {
		this.screenshotScrollTimeout = timeout;
	}

	/**
	 * Get TakesScreenshot instance.
	 *
	 * @return TakesScreenshot instance.
	 */
	protected TakesScreenshot getTakesScreenshot() {
		if (driver instanceof TakesScreenshot) {
			log.info("Take screenshot from: {}.", driver.getClass());
			return (TakesScreenshot) driver;
		} else {
			log.info("Driver does not support screenshot.");
			return null;
		}
	}

	/**
	 * Take screenshot.
	 *
	 * @param file       file instance.
	 * @param entirePage true if take screenshot of entire page.
	 * @return path of screenshot or null.
	 * @throws UnsupportedOperationException throw this exception if
	 *                                       {@code getTakesScreenshot()} returns
	 *                                       null.
	 * @throws WebDriverException            throw this exception if it fails to get
	 *                                       the screenshot.
	 */
	protected String takeScreenshot(File file, boolean entirePage)
			throws UnsupportedOperationException, WebDriverException {
		TakesScreenshot tss = getTakesScreenshot();
		if (tss == null)
			throw new UnsupportedOperationException("webdriver does not support capturing screenshot.");
		file = file.getAbsoluteFile();
		try {
			driver.switchTo().defaultContent();
		} catch (Exception e) {
			log.error("Switch to raised an error: {}", e);
			// some times switching to default context throws exceptions like:
			// Method threw 'org.openqa.selenium.UnhandledAlertException' exception.
		}
		File tmp;
		try {
			File dir = file.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
				log.info("Make the directory for screenshot: {}", dir);
			}
			if (entirePage) {
				tmp = File.createTempFile("sstmp-", ".png", dir);

				JavascriptExecutor je = (JavascriptExecutor) tss;
				String getScrollCoord = "return { top: window.scrollY||0, left: window.scrollX };";

				Map<?, ?> initialCoord = (Map<?, ?>) je.executeScript(getScrollCoord);

				Shutterbug.shootPage((WebDriver) tss, ScrollStrategy.WHOLE_PAGE, screenshotScrollTimeout)
						.withName(FilenameUtils.removeExtension(tmp.getName())).save(dir.getPath());

				if (!initialCoord.equals(je.executeScript(getScrollCoord))) {
					je.executeScript("scrollTo(arguments[0]); return false;", initialCoord);
				}
			} else {
				tmp = tss.getScreenshotAs(OutputType.FILE);
			}
			FileUtils.moveFile(tmp, file);
		} catch (IOException e) {
			throw new RuntimeException("failed to rename captured screenshot image: " + file, e);
		}
		String path = file.getPath();
		log.info("- captured screenshot: {}", path);
//        if (currentTestCase.getLogRecorder() != null) {
//        	currentTestCase.getLogRecorder().info("[[ATTACHMENT|" + path + "]]");
//        } else {
//        	log.info("LogRecorder is null!");
//        }
		return path;
	}

	/**
	 * Normalize filename for screenshot.
	 *
	 * @param filename filename for screenshot.
	 * @return normalized file instance.
	 */
	protected File normalizeScreenshotFilename(String filename) {
		File file = new File(PathUtils.normalize(filename));
		if (screenshotDir != null)
			return new File(screenshotDir, file.getName());
		else
			return file;
	}

	/**
	 * Get filename for screenshot all.
	 *
	 * @param prefix filename prefix.
	 * @param index  filename index.
	 * @return File instance for screenshot all.
	 */
	protected File getFilenameForScreenshotAll(String prefix, int index) {
		String filename = String.format("%s_%s_%d.png", prefix, FILE_DATE_TIME.format(ZonedDateTime.now()), index);
		return new File(screenshotAllDir, filename);
	}

	/**
	 * Get filename for screenshot on fail.
	 *
	 * @param prefix filename prefix.
	 * @param index  filename index.
	 * @return File instance for screenshot on fail.
	 */
	protected File getFilenameForScreenshotOnFail(String prefix, int index) {
		String filename = String.format("%s_%s_%d_fail.png", prefix, FILE_DATE_TIME.format(ZonedDateTime.now()), index);
		return new File(screenshotOnFailDir, filename);
	}

	@Override
	public String takeEntirePageScreenshot(String filename) throws WebDriverException, UnsupportedOperationException {
		return takeScreenshot(normalizeScreenshotFilename(filename), true);
	}

	@Override
	public String takeScreenshot(String filename) throws WebDriverException, UnsupportedOperationException {
		return takeScreenshot(normalizeScreenshotFilename(filename), false);
	}

	@Override
	public String takeScreenshotAll(String prefix, int index) {
		log.info("Take screenshot for all commands.");
		if (screenshotAllDir == null) {
			log.info("No screenshot because its deactivated.");
			return null;
		}
		try {
			return takeScreenshot(getFilenameForScreenshotAll(prefix, index), false);
		} catch (UnsupportedOperationException e) {
			return null;
		} catch (WebDriverException e) {
			log.warn("- failed to capture screenshot: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			return null;
		}
	}

	@Override
	public String takeScreenshotOnFail(String prefix, int index) {
		if (screenshotOnFailDir == null)
			return null;
		try {
			return takeScreenshot(getFilenameForScreenshotOnFail(prefix, index), true);
		} catch (UnsupportedOperationException e) {
			return null;
		} catch (WebDriverException e) {
			log.warn("- failed to capture screenshot: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			return null;
		}
	}

	public void setIgnoredScreenshotCommand(boolean isIgnoredScreenshotCommand) {
		this.isIgnoredScreenshotCommand = isIgnoredScreenshotCommand;
		log.info("Screenshot command: {}", isIgnoredScreenshotCommand ? "ignored" : "enabled");
	}

	@Override
	public boolean isIgnoredScreenshotCommand() {
		return isIgnoredScreenshotCommand;
	}
}