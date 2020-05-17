package org.vebqa.vebtal.selenese.commands;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.command.AbstractCommand;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Success;

/**
 * 
 * Implement keyword to resize browser window at runtime.
 * 
 * @author doerges
 * 
 *
 */
@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "resize")		
public class ResizeWindow extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ResizeWindow.class);

	public ResizeWindow(int index, String name, String... args) {
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
		logger.info("Set windows dimension to: width:" + width + " and height:" + height);
		Dimension dim = new Dimension(width, height);
		context.getWrappedDriver().manage().window().setSize(dim);
		return new Success("ok");
	}
}
