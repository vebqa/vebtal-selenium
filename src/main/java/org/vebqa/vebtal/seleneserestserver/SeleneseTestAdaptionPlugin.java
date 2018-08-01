package org.vebqa.vebtal.seleneserestserver;

import java.io.File;
import java.util.Set;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.AbstractTestAdaptionPlugin;
import org.vebqa.vebtal.TestAdaptionType;
import org.vebqa.vebtal.model.Command;
import org.vebqa.vebtal.model.CommandResult;
import org.vebqa.vebtal.model.CommandType;
import org.vebqa.vebtal.seleneserestserver.util.DriverManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

@SuppressWarnings("restriction")
public class SeleneseTestAdaptionPlugin extends AbstractTestAdaptionPlugin {

	private static final Logger logger = LoggerFactory.getLogger(SeleneseTestAdaptionPlugin.class);
	
	public static final String ID = "selenese";
	
	private static final String DEFAULTBROWSER = "Chrome";
	private static final String DEFAULTPROXY = "Proxy: None";
	
	public SeleneseTestAdaptionPlugin() {
		super(TestAdaptionType.ADAPTER);
	}
	
	/** Browser selection **/
	private static final ComboBox<String> browserComboBox = new ComboBox<>();

	/** Proxy selection **/
	private static final ComboBox<String> proxyComboBox = new ComboBox<>();
	
	
	private static final TableView<CommandResult> commandList = new TableView<>();
	private static final ObservableList<CommandResult> clData = FXCollections.observableArrayList();

	@Override
	public String getName() {
		return "Selenese Plugin for RoboManager";
	}

	@Override
	public Class<?> getImplementation() {
		return null;
	}
	
	@Override
	public CombinedConfiguration loadConfig() {
		return loadConfig(ID);
	}
	
	@Override
	public Tab startup() {
		Tab seleneseTab = createTab(ID, commandList, clData);
		
		// Auswahlbox Browser
		DriverManager dm = new DriverManager();
		dm.scanDirectory();

		Set<String> allDrivers = dm.getDriverKeys();
		if (allDrivers.isEmpty()) {
			browserComboBox.getItems().add("No browser found");
		} else {
			for (String aDriver : allDrivers) {
				browserComboBox.getItems().add(aDriver);
			}
		}

		// Default: Chrome
		browserComboBox.getSelectionModel().select(DEFAULTBROWSER);

		// Default: none
		proxyComboBox.getItems().add("Proxy: None");
		proxyComboBox.getItems().add("8888");
		proxyComboBox.getSelectionModel().select(DEFAULTPROXY);
		
		BorderPane pane = (BorderPane)seleneseTab.getContent();
		// Top bauen
		HBox hbox = new HBox();
		hbox.getChildren().addAll(browserComboBox, proxyComboBox);
		pane.setTop(hbox);
		
		return seleneseTab;
	}
	
	@Override
	public boolean shutdown() {
		return true;
	}
	
	public static String getSelectedDriver() {
		return browserComboBox.getValue();
	}
	
	public static String getSelectedProxy() {
		return proxyComboBox.getValue();
	}

	public static void disableComboBox() {
		browserComboBox.setDisable(true);
		proxyComboBox.setDisable(true);
	}

	public static void enableCombobox() {
		browserComboBox.setDisable(false);
		proxyComboBox.setDisable(false);
	}

	public static void addCommandToList(Command aCmd, CommandType aType) {
		final CommandResult tCR = new CommandResult(aCmd.getCommand(), aCmd.getTarget(), aCmd.getValue(), aType);
		// Platform.runLater(() -> clData.add(tCR));
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				clData.add(tCR);
			}
		});
	}

	public static void setLatestResult(Boolean success, final String aResult) {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				clData.get(clData.size() - 1).setLogInfo(aResult);
				clData.get(clData.size() - 1).setResult(success);
				commandList.refresh();
				commandList.scrollTo(clData.size() - 1);
			}
		});
		
	}
	
	@Override
	public String getAdaptionID() {
		return ID;
	}	
}
