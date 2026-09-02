package eva.evangelion.view.UIElements;

import javafx.event.ActionEvent;
import javafx.scene.Node;

/**
 * A button that toggles the visibility of an associated UI element.
 * Within a group, only one button's UI element is visible at a time.
 * Supports nesting: children are automatically closed when their parent is closed.
 */
public class ToggleUIButton extends BetterButton {

    private final String groupId;
    private final Node uiNode;
    private UIManager manager;
    private ToggleUIButton parentButton;

    public ToggleUIButton(String groupId, Node uiNode, String text) {
        super(text);
        this.groupId = groupId;
        this.uiNode = uiNode;
        if (uiNode != null) uiNode.setVisible(false);
        setOnAction(this::handleAction);
    }

    private void handleAction(ActionEvent e) {
        if (manager != null) {
            manager.showButtonUI(this);
        } else {
            boolean newState = !uiNode.isVisible();
            uiNode.setVisible(newState);
        }
    }

    // ---------- Package-private methods for UIManager ----------
    void setUIStateManager(UIManager manager) {
        this.manager = manager;
    }

    void setParentButton(ToggleUIButton parent) {
        this.parentButton = parent;
    }

    String getGroupId() {
        return groupId;
    }

    boolean isUIVisible() {
        return uiNode != null && uiNode.isVisible();
    }

    void showUI() {
        if (uiNode != null) {
            uiNode.setVisible(true);
            if (manager != null) manager.notifyUIVisible(this, true);
        }
    }

    void hideUISilently() {
        if (uiNode != null) {
            uiNode.setVisible(false);
        }
    }

    void hideUI() {
        if (uiNode != null) {
            hideUISilently();
            if (manager != null) {
                manager.notifyUIVisible(this, false);
            }
        }
    }

    public Node getUiNode() {
        return uiNode;
    }

    public ToggleUIButton getParentButton() {
        return parentButton;
    }
}