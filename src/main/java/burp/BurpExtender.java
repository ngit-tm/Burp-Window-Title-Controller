package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.*;
import java.awt.*;
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
    private static final int MAX_TITLE_LENGTH = 120;

    // Menu item to display current title (read-only)
    private JMenuItem currentItem;
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

        // On unload, restore the original title
        api.extension().registerUnloadingHandler(() -> {
            safeSetTitle(originalTitle);
        });

        // logging
        api.logging().logToOutput("Burp Window Title Controller extension loaded.");
        api.logging().logToOutput("Original Title: " + originalTitle);
    }

    // Add Title menu
    // Remove an existing instance (if any) before adding a fresh one
    private void addTitleMenu(){
        JMenu titleMenu = new JMenu("Title");

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
            JTextField field = new JTextField(initial, 40);
            Dimension pref = field.getPreferredSize();
            field.setPreferredSize(new Dimension(480, pref.height));
            JLabel info = new JLabel("Enter window title (max " + MAX_TITLE_LENGTH + " characters).");

            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            content.add(info, BorderLayout.NORTH);
            content.add(field, BorderLayout.CENTER);

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            buttonsPanel.add(cancelButton);
            buttonsPanel.add(okButton);
            content.add(buttonsPanel, BorderLayout.SOUTH);

            final boolean[] accepted = {false};
            JDialog dialog = new JDialog(burpFrame, "Set Custom Title", true);
            okButton.addActionListener(evt -> {
                accepted[0] = true;
                dialog.dispose();
            });
            cancelButton.addActionListener(evt -> dialog.dispose());

            dialog.setContentPane(content);
            dialog.pack();
            dialog.setLocationRelativeTo(burpFrame);
            dialog.getRootPane().setDefaultButton(okButton);
            SwingUtilities.invokeLater(field::requestFocusInWindow);
            dialog.setVisible(true);

            if (accepted[0]) {
                String input = field.getText();
                if (input != null) {
                    input = input.trim();
                    if (input.isEmpty()) {
                        JOptionPane.showMessageDialog(burpFrame, "Title cannot be empty.");
                        return;
                    }
                    int length = input.codePointCount(0, input.length());
                    if (length > MAX_TITLE_LENGTH) {
                        JOptionPane.showMessageDialog(
                                burpFrame,
                                "Title must be " + MAX_TITLE_LENGTH + " characters or fewer."
                        );
                        return;
                    }
                    customTitle = input;
                    applyTitle(true);
                    updateCurrentMenuItem();
                    api.logging().logToOutput("Custom title = " + customTitle);
                }
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

        if (api.userInterface().menuBar().registerMenu(titleMenu) == null) {
            api.logging().logToError("Failed to register Title menu via Montoya API.");
        }
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

}
