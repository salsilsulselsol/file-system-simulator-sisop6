package filesystemsimulator.filesystem;

import filesystemsimulator.datastructures.LinkedList;
import filesystemsimulator.datastructures.StringAppender;
import filesystemsimulator.exceptions.FileSystemException;
import filesystemsimulator.filestructures.container.DataBlock;
import filesystemsimulator.filestructures.container.IndexNode;
import filesystemsimulator.filestructures.container.SuperBlock;
import filesystemsimulator.filestructures.data.DirectoryTree;
import filesystemsimulator.filestructures.data.FileType;
import filesystemsimulator.filesystem.core.BlockAllocator;
import filesystemsimulator.filesystem.core.DataBlockManager;
import filesystemsimulator.filesystem.core.InodeManager;
import filesystemsimulator.filesystem.core.PathResolver;
import filesystemsimulator.utils.ArrayManipulator;
import filesystemsimulator.utils.StringManipulator;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystem {

    public static final byte BYTE_MAX = (byte) 0xff; // 255

    private RandomAccessFile containerFile;
    public final DirectoryTree tree;
    private final SuperBlock superBlock;

    // Core Modules
    private final BlockAllocator blockAllocator;
    private final InodeManager inodeManager;
    private final DataBlockManager dataBlockManager;
    private final PathResolver pathResolver;

    public FileSystem(String systemPath, long size) throws FileSystemException {
        this.superBlock = new SuperBlock();

        try {
            this.containerFile = new RandomAccessFile(systemPath, "rw");

            this.blockAllocator = new BlockAllocator(containerFile, superBlock);
            this.inodeManager = new InodeManager(containerFile, superBlock);
            this.dataBlockManager = new DataBlockManager(containerFile, superBlock, blockAllocator);

            initializeFileSystemStructure(size);

            this.tree = new DirectoryTree("root", 0); // Inode 0 untuk root
            this.pathResolver = new PathResolver(tree);

            IndexNode rootInodeFromDisk = inodeManager.readNode(0);
            if (!tree.root.name.equals(rootInodeFromDisk.getNameString())) {
                System.err.println("Warning: DirectoryTree root name ('" + tree.root.name +
                    "') and Inode 0 name ('" + rootInodeFromDisk.getNameString() +
                    "') mismatch after init. Synchronizing tree name from disk.");
                tree.root.name = rootInodeFromDisk.getNameString();
            }
            tree.root.inodeNumber = 0;

        } catch (IOException e) {
            throw new FileSystemException("An i/o error occurred while initializing the file system: " + e.getMessage(), e);
        }
    }

    private void initializeFileSystemStructure(long size) throws IOException, FileSystemException {
        superBlock.initialize(size);

        containerFile.setLength(0);
        containerFile.setLength((long) superBlock.getTotalBlockCount() * superBlock.getBlockSize());

        containerFile.seek(0);
        superBlock.write(containerFile);

        blockAllocator.initializeBitmaps();
        initializeRootInodeOnDisk();
    }

    private void initializeRootInodeOnDisk() throws IOException, FileSystemException {
        int rootInodeNum = blockAllocator.allocateInode();
        if (rootInodeNum != 0) {
            throw new FileSystemException("Critical: Failed to allocate inode 0 for root. Allocated: " + rootInodeNum);
        }

        IndexNode rootDiskInode = new IndexNode();
        rootDiskInode.setName("root");
        rootDiskInode.setType(FileType.DIRECTORY);
        rootDiskInode.setParentInode(rootInodeNum); // Parent root adalah dirinya sendiri
        rootDiskInode.setSize(0);
        inodeManager.writeNode(rootDiskInode, rootInodeNum);
    }

    public String getCurrentPath() {
        return tree.getPath();
    }

    public void changeDir(String pathStr) throws FileSystemException {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            throw new FileSystemException("Path cannot be empty for cd.");
        }
        if ("/".equals(pathStr)) {
            tree.goToRoot();
            return;
        }
        if ("..".equals(pathStr)) {
            tree.goToParent();
            return;
        }

        PathResolver.PathResolutionResult result = pathResolver.resolve(pathStr, tree.getCurrentDir());

        if (result.exists && result.node != null && result.node.type == FileType.DIRECTORY) {
            if (pathStr.startsWith("/")) {
                tree.goToRoot();
                if (pathStr.equals("/")) return;
                String[] sequence = StringManipulator.split(pathStr.substring(1), '/');
                tree.goTo(sequence);
            } else {
                String[] sequence = StringManipulator.split(pathStr, '/');
                tree.goTo(sequence);
            }
        } else {
            throw new FileSystemException("Path not found or is not a directory: " + pathStr);
        }
    }


    public void makeFile(String name, FileType type) throws FileSystemException {
        if (name == null || name.trim().isEmpty()) {
            throw new FileSystemException("File/Directory name cannot be empty.");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new FileSystemException("File/Directory name cannot contain path separators.");
        }
        if (name.equals(".") || name.equals("..")) {
            throw new FileSystemException("File/Directory name cannot be '.' or '..'.");
        }


        if (tree.fileExists(name) || tree.dirExists(name)) {
            throw new FileSystemException("An item named '" + name + "' already exists in " + tree.getCurrentDir().name);
        }

        try {
            int newInodeNum = blockAllocator.allocateInode();
            int parentDirInodeNum = tree.getCurrentDir().inodeNumber;

            IndexNode newFileInode = new IndexNode();
            newFileInode.setName(name);
            newFileInode.setType(type);
            newFileInode.setParentInode(parentDirInodeNum);
            if (type == FileType.DIRECTORY) {
                newFileInode.setSize(0);
            }
            inodeManager.writeNode(newFileInode, newInodeNum);

            IndexNode parentDirInode = inodeManager.readNode(parentDirInodeNum);
            parentDirInode.addDirectBlock(newInodeNum);
            inodeManager.writeNode(parentDirInode, parentDirInodeNum);

            tree.addChild(name, newInodeNum, type);
        } catch (IOException e) {
            throw new FileSystemException("I/O error creating '" + name + "': " + e.getMessage(), e);
        }
    }

    public void listCurrentDir() {
        DirectoryTree.Node current = tree.getCurrentDir();
        System.out.print(current.name + "$ ls: ");
        if (current != tree.root && current.parent != null) {
            System.out.print("..  ");
        }
        if (current.childNodes != null) {
            Object[] children = current.childNodes.toArray();
            for (Object childObj : children) {
                DirectoryTree.Node childNode = (DirectoryTree.Node) childObj;
                System.out.print(childNode.name + (childNode.type == FileType.DIRECTORY ? "/" : "") + "  ");
            }
        }
        System.out.println();
    }

    public void removeDir() throws FileSystemException {
        DirectoryTree.Node dirToRemoveNode = tree.getCurrentDir();
        if (dirToRemoveNode == tree.root) {
            throw new FileSystemException("Cannot remove the root directory.");
        }

        try {
            IndexNode dirInode = inodeManager.readNode(dirToRemoveNode.inodeNumber);
            if (!dirInode.isEmpty()) {
                throw new FileSystemException("Directory '" + dirToRemoveNode.name + "' is not empty.");
            }

            int parentOfDirToRemoveInodeNum = dirInode.getParent();
            IndexNode parentInode = inodeManager.readNode(parentOfDirToRemoveInodeNum);
            parentInode.removeDirectBlock(dirToRemoveNode.inodeNumber);
            inodeManager.writeNode(parentInode, parentOfDirToRemoveInodeNum);

            blockAllocator.freeInode(dirToRemoveNode.inodeNumber);
            tree.removeCurrent();
        } catch (IOException e) {
            throw new FileSystemException("I/O error removing directory '" + dirToRemoveNode.name + "': " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileName) throws FileSystemException {
        DirectoryTree.Node fileNode = tree.getChild(fileName);
        if (fileNode == null || fileNode.type == FileType.DIRECTORY) {
            throw new FileSystemException("File '" + fileName + "' not found or is a directory in the current path.");
        }
        try {
            IndexNode fileInode = inodeManager.readNode(fileNode.inodeNumber);
            for (int dataBlockNum : fileInode.getAllocatedDirectBlocks()) {
                dataBlockManager.wipeBlock(dataBlockNum);
            }

            int parentInodeNum = fileInode.getParent();
            IndexNode parentInode = inodeManager.readNode(parentInodeNum);
            parentInode.removeDirectBlock(fileNode.inodeNumber);
            inodeManager.writeNode(parentInode, parentInodeNum);

            blockAllocator.freeInode(fileNode.inodeNumber);
            tree.removeChild(fileName);
        } catch (IOException e) {
            throw new FileSystemException("I/O error deleting file '" + fileName + "': " + e.getMessage(), e);
        }
    }

    public void writeToFile(String fileName, byte[] bytes) throws FileSystemException {
        if (tree.dirExists(fileName)) {
            throw new FileSystemException("Cannot write to '" + fileName + "', it is a directory.");
        }
        if (tree.fileExists(fileName)) {
            deleteFile(fileName);
        }

        makeFile(fileName, FileType.FILE);
        DirectoryTree.Node fileNode = tree.getChild(fileName);
        if (fileNode == null) {
            throw new FileSystemException("Internal error: File node not found after creation for '" + fileName + "'.");
        }

        try {
            IndexNode fileInode = inodeManager.readNode(fileNode.inodeNumber);
            int neededDataBlocks = calculateNeededBlocks(bytes.length);
            int totalBytesWritten = 0;

            for (int i = 0; i < neededDataBlocks; i++) {
                int start = i * superBlock.getBlockSize();
                int length = Math.min(superBlock.getBlockSize(), bytes.length - start);
                byte[] segment = ArrayManipulator.subArray(bytes, start, start + length);

                int newDataBlockNum = blockAllocator.allocateDataBlock();
                DataBlock dataBlock = new DataBlock();
                dataBlock.setBytes(segment);
                dataBlockManager.writeBlock(dataBlock, newDataBlockNum);

                fileInode.addDirectBlock(newDataBlockNum);
                totalBytesWritten += length;
            }
            fileInode.setSize(totalBytesWritten);
            inodeManager.writeNode(fileInode, fileNode.inodeNumber);

        } catch (IOException e) {
            try { if (tree.fileExists(fileName)) deleteFile(fileName); } catch (FileSystemException ignored) {}
            throw new FileSystemException("I/O error writing to file '" + fileName + "': " + e.getMessage(), e);
        }
    }

    public void appendToFile(String fileName, byte[] bytesToAppend) throws FileSystemException {
        if (bytesToAppend == null || bytesToAppend.length == 0) return;

        DirectoryTree.Node fileNode = tree.getChild(fileName);
        if (fileNode == null) {
            writeToFile(fileName, bytesToAppend);
            return;
        }
        if (fileNode.type == FileType.DIRECTORY) {
            throw new FileSystemException("Cannot append to '" + fileName + "', it is a directory.");
        }

        try {
            IndexNode fileInode = inodeManager.readNode(fileNode.inodeNumber);
            int currentFileSize = fileInode.getSize();
            int bytesAppendedThisOperation = 0;

            int[] currentDataBlocks = fileInode.getAllocatedDirectBlocks();
            if (currentDataBlocks.length > 0) {
                int lastDataBlockNum = currentDataBlocks[currentDataBlocks.length - 1];
                DataBlock lastBlock = dataBlockManager.readBlock(lastDataBlockNum);
                int bytesInLastBlock = currentFileSize % superBlock.getBlockSize();
                if (bytesInLastBlock == 0 && currentFileSize > 0) bytesInLastBlock = superBlock.getBlockSize();

                int freeSpaceInLastBlock = superBlock.getBlockSize() - bytesInLastBlock;
                if (freeSpaceInLastBlock > 0) {
                    int countToAppendToLast = Math.min(bytesToAppend.length, freeSpaceInLastBlock);
                    byte[] segmentForLast = ArrayManipulator.subArray(bytesToAppend, 0, countToAppendToLast);
                    lastBlock.appendBytes(segmentForLast);
                    dataBlockManager.writeBlock(lastBlock, lastDataBlockNum);
                    currentFileSize += countToAppendToLast;
                    bytesAppendedThisOperation += countToAppendToLast;
                }
            }

            if (bytesAppendedThisOperation < bytesToAppend.length) {
                byte[] remainingBytes = ArrayManipulator.subArray(bytesToAppend, bytesAppendedThisOperation, bytesToAppend.length);
                int neededNewBlocks = calculateNeededBlocks(remainingBytes.length);

                for (int i = 0; i < neededNewBlocks; i++) {
                    int start = i * superBlock.getBlockSize();
                    int length = Math.min(superBlock.getBlockSize(), remainingBytes.length - start);
                    byte[] segment = ArrayManipulator.subArray(remainingBytes, start, start + length);

                    int newDataBlockNum = blockAllocator.allocateDataBlock();
                    DataBlock newBlock = new DataBlock();
                    newBlock.setBytes(segment);
                    dataBlockManager.writeBlock(newBlock, newDataBlockNum);
                    fileInode.addDirectBlock(newDataBlockNum);
                    currentFileSize += length;
                }
            }
            fileInode.setSize(currentFileSize);
            inodeManager.writeNode(fileInode, fileNode.inodeNumber);

        } catch (IOException e) {
            throw new FileSystemException("I/O error appending to file '" + fileName + "': " + e.getMessage(), e);
        }
    }


    public void printFile(String fileName) throws FileSystemException {
        DirectoryTree.Node fileNode = tree.getChild(fileName);
        if (fileNode == null || fileNode.type == FileType.DIRECTORY) {
            throw new FileSystemException("File '" + fileName + "' not found or is a directory in current path.");
        }
        try {
            IndexNode fileInode = inodeManager.readNode(fileNode.inodeNumber);
            StringAppender content = new StringAppender();
            int fileSize = fileInode.getSize();
            int totalBytesRead = 0;

            for (int dataBlockNum : fileInode.getAllocatedDirectBlocks()) {
                if (totalBytesRead >= fileSize) break;
                DataBlock dataBlock = dataBlockManager.readBlock(dataBlockNum);
                int bytesToReadFromThisBlock = Math.min(superBlock.getBlockSize(), fileSize - totalBytesRead);

                byte[] blockBytes = dataBlock.getBytes();
                byte[] relevantBytes = new byte[bytesToReadFromThisBlock];
                System.arraycopy(blockBytes, 0, relevantBytes, 0, bytesToReadFromThisBlock);
                content.append(new String(relevantBytes));

                totalBytesRead += bytesToReadFromThisBlock;
            }
            System.out.print(content.toString());
            if(totalBytesRead > 0 && !content.toString().endsWith("\n")) System.out.println();


        } catch (IOException e) {
            throw new FileSystemException("I/O error printing file '" + fileName + "': " + e.getMessage(), e);
        }
    }

    public void copyFile(String sourceName, String destinationName) throws FileSystemException {
        DirectoryTree.Node sourceNode = tree.getChild(sourceName);
        if (sourceNode == null || sourceNode.type == FileType.DIRECTORY) {
            throw new FileSystemException("Source file '" + sourceName + "' not found or is a directory.");
        }
        if (tree.fileExists(destinationName) || tree.dirExists(destinationName)) {
            throw new FileSystemException("Destination '" + destinationName + "' already exists.");
        }

        makeFile(destinationName, FileType.FILE);
        DirectoryTree.Node destNode = tree.getChild(destinationName);
        if (destNode == null) throw new FileSystemException("Failed to create destination file '"+destinationName+"' for copy.");


        try {
            IndexNode sourceInode = inodeManager.readNode(sourceNode.inodeNumber);
            IndexNode destInode = inodeManager.readNode(destNode.inodeNumber);
            int totalBytesCopied = 0;

            for (int sourceDataBlockNum : sourceInode.getAllocatedDirectBlocks()) {
                DataBlock sourceBlockData = dataBlockManager.readBlock(sourceDataBlockNum);

                int newDataBlockNum = blockAllocator.allocateDataBlock();
                DataBlock destBlockData = new DataBlock();
                destBlockData.setBytes(sourceBlockData.getBytes());
                dataBlockManager.writeBlock(destBlockData, newDataBlockNum);

                destInode.addDirectBlock(newDataBlockNum);

                if (totalBytesCopied + superBlock.getBlockSize() <= sourceInode.getSize()){
                    totalBytesCopied += superBlock.getBlockSize();
                } else {
                    totalBytesCopied += (sourceInode.getSize() % superBlock.getBlockSize());
                }

            }
            destInode.setSize(sourceInode.getSize());
            inodeManager.writeNode(destInode, destNode.inodeNumber);

        } catch (IOException e) {
            try { if (tree.fileExists(destinationName)) deleteFile(destinationName); } catch (FileSystemException ignored) {}
            throw new FileSystemException("I/O error copying file '" + sourceName + "' to '" + destinationName + "': " + e.getMessage(), e);
        }
    }


    public void moveItem(String sourcePathStr, String destPathStr) throws FileSystemException {
        PathResolver.PathResolutionResult sourceRes = pathResolver.resolve(sourcePathStr, tree.getCurrentDir());

        if (!sourceRes.exists || sourceRes.node == null) {
            throw new FileSystemException("Source path not found: " + sourcePathStr);
        }
        if (sourceRes.node == tree.root) {
            throw new FileSystemException("Cannot move the root directory.");
        }

        DirectoryTree.Node sourceNode = sourceRes.node;
        DirectoryTree.Node originalParentNodeInTree = sourceRes.parentNode;
        if (originalParentNodeInTree == null && sourceNode != tree.root) {
            if (sourceNode.parent == tree.root) originalParentNodeInTree = tree.root;
            else throw new FileSystemException("Cannot determine original parent for source: " + sourcePathStr);
        }
        if (originalParentNodeInTree == null) {
            throw new FileSystemException("Critical: Original parent node is null for non-root source.");
        }


        PathResolver.PathResolutionResult destRes = pathResolver.resolve(destPathStr, tree.getCurrentDir());
        DirectoryTree.Node finalDestParentNodeInTree;
        String newItemName;

        if (destRes.exists && destRes.node != null && destRes.node.type == FileType.DIRECTORY) {
            finalDestParentNodeInTree = destRes.node;
            newItemName = sourceNode.name;
        } else if (destRes.exists && destRes.node != null && destRes.node.type == FileType.FILE) {
            throw new FileSystemException("Destination path is an existing file, cannot overwrite with move: " + destPathStr);
        } else {
            if (destRes.parentNode == null || destRes.parentNode.type != FileType.DIRECTORY) {
                throw new FileSystemException("Invalid destination parent directory for: " + destPathStr + ". Parent found: " + (destRes.parentNode != null ? destRes.parentNode.name : "null"));
            }
            finalDestParentNodeInTree = destRes.parentNode;
            newItemName = destRes.name;
            if (newItemName == null || newItemName.trim().isEmpty() || destPathStr.trim().endsWith("/")) {
                newItemName = sourceNode.name;
            }
        }

        if (newItemName.length() >= IndexNode.MAX_NAME_SIZE) {
            throw new FileSystemException("New name '"+ newItemName +"' is too long (max " + (IndexNode.MAX_NAME_SIZE -1) + " chars).");
        }


        if (finalDestParentNodeInTree.childNodes != null) {
            for (Object childObj : finalDestParentNodeInTree.childNodes.toArray()) {
                DirectoryTree.Node child = (DirectoryTree.Node) childObj;
                if (child.name.equals(newItemName) && child.inodeNumber != sourceNode.inodeNumber) {
                    throw new FileSystemException("An item named '" + newItemName + "' already exists in '" + finalDestParentNodeInTree.name + "'.");
                }
            }
        }

        if (sourceNode.type == FileType.DIRECTORY) {
            DirectoryTree.Node tempParent = finalDestParentNodeInTree;
            while (tempParent != null) {
                if (tempParent == sourceNode) {
                    throw new FileSystemException("Cannot move a directory into itself or one of its subdirectories.");
                }
                tempParent = tempParent.parent;
            }
        }

        try {
            IndexNode originalParentInodeObj = inodeManager.readNode(originalParentNodeInTree.inodeNumber);
            originalParentInodeObj.removeDirectBlock(sourceNode.inodeNumber);
            inodeManager.writeNode(originalParentInodeObj, originalParentNodeInTree.inodeNumber);

            IndexNode sourceItemInodeObj = inodeManager.readNode(sourceNode.inodeNumber);
            sourceItemInodeObj.setParentInode(finalDestParentNodeInTree.inodeNumber);
            if (!sourceItemInodeObj.getNameString().equals(newItemName)) {
                sourceItemInodeObj.setName(newItemName);
            }
            inodeManager.writeNode(sourceItemInodeObj, sourceNode.inodeNumber);

            if (originalParentNodeInTree.inodeNumber != finalDestParentNodeInTree.inodeNumber) {
                IndexNode destParentInodeObj = inodeManager.readNode(finalDestParentNodeInTree.inodeNumber);
                destParentInodeObj.addDirectBlock(sourceNode.inodeNumber);
                inodeManager.writeNode(destParentInodeObj, finalDestParentNodeInTree.inodeNumber);
            }

            if (originalParentNodeInTree.childNodes != null) {
                originalParentNodeInTree.childNodes.remove(sourceNode);
            }
            sourceNode.parent = finalDestParentNodeInTree;
            sourceNode.name = newItemName;
            if (finalDestParentNodeInTree.childNodes == null) {
                finalDestParentNodeInTree.childNodes = new LinkedList<>();
            }
            finalDestParentNodeInTree.childNodes.append(sourceNode);

            System.out.println("LOG: Moved '" + sourcePathStr + "' to '" + destPathStr + "' as '" + newItemName + "'");

        } catch (IOException e) {
            throw new FileSystemException("I/O error during move operation: " + e.getMessage(), e);
        }
    }

    public void importFile(String externalPath, String destinationFileNameInSim) throws FileSystemException {
        Path extPath = Paths.get(externalPath);
        if (Files.notExists(extPath)) {
            throw new FileSystemException("External file does not exist: " + externalPath);
        }
        if (tree.fileExists(destinationFileNameInSim) || tree.dirExists(destinationFileNameInSim)) {
            throw new FileSystemException("Item named '" + destinationFileNameInSim + "' already exists in simulator's current directory.");
        }

        makeFile(destinationFileNameInSim, FileType.FILE);
        DirectoryTree.Node destNode = tree.getChild(destinationFileNameInSim);
        if (destNode == null) throw new FileSystemException("Failed to create destination file '"+destinationFileNameInSim+"' in simulator for import.");


        try (RandomAccessFile srcExtFile = new RandomAccessFile(externalPath, "r")) {
            IndexNode destInode = inodeManager.readNode(destNode.inodeNumber);
            byte[] buffer = new byte[superBlock.getBlockSize()];
            int bytesReadFromExt;
            long totalBytesImported = 0;

            while ((bytesReadFromExt = srcExtFile.read(buffer)) != -1) {
                int newDataBlockNum = blockAllocator.allocateDataBlock();
                DataBlock simDataBlock = new DataBlock();
                if (bytesReadFromExt < buffer.length) {
                    simDataBlock.setBytes(ArrayManipulator.subArray(buffer, 0, bytesReadFromExt));
                } else {
                    simDataBlock.setBytes(buffer);
                }
                dataBlockManager.writeBlock(simDataBlock, newDataBlockNum);
                destInode.addDirectBlock(newDataBlockNum);
                totalBytesImported += bytesReadFromExt;
                if (bytesReadFromExt < buffer.length) break;
            }
            destInode.setSize((int) totalBytesImported);
            inodeManager.writeNode(destInode, destNode.inodeNumber);

        } catch (IOException e) {
            try { if (tree.fileExists(destinationFileNameInSim)) deleteFile(destinationFileNameInSim); } catch (FileSystemException ignored) {}
            throw new FileSystemException("I/O error importing file '" + externalPath + "': " + e.getMessage(), e);
        }
    }

    public void exportFile(String fileNameInSim, String externalPath) throws FileSystemException {
        DirectoryTree.Node sourceNode = tree.getChild(fileNameInSim);
        if (sourceNode == null || sourceNode.type == FileType.DIRECTORY) {
            throw new FileSystemException("Simulator file '" + fileNameInSim + "' not found or is a directory.");
        }
        Path extPath = Paths.get(externalPath);
        if (Files.exists(extPath)) {
            throw new FileSystemException("External file '" + externalPath + "' already exists.");
        }

        try (RandomAccessFile destExtFile = new RandomAccessFile(externalPath, "rw")) {
            IndexNode sourceInode = inodeManager.readNode(sourceNode.inodeNumber);
            int fileSize = sourceInode.getSize();
            int totalBytesExported = 0;

            for (int simDataBlockNum : sourceInode.getAllocatedDirectBlocks()) {
                if (totalBytesExported >= fileSize) break;
                DataBlock simDataBlock = dataBlockManager.readBlock(simDataBlockNum);
                int bytesToWriteFromBlock = Math.min(superBlock.getBlockSize(), fileSize - totalBytesExported);
                destExtFile.write(simDataBlock.getBytes(), 0, bytesToWriteFromBlock);
                totalBytesExported += bytesToWriteFromBlock;
            }
        } catch (IOException e) {
            throw new FileSystemException("I/O error exporting file '" + fileNameInSim + "' to '" + externalPath + "': " + e.getMessage(), e);
        }
    }

    private int calculateNeededBlocks(int amountOfBytes) {
        if (amountOfBytes == 0) return 0;
        return (int) Math.ceil((float) amountOfBytes / superBlock.getBlockSize());
    }

    public void showDirectoryInfo() throws FileSystemException {
        DirectoryTree.Node current = tree.getCurrentDir();
        try {
            int totalItems = 0;
            int totalFiles = 0;
            int totalFolders = 0;
            long totalSize = 0;

            System.out.println("📁 Directory Information: " + current.name);
            System.out.println("└─ Path: " + getCurrentPath());
            System.out.println("└─ Inode: " + current.inodeNumber);
            System.out.println();

            if (current.childNodes != null) {
                Object[] children = current.childNodes.toArray();
                totalItems = children.length;

                for (Object childObj : children) {
                    DirectoryTree.Node childNode = (DirectoryTree.Node) childObj;
                    IndexNode childInode = inodeManager.readNode(childNode.inodeNumber);

                    if (childNode.type == FileType.DIRECTORY) {
                        totalFolders++;
                        totalSize += calculateDirectorySize(childNode);
                    } else {
                        totalFiles++;
                        totalSize += childInode.getSize();
                    }
                }
            }

            System.out.println("📊 Summary:");
            System.out.println("   Total Items: " + totalItems);
            System.out.println("   📁 Folders: " + totalFolders);
            System.out.println("   📄 Files: " + totalFiles);
            System.out.println("   💾 Total Size: " + formatFileSize(totalSize));
            System.out.println();

        } catch (IOException e) {
            throw new FileSystemException("I/O error reading directory info: " + e.getMessage(), e);
        }
    }

    public void listCurrentDirDetailed() throws FileSystemException {
        DirectoryTree.Node current = tree.getCurrentDir();
        System.out.println("📁 " + current.name + "$ ls -la:");
        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %-8s │ %-20s │ %-12s │ %-8s │%n", "Type", "Name", "Size", "Inode");
        System.out.println("├────────────────────────────────────────────────────────────┤");

        if (current != tree.root && current.parent != null) {
            System.out.printf("│ %-8s │ %-20s │ %-12s │ %-8s │%n",
                "📁 DIR", "..", "<DIR>", "");
        }

        try {
            if (current.childNodes != null) {
                Object[] children = current.childNodes.toArray();
                for (Object childObj : children) {
                    DirectoryTree.Node childNode = (DirectoryTree.Node) childObj;
                    IndexNode childInode = inodeManager.readNode(childNode.inodeNumber);

                    String typeIcon = childNode.type == FileType.DIRECTORY ? "📁 DIR" : "📄 FILE";
                    String sizeStr = childNode.type == FileType.DIRECTORY ?
                        "<DIR>" : formatFileSize(childInode.getSize());

                    String nameWithoutColor = getColoredName(childNode.name, childNode.type);

                    System.out.printf("│ %-8s │ %-20s │ %-12s │ %-8d │%n",
                        typeIcon, nameWithoutColor, sizeStr, childNode.inodeNumber);
                }
            }
        } catch (IOException e) {
            throw new FileSystemException("I/O error reading directory details: " + e.getMessage(), e);
        }

        System.out.println("└────────────────────────────────────────────────────────────┘");
    }

    public void showMemoryVisualization() throws FileSystemException {
        try {
            System.out.println("💾 Memory Visualization");
            System.out.println("══════════════════════════════════════════════════════════");

            int totalInodes = superBlock.getInodeCount();
            int usedInodes = blockAllocator.getUsedInodeCount();

            int totalDataBlocks = superBlock.getDataBlockCount();
            int usedDataBlocks = blockAllocator.getUsedDataBlockCount();

            System.out.println("🔍 INFO:");
            System.out.println("   Total Data Blocks Available: " + totalDataBlocks);
            System.out.println("   Used Data Blocks (from bitmap): " + usedDataBlocks);

            int actualFileDataBlocks = calculateTotalFileDataBlocks();
            System.out.println("   Actual File Data Blocks: " + actualFileDataBlocks);
            System.out.println();

            System.out.println("📦 Inode Usage:");
            System.out.print("   ");
            drawProgressBar(usedInodes, totalInodes, 40, "🟦", "⬜");
            System.out.printf(" %d/%d (%.1f%%)%n", usedInodes, totalInodes,
                totalInodes == 0 ? 0 : (double)usedInodes/totalInodes*100);

            System.out.println("💿 Data Block Usage (Total Allocated):");
            System.out.print("   ");
            drawProgressBar(usedDataBlocks, totalDataBlocks, 40, "🟩", "⬜");
            System.out.printf(" %d/%d (%.1f%%)%n", usedDataBlocks, totalDataBlocks,
                totalDataBlocks == 0 ? 0 : (double)usedDataBlocks/totalDataBlocks*100);

            System.out.println("🗄️  File Data Usage (Content Only):");
            System.out.print("   ");
            drawProgressBar(actualFileDataBlocks, totalDataBlocks, 40, "🟨", "⬜");
            System.out.printf(" %s/%s (%.1f%%)%n",
                formatFileSize((long)actualFileDataBlocks * superBlock.getBlockSize()),
                formatFileSize((long)totalDataBlocks * superBlock.getBlockSize()),
                totalDataBlocks == 0 ? 0 : (double)actualFileDataBlocks/totalDataBlocks*100);

            long totalSystemStorage = (long)superBlock.getTotalBlockCount() * superBlock.getBlockSize();
            long usedDataStorage = (long)usedDataBlocks * superBlock.getBlockSize();
            long usedInodeStorage = (long)usedInodes * IndexNode.INODE_SIZE;
            long usedMetadataStorage = (long)superBlock.getBlockSize() * (superBlock.getInodeBlockOffset() - superBlock.getInodeBitmapOffset());

            long totalUsedStorage = usedDataStorage + usedInodeStorage + usedMetadataStorage;

            System.out.println("🗄️  Total System Usage:");
            System.out.print("   ");
            drawProgressBar(totalUsedStorage, totalSystemStorage, 40, "🟪", "⬜");
            System.out.printf(" %s/%s (%.1f%%)%n",
                formatFileSize(totalUsedStorage), formatFileSize(totalSystemStorage),
                totalSystemStorage == 0 ? 0 : (double)totalUsedStorage/totalSystemStorage*100);

            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("📊 Detailed Statistics:");
            System.out.printf("   Block Size: %s%n", formatFileSize(superBlock.getBlockSize()));
            System.out.printf("   Free Inodes: %d%n", totalInodes - usedInodes);
            System.out.printf("   Free Data Blocks: %d%n", totalDataBlocks - usedDataBlocks);
            System.out.printf("   Free Data Space: %s%n", formatFileSize((long)(totalDataBlocks - usedDataBlocks) * superBlock.getBlockSize()));
            long actualFileContentSize = calculateNodeContentSize(tree.root);
            long allocatedDataSpace = (long) actualFileDataBlocks * superBlock.getBlockSize();
            System.out.printf("   Actual File Content: %s%n", formatFileSize(actualFileContentSize));
            System.out.printf("   Storage Efficiency: %.2f%% (content/allocated)%n",
                (allocatedDataSpace == 0) ? 0.0 : (double)actualFileContentSize / allocatedDataSpace * 100);

        } catch (Exception e) {
            throw new FileSystemException("Error generating memory visualization: " + e.getMessage(), e);
        }
    }

    private long calculateNodeContentSize(DirectoryTree.Node node) throws FileSystemException {
        long totalSize = 0;

        if (node.type == FileType.FILE) {
            try {
                IndexNode inode = inodeManager.readNode(node.inodeNumber);
                totalSize += inode.getSize();
            } catch (IOException e) {
                // Abaikan jika ada error
            }
        } else if (node.childNodes != null) {
            Object[] children = node.childNodes.toArray();
            for (Object childObj : children) {
                DirectoryTree.Node child = (DirectoryTree.Node) childObj;
                totalSize += calculateNodeContentSize(child);
            }
        }

        return totalSize;
    }

    private int calculateTotalFileDataBlocks() throws FileSystemException {
        return calculateNodeDataBlocks(tree.root);
    }

    private int calculateNodeDataBlocks(DirectoryTree.Node node) throws FileSystemException {
        int totalBlocks = 0;

        if (node.type == FileType.FILE) {
            try {
                IndexNode inode = inodeManager.readNode(node.inodeNumber);
                totalBlocks += inode.getAllocatedDirectBlocks().length;
            } catch (IOException e) {
                // Abaikan jika ada error
            }
        } else if (node.childNodes != null) {
            Object[] children = node.childNodes.toArray();
            for (Object childObj : children) {
                DirectoryTree.Node child = (DirectoryTree.Node) childObj;
                totalBlocks += calculateNodeDataBlocks(child);
            }
        }

        return totalBlocks;
    }

    private void drawProgressBar(long used, long total, int width, String usedChar, String freeChar) {
        if (total <= 0) {
            total = 1;
            used = 0;
        }
        int usedWidth = (int)((double)used / total * width);
        int freeWidth = width - usedWidth;

        System.out.print("[");
        for (int i = 0; i < usedWidth; i++) {
            System.out.print(usedChar);
        }
        for (int i = 0; i < freeWidth; i++) {
            System.out.print(freeChar);
        }
        System.out.print("]");
    }

    private String getColoredName(String name, FileType type) {
        return name;
    }

    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        else return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private long calculateDirectorySize(DirectoryTree.Node dirNode) throws IOException {
        long totalSize = 0;
        if (dirNode.childNodes != null) {
            Object[] children = dirNode.childNodes.toArray();
            for (Object childObj : children) {
                DirectoryTree.Node childNode = (DirectoryTree.Node) childObj;
                IndexNode childInode = inodeManager.readNode(childNode.inodeNumber);

                if (childNode.type == FileType.DIRECTORY) {
                    totalSize += calculateDirectorySize(childNode);
                } else {
                    totalSize += childInode.getSize();
                }
            }
        }
        return totalSize;
    }

    public void showFileSystemTree() {
        System.out.println("🌳 File System Tree:");
        System.out.println("════════════════════");
        printTreeNode(tree.root, "", true);
        System.out.println();
    }

    private void printTreeNode(DirectoryTree.Node node, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        String icon = node.type == FileType.DIRECTORY ? "📁" : "📄";

        try {
            IndexNode nodeInode = inodeManager.readNode(node.inodeNumber);
            String sizeInfo = node.type == FileType.DIRECTORY ?
                "" : " (" + formatFileSize(nodeInode.getSize()) + ")";

            System.out.println(prefix + connector + icon + " " + node.name + sizeInfo);

            if (node.type == FileType.DIRECTORY && node.childNodes != null) {
                Object[] children = node.childNodes.toArray();
                for (int i = 0; i < children.length; i++) {
                    DirectoryTree.Node child = (DirectoryTree.Node) children[i];
                    boolean isLastChild = (i == children.length - 1);
                    String newPrefix = prefix + (isLast ? "    " : "│   ");
                    printTreeNode(child, newPrefix, isLastChild);
                }
            }
        } catch (IOException e) {
            System.out.println(prefix + connector + "❌ " + node.name + " (Error reading)");
        }
    }

    // --- Metode Getter untuk GUI ---

    public long getTotalDataSpace() {
        return (long) superBlock.getDataBlockCount() * superBlock.getBlockSize();
    }

    public long getUsedDataSpace() throws FileSystemException {
        return (long) blockAllocator.getUsedDataBlockCount() * superBlock.getBlockSize();
    }

    public int getTotalInodeCount() {
        return superBlock.getInodeCount();
    }

    public int getUsedInodeCount() throws FileSystemException {
        return blockAllocator.getUsedInodeCount();
    }

    public int getTotalDataBlockCount() {
        return superBlock.getDataBlockCount();
    }

    public int getUsedDataBlockCount() throws FileSystemException {
        return blockAllocator.getUsedDataBlockCount();
    }

    public long getActualFileContentSize() throws FileSystemException {
        return calculateNodeContentSize(tree.root);
    }

    public long getAllocatedFileSpaceSize() throws FileSystemException {
        long actualFileDataBlocks = calculateTotalFileDataBlocks();
        return actualFileDataBlocks * superBlock.getBlockSize();
    }
}
