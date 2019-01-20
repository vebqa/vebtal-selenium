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

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
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
public class ZAPSeleneseExtensions implements ICommandFactory {

	private static final Logger logger = LoggerFactory.getLogger(ZAPSeleneseExtensions.class);

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
	
	/**
	 * Load new commands at runtime.
	 * 
	 * Return null if command was not found.
	 * 
	 * @author doerges
	 */
	public ICommand newCommand(int index, String name, String... args) {
		LoggerUtils.quote("Called newCommand for " + name);
		if (name.contentEquals("startZAP")) {
			return new StartZAP(index, name, args);
		}

		return null;
	}
}
