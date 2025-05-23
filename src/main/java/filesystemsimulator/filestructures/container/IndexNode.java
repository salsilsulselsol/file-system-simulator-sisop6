package filesystemsimulator.filestructures.container;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays; // Untuk Arrays.equals

import filesystemsimulator.exceptions.FileSystemException;
import filesystemsimulator.filestructures.data.FileType;
import filesystemsimulator.utils.ArrayManipulator;

/**
 * Represents an index node file system structure. An index node contains information about a file,
 * like its size, name and references to the data blocks that the file occupies.
 */
public class IndexNode {

    public static final int INODE_SIZE = 256; // Ukuran total inode dalam byte
    public static final int MAX_DIRECT_BLOCKS = 56; // Jumlah maksimum direct block pointers
    public static final int MAX_NAME_SIZE = 16; // Ukuran maksimum nama file/dir dalam byte (termasuk null terminator jika ada)

    FileType type;
    int size; // Ukuran file dalam byte
    int allocatedBlockCount; // Jumlah direct blocks yang terpakai (termasuk parent pointer di index 0)
    int[] directBlocks; // directBlocks[0] adalah inode parent, sisanya blok data
    int nameSize; // Panjang nama aktual
    byte[] name; // Nama file/direktori (max MAX_NAME_SIZE bytes)

    public IndexNode() {
        initialize();
    }

