package org.vebqa.vebtal.selenese.commands;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import org.vebqa.vebtal.GuiManager;
import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.seleneserestserver.SeleneseResource;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;
import org.vebqa.vebtal.sut.SutStatus;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.command.AbstractCommand;
import jp.vmi.selenium.selenese.result.Failure;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Success;

/**
 * Close an active brwoser instance and quit the (proxy) driver.
 * 
 * @author doerges
 *
 */
@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "close")		
public class Close extends AbstractCommand {

	public Close(int index, String name, String... args) {
		super(index, name, args, VALUE, VALUE, VALUE);
	}

	@Override
	protected Result executeImpl(Context context, String... curArgs) {
		try {
			
			// context.getWrappedDriver().close();
			SeleneseResource.getManager().quitDriver();
			SeleneseResource.destroyManager();
		} catch (Exception e) {
			return new Failure("Something went wrong while closing the webdriver! " + e.getMessage());
		}
					
		// Browser Auswahlbox kann wieder aktiviert werden.
		SeleneseTestAdaptionPlugin.enableCombobox();
		GuiManager.getinstance().setTabStatus(SeleneseTestAdaptionPlugin.ID, SutStatus.DISCONNECTED);
		
		return new Success("ok");
	}
}
