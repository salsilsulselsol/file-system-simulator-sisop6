package filesystemsimulator.exceptions;

/**
 * This exception is thrown whenever an error occurs while a FileSystem object is functioning.
 */
public class FileSystemException extends Exception {

    // Konstruktor yang sudah ada
    public FileSystemException(String errorMessage) {
        super(errorMessage);
    }

    // Konstruktor BARU untuk menerima penyebab (cause)
    public FileSystemException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
