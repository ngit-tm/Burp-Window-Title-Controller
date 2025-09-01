package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.*;
import java.util.regex.Pattern;

public class BurpExtender implements BurpExtension {
    // State
    private MontoyaApi api;
    private JFrame burpFrame;

    private String originalTitle;
    private String customTitle = null;
    private boolean isLicenseHidden = false;

    // - licensed to .を抽出するパターン
    private static final Pattern LICENSED_TO_PATTERN = Pattern.compile(" - licensed to .*",Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Burp Window Title Controller");
        
        // Get the main Burp frame using the recommended method
        this.burpFrame = (JFrame) api.userInterface().swingUtils().suiteFrame();
        this.originalTitle = burpFrame.getTitle();

        applyTitle(false);

        api.extension().registerUnloadingHandler(()-> safeSetTitle(originalTitle));

        // Log extension loaded
        api.logging().logToOutput("Burp Window Title Controller extension loaded.");
        api.logging().logToOutput("Original Title: " + originalTitle);
    }

        private void applyTitle(boolean force) {
        String desired = computeDesiredTitle(burpFrame.getTitle());
        String now = burpFrame.getTitle();
        if (force || !desired.equals(now)) {
            safeSetTitle(desired);
        }
    }

    private String computeDesiredTitle(String current) {
        String base = (current == null) ? "" : current;
        if (isLicenseHidden) {
            base = LICENSED_TO_PATTERN.matcher(base).replaceFirst("");
        }
        return (customTitle != null) ? customTitle : base.trim();
    }

    private void safeSetTitle(String title) {
        if (SwingUtilities.isEventDispatchThread()) {
            burpFrame.setTitle(title);
        } else {
            SwingUtilities.invokeLater(() -> burpFrame.setTitle(title));
        }
    }
}
