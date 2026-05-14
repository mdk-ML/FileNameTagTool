package cn.mdkml.filenametagtool.component;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * 标签列表拖拽传输处理器，支持在 JList 内部拖拽重排序。
 */
public class TagListTransferHandler extends TransferHandler {
    private final DefaultListModel<String> model;
    private int dragIndex = -1;

    public TagListTransferHandler(DefaultListModel<String> model) {
        this.model = model;
    }

    @Override
    protected Transferable createTransferable(JComponent component) {
        JList<?> list = (JList<?>) component;
        dragIndex = list.getSelectedIndex();
        String value = list.getSelectedValue().toString();
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.stringFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.stringFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return value;
            }
        };
    }

    @Override
    public int getSourceActions(JComponent component) {
        return MOVE;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        int dropIndex = dropLocation.getIndex();

        try {
            String draggedItem = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            if (dragIndex >= 0 && dragIndex < model.size()) {
                model.remove(dragIndex);
                if (dropIndex > dragIndex) {
                    dropIndex--;
                }
            }
            model.add(dropIndex, draggedItem);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    protected void exportDone(JComponent component, Transferable data, int action) {
        dragIndex = -1;
    }
}
