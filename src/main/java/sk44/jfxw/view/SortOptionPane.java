/*
 *
 *
 *
 */
package sk44.jfxw.view;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import lombok.Getter;
import lombok.Setter;
import sk44.jfxw.model.PathSortType;
import sk44.jfxw.model.message.Message;

/**
 *
 * @author sk
 */
public class SortOptionPane extends FlowPane {

    @Getter
    @Setter
    private static class SortTypeLabel extends Label {

        private boolean selected;
        private final PathSortType sortType;

        public SortTypeLabel(PathSortType sortType, boolean selected, ReadOnlyDoubleProperty widthProperty) {
            super(sortType.getDisplayName());
            this.sortType = sortType;
            this.selected = selected;
            this.prefWidthProperty().bind(widthProperty);
            this.updateStyle();
        }

        private void updateStyle() {
            // TODO 専用の css クラスをつくる
            if (this.selected) {
                getStyleClass().add("currentRow");
            } else {
                getStyleClass().remove("currentRow");
            }
        }

        public void updateSelected(boolean selected) {
            this.selected = selected;
            updateStyle();
        }
    }

    private List<SortTypeLabel> sortTypes;
    private int currentIndex;

    public SortOptionPane(PathSortType selectedSortType) {
        // TODO スタイルが効かない
//        this.getStylesheets().add(getClass().getResource("/styles/Styles.css").toExternalForm());
        this.getStylesheets().add("/styles/Styles.css");
//        this.getStyleClass().add("test");
        int index = 0;
        this.sortTypes = new ArrayList<>(PathSortType.values().length);
        for (PathSortType sortType : PathSortType.values()) {
            boolean selected = selectedSortType == sortType;
            this.sortTypes.add(new SortTypeLabel(sortType, selected, this.widthProperty()));
            if (selected) {
                this.currentIndex = index;
            }
            index++;
        }
        getChildren().addAll(sortTypes);
    }

    public void handleKeyEvent(KeyEvent event) {
        // TODO
        switch (event.getCode()) {
            case DOWN:
                updateSelected(false);
                break;
            case UP:
                updateSelected(true);
                break;
            default:
                break;
        }
    }

    private void updateSelected(boolean up) {
        int nextIndex = up ? this.currentIndex - 1 : this.currentIndex + 1;
        if (nextIndex < 0 || this.sortTypes.size() <= nextIndex) {
            return;
        }
        this.currentIndex = nextIndex;
        int index = 0;
        for (SortTypeLabel label : this.sortTypes) {
            if (this.currentIndex == index) {
                label.updateSelected(true);
                Message.debug("selectedindex: " + index);
            } else {
                label.updateSelected(false);
            }
            index++;
        }
    }

    public PathSortType getSelectedSortType() {
        return this.sortTypes.stream().filter(label -> label.isSelected()).findAny().get().getSortType();
    }

}
