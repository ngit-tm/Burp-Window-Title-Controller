package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.*;

public class BurpExtender implements BurpExtension {
    private Timer titleCheckTimer;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Burp License Title Cleaner");
        
        // Get the main Burp frame using the recommended method
        JFrame burpFrame = (JFrame) api.userInterface().swingUtils().suiteFrame();
        
        // Create a timer to periodically check and update the title
        titleCheckTimer = new Timer(1000, _ -> {
            String currentTitle = burpFrame.getTitle();
            if (currentTitle.contains(" - licensed to ")) {
                // Remove the license information and set the clean title
                String cleanTitle = currentTitle.replaceFirst(" - licensed to .*", "");
                burpFrame.setTitle(cleanTitle);
            }
        });
        
        // Start the timer
        titleCheckTimer.start();
        
        // Log extension loaded
        api.logging().logToOutput("Burp License Title Cleaner extension loaded");
    }
}