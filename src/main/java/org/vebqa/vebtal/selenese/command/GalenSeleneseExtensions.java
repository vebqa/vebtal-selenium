package org.vebqa.vebtal.selenese.command;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;

import com.galenframework.api.Galen;
import com.galenframework.browser.SeleniumBrowser;
import com.galenframework.parser.IndentationStructureParser;
import com.galenframework.parser.StructNode;
import com.galenframework.reports.TestReport;
import com.galenframework.reports.model.LayoutReport;
import com.galenframework.speclang2.pagespec.MacroProcessor;
import com.galenframework.speclang2.pagespec.PageSpecHandler;
import com.galenframework.speclang2.pagespec.PageSpecReader;
import com.galenframework.speclang2.pagespec.PostProcessor;
import com.galenframework.speclang2.pagespec.SectionFilter;
import com.galenframework.specs.page.PageSpec;
import com.galenframework.validation.ValidationResult;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.command.AbstractCommand;
import jp.vmi.selenium.selenese.command.ICommand;
import jp.vmi.selenium.selenese.command.ICommandFactory;
import jp.vmi.selenium.selenese.result.Failure;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Success;
import jp.vmi.selenium.selenese.utils.LoggerUtils;

/**
 * VEB spezifische Erweiterungen fuer den Selenese Runner.
 * 
 * @author doerges
 *
 */
public class GalenSeleneseExtensions implements ICommandFactory {

	private static final Logger logger = LoggerFactory.getLogger(GalenSeleneseExtensions.class);

	/**
	 * Command "checkLayout" - Verwendung von Galen Specidication Tests
	 * 
	 * Getestet wird gegen die aktuelle Seite / DOM. Angegeben wird das gspec File.
	 * 
	 * @author doerges
	 *
	 */
	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "checkLayout", hintTarget = "<spec file>", hintValue = "<spec text>")
	private static class CheckLayout extends AbstractCommand {

		private static final List<String> NO_TAGS = emptyList();
		private static final Map<String, Object> NO_JS_VARIABLES = emptyMap();
		private static final Properties NO_PROPERTIES = new Properties();

		protected ThreadLocal<TestReport> report = new ThreadLocal<>();

		public CheckLayout(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {

			boolean isFrameTest = false;
			
			// get gspec file if specidfied
			String tGSpec = null;
			if (curArgs[0] != null) {
				tGSpec = curArgs[0].trim();
			}

			// get gspec as value if specified
			String tGspecValue = null;
			if (curArgs[1] != null) {
				tGspecValue = curArgs[1];
			}
			
			// POC: iframes (chain) support (by name only)
			if (tGSpec.startsWith("iframes=")) {
				tGSpec = tGSpec.replaceAll("iframes=", "");
				String allFrameNames[] = tGSpec.split(",");
				context.getWrappedDriver().switchTo().defaultContent();
				for (String aFrameName : allFrameNames) {
					context.getWrappedDriver().switchTo().frame(context.getWrappedDriver().findElement(By.name(aFrameName)));
					logger.info("Switched to frame {}", aFrameName);
					isFrameTest = true;
				}
				
				// set tGspecValue to null as its not used again
				tGSpec=null;
			}
			
			// POC: iframe support
			if ((tGSpec != null) &&  (tGSpec.startsWith("iframe="))) {
				tGSpec = tGSpec.replaceAll("iframe=", "");
				context.getWrappedDriver().switchTo().defaultContent();
				context.getWrappedDriver().switchTo().frame(context.getWrappedDriver().findElement(By.xpath(tGSpec)));
				logger.info("Switched to frame {}", tGSpec);
				isFrameTest = true;
				
				// set tGspecValue to null as its not used again
				tGSpec=null;
			}

			PageSpec actualPageSpec = new PageSpec();
			PageSpecReader reader = new PageSpecReader();

			// use specified value first!
			if (tGspecValue != null) {
				IndentationStructureParser structParser = new IndentationStructureParser();
				try {
				List<StructNode> structs = structParser.parse(tGspecValue);
				PageSpec tempSpec = new PageSpec();
				PageSpecHandler pageSpecHandler = new PageSpecHandler(tempSpec,
						new SeleniumBrowser(context.getWrappedDriver()).getPage(), new SectionFilter(NO_TAGS, NO_TAGS),
						"", null, null);

				List<StructNode> allProcessedChildNodes = new MacroProcessor(pageSpecHandler).process(structs);
				new PostProcessor(pageSpecHandler).process(allProcessedChildNodes);

				actualPageSpec = pageSpecHandler.buildPageSpec();
				} catch (IOException e) {
					
				}

			} else {

				try {
					actualPageSpec = reader.read(tGSpec, new SeleniumBrowser(context.getWrappedDriver()).getPage(),
							new SectionFilter(NO_TAGS, NO_TAGS), null, null, null);
				} catch (IOException e) {

				}
			}
			String title = "Check layout: " + tGSpec;
			LayoutReport layoutReport = null;
			try {
				layoutReport = Galen.checkLayout(new SeleniumBrowser(context.getWrappedDriver()), actualPageSpec,
						new SectionFilter(NO_TAGS, NO_TAGS), null);
			} catch (IOException e) {
				logger.error("Error reading file: {}", tGSpec, e);
			}

			// back to default!
			if (isFrameTest) {
				context.getWrappedDriver().switchTo().defaultContent();
				logger.info("Switched back to default content.");
			}
			
			if (layoutReport != null && layoutReport.errors() > 0) {
				int errorCount = 0;
				StringBuilder resultMessage = new StringBuilder();
				List<ValidationResult> results = layoutReport.getValidationErrorResults();
				for (ValidationResult resultItem : results) {
					errorCount++;
					resultMessage.append("Error: " + errorCount + "\n");
					for (String msg : resultItem.getError().getMessages()) {
						resultMessage.append("* " + msg);
						resultMessage.append('\n');
					}
				}
				return new Failure(resultMessage.toString());
			}
			return new Success("ok");
		}
	}

	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "resize", hintTarget = "width=;height=")
	private static class ResizeWindow extends AbstractCommand {

		ResizeWindow(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// Dimension
			// Beispiel: width=800;height=600
			int width = 800;
			int height = 600;
			String target = curArgs[0].trim();
			String[] someToken = target.split(";");

			for (String aToken : someToken) {
				// Needs an equal
				String[] parts = aToken.split("=");
				switch (parts[0]) {
				case "width":
					width = Integer.valueOf(parts[1]);
					break;
				case "height":
					height = Integer.valueOf(parts[1]);
					break;
				}
			}
			logger.info("Set windows dimension to: width: {} and height: {}" + width, height);
			Dimension dim = new Dimension(width, height);
			context.getWrappedDriver().manage().window().setSize(dim);
			return new Success("ok");
		}
	}

	public ICommand newCommand(int index, String name, String... args) {
		LoggerUtils.quote("Called newCommand for " + name);

		// Galen Framework integration
		if (name.contentEquals("checkLayout")) {
			return new CheckLayout(index, name, args);
		}

		if (name.contentEquals("resize")) {
			return new ResizeWindow(index, name, args);
		}

		return null;
	}
}