    /**
     * Writes the index node to the given file, at the file's current position.
     *
     * @param file the file to write the index node to.
     */
    public void write(RandomAccessFile file) {
        try {
            file.writeShort(type == FileType.DIRECTORY ? 0 : 1);
            file.writeInt(size);
            file.writeInt(allocatedBlockCount);
            for (int i = 0; i < MAX_DIRECT_BLOCKS; i++) {
                file.writeInt(directBlocks[i]);
            }
            file.writeInt(nameSize);
            // Tulis nama, pastikan panjangnya MAX_NAME_SIZE dengan padding jika perlu
            byte[] nameToWrite = new byte[MAX_NAME_SIZE];
            System.arraycopy(this.name, 0, nameToWrite, 0, this.nameSize);
            file.write(nameToWrite);

            // Hitung sisa byte untuk padding agar total INODE_SIZE
            int bytesWritten = 2 // type
                + 4 // size
                + 4 // allocatedBlockCount
                + (MAX_DIRECT_BLOCKS * 4) // directBlocks
                + 4 // nameSize
                + MAX_NAME_SIZE; // name
            int padding = INODE_SIZE - bytesWritten;
            if (padding > 0) {
                file.write(new byte[padding]);
            }

        } catch (IOException e) {
            System.err.println("Error while writing index node to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reads the index node from the given file, starting at the file's current position.
     *
     * @param file the file to read the index node from.
     */
    public void read(RandomAccessFile file) {
        try {
            type = (file.readShort() == 0)
                ? FileType.DIRECTORY
                : FileType.FILE;
            size = file.readInt();
            allocatedBlockCount = file.readInt();
            for (int i = 0; i < MAX_DIRECT_BLOCKS; i++) {
                directBlocks[i] = file.readInt();
            }
            nameSize = file.readInt();
            this.name = new byte[MAX_NAME_SIZE]; // Inisialisasi ulang array nama
            file.readFully(this.name); // Baca tepat MAX_NAME_SIZE byte untuk nama

            // Bersihkan nama dari padding null jika ada (setelah nameSize aktual)
            // Ini penting jika nama lebih pendek dari MAX_NAME_SIZE
            // byte[] actualNameBytes = new byte[this.nameSize];
            // System.arraycopy(this.name, 0, actualNameBytes, 0, this.nameSize);
            // this.name = actualNameBytes; // Ini akan mengubah ukuran array, mungkin tidak diinginkan jika fixed buffer
            // Biarkan this.name tetap MAX_NAME_SIZE, tapi getNameString() akan handle nameSize

            int bytesRead = 2 + 4 + 4 + (MAX_DIRECT_BLOCKS * 4) + 4 + MAX_NAME_SIZE;
            int paddingToSkip = INODE_SIZE - bytesRead;
            if (paddingToSkip > 0) {
                file.skipBytes(paddingToSkip);
            }


        } catch (IOException e) {
            System.err.println("Error while reading index node from file: " + e.getMessage());
            e.printStackTrace();
            initialize(); // Reset ke state default jika ada error baca
        }
    }

    /**
     * Adds a reference to a data block in the direct block list of the index node, if there is free space.
     * Direct blocks untuk data dimulai dari index 1 (index 0 untuk parent).
     * @param block the number of the data block to be added.
     * @throws FileSystemException if the maximum file/directory size is reached.
     */
    public void addDirectBlock(int block)
        throws FileSystemException {
        if (allocatedBlockCount >= MAX_DIRECT_BLOCKS) { // Periksa apakah array directBlocks sudah penuh
            if (type == FileType.FILE) {
                throw new FileSystemException("Max file size reached (cannot add more data blocks).");
            } else {
                throw new FileSystemException("Max directory size reached (cannot add more child entries).");
            }
        }
        directBlocks[allocatedBlockCount++] = block; // Tambah di akhir dan increment count
    }

    /**
     * Removes a direct block from the direct block list of the index node.
     * Ini mencari nomor blok (inode anak atau blok data) dan menghapusnya.
     * @param blockToRemove the number of the data block to be removed.
     */
    public void removeDirectBlock(int blockToRemove) {
        for (int i = 0; i < allocatedBlockCount; i++) { // Iterasi hanya sampai allocatedBlockCount
            if (directBlocks[i] == blockToRemove) {
                ArrayManipulator.shiftArrayLeft(directBlocks, i, allocatedBlockCount);
                directBlocks[allocatedBlockCount - 1] = -1; // Bersihkan elemen terakhir setelah shift
                allocatedBlockCount--;
                return; // Hapus hanya satu kejadian
            }
        }
    }

    public int[] getDirectBlocks() { // Getter untuk semua direct blocks (termasuk parent)
        return directBlocks;
    }

    /**
     * Returns an array containing the allocated direct blocks of the node
     * EXCLUDING the parent pointer (directBlocks[0]).
     *
     * @return the allocated data blocks (for files) or child inode numbers (for directories).
     */
    public int[] getAllocatedDirectBlocks() {
        if (allocatedBlockCount <= 1) { // Hanya ada parent pointer atau kosong
            return new int[0];
        }
        int[] result = new int[allocatedBlockCount - 1]; // Kurangi 1 untuk parent
        // int count = 0;
        for (int i = 1; i < allocatedBlockCount; i++) { // Mulai dari 1 untuk skip parent
            result[i-1] = directBlocks[i];
        }
        return result;
    }

    public void setType(FileType type) {
        this.type = type;
    }
    public FileType getType() {
        return this.type;
    }

    public int getAllocatedBlockCount() {
        return allocatedBlockCount;
    }

    public int getSize() {
        return this.size;
    }
    public void setSize(int newSize) {
        this.size = newSize;
    }


    /**
     * Sets the node's name to the given string.
     *
     * @param newName the new name of the node.
     * @throws FileSystemException if the given string is longer than the maximum allowed name size.
     */
    public void setName(String newName)
        throws FileSystemException {
        if (newName.length() >= MAX_NAME_SIZE) { // >= karena MAX_NAME_SIZE mungkin termasuk null term jika desainnya begitu
            // Jika MAX_NAME_SIZE adalah panjang murni, maka > MAX_NAME_SIZE -1
            throw new FileSystemException("Name is too long! Max " + (MAX_NAME_SIZE -1) + " characters. Given: '" + newName + "' (" + newName.length() + ")");
        }
        // Reset array nama
        this.name = new byte[MAX_NAME_SIZE];
        ArrayManipulator.fillArray(this.name, (byte)0); // Isi dengan null bytes

        byte[] newNameBytes = newName.getBytes(); // Gunakan encoding default
        System.arraycopy(newNameBytes, 0, this.name, 0, newNameBytes.length);
        this.nameSize = newNameBytes.length;
    }

    /**
     * Gets the name of the node as a String.
     * @return The name string.
     */
    public String getNameString() {
        // Buat string hanya dari bagian yang valid dari array byte nama
        return new String(this.name, 0, this.nameSize); // Gunakan encoding default
    }

    /**
     * Gets the raw name bytes.
     * @return The name as byte array (padded to MAX_NAME_SIZE).
     */
    public byte[] getNameBytes() {
        return this.name; // Mengembalikan buffer internal, hati-hati jika diubah eksternal
    }


    public int getLastAllocatedBlock() { // Blok data terakhir, atau parent jika hanya parent
        if (allocatedBlockCount == 0) return -1; // Tidak ada blok sama sekali (seharusnya tidak terjadi jika inode valid)
        if (allocatedBlockCount == 1) return -1; // Hanya parent, belum ada blok data
        return directBlocks[allocatedBlockCount - 1]; // Elemen terakhir yang valid
    }

    /**
     * Checks whether the directory or file, represented by the index node, is empty.
     * Empty berarti tidak ada child (untuk direktori) atau tidak ada blok data (untuk file).
     * allocatedBlockCount = 1 berarti hanya ada pointer ke parent.
     * @return true if the directory/file is empty, false otherwise.
     */
    public boolean isEmpty() {
        return allocatedBlockCount <= 1; // Kurang dari atau sama dengan 1 berarti hanya ada parent pointer (atau tidak ada sama sekali)
    }

    /**
     * Returns the number of the index node's parent node.
     *
     * @return the first element of the direct block list, which represents the node's parent.
     */
    public int getParent() {
        if (allocatedBlockCount > 0) {
            return directBlocks[0];
        }
        return -1; // Tidak ada parent (misal, inode belum sepenuhnya diinisialisasi)
    }

    /**
     * Sets the parent inode number for this node. Stored in directBlocks[0].
     * Jika directBlocks[0] belum dialokasikan (misal, saat inisialisasi node baru),
     * ini juga akan menaikkan allocatedBlockCount.
     * @param parentInodeNumber The inode number of the parent.
     */
    public void setParentInode(int parentInodeNumber) {
        if (allocatedBlockCount == 0) { // Jika ini adalah blok pertama yang dialokasikan untuk node ini
            directBlocks[0] = parentInodeNumber;
            allocatedBlockCount = 1; // Sekarang ada satu blok teralokasi (yaitu parent pointer)
        } else { // Jika allocatedBlockCount > 0, directBlocks[0] sudah ada
            directBlocks[0] = parentInodeNumber;
        }
    }


    /**
     * Checks whether the file is at the maximum size (semua direct blocks terpakai).
     *
     * @return true if the amount of allocated blocks has reached the max amount, false otherwise.
     */
    public boolean isMaxSize() {
        return allocatedBlockCount >= MAX_DIRECT_BLOCKS;
    }

    /**
     * Initializes the index node's fields.
     */
    private void initialize() {
        directBlocks = new int[MAX_DIRECT_BLOCKS];
        ArrayManipulator.fillArray(directBlocks, -1); // Isi dengan -1 sebagai tanda belum terpakai
        allocatedBlockCount = 0; // Awalnya tidak ada blok yang teralokasi (bahkan parent)
        size = 0;
        type = FileType.FILE; // Default ke file, bisa diubah
        nameSize = 0;
        name = new byte[MAX_NAME_SIZE]; // Array byte untuk nama, diisi null/0
        ArrayManipulator.fillArray(this.name, (byte)0);
    }
}
