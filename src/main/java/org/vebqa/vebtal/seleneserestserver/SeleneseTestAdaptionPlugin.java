package org.vebqa.vebtal.seleneserestserver;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vebqa.vebtal.AbstractTestAdaptionPlugin;
import org.vebqa.vebtal.CommandAutoComplete;
import org.vebqa.vebtal.GuiManager;
import org.vebqa.vebtal.KeywordEntry;
import org.vebqa.vebtal.KeywordFinder;
import org.vebqa.vebtal.TestAdaptionType;
import org.vebqa.vebtal.model.Command;
import org.vebqa.vebtal.model.CommandResult;
import org.vebqa.vebtal.model.CommandType;
import org.vebqa.vebtal.selenese.command.SeleneseCommands;
import org.vebqa.vebtal.seleneserestserver.util.DriverManager;
import org.vebqa.vebtal.sut.SutStatus;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

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
		proxyComboBox.getItems().add("NoProxy");
		// manual proxy from settings
		String tProxyPort = GuiManager.getinstance().getConfig().getString("browser.proxy.port");
		if (!tProxyPort.contentEquals("0")) {
			proxyComboBox.getItems().add(tProxyPort);
		}
		String zapMode = GuiManager.getinstance().getConfig().getString("zap.mode");
		switch (zapMode) {
		case "disable":
			logger.info("disable zap support!");
			break;
		case "auto":
			proxyComboBox.getItems().add("ZAP");
			break;
		case "manual":
			proxyComboBox.getItems().add("ZAP");
			break;
		default:
			logger.info("skipped value: {} for key: zap.mode. Dont know hat to do!", zapMode);
			break;
		}
		
		proxyComboBox.getSelectionModel().select(DEFAULTPROXY);
		
		// Add
		// Add (Test generation)
		Text txtGeneration = new Text();
		txtGeneration.setText("test generation");
		txtGeneration.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 20)); 
		
		List<KeywordEntry> allModuleKeywords = KeywordFinder.getinstance().getKeywordsByModule(SeleneseTestAdaptionPlugin.ID);
		TreeSet<String> sortedKeywords = new TreeSet<>();
		for (KeywordEntry aKeyword : allModuleKeywords) {
			sortedKeywords.add(aKeyword.getCommand());
		}
		
		SeleneseCommands seleneseCmds = new SeleneseCommands();
		List<KeywordEntry> allSeleneseCmds = seleneseCmds.getKeywords();
		for (KeywordEntry aKeyword : allSeleneseCmds) {
			sortedKeywords.add(aKeyword.getCommand());
		}
		
		final CommandAutoComplete addCommand = new CommandAutoComplete(sortedKeywords);
        addCommand.setPromptText("Command");
        addCommand.setMaxWidth(200);
        final TextField addTarget = new TextField();
        addTarget.setMaxWidth(350);
        addTarget.setPromptText("Target");
        final TextField addValue = new TextField();
        addValue.setMaxWidth(350);
        addValue.setPromptText("Value");
 
        final Button addButton = new Button("Go");
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
            	Command newCmd = new Command(addCommand.getText(), addTarget.getText(), addValue.getText());
            	
                addCommand.clear();
                addTarget.clear();
                addValue.clear();
                
                SeleneseResource aResource = new SeleneseResource();
                GuiManager.getinstance().setTabStatus(SeleneseTestAdaptionPlugin.ID, SutStatus.CONNECTED);
                aResource.execute(newCmd);
                GuiManager.getinstance().setTabStatus(SeleneseTestAdaptionPlugin.ID, SutStatus.DISCONNECTED);
            }
        });
 
        HBox hcmdbox = new HBox();
         
        hcmdbox.getChildren().addAll(txtGeneration, addCommand, addTarget, addValue, addButton);
        hcmdbox.setSpacing(5);
        hcmdbox.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, new CornerRadii(2), new BorderWidths(3))));
		
		BorderPane pane = (BorderPane)seleneseTab.getContent();
		// Top bauen
		HBox hbox = new HBox();
		hbox.getChildren().addAll(browserComboBox, proxyComboBox);
		
		VBox stack = new VBox();
		stack.getChildren().addAll(hbox, hcmdbox);
		pane.setTop(stack);
		
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
