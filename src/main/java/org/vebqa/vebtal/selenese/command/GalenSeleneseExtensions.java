package org.vebqa.vebtal.selenese.command;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.galenframework.api.Galen;
import com.galenframework.reports.TestReport;
import com.galenframework.reports.model.LayoutReport;
import com.galenframework.speclang2.pagespec.SectionFilter;
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
			String tGSpec = curArgs[0].trim();
			String title = "Check layout: " + tGSpec;
			LayoutReport layoutReport = null;
			try {
				layoutReport = Galen.checkLayout(context.getWrappedDriver(), tGSpec,
						new SectionFilter(NO_TAGS, NO_TAGS), NO_PROPERTIES, NO_JS_VARIABLES);
			} catch (IOException e) {
				logger.error("Error reading file: {}", tGSpec, e);
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
	
	/**
	 * Weiche fuer die neuen Commands: - loadUserCredentials <pathToJS> -
	 * saveVariables <pathToExport> - loadVariables <pathToExport> - downloadFile -
	 * checkUrl <target> <StatusCode>
	 * 
	 * TODO: Refactor to map.
	 */
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
