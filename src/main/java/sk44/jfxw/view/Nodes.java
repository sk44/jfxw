/*
 *
 *
 *
 */
package sk44.jfxw.view;

import javafx.scene.Node;

/**
 *
 * @author sk
 */
public class Nodes {

    private Nodes() {
    }

    public static void addStyleClassTo(Node node, String className) {
        if (node.getStyleClass().contains(className)) {
            return;
        }
        node.getStyleClass().add(className);
    }

    public static void removeStyleClassFrom(Node node, String className) {
        if (node.getStyleClass().contains(className) == false) {
            return;
        }
        node.getStyleClass().remove(className);
    }
}
