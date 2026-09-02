package eva.evangelion.view.UIElements;

import javafx.scene.Node;
import java.util.*;

/**
 * Manages the visibility of UI elements associated with ToggleUIButton.
 * Handles groups and parent-child relationships for cascading closes.
 */
public class UIManager {
    private static UIManager instance;

    private final Map<String, List<ToggleUIButton>> groups = new HashMap<>();
    private final Map<ToggleUIButton, List<ToggleUIButton>> childrenMap = new HashMap<>();

    private UIManager() {}

    public static UIManager getInstance() {
        if (instance == null) instance = new UIManager();
        return instance;
    }

    /**
     * Registers a button and its UI node with a group.
     */
    public void register(ToggleUIButton button, String groupId, Node uiNode) {
        groups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(button);
        button.setUIStateManager(this);
    }

    /**
     * Defines a parent-child relationship between two buttons.
     * When the parent's UI node is hidden, all children's nodes will be hidden too.
     */
    public void addChild(ToggleUIButton parent, ToggleUIButton child) {
        childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
        child.setParentButton(parent);
    }

    /**
     * Shows the UI node of the given button and hides all other buttons in the same group.
     * Also ensures that any currently visible node that is a descendant of a hidden node
     * is closed recursively.
     */
    public void showButtonUI(ToggleUIButton button) {
        String groupId = button.getGroupId();
        List<ToggleUIButton> group = groups.get(groupId);
        if (group == null) return;

        // Hide all other buttons' UI nodes in the same group
        for (ToggleUIButton other : group) {
            if (other != button && other.isUIVisible()) {
                hideUIAndDescendants(other);
            }
        }

        // Show the selected button's UI node
        button.showUI();
    }

    /**
     * Hides a button's UI node and recursively hides all its registered children.
     */
    public void hideUIAndDescendants(ToggleUIButton button) {
        button.hideUISilently();           // no notification → no recursion
        List<ToggleUIButton> children = childrenMap.get(button);
        if (children != null) {
            for (ToggleUIButton child : children) {
                hideUIAndDescendants(child);
            }
        }
    }

    /**
     * Called when a button's UI node is hidden by external means (e.g., a close button inside the node).
     * This ensures the manager's state stays consistent.
     */
    public void notifyUIVisible(ToggleUIButton button, boolean visible) {
        if (!visible) {
            // If this button's UI becomes hidden externally, also hide all descendants
            hideUIAndDescendants(button);
        }
    }
}