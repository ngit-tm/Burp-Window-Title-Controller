package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.*;

public class BurpExtender implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Burp License Title Cleaner");
        
        // Get the main Burp frame using the recommended method
        JFrame burpFrame = (JFrame) api.userInterface().swingUtils().suiteFrame();
        
        // Clean the title immediately
        String currentTitle = burpFrame.getTitle();
        if (currentTitle.contains(" - licensed to ")) {
            String cleanTitle = currentTitle.replaceFirst(" - licensed to .*", "");
            burpFrame.setTitle(cleanTitle);
        }
        
        // Log extension loaded
        api.logging().logToOutput("Burp License Title Cleaner extension loaded");
    }
}