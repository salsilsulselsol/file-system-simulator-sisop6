package filesystemsimulator; // Paket sejajar dengan Main.java

import filesystemsimulator.filestructures.data.DirectoryTree; // Impor diperlukan
import filesystemsimulator.filestructures.data.FileType; // Impor diperlukan

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Vector;

public class FileSystemTreeModel implements TreeModel {

    private DirectoryTree.Node rootNode;
    private Vector<TreeModelListener> listeners = new Vector<>();

    public FileSystemTreeModel(DirectoryTree.Node rootNode) {
        this.rootNode = rootNode;
    }

    public void updateTreeStructure(DirectoryTree.Node newRootNode) {
        this.rootNode = newRootNode;
        // Memberi tahu listener bahwa seluruh struktur di bawah root mungkin telah berubah.
        // TreePath ke root adalah new Object[]{this.rootNode}
        // Jika rootNode itu sendiri adalah null (misalnya, file system belum siap), JTree akan kosong.
        if (this.rootNode != null) {
            fireTreeStructureChanged(this, new Object[]{this.rootNode}, null, null);
        } else {
            // Jika root menjadi null, kirim event dengan source saja atau path kosong jika didukung
            fireTreeStructureChanged(this, new Object[]{}, null, null);
        }
    }


    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == null) return null;
        DirectoryTree.Node parentNode = (DirectoryTree.Node) parent;
        if (parentNode.childNodes != null) {
            // LinkedList tidak memiliki get(index) yang efisien.
            // Mengonversi ke array untuk akses indeks.
            Object[] children = parentNode.childNodes.toArray(); //
            if (index >= 0 && index < children.length) {
                return children[index];
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == null) return 0;
        DirectoryTree.Node parentNode = (DirectoryTree.Node) parent;
        if (parentNode.childNodes != null) {
            return parentNode.childNodes.toArray().length; //
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node == null) return true; // Node null dianggap leaf
        DirectoryTree.Node fsNode = (DirectoryTree.Node) node;
        if (fsNode.type == FileType.FILE) { //
            return true;
        }
        // Direktori dianggap leaf jika tidak punya child
        return fsNode.childNodes == null || fsNode.childNodes.toArray().length == 0; //
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Tidak diimplementasikan untuk simulator ini (tidak ada edit nama via tree)
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) return -1;
        DirectoryTree.Node parentNode = (DirectoryTree.Node) parent;
        DirectoryTree.Node childNode = (DirectoryTree.Node) child;
        if (parentNode.childNodes != null) {
            Object[] children = parentNode.childNodes.toArray(); //
            for (int i = 0; i < children.length; i++) {
                if (children[i].equals(childNode)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.addElement(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.removeElement(l);
    }

    // Metode untuk memberi tahu listener tentang perubahan
    // source adalah model ini, path adalah path ke node yang berubah
    protected void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
        TreeModelEvent e = new TreeModelEvent(source, path, childIndices, children);
        for (TreeModelListener tml : listeners) {
            tml.treeStructureChanged(e);
        }
    }
}
