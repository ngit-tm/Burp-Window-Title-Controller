package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import javax.swing.*;
import java.util.regex.Pattern;

public class BurpExtender implements BurpExtension {
    private MontoyaApi api;
    private JFrame burpFrame;

    // タイトル制御で使う状態
    // State for title control
    private String originalTitle;
    private String customTitle = null;
    private boolean isLicenseHidden = false;

    // 「 - licensed to ...」にマッチする正規表現
    // Regex pattern to match " - licensed to ..."
    private static final Pattern LICENSED_TO_PATTERN =
            Pattern.compile("\\s-\\slicensed\\sto\\s.*", Pattern.CASE_INSENSITIVE);

    // メニューに現在のタイトルを表示する項目（読み取り専用）
    // Menu item to display current title (read-only)
    private JMenuItem currentItem;

    @Override
    public void initialize(MontoyaApi api) {
        // 拡張の初期化。必要な参照を取得し、メニューを組み立てる。
        // Initialize extension: get references and build menu.
        this.api = api;
        api.extension().setName("Burp Window Title Controller");

        this.burpFrame = (JFrame) api.userInterface().swingUtils().suiteFrame();
        this.originalTitle = burpFrame.getTitle();

        addTitleMenu();

        // 初期状態を反映し、表示も更新
        // Apply initial state and update display
        applyTitle(true);
        updateCurrentMenuItem();

        // アンロード時は元のタイトルへ戻す
        // Restore original title on unload
        api.extension().registerUnloadingHandler(() -> safeSetTitle(originalTitle));

        // ログ出力
        // logging
        api.logging().logToOutput("Burp Window Title Controller extension loaded.");
        api.logging().logToOutput("Original Title: " + originalTitle);
    }

    // Titleメニューを追加する。
    // Add Title menu
    private void addTitleMenu(){
        JMenuBar bar = burpFrame.getJMenuBar();
        if(bar == null){
            api.logging().logToError("Error: MenuBar not found.");
            return;
        }
        JMenu titleMenu = new JMenu("Title");

        // 現在表示されているウィンドウのタイトル（読み取り専用の項目
        // Current window title (read only item)
        currentItem = new JMenuItem();
        currentItem.setEnabled(false);
        titleMenu.add(currentItem);

        titleMenu.addSeparator();

        // ライセンス表記の表示/非表示を切り替える
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

        // 任意タイトルの設定
        // Set custom title
        JMenuItem setCustom = new JMenuItem("Set Custom Title...");
        setCustom.addActionListener(e -> {
            // 初期値はカスタムがあればそれにする
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

        // 任意タイトルの解除（Burpの元タイトルに戻す）
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

    // メニュー側の「Current: ...」表示を最新のタイトルで更新する
    // Update "Current: ..." menu item to reflect current title
    private void updateCurrentMenuItem() {
        String shown = burpFrame.getTitle();
        currentItem.setText("Current: " + (shown == null ? "" : shown));
    }

    // いま表示すべきタイトルを適用する
    // Compute and apply the desired title if changed
    private void applyTitle(boolean force) {
        String desired = computeDesiredTitle();
        String now = burpFrame.getTitle();
        if (force || !desired.equals(now)) {
            safeSetTitle(desired);
        }
    }

    // 最終的に見せたいタイトルを返す
    // Return the final title to be shown
    private String computeDesiredTitle() {
        String base = (originalTitle == null) ? "" : originalTitle;
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
