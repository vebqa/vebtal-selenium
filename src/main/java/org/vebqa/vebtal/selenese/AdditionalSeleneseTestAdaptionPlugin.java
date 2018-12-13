package org.vebqa.vebtal.selenese;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.vebqa.vebtal.AbstractTestAdaptionPlugin;
import org.vebqa.vebtal.TestAdaptionType;

import javafx.scene.control.Tab;

public class AdditionalSeleneseTestAdaptionPlugin extends AbstractTestAdaptionPlugin {

	public static final String ID = "addselenese";
	
	public AdditionalSeleneseTestAdaptionPlugin() {
		super(TestAdaptionType.EXTENSION);
	}
	
	public String getName() {
		return "Selenese Extension Plugin for VEBTAL. Implementing miscelanous commands, e.g. resizing, downloading.";
	}

	@Override
	public String getAdaptionID() {
		return ID;
	}
	
	public boolean shutdown() {
		return false;
	}

	// register something but dont create tab
	public Tab startup() {
		return null;
	}

	@Override
	public CombinedConfiguration loadConfig() {
		// TODO Auto-generated method stub
		return null;
	}

}
