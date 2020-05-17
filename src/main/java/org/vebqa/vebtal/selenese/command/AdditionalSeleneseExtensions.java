package org.vebqa.vebtal.selenese.command;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.annotations.Keyword;
import org.vebqa.vebtal.selenese.commands.CheckUrl;
import org.vebqa.vebtal.selenese.commands.ClearCookies;
import org.vebqa.vebtal.selenese.commands.Close;
import org.vebqa.vebtal.selenese.commands.ResizeWindow;
import org.vebqa.vebtal.selenese.filedownloader.FileDownloader;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.io.Files;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.VarsMap;
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
public class AdditionalSeleneseExtensions implements ICommandFactory {

	private static final Logger logger = LoggerFactory.getLogger(AdditionalSeleneseExtensions.class);

	/**
	 * Command "downloadFileFromUrl" - lade Daten aus einem Strom herunter.
	 * 
	 * @author doerges
	 *
	 */
	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "downloadFileFromUrl", hintTarget = "<URL>", hintValue = "<path/to/file>")
	private static class DownloadFileFromUrl extends AbstractCommand {

		public DownloadFileFromUrl(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// downloadUrl
			String tDownloadUrl = curArgs[0];
			if (tDownloadUrl.startsWith("$")) {
				tDownloadUrl = tDownloadUrl.replaceAll("${", "");
				tDownloadUrl = tDownloadUrl.replaceAll("}", "");
				LoggerUtils.quote("Key: " + tDownloadUrl);
				tDownloadUrl = (String) context.getVarsMap().get(tDownloadUrl.trim());
				LoggerUtils.quote("URL: " + tDownloadUrl);
			}

			// URL erweitern?
			if (tDownloadUrl.startsWith("/")) {
				tDownloadUrl = context.getCurrentBaseURL() + tDownloadUrl;
				LoggerUtils.quote("URL: " + tDownloadUrl);
			}

			// localStore
			String tStoreName = curArgs[1];
			// String thisScript = context.getCurrentTestCase().getFilename();
			// String thisFolder = thisScript.substring(0, thisScript.lastIndexOf('\\'));

			File writeFile = new File(tStoreName);
			FileDownloader downloadHandler = new FileDownloader(context.getWrappedDriver());
			try {
				downloadHandler.setURI(tDownloadUrl);
				File downloadedFile = downloadHandler.downloadFile();

				Files.copy(downloadedFile, writeFile);
			} catch (Exception e) {
				return new Failure(e);
			}
			return new Success("Downloaded file and saved to: " + writeFile.getAbsolutePath());
		}
	}

	/**
	 * Command "downloadFileFromUrl" - lade Daten aus einem Strom herunter.
	 * 
	 * @author doerges
	 *
	 */
	private static class DownloadFileFromUrlToDisk extends AbstractCommand {

		public DownloadFileFromUrlToDisk(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// downloadUrl
			String tDownloadUrl = curArgs[0];
			if (tDownloadUrl.startsWith("$")) {
				tDownloadUrl = tDownloadUrl.replaceAll("${", "");
				tDownloadUrl = tDownloadUrl.replaceAll("}", "");
				LoggerUtils.quote("Key: " + tDownloadUrl);
				tDownloadUrl = (String) context.getVarsMap().get(tDownloadUrl.trim());
				LoggerUtils.quote("URL: " + tDownloadUrl);
			}

			// URL erweitern?
			if (tDownloadUrl.startsWith("/")) {
				tDownloadUrl = context.getCurrentBaseURL() + tDownloadUrl;
				LoggerUtils.quote("URL: " + tDownloadUrl);
			}

			// localStore
			String tStoreName = curArgs[1];
			File writeFile = new File(tStoreName);

			FileDownloader downloadHandler = new FileDownloader(context.getWrappedDriver());
			try {
				downloadHandler.setURI(tDownloadUrl);
				File downloadedFile = downloadHandler.downloadFile();

				Files.copy(downloadedFile, writeFile);
			} catch (Exception e) {
				return new Failure(e);
			}
			return new Success("Downloaded file and saved to: " + writeFile.getAbsolutePath());
		}
	}

	/**
	 * Command "downloadFile" - lade Daten aus einem Strom herunter.
	 * 
	 * @author doerges
	 *
	 */
	private static class DownloadFile extends AbstractCommand {

		public DownloadFile(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// downloadUrl
			String tDownloadUrl = curArgs[0];
			String url = context.getWrappedDriver().findElement(By.xpath(tDownloadUrl)).getAttribute("href");
			// localStore
			String tStoreName = curArgs[1];
			String thisScript = context.getCurrentTestCase().getFilename();
			String thisFolder = thisScript.substring(0, thisScript.lastIndexOf('\\'));

			File writeFile = new File(thisFolder + "/" + tStoreName);
			FileDownloader downloadHandler = new FileDownloader(context.getWrappedDriver());
			try {
				downloadHandler.setURI(tDownloadUrl);
				File downloadedFile = downloadHandler.downloadFile();

				Files.copy(downloadedFile, writeFile);
			} catch (Exception e) {
				return new Failure(e);
			}
			return new Success("Downloaded file and saved to: " + writeFile.getAbsolutePath());
		}
	}

	/**
	 * Command "downloadFileByClick" - Implementierung fuer das Extrakreditportal.
	 * 
	 * @author doerges
	 *
	 */
	private static class DownloadFileByClick extends AbstractCommand {

		public DownloadFileByClick(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// Id
			String tId = curArgs[0];
			// localStore
			String tStoreName = curArgs[1];

			File writeFile = new File(tStoreName);
			FileDownloader downloadHandler = new FileDownloader(context.getWrappedDriver());

			String tSourceUrl = null;

			try {
				logger.info("try to get pdf by clicking on: {} ", tId);
				WebElement downloadLink = context.getWrappedDriver().findElement(By.id(tId));
				downloadLink.click();
				// tab wechseln
				ArrayList<String> tabs = new ArrayList<String>(context.getWrappedDriver().getWindowHandles());
				logger.info("Tab Count: {} ", tabs.size());

				context.getWrappedDriver().switchTo().window(tabs.get(1));
				downloadHandler.setURI(context.getWrappedDriver().getCurrentUrl());
				// Zwischenspeichern fuer das Log
				tSourceUrl = context.getWrappedDriver().getCurrentUrl();
				File downloadedFile = downloadHandler.downloadFile();
				Files.copy(downloadedFile, writeFile);

				context.getWrappedDriver().switchTo().window(tabs.get(0));
			} catch (Exception e) {
				return new Failure(e);
			} catch (Throwable te) {
				return new Failure(te.getMessage());
			}
			return new Success(
					"Downloaded file from source: " + tSourceUrl + " and saved to: " + writeFile.getAbsolutePath());
		}
	}

	private static class DownloadFileByClickFromBrowser extends AbstractCommand {

		public DownloadFileByClickFromBrowser(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// Id
			String tId = curArgs[0];
			// localStore
			String tStoreName = curArgs[1];

			String thisScript = context.getCurrentTestCase().getFilename();
			String thisFolder = thisScript.substring(0, thisScript.lastIndexOf('\\'));

			logger.warn("DEPRECATED: resolved folder: {}", thisFolder);

			File writeFile = new File(thisFolder + "/" + tStoreName);
			FileDownloader downloadHandler = new FileDownloader(context.getWrappedDriver());
			try {
				logger.info("try to get pdf by clicking on: " + tId);
				WebElement downloadLink = context.getWrappedDriver().findElement(By.id(tId));
				downloadLink.click();
				downloadHandler.setURI(context.getWrappedDriver().getCurrentUrl());
				File downloadedFile = downloadHandler.downloadFile();
				Files.copy(downloadedFile, writeFile);
			} catch (Exception e) {
				return new Failure(e);
			} catch (Throwable te) {
				return new Failure(te.getMessage());
			}
			return new Success("Downloaded file and saved to: " + writeFile.getAbsolutePath());
		}
	}

	/**
	 * Command "loadUserCredentials" laedt Benutzerkennung und Passwort aus einer JS
	 * Datei in der Umgebung. Diese JS Datei ist identisch mit der JS Datei welche
	 * in der Selenium IDE verwendet wird.
	 * 
	 * @author doerges
	 *
	 */
	private static class LoadUserCredentials extends AbstractCommand {

		LoadUserCredentials(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			LoggerUtils.quote("Called executeImpl with Argument lengt: " + curArgs.length);

			// credentials specifiction file
			String tUserJS = curArgs[0];
			LoggerUtils.quote("Read user credentials from: " + tUserJS);

			// ApplicationScope
			String tScope = curArgs[1];

			// Actual folder
			String thisScript = context.getCurrentTestCase().getFilename();
			String thisFolder = thisScript.substring(0, thisScript.lastIndexOf('\\'));

			String aUser = "unknown";
			try {
				File fXmlFile = new File(thisFolder + "/" + tUserJS);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(fXmlFile);
				doc.getDocumentElement().normalize();

				NodeList tList = doc.getElementsByTagName(tScope);
				Node tScopeNode = tList.item(0);
				Element tElement = (Element) tScopeNode;

				// varUserName und varPassword setzen
				aUser = tElement.getElementsByTagName("user").item(0).getTextContent();
				context.getVarsMap().put("varUserName", aUser);
				context.getVarsMap().put("varPassword", tElement.getElementsByTagName("pass").item(0).getTextContent());
			} catch (IOException e) {
				return new Failure(e);
			} catch (SAXException e) {
				return new Failure(e);
			} catch (ParserConfigurationException e) {
				return new Failure(e);
			}

			return new Success("User Credentials added to variable map (use: varUserName=>" + aUser
					+ " and varPassword =>*****)!");
		}
	}

	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "takeScreenshot", hintTarget = "<path/to/file.png>")
	private static class TakeScreenshot extends AbstractCommand {

		TakeScreenshot(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			logger.info("Called executeImpl with Argument lengt: {}", curArgs.length);
			String tScreenshotFile = curArgs[0];
			
			File scrFile = ((TakesScreenshot)context.getWrappedDriver()).getScreenshotAs(OutputType.FILE);
			File targetFile = new File(tScreenshotFile);
			logger.info("target file for screenshot: {}", targetFile);
			
			try {
				FileUtils.copyFile(scrFile, targetFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new Success("ok");
		}
	}
	
	/**
	 * Alternate keyword to realize an enter event without using javascript function. 
	 * Used to automate VAADIN gui.
	 * 
	 * @author doerges
	 *
	 */
	@Keyword(module = SeleneseTestAdaptionPlugin.ID, command = "pressEnter")	
	private static class PressEnter extends AbstractCommand {
		
		PressEnter(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}
		
		protected Result executeImpl(Context context, String... curArgs) {
			String locator = curArgs[0];
			WebDriver driver = context.getWrappedDriver();
			WebElement element = context.getElementFinder().findElement(driver, locator);
			element.sendKeys(Keys.ENTER);
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
		if (name.contentEquals("loadUserCredentials")) {
			return new LoadUserCredentials(index, name, args);
		}
		if (name.contentEquals("downloadFileFromUrl")) {
			return new DownloadFileFromUrl(index, name, args);
		}
		if (name.contentEquals("downloadFileFromUrlToDisk")) {
			return new DownloadFileFromUrlToDisk(index, name, args);
		}
		if (name.contentEquals("downloadFileByClick")) {
			return new DownloadFileByClick(index, name, args);
		}
		if (name.contentEquals("checkUrl")) {
			return new CheckUrl(index, name, args);
		}
		if (name.contentEquals("downloadFile")) {
			return new DownloadFile(index, name, args);
		}
		if (name.contentEquals("downloadFileByClickFromBrowser")) {
			return new DownloadFileByClickFromBrowser(index, name, args);
		}
		if (name.contentEquals("takeScreenshot")) {
			return new TakeScreenshot(index, name, args);
		}
		if (name.contentEquals("resize")) {
			return new ResizeWindow(index, name, args);
		}
		if (name.contentEquals("close")) {
			return new Close(index, name, args);
		}
		if (name.contentEquals("clearCookies")) {
			return new ClearCookies(index, name, args);
		}
		if (name.contentEquals("pressEnter")) {
			return new PressEnter(index, name, args);
		}

		return null;
	}
}
