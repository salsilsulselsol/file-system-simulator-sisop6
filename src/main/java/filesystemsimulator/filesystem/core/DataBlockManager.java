package filesystemsimulator.filesystem.core;

import filesystemsimulator.filestructures.container.DataBlock;
import filesystemsimulator.filestructures.container.SuperBlock;
import filesystemsimulator.utils.ArrayManipulator; // Diperlukan untuk fillArray

import java.io.IOException;
import java.io.RandomAccessFile;

public class DataBlockManager {

    private final RandomAccessFile containerFile;
    private final SuperBlock superBlock;
    private final BlockAllocator blockAllocator; // Mungkin diperlukan untuk freeDataBlock saat wipe

    public DataBlockManager(RandomAccessFile containerFile, SuperBlock superBlock, BlockAllocator blockAllocator) {
        this.containerFile = containerFile;
        this.superBlock = superBlock;
        this.blockAllocator = blockAllocator;
    }

    public DataBlock readBlock(int dataBlockNumber) throws IOException {
        DataBlock block = new DataBlock();
        long seekPosition = (long)(superBlock.getDataBlockOffset() + dataBlockNumber) * superBlock.getBlockSize();
        containerFile.seek(seekPosition);
        block.read(containerFile);
        return block;
    }

    public void writeBlock(DataBlock block, int dataBlockNumber) throws IOException {
        long seekPosition = (long)(superBlock.getDataBlockOffset() + dataBlockNumber) * superBlock.getBlockSize();
        containerFile.seek(seekPosition);
        block.write(containerFile);
    }

    public void wipeBlock(int dataBlockNumber) throws IOException {
        DataBlock blockToWipe = new DataBlock(); // Buffer
        // Tidak perlu baca, langsung timpa dengan nol
        ArrayManipulator.fillArray(blockToWipe.getBytes(), (byte) 0);
        writeBlock(blockToWipe, dataBlockNumber);
        blockAllocator.freeDataBlock(dataBlockNumber); // Bebaskan di bitmap
    }
}
