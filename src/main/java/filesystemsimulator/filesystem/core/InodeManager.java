package filesystemsimulator.filesystem.core;

import filesystemsimulator.filestructures.container.IndexNode;
import filesystemsimulator.filestructures.container.SuperBlock;
import filesystemsimulator.exceptions.FileSystemException; // Jika diperlukan

import java.io.IOException;
import java.io.RandomAccessFile;

public class InodeManager {

    private final RandomAccessFile containerFile;
    private final SuperBlock superBlock;

    public InodeManager(RandomAccessFile containerFile, SuperBlock superBlock) {
        this.containerFile = containerFile;
        this.superBlock = superBlock;
    }

    public IndexNode readNode(int inodeNumber) throws IOException {
        IndexNode node = new IndexNode(); // Buat objek baru untuk hasil baca
        int inodesPerBlock = superBlock.getBlockSize() / IndexNode.INODE_SIZE;
        int blockOffsetForInode = inodeNumber / inodesPerBlock;
        int positionInBlock = (inodeNumber % inodesPerBlock) * IndexNode.INODE_SIZE;

        long absoluteBlockAddress = (long)(superBlock.getInodeBlockOffset() + blockOffsetForInode) * superBlock.getBlockSize();
        long finalSeekPosition = absoluteBlockAddress + positionInBlock;

        containerFile.seek(finalSeekPosition);
        node.read(containerFile);
        return node;
    }

    public void writeNode(IndexNode node, int inodeNumber) throws IOException {
        int inodesPerBlock = superBlock.getBlockSize() / IndexNode.INODE_SIZE;
        int blockOffsetForInode = inodeNumber / inodesPerBlock;
        int positionInBlock = (inodeNumber % inodesPerBlock) * IndexNode.INODE_SIZE;

        long absoluteBlockAddress = (long)(superBlock.getInodeBlockOffset() + blockOffsetForInode) * superBlock.getBlockSize();
        long finalSeekPosition = absoluteBlockAddress + positionInBlock;

        containerFile.seek(finalSeekPosition);
        node.write(containerFile);
    }
}
