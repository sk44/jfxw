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
}
