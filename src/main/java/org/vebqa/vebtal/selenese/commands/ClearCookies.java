package org.vebqa.vebtal.selenese.commands;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.command.AbstractCommand;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Success;

/**
 * Delete all existing cookies.
 * 
 * @author doerges
 *
 */
@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "clearCookies")
public class ClearCookies extends AbstractCommand {

	public ClearCookies(int index, String name, String... args) {
		super(index, name, args, VALUE, VALUE, VALUE);
	}

	@Override
	protected Result executeImpl(Context context, String... curArgs) {

		context.getWrappedDriver().manage().deleteAllCookies();

		return new Success("ok");
	}
}
