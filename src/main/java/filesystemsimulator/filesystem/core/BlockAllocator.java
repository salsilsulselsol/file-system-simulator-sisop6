package filesystemsimulator.filesystem.core;

import filesystemsimulator.exceptions.FileSystemException;
import filesystemsimulator.filestructures.container.Bitmap;
import filesystemsimulator.filestructures.container.SuperBlock;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BlockAllocator {

    private final RandomAccessFile containerFile;
    private final SuperBlock superBlock;
    private final Bitmap inodeBitmapBuffer;
    private final Bitmap dataBitmapBuffer;

    public BlockAllocator(RandomAccessFile containerFile, SuperBlock superBlock) {
        this.containerFile = containerFile;
        this.superBlock = superBlock;
        this.inodeBitmapBuffer = new Bitmap();
        this.dataBitmapBuffer = new Bitmap();
    }

    private void readBitmapToBuffer(Bitmap buffer, int regionOffset, int bitmapBlockIndexInRegion) throws IOException {
        long absoluteBlockNumber = regionOffset + bitmapBlockIndexInRegion;
        containerFile.seek(absoluteBlockNumber * superBlock.getBlockSize());
        buffer.read(containerFile);
    }

    private void writeBitmapFromBuffer(Bitmap buffer, int regionOffset, int bitmapBlockIndexInRegion) throws IOException {
        long absoluteBlockNumber = regionOffset + bitmapBlockIndexInRegion;
        containerFile.seek(absoluteBlockNumber * superBlock.getBlockSize());
        buffer.write(containerFile);
    }

    public int allocateInode() throws IOException, FileSystemException {
        int inodeBitmapBlockCount = superBlock.getDataBitmapOffset() - superBlock.getInodeBitmapOffset();
        for (int i = 0; i < inodeBitmapBlockCount; i++) {
            readBitmapToBuffer(inodeBitmapBuffer, superBlock.getInodeBitmapOffset(), i);
            int firstFreeBitInBlock = inodeBitmapBuffer.getFirstFreeBit();
            if (firstFreeBitInBlock != -1) {
                inodeBitmapBuffer.resetBit(firstFreeBitInBlock);
                writeBitmapFromBuffer(inodeBitmapBuffer, superBlock.getInodeBitmapOffset(), i);
                return (i * (superBlock.getBlockSize() * 8)) + firstFreeBitInBlock;
            }
        }
        throw new FileSystemException("No free inodes available.");
    }

    public void freeInode(int inodeNumber) throws IOException {
        int bitsPerBitmapBlock = superBlock.getBlockSize() * 8;
        int bitmapBlockIndexRelativeToRegion = inodeNumber / bitsPerBitmapBlock;
        int bitIndexInBitmapBlock = inodeNumber % bitsPerBitmapBlock;

        readBitmapToBuffer(inodeBitmapBuffer, superBlock.getInodeBitmapOffset(), bitmapBlockIndexRelativeToRegion);
        inodeBitmapBuffer.setBit(bitIndexInBitmapBlock);
        writeBitmapFromBuffer(inodeBitmapBuffer, superBlock.getInodeBitmapOffset(), bitmapBlockIndexRelativeToRegion);
    }

    public int allocateDataBlock() throws IOException, FileSystemException {
        int dataBitmapBlockCount = superBlock.getInodeBlockOffset() - superBlock.getDataBitmapOffset();
        for (int i = 0; i < dataBitmapBlockCount; i++) {
            readBitmapToBuffer(dataBitmapBuffer, superBlock.getDataBitmapOffset(), i);
            int firstFreeBitInBlock = dataBitmapBuffer.getFirstFreeBit();
            if (firstFreeBitInBlock != -1) {
                dataBitmapBuffer.resetBit(firstFreeBitInBlock);
                writeBitmapFromBuffer(dataBitmapBuffer, superBlock.getDataBitmapOffset(), i);
                return (i * (superBlock.getBlockSize() * 8)) + firstFreeBitInBlock;
            }
        }
        throw new FileSystemException("No free data blocks available.");
    }

    public void freeDataBlock(int dataBlockNumber) throws IOException {
        int bitsPerBitmapBlock = superBlock.getBlockSize() * 8;
        int bitmapBlockIndexRelativeToRegion = dataBlockNumber / bitsPerBitmapBlock;
        int bitIndexInBitmapBlock = dataBlockNumber % bitsPerBitmapBlock;

        readBitmapToBuffer(dataBitmapBuffer, superBlock.getDataBitmapOffset(), bitmapBlockIndexRelativeToRegion);
        dataBitmapBuffer.setBit(bitIndexInBitmapBlock);
        writeBitmapFromBuffer(dataBitmapBuffer, superBlock.getDataBitmapOffset(), bitmapBlockIndexRelativeToRegion);
    }

    public void initializeBitmaps() throws IOException {
        byte[] allFreeBitmapBytes = new byte[superBlock.getBlockSize()];
        filesystemsimulator.utils.ArrayManipulator.fillArray(allFreeBitmapBytes, filesystemsimulator.filesystem.FileSystem.BYTE_MAX);
        Bitmap freeBitmapBlock = new Bitmap(allFreeBitmapBytes);

        int inodeBitmapBlockCount = superBlock.getDataBitmapOffset() - superBlock.getInodeBitmapOffset();
        for (int i = 0; i < inodeBitmapBlockCount; i++) {
            writeBitmapFromBuffer(freeBitmapBlock, superBlock.getInodeBitmapOffset(), i);
        }

        int dataBitmapBlockCount = superBlock.getInodeBlockOffset() - superBlock.getDataBitmapOffset();
        for (int i = 0; i < dataBitmapBlockCount; i++) {
            writeBitmapFromBuffer(freeBitmapBlock, superBlock.getDataBitmapOffset(), i);
        }
    }

    // PERBAIKAN: Logika penghitungan yang lebih akurat untuk mengabaikan padding bits.
    private int countUsedInBitmap(int bitmapOffset, int totalItems, int bitmapSizeInBytes) throws IOException {
        containerFile.seek((long) bitmapOffset * superBlock.getBlockSize());
        byte[] bitmap = new byte[bitmapSizeInBytes];
        containerFile.readFully(bitmap);

        int freeCount = 0;
        int fullBytes = totalItems / 8;
        int remainingBits = totalItems % 8;

        for (int i = 0; i < fullBytes; i++) {
            freeCount += Integer.bitCount(bitmap[i] & 0xFF);
        }

        if (remainingBits > 0) {
            byte lastByte = bitmap[fullBytes];
            for (int i = 0; i < remainingBits; i++) {
                if ((lastByte & (1 << (7 - i))) != 0) {
                    freeCount++;
                }
            }
        }

        return totalItems - freeCount;
    }

    public int getUsedInodeCount() throws FileSystemException {
        try {
            return countUsedInBitmap(
                superBlock.getInodeBitmapOffset(),
                superBlock.getInodeCount(),
                superBlock.getInodeBitmapSize()
            );
        } catch (IOException e) {
            throw new FileSystemException("Error reading inode bitmap: " + e.getMessage(), e);
        }
    }

    public int getUsedDataBlockCount() throws FileSystemException {
        try {
            return countUsedInBitmap(
                superBlock.getDataBlockBitmapOffset(),
                superBlock.getDataBlockCount(),
                superBlock.getDataBlockBitmapSize()
            );
        } catch (IOException e) {
            throw new FileSystemException("Error reading data block bitmap: " + e.getMessage(), e);
        }
    }
}
