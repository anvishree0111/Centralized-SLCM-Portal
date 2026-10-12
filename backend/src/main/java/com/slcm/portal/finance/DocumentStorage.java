package com.slcm.portal.finance;

import static com.slcm.portal.finance.FinanceAccess.error;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentStorage {
    private static final Map<String,String> EXTENSIONS = Map.of(
            "application/pdf", "pdf", "image/png", "png", "image/jpeg", "jpg", "text/plain", "txt");
    private final Path root;
    private final int maxBytes;
    public record Stored(String originalName, String storedName, String contentType, long size, String sha256) {}

    public DocumentStorage(@Value("${app.storage.documents-path:./storage/documents}") String directory,
                    @Value("${app.storage.max-document-bytes:5242880}") int maxBytes) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        if (maxBytes < 1 || maxBytes > 25 * 1024 * 1024) throw new IllegalArgumentException("Invalid document size limit");
        this.maxBytes = maxBytes;
    }

    public Stored store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "A document file is required");
        String type = file.getContentType();
        if (type == null || !EXTENSIONS.containsKey(type)) throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Allowed types: PDF, PNG, JPEG and plain text");
        if (file.getSize() > maxBytes) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "Document exceeds size limit");
        try {
            byte[] bytes;
            try (InputStream input = file.getInputStream()) { bytes = input.readNBytes(maxBytes+1); }
            if (bytes.length == 0 || bytes.length > maxBytes) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "Invalid document size");
            validateSignature(type, bytes);
            Files.createDirectories(root);
            String name = UUID.randomUUID() + "." + EXTENSIONS.get(type);
            Path target = root.resolve(name);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try { Files.deleteIfExists(target); } catch (IOException ignored) { /* orphan never becomes downloadable */ }
                        }
                    }
                });
            }
            String original = file.getOriginalFilename();
            if (original == null) original = "document." + EXTENSIONS.get(type);
            original = original.replace('\\','/');
            original = original.substring(original.lastIndexOf('/')+1).replaceAll("[^a-zA-Z0-9._ -]", "_");
            if (original.isBlank() || original.length()>200) original = "document." + EXTENSIONS.get(type);
            return new Stored(original, name, type, bytes.length, hash(bytes));
        } catch (IOException e) { throw error(HttpStatus.SERVICE_UNAVAILABLE, "Document storage unavailable"); }
    }

    byte[] read(String name, long expectedSize, String expectedHash) {
        if (name == null || !name.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png|jpg|txt)"))
            throw error(HttpStatus.NOT_FOUND, "Document file unavailable");
        Path target = root.resolve(name).normalize();
        if (!target.getParent().equals(root) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
            throw error(HttpStatus.NOT_FOUND, "Document file unavailable");
        try (InputStream input = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(maxBytes+1);
            if (bytes.length > maxBytes || bytes.length != expectedSize || !hash(bytes).equals(expectedHash))
                throw error(HttpStatus.CONFLICT, "Document integrity check failed");
            return bytes;
        } catch (IOException e) { throw error(HttpStatus.NOT_FOUND, "Document file unavailable"); }
    }

    private static void validateSignature(String type, byte[] bytes) {
        boolean valid = switch (type) {
            case "application/pdf" -> startsWith(bytes, new byte[]{37,80,68,70,45});
            case "image/png" -> startsWith(bytes, new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "image/jpeg" -> startsWith(bytes, new byte[]{(byte)255,(byte)216,(byte)255});
            case "text/plain" -> {
                boolean text = true;
                for (byte b : bytes) if (b == 0 || (b >= 0 && b < 9)) { text = false; break; }
                yield text;
            }
            default -> false;
        };
        if (!valid) throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File content does not match its type");
    }
    private static boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length && Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
