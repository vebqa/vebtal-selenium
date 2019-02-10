package org.vebqa.vebtal.selenese.command;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.command.AbstractCommand;
import jp.vmi.selenium.selenese.command.ICommand;
import jp.vmi.selenium.selenese.command.ICommandFactory;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Success;
import jp.vmi.selenium.selenese.utils.LoggerUtils;

/**
 * VEB spezifische Erweiterungen fuer den Selenese Runner.
 * 
 * @author doerges
 *
 */
public class ZAPSeleneseExtensions implements ICommandFactory {

	private static final Logger logger = LoggerFactory.getLogger(ZAPSeleneseExtensions.class);

	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "startZap")
	private static class StartZAP extends AbstractCommand {

		StartZAP(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			logger.info("Called executeImpl with Argument lengt: {}", curArgs.length);
			return new Success("ok");
		}
	}

	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "stopZap")
	private static class StopZAP extends AbstractCommand {

		StopZAP(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			logger.info("Called executeImpl with Argument lengt: {}", curArgs.length);
			return new Success("ok");
		}
	}
	
	/**
	 * Load new commands at runtime.
	 * 
	 * Return null if command was not found.
	 * 
	 * @author doerges
	 */
	public ICommand newCommand(int index, String name, String... args) {
		LoggerUtils.quote("Called newCommand for " + name);
		if (name.contentEquals("startZap")) {
			return new StartZAP(index, name, args);
		}
		if (name.contentEquals("stopZap")) {
			return new StopZAP(index, name, args);
		}

		return null;
	}
}
