package org.vebqa.vebtal.seleneserestserver;

import java.util.Set;

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

	public static final String ID = "selenese";
	
	public SeleneseTestAdaptionPlugin() {
		super(TestAdaptionType.ADAPTER);
	}
	
	/** Browser selection **/
	private static final ComboBox<String> browserComboBox = new ComboBox<>();

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
		browserComboBox.getSelectionModel().select("Chrome");

		BorderPane pane = (BorderPane)seleneseTab.getContent();
		// Top bauen
		HBox hbox = new HBox();
		hbox.getChildren().addAll(browserComboBox);
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

	public static void disableComboBox() {
		browserComboBox.setDisable(true);
	}

	public static void enableCombobox() {
		browserComboBox.setDisable(false);
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
