package org.vebqa.vebtal.seleneserestserver;

import java.util.Set;

import org.vebqa.vebtal.AbstractTestAdaptionPlugin;
import org.vebqa.vebtal.model.Command;
import org.vebqa.vebtal.model.CommandResult;
import org.vebqa.vebtal.seleneserestserver.util.DriverManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

@SuppressWarnings("restriction")
public class SeleneseTestAdaptionPlugin extends AbstractTestAdaptionPlugin {

	/** Start/Stop Button **/
	private static final Button btnStartStop = new Button();

	/** Browser selection **/
	private static final ComboBox<String> browserComboBox = new ComboBox<>();

	private static final TableView<CommandResult> commandList = new TableView<>();
	private static final ObservableList<CommandResult> clData = FXCollections.observableArrayList();

	@Override
	public String getName() {
		return "Selenese Plugin for RoboManager";
	}

	@Override
	public Class getImplementation() {
		return SeleneseResource.class;
	}
	
	@Override
	public Tab startup() {
		// Richtet den Plugin-spezifischen Tab ein
		
		Tab seleneseTab = new Tab();
		seleneseTab.setText("Selenese / Webdriver");
		
		btnStartStop.setText("Restart Context");
		btnStartStop.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
			}
		});

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

		// LogBox
		BorderPane root = new BorderPane();

		// Top bauen
		HBox hbox = new HBox();
		hbox.getChildren().addAll(btnStartStop, browserComboBox);

		// Table bauen
		TableColumn selCommand = new TableColumn("Command");
		selCommand.setCellValueFactory(new PropertyValueFactory<CommandResult, String>("command"));
		selCommand.setSortable(false);
		selCommand.prefWidthProperty().bind(commandList.widthProperty().multiply(0.15));

		TableColumn selTarget = new TableColumn("Target");
		selTarget.setCellValueFactory(new PropertyValueFactory<CommandResult, String>("target"));
		selTarget.setSortable(false);
		selTarget.prefWidthProperty().bind(commandList.widthProperty().multiply(0.20));

		TableColumn selValue = new TableColumn("Value");
		selValue.setCellValueFactory(new PropertyValueFactory<CommandResult, String>("value"));
		selValue.setSortable(false);
		selValue.prefWidthProperty().bind(commandList.widthProperty().multiply(0.20));

		TableColumn selResult = new TableColumn("Result");
		selResult.setCellValueFactory(new PropertyValueFactory<CommandResult, Image>("result"));
		selResult.setSortable(false);
		selResult.prefWidthProperty().bind(commandList.widthProperty().multiply(0.10));

		TableColumn selInfo = new TableColumn("LogInfo");
		selInfo.setCellValueFactory(new PropertyValueFactory<CommandResult, String>("loginfo"));
		selInfo.setSortable(false);
		selInfo.prefWidthProperty().bind(commandList.widthProperty().multiply(0.35));

		commandList.setItems(clData);
		commandList.getColumns().addAll(selCommand, selTarget, selValue, selResult, selInfo);

		// einfuegen
		root.setTop(hbox);
		root.setCenter(commandList);
		seleneseTab.setContent(root);
		
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

	public static void addCommandToList(Command aCmd) {
		CommandResult tCR = new CommandResult(aCmd.getCommand(), aCmd.getTarget(), aCmd.getValue());
		Platform.runLater(() -> clData.add(tCR));
	}

	public static void setLatestResult(Boolean success, final String aResult) {
		Platform.runLater(() -> clData.get(clData.size() - 1).setLogInfo(aResult));
		Platform.runLater(() -> clData.get(clData.size() - 1).setResult(success));

		commandList.refresh();
		commandList.scrollTo(clData.size() - 1);
	}
	
}
