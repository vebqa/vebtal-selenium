package org.vebqa.vebtal.selenese.command;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.GuiManager;
import org.vebqa.vebtal.selenese.filedownloader.FileDownloader;
import org.vebqa.vebtal.selenese.filedownloader.RequestMethod;
import org.vebqa.vebtal.selenese.filedownloader.URLStatus;
import org.vebqa.vebtal.seleneserestserver.SeleneseResource;
import org.vebqa.vebtal.seleneserestserver.SeleneseTestAdaptionPlugin;
import org.vebqa.vebtal.sut.SutStatus;
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
	 * Laedt Variablen aus der Eingangsschnittstelle in den Selenese Variablen-Raum.
	 * 
	 * @author doerges
	 *
	 */
	private static class LoadVariables extends AbstractCommand {

		/**
		 * Constructor
		 * 
		 * @param index
		 * @param name
		 * @param args
		 */
		public LoadVariables(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		/**
		 * Implementierung des "loadVariables" commands
		 */
		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			LoggerUtils.quote("Called executeImpl with Argument lengt: " + curArgs.length);
			String tVarsFile = curArgs[0];

			// Actual folder
			String thisScript = context.getCurrentTestCase().getFilename();
			String thisFolder = thisScript.substring(0, thisScript.lastIndexOf('\\'));

			File varFile = new File(thisFolder + "/" + tVarsFile);

			if (!varFile.exists()) {
				return new Failure("Variable file does not exists: " + varFile.getAbsolutePath());
			}

			// load export file
			Document doc = null;
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.parse(varFile);
				doc.getDocumentElement().normalize();

			} catch (IOException e) {
				return new Failure("Can not read variable file: " + varFile);
			} catch (ParserConfigurationException e) {
				return new Failure("Can not parse variable file: " + varFile + " because: " + e.getMessage());
			} catch (SAXException e) {
				return new Failure("Can not parse variable file: " + varFile + " because: " + e.getMessage());
			}
			// nach vereinbarung gibt es nur ein erstes Tag "variables"
			Node nVarNode = doc.getElementsByTagName("variables").item(0);

			NodeList nVarChildren = nVarNode.getChildNodes();

			// transfer values to var system
			for (int temp = 0; temp < nVarChildren.getLength(); temp++) {
				Node nNode = nVarChildren.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String tNodeName = eElement.getNodeName();
					String tNodeValue = eElement.getTextContent();
					// Variable uebertragen wenn der Key noch nicht vorhanden
					// ist.
					context.getVarsMap().putIfAbsent(tNodeName, tNodeValue);
				}
			}

			return new Success("Variables successfully loaded from file and transfered to variable system");
		}

	}

	/**
	 * Command "saveVariables", erlaubt das Speichern der angelegten Variablen in
	 * einer XML Datei fuer die Weiterverarbeitung.
	 * 
	 * @author doerges
	 *
	 */
	private static class SaveVariables extends AbstractCommand {

		public SaveVariables(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			LoggerUtils.quote("Called executeImpl with Argument length: " + curArgs.length);

			String tVarsFile = curArgs[0];
			File varFile = new File(tVarsFile);

			VarsMap tVarsMap = context.getVarsMap();
			if (tVarsMap.containsKey("SeleniumTemp")) {
				String fileName = varFile.getName();
				String seleniumTemp = (String) tVarsMap.get("SeleniumTemp");
				varFile = new File(seleniumTemp + File.separator + fileName);
			}
			Set<Entry<String, Object>> tEntries = tVarsMap.entrySet();

			int countElements = 0;

			try {

				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

				// root elements
				Document doc = docBuilder.newDocument();
				Element rootElement = doc.createElement("exchange");
				doc.appendChild(rootElement);

				for (Entry<String, Object> tEntry : tEntries) {
					// Alles ausser KEY exportieren, das sind SELENESE interne
					// Variablen die uns nicht weiter interessieren.
					if (!tEntry.getKey().startsWith("KEY_") && !tEntry.getKey().startsWith("space")
							&& !tEntry.getKey().startsWith("nbsp")) {
						// elements
						Element element = doc.createElement(tEntry.getKey());
						// Loesung finden fuer die Konvertierung in einen String
						Object tVal = tEntry.getValue();
						if (tVal instanceof Boolean) {
							element.appendChild(doc.createTextNode((new Boolean((Boolean) tVal)).toString()));
						} else if (tVal instanceof String) {
							element.appendChild(doc.createTextNode((String) tVal));
						}
						rootElement.appendChild(element);
						countElements++;
					}
				}

				// write the content into xml file
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(varFile);

				transformer.transform(source, result);

			} catch (ParserConfigurationException pce) {
				return new Failure(pce);
			} catch (TransformerException tfe) {
				return new Failure(tfe);
			}
			return new Success(countElements + " variables successfully written to file: " + varFile.getAbsolutePath());
		}

	}

	/**
	 * Command "loadData" - lade Daten aus der Import Schnittstelle in den Selenese
	 * Variablen Raum. Die Import Schnittstelle ist ein XML File, wobei die Keys das
	 * Tag repraesentieren und der Textcontent den Value.
	 * 
	 * @author doerges
	 *
	 */
	private static class LoadData extends AbstractCommand {

		public LoadData(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/**
	 * Command "downloadFileFromUrl" - lade Daten aus einem Strom herunter.
	 * 
	 * @author doerges
	 *
	 */
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
	 * Command "checkUrl" - Generische Implementierung fuer alle StatusCodes. - 200:
	 * OK - 301: Moved permanently - 404: Not found
	 * 
	 * Expected Status Codes koennen als Liste angegeben werden, z.B.: 200,503
	 * 
	 * @author doerges
	 *
	 */
	private static class CheckUrl extends AbstractCommand {

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

	private static class LoadFuzzingData extends AbstractCommand {

		LoadFuzzingData(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			String aCollectionName = curArgs[0];
			context.getCollectionMap().addCollection(aCollectionName);
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
			logger.info("Set windows dimension to: width:" + width + " and height:" + height);
			Dimension dim = new Dimension(width, height);
			context.getWrappedDriver().manage().window().setSize(dim);
			return new Success("ok");
		}
	}

	private static class Close extends AbstractCommand {

		Close(int index, String name, String... args) {
			super(index, name, args, VALUE, VALUE, VALUE);
		}

		@Override
		protected Result executeImpl(Context context, String... curArgs) {
			try {
				context.getWrappedDriver().close();
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
	
	
	public ICommand newCommand(int index, String name, String... args) {
		LoggerUtils.quote("Called newCommand for " + name);
		if (name.contentEquals("loadUserCredentials")) {
			return new LoadUserCredentials(index, name, args);
		}
		if (name.contentEquals("saveVariables")) {
			return new SaveVariables(index, name, args);
		}
		if (name.contentEquals("loadVariables")) {
			return new LoadVariables(index, name, args);
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

		if (name.contentEquals("resize")) {
			return new ResizeWindow(index, name, args);
		}
		if (name.contentEquals("close")) {
			return new Close(index, name, args);
		}

		return null;
	}
}
