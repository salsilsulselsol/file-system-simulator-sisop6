package filesystemsimulator.filesystem.core;

import filesystemsimulator.exceptions.FileSystemException;
import filesystemsimulator.filestructures.data.DirectoryTree;
import filesystemsimulator.filestructures.data.FileType;
import filesystemsimulator.utils.StringManipulator;

public class PathResolver {

    private final DirectoryTree tree;

    public static class PathResolutionResult {
        public final DirectoryTree.Node node;
        public final DirectoryTree.Node parentNode;
        public final String name;
        public final FileType type;
        public final boolean exists;

        public PathResolutionResult(DirectoryTree.Node node, DirectoryTree.Node parentNode, String name, FileType type, boolean exists) {
            this.node = node;
            this.parentNode = parentNode;
            this.name = name;
            this.type = type;
            this.exists = exists;
        }
    }

    public PathResolver(DirectoryTree tree) {
        this.tree = tree;
    }

    public PathResolutionResult resolve(String pathStr, DirectoryTree.Node startDirNodeIfRelative) throws FileSystemException {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            throw new FileSystemException("Path cannot be empty.");
        }

        DirectoryTree.Node currentIterationNode;
        String[] pathComponents;

        if (pathStr.equals("/")) {
            return new PathResolutionResult(tree.root, null, tree.root.name, tree.root.type, true);
        }

        if (pathStr.startsWith("/")) {
            currentIterationNode = tree.root;
            pathComponents = StringManipulator.split(pathStr.substring(1), '/');
        } else {
            currentIterationNode = startDirNodeIfRelative; // Mulai dari direktori saat ini untuk path relatif
            pathComponents = StringManipulator.split(pathStr, '/');
        }

        DirectoryTree.Node parentOfTarget = null;
        if (pathStr.startsWith("/")) {
            parentOfTarget = (pathComponents.length > 0) ? tree.root : null;
        } else {
            parentOfTarget = startDirNodeIfRelative; // Awalnya, parent adalah startDir itu sendiri jika path hanya satu komponen
        }


        String targetName = "";
        boolean found = true;

        if (pathComponents.length == 0 && !pathStr.isEmpty()) { // Kasus "a" tanpa slash
            pathComponents = new String[]{pathStr};
        } else if (pathComponents.length == 0 && pathStr.isEmpty()) { // String kosong setelah split "/"
            return new PathResolutionResult(currentIterationNode, currentIterationNode.parent, currentIterationNode.name, currentIterationNode.type, true);
        }


        for (int i = 0; i < pathComponents.length; i++) {
            String component = pathComponents[i];

            if (component.isEmpty()) {
                if (i == pathComponents.length - 1) { // Trailing slash
                    targetName = currentIterationNode.name;
                    // parentOfTarget sudah benar dari iterasi sebelumnya atau inisialisasi
                    break;
                }
                continue; // Abaikan empty component di tengah
            }

            targetName = component;
            parentOfTarget = currentIterationNode;

            if (component.equals("..")) {
                if (currentIterationNode != tree.root && currentIterationNode.parent != null) {
                    currentIterationNode = currentIterationNode.parent;
                } else {
                    // Di root, atau parent null (seharusnya tidak terjadi jika bukan root)
                    // Jika ".." adalah bagian dari path yg mungkin valid di akhirnya, jangan error di sini
                }
                if (i == pathComponents.length -1) targetName = currentIterationNode.name; // Nama target adalah parent yang baru dicapai
            } else if (component.equals(".")) {
                // Tetap di direktori saat ini
                if (i == pathComponents.length -1) targetName = currentIterationNode.name;
            } else {
                DirectoryTree.Node childNode = null;
                if (currentIterationNode.childNodes != null && currentIterationNode.type == FileType.DIRECTORY) {
                    for (Object cn : currentIterationNode.childNodes.toArray()) {
                        DirectoryTree.Node tempNode = (DirectoryTree.Node) cn;
                        if (tempNode.name.equals(component)) {
                            childNode = tempNode;
                            break;
                        }
                    }
                }

                if (childNode != null) {
                    currentIterationNode = childNode; // Pindah ke anak
                    // targetName sudah di-set ke component
                    if (childNode.type == FileType.FILE && i < pathComponents.length - 1) {
                        found = false; // File di tengah path tidak valid
                        targetName = component; // Nama item yang gagal
                        // parentOfTarget adalah currentIterationNode sebelum gagal
                        break;
                    }
                } else {
                    found = false; // Anak tidak ditemukan
                    targetName = component; // Nama item yang gagal ditemukan
                    // parentOfTarget adalah currentIterationNode sebelum gagal
                    break;
                }
            }
        }

        if (found) {
            return new PathResolutionResult(currentIterationNode, currentIterationNode.parent, currentIterationNode.name, currentIterationNode.type, true);
        } else {
            // Jika tidak ditemukan, parentOfTarget adalah direktori terakhir yang valid,
            // dan targetName adalah nama yang gagal ditemukan/diresolusi.
            return new PathResolutionResult(null, parentOfTarget, targetName, null, false);
        }
    }
}
