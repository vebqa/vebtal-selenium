package org.vebqa.vebtal.selenese;

import org.vebqa.vebtal.AbstractTestAdaptionPlugin;
import org.vebqa.vebtal.TestAdaptionType;

import javafx.scene.control.Tab;

public class AdditionalSeleneseTestAdaptionPlugin extends AbstractTestAdaptionPlugin {

	public AdditionalSeleneseTestAdaptionPlugin() {
		super(TestAdaptionType.EXTENSION);
	}
	
	public String getName() {
		return "Selenese Extension Plugin for VEBTAL. Implementing miscelanous commands, e.g. resizing, downloading.";
	}

	public boolean shutdown() {
		return false;
	}

	// register something but dont create tab
	public Tab startup() {
		return null;
	}

}
