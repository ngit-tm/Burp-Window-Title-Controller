package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.*;
import java.util.regex.Pattern;

public class BurpExtender implements BurpExtension {
    private MontoyaApi api;
    private JFrame burpFrame;

    // State for title control
    private String originalTitle;
    private String customTitle = null;
    private boolean isLicenseHidden = false;

    // Regex pattern to match " - licensed to ..."
    private static final Pattern LICENSED_TO_PATTERN =
            Pattern.compile("\\s-\\slicensed\\sto\\s.*", Pattern.CASE_INSENSITIVE);

    // Menu item to display current title (read-only)
    private JMenuItem currentItem;
    // Prevent duplicate "Title" menus on reload/unload
    private JMenu titleMenu;
    private static final String MENU_ID = "bwtc-title-menu";


    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Burp Window Title Controller");

        this.burpFrame = (JFrame) api.userInterface().swingUtils().suiteFrame();
        this.originalTitle = burpFrame.getTitle();

        addTitleMenu();

        // Apply initial state and update display
        applyTitle(true);
        updateCurrentMenuItem();

        // On unload, restore the original title and remove our menu to avoid leftover UI artifacts
        api.extension().registerUnloadingHandler(() -> {
            safeSetTitle(originalTitle);
            removeMenuIfPresent();
        });

        // logging
        api.logging().logToOutput("Burp Window Title Controller extension loaded.");
        api.logging().logToOutput("Original Title: " + originalTitle);
    }

    // Add Title menu
    // Remove an existing instance (if any) before adding a fresh one
    private void addTitleMenu(){
        JMenuBar bar = burpFrame.getJMenuBar();
        if(bar == null){
            api.logging().logToError("Error: MenuBar not found.");
            return;
        }
        removeMenuIfPresent();

        this.titleMenu = new JMenu("Title");
        this.titleMenu.setName(MENU_ID);

        // Current window title (read only item)
        currentItem = new JMenuItem();
        currentItem.setEnabled(false);
        titleMenu.add(currentItem);

        titleMenu.addSeparator();

        // Toggle license suffix visibility
        JCheckBoxMenuItem hide = new JCheckBoxMenuItem("Hide 'licensed to' suffix", isLicenseHidden);
        hide.addActionListener(e -> {
            isLicenseHidden = hide.getState();
            applyTitle(true);
            updateCurrentMenuItem();
            api.logging().logToOutput("Hide license suffix = " + isLicenseHidden);
        });
        titleMenu.add(hide);

        titleMenu.addSeparator();

        // Set custom title
        JMenuItem setCustom = new JMenuItem("Set Custom Title...");
        setCustom.addActionListener(e -> {
            // Initial value: use customTitle if set, else current title
            String initial = (customTitle != null) ? customTitle : burpFrame.getTitle();
            String input = (String) JOptionPane.showInputDialog(
                    burpFrame,
                    "Enter window title:",
                    "Set Custom Title",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    initial
            );
            if (input != null) {
                input = input.trim();
                if (input.isEmpty()) {
                    JOptionPane.showMessageDialog(burpFrame, "Title cannot be empty.");
                    return;
                }
                customTitle = input;
                applyTitle(true);
                updateCurrentMenuItem();
                api.logging().logToOutput("Custom title = " + customTitle);
            }
        });
        titleMenu.add(setCustom);

        // Reset custom title
        JMenuItem reset = new JMenuItem("Reset Title to Burp Default");
        reset.addActionListener(e -> {
            customTitle = null;
            applyTitle(true);
            updateCurrentMenuItem();
            api.logging().logToOutput("Custom title reset");
        });
        titleMenu.add(reset);

        bar.add(titleMenu);
        bar.revalidate();
        bar.repaint();
    }

    // Update "Current: ..." menu item to reflect current title
    private void updateCurrentMenuItem() {
        if (currentItem == null) return;
        String shown = burpFrame.getTitle();
        currentItem.setText("Current: " + (shown == null ? "" : shown));
    }

    // Compute and apply the desired title if changed
    private void applyTitle(boolean force) {
        String desired = computeDesiredTitle();
        String now = burpFrame.getTitle();
        if (force || !desired.equals(now)) {
            safeSetTitle(desired);
        }
    }

    // Return the final title to be shown
    private String computeDesiredTitle() {
        String base = (originalTitle == null) ? "" : originalTitle;
        if (isLicenseHidden) {
            base = LICENSED_TO_PATTERN.matcher(base).replaceFirst("");
        }
        return (customTitle != null) ? customTitle : base.trim();
    }

    // Update the frame title on Swing's Event Dispatch Thread.
    private void safeSetTitle(String title) {
        if (SwingUtilities.isEventDispatchThread()) {
            burpFrame.setTitle(title);
        } else {
            SwingUtilities.invokeLater(() -> burpFrame.setTitle(title));
        }
    }

    // Find and remove our menu by MENU_ID.
    private void removeMenuIfPresent() {
    Runnable r = () -> {
        JMenuBar bar = burpFrame.getJMenuBar();
        if (bar == null) return;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu m = bar.getMenu(i);
            if (m != null && MENU_ID.equals(m.getName())) {
                bar.remove(i);
                bar.revalidate();
                bar.repaint();
                api.logging().logToOutput("Removed existing Title menu.");
                break;
            }
        }
    };
    if (SwingUtilities.isEventDispatchThread()) r.run();
    else SwingUtilities.invokeLater(r);
    }
}
