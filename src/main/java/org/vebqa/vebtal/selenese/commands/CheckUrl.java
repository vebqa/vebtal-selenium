package org.vebqa.vebtal.selenese.commands;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.selenese.filedownloader.RequestMethod;
import org.vebqa.vebtal.selenese.filedownloader.URLStatus;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.command.AbstractCommand;
import jp.vmi.selenium.selenese.result.Failure;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Success;

/**
 * Command "checkUrl" - Generische Implementierung fuer alle StatusCodes. - 200:
 * OK - 301: Moved permanently - 404: Not found
 * 
 * Expected Status Codes koennen als Liste angegeben werden, z.B.: 200,503
 * 
 * @author doerges
 *
 */
@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "checkUrl")		
public class CheckUrl extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(CheckUrl.class);

	public CheckUrl(int index, String name, String... args) {
		super(index, name, args, VALUE, VALUE, VALUE);
	}

	@Override
	protected Result executeImpl(Context context, String... curArgs) {
		String tUrl = curArgs[0].trim();
		String tStatusCode = curArgs[1].trim();

		// Statuscodes aufloesen
		String[] allStatusCodes = tStatusCode.split(",");
		List<Integer> codes = new ArrayList<Integer>();
		for (String aCode : allStatusCodes) {
			Integer tCode = Integer.parseInt(aCode.trim());
			codes.add(tCode);
		}

		String url = null;
		// entscheiden ob link oder id
		// fallback: link annehmen und ggf. http:// erweitern

		if (tUrl.startsWith("link=")) {
			tUrl = tUrl.replace("link=", "");
			url = context.getWrappedDriver().findElement(By.linkText(tUrl)).getAttribute("href");
		} else if (tUrl.startsWith("id=")) {
			tUrl = tUrl.replace("id=", "");
			url = context.getWrappedDriver().findElement(By.id(tUrl)).getAttribute("href");
		} else if (tUrl.startsWith("xpath=")) {
			tUrl = tUrl.replace("xpath=", "");
			url = context.getWrappedDriver().findElement(By.xpath(tUrl)).getAttribute("href");
		} else if (tUrl.startsWith("http://")) {
			url = tUrl;
		} else if (tUrl.startsWith("/")) {
			// um base erweitern!
			url = context.getCurrentBaseURL() + tUrl;
			logger.warn("Erweitert um base: {}", url);
		} else {
			// Fallback
			url = "http://" + tUrl;
		}

		try {
			URLStatus urlStatusCheck = new URLStatus(context.getWrappedDriver());
			urlStatusCheck.setURIToCheck(url);
			urlStatusCheck.setHTTPRequestMethod(RequestMethod.GET);

			// Pruefen ob der Statuscode in der Liste der erwarteten Codes
			// ist
			boolean statusOK = false;
			for (Integer tExpectedCode : codes) {
				if (urlStatusCheck.getHTTPStatusCode() == tExpectedCode) {
					statusOK = true;
				}
			}
			if (!statusOK) {
				return new Failure("Expected status code is:<" + tStatusCode + "> but i got <"
						+ urlStatusCheck.getHTTPStatusCode() + ">");

			} else {
				return new Success("Expected/resolved status code is:<" + urlStatusCheck.getHTTPStatusCode() + ">");
			}
		} catch (Exception e) {
			return new Failure(e);
		}
	}
}
