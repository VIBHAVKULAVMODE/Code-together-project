package com.codeTogether.service;

import com.codeTogether.entity.FileData;
import com.codeTogether.repository.StorageRepository;
import com.codeTogether.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class StorageService {

    @Autowired
    private StorageRepository repository;

    private static final String ROOT_DIR = "uploads/";

    // Upload a file with version control logic
    public FileData uploadFile(MultipartFile file, String uploadedBy, String role, String projectFolder) throws IOException {
        String fileName = file.getOriginalFilename();
        // Check if an active version exists
        Optional<FileData> activeFileOpt = repository.findByFileNameAndVersionGreaterThan(fileName, 0);
        if (activeFileOpt.isPresent()) {
            // Treat as update: mark the active file as previous by negating its version
            FileData activeFile = activeFileOpt.get();
            int currentVersion = activeFile.getVersion();
            activeFile.setVersion(-currentVersion);
            repository.save(activeFile);
            int newVersion = currentVersion + 1;
            FileData newFile = FileData.builder()
                    .fileName(fileName)
                    .fileType(file.getContentType())
                    .uploadedBy(uploadedBy)
                    .role(role)
                    .projectFolder(projectFolder)
                    .fileContent(FileUtils.compressFile(file.getBytes()))
                    .uploadDate(java.time.LocalDate.now().toString())
                    .version(newVersion)
                    .build();
            FileData savedFile = repository.save(newFile);
            // Save file to filesystem
            Path filePath = Paths.get(ROOT_DIR + projectFolder + "/" + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            return savedFile;
        } else {
            // No active file exists; new upload starts with version 1.
            FileData fileData = FileData.builder()
                    .fileName(fileName)
                    .fileType(file.getContentType())
                    .uploadedBy(uploadedBy)
                    .role(role)
                    .projectFolder(projectFolder)
                    .fileContent(FileUtils.compressFile(file.getBytes()))
                    .uploadDate(java.time.LocalDate.now().toString())
                    .version(1)
                    .build();
            FileData savedFile = repository.save(fileData);
            // Save file to filesystem
            Path filePath = Paths.get(ROOT_DIR + projectFolder + "/" + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            return savedFile;
        }
    }

    // Fetch all active files (version > 0)
    public List<FileData> getActiveFiles() {
        return repository.findByVersionGreaterThan(0);
    }

    // Fetch previous versions for a specific file (version < 0)
    public List<FileData> getPreviousVersions(String fileName) {
        return repository.findByFileNameAndVersionLessThan(fileName, 0);
    }

    // Fetch all previous versions for all files (version < 0)
    public List<FileData> getAllPreviousVersions() {
        return repository.findByVersionLessThan(0);
    }

    public byte[] downloadFile(String fileName) {
        // Download only the active version
        Optional<FileData> dbFileData = repository.findByFileNameAndVersionGreaterThan(fileName, 0);
        if (dbFileData.isPresent()) {
            return FileUtils.decompressFile(dbFileData.get().getFileContent());
        } else {
            throw new RuntimeException("File not found: " + fileName);
        }
    }
    
    // New method to download a file's content for a specific version (e.g. a previous version)
    public byte[] downloadFileByVersion(String fileName, int version) {
        Optional<FileData> fileDataOpt = repository.findByFileNameAndVersion(fileName, version);
        if (fileDataOpt.isPresent()) {
            return FileUtils.decompressFile(fileDataOpt.get().getFileContent());
        } else {
            throw new RuntimeException("File " + fileName + " with version " + version + " not found.");
        }
    }

    @Transactional
    public void deleteFile(String fileName) {
        try {
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            Optional<FileData> fileDataOpt = repository.findByFileNameAndVersionGreaterThan(decodedFileName, 0);
            if (fileDataOpt.isPresent()) {
                // Delete the active file record
                repository.delete(fileDataOpt.get());
                // Also delete from filesystem
                Path filePath = Paths.get(ROOT_DIR + fileDataOpt.get().getProjectFolder() + "/" + decodedFileName);
                Files.deleteIfExists(filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting file: " + e.getMessage(), e);
        }
    }

    // Update an existing file with version control
    public FileData updateFile(MultipartFile file, String fileName, String uploadedBy, String projectFolder) throws IOException {
        // Look up the active file (version > 0) by fileName
        Optional<FileData> activeFileOpt = repository.findByFileNameAndVersionGreaterThan(fileName, 0);
        if (activeFileOpt.isPresent()) {
            FileData activeFile = activeFileOpt.get();
            int currentVersion = activeFile.getVersion();
            // Mark the active file as previous (by negating its version)
            activeFile.setVersion(-currentVersion);
            repository.save(activeFile);
            int newVersion = currentVersion + 1;
            // Create a new record for the updated file
            FileData newFile = FileData.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .uploadedBy(uploadedBy)
                    .role(activeFile.getRole())
                    .projectFolder(projectFolder)
                    .fileContent(FileUtils.compressFile(file.getBytes()))
                    .uploadDate(java.time.LocalDate.now().toString())
                    .version(newVersion)
                    .build();
            FileData savedFile = repository.save(newFile);
            // Update file in filesystem
            Path filePath = Paths.get(ROOT_DIR + projectFolder + "/" + file.getOriginalFilename());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            return savedFile;
        }
        throw new RuntimeException("Active file not found for update: " + fileName);
    }
    
    // NEW: Rollback a file to a previous version
    @Transactional
    public FileData rollbackFile(String fileName, int rollbackVersion) throws IOException {
        // Look up the current active file (version > 0)
        Optional<FileData> activeFileOpt = repository.findByFileNameAndVersionGreaterThan(fileName, 0);
        int newVersion;
        String projectFolder;
        if (activeFileOpt.isPresent()) {
            FileData activeFile = activeFileOpt.get();
            int currentVersion = activeFile.getVersion();
            // Mark the current active file as previous
            activeFile.setVersion(-currentVersion);
            repository.save(activeFile);
            newVersion = currentVersion + 1;
            projectFolder = activeFile.getProjectFolder();
        } else {
            // If no active file exists, we'll assume rollback creates the first active version.
            newVersion = 1;
            // We'll retrieve projectFolder from the rollback version.
            projectFolder = "";
        }
        // Find the rollback file record by fileName and the provided rollback version
        Optional<FileData> rollbackFileOpt = repository.findByFileNameAndVersion(fileName, rollbackVersion);
        if (rollbackFileOpt.isPresent()) {
            FileData rollbackFile = rollbackFileOpt.get();
            if (projectFolder.isEmpty()) {
                projectFolder = rollbackFile.getProjectFolder();
            }
            // Create a new active record with content from the rollback file
            FileData newActiveFile = FileData.builder()
                    .fileName(fileName)
                    .fileType(rollbackFile.getFileType())
                    .uploadedBy(rollbackFile.getUploadedBy()) // you may choose to set current user if desired
                    .role(rollbackFile.getRole())
                    .projectFolder(projectFolder)
                    .fileContent(rollbackFile.getFileContent())
                    .uploadDate(java.time.LocalDate.now().toString())
                    .version(newVersion)
                    .build();
            FileData savedFile = repository.save(newActiveFile);
            // Write the restored file to the filesystem
            Path filePath = Paths.get(ROOT_DIR + projectFolder + "/" + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, FileUtils.decompressFile(rollbackFile.getFileContent()));
            return savedFile;
        } else {
            throw new RuntimeException("Rollback version " + rollbackVersion + " not found for file: " + fileName);
        }
    }

    // The following methods remain unchanged:
    public List<FileData> getAllFilesInFolder(String folderName) {
        return repository.findByProjectFolder(folderName);
    }

    public boolean createFolder(String folderName) {
        try {
            Path path = Paths.get(ROOT_DIR + folderName);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Error while creating folder: " + e.getMessage(), e);
        }
    }

    public boolean deleteFolder(String folderName) {
        try {
            List<FileData> files = repository.findByProjectFolder(folderName);
            if (!files.isEmpty()) {
                throw new RuntimeException("Folder is not empty");
            }
            repository.deleteByProjectFolder(folderName);
            Path path = Paths.get(ROOT_DIR + folderName);
            Files.deleteIfExists(path);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting folder: " + e.getMessage(), e);
        }
    }
}
