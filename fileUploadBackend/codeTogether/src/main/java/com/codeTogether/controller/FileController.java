package com.codeTogether.controller;

import com.codeTogether.entity.FileData;
import com.codeTogether.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/files")
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);
    private static final Path PUBLIC_DIR = Paths.get(System.getProperty("user.dir"), "public");

    @Autowired
    private StorageService storageService;

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestParam("files[]") List<MultipartFile> files) throws IOException {
        for (MultipartFile file : files) {
            String fileName = FilenameUtils.separatorsToSystem(file.getOriginalFilename());
            Path filePath = PUBLIC_DIR.resolve(fileName);
            if (Files.notExists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("write file: [{}] {}", file.getSize(), filePath);
        }
        return "ok";
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/folders")
    public List<String> listFolders() throws IOException {
        List<String> folderAndFilesList = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(PUBLIC_DIR)) {
            folderAndFilesList = paths
                    .map(PUBLIC_DIR::relativize)
                    .map(Path::toString)
                    .map(s -> s.replace(File.separator, "/"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return folderAndFilesList;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/foldersOnly")
    public List<String> listFoldersOnly() throws IOException {
        List<String> folderList = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(PUBLIC_DIR)) {
            folderList = paths.filter(Files::isDirectory)
                    .map(PUBLIC_DIR::relativize)
                    .map(Path::toString)
                    .map(s -> s.replace(File.separator, "/"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return folderList;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/folders/**")
    public List<String> getFilesAndSubdirectoriesInFolder(@PathVariable String folderName) throws IOException {
        List<String> filesAndSubdirectories = new ArrayList<>();
        Path folderPath = PUBLIC_DIR.resolve(folderName);
        if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
            try (Stream<Path> paths = Files.walk(folderPath)) {
                filesAndSubdirectories = paths.map(folderPath::relativize)
                        .map(Path::toString)
                        .map(s -> s.replace(File.separator, "/"))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
        } else {
            throw new IOException("Folder does not exist or is not a directory");
        }
        return filesAndSubdirectories;
    }

    // Endpoint to upload a file with version control
    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/fileUpload")
    public ResponseEntity<FileData> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("role") String role,
            @RequestParam("projectFolder") String projectFolder) throws IOException {
        FileData savedFile = storageService.uploadFile(file, uploadedBy, role, projectFolder);
        return new ResponseEntity<>(savedFile, HttpStatus.OK);
    }

    // Endpoint to get all active files (version > 0)
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/all")
    public ResponseEntity<List<FileData>> getAllActiveFiles() {
        List<FileData> files = storageService.getActiveFiles();
        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    // Endpoint to download a file by its active version
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) throws IOException {
        byte[] fileData = storageService.downloadFile(fileName);
        return ResponseEntity.ok(fileData);
    }

    // Endpoint to delete a file by its active version
    @CrossOrigin(origins = "http://localhost:5173")
    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            storageService.deleteFile(decodedFileName);
            return new ResponseEntity<>("File deleted successfully: " + decodedFileName, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoint to update a file (version control applies)
    @CrossOrigin(origins = "http://localhost:5173")
    @PutMapping("/update/{fileName}")
    public ResponseEntity<FileData> updateFile(
            @PathVariable String fileName,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("projectFolder") String projectFolder) throws IOException {
        FileData updatedFile = storageService.updateFile(file, fileName, uploadedBy, projectFolder);
        return new ResponseEntity<>(updatedFile, HttpStatus.OK);
    }
    
    // Endpoint to download a specific version of a file (previous version)
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/download/previous/{fileName}/{version}")
    public ResponseEntity<byte[]> downloadPreviousVersion(
            @PathVariable String fileName,
            @PathVariable int version) throws IOException {
        byte[] fileData = storageService.downloadFileByVersion(fileName, version);
        return ResponseEntity.ok(fileData);
    }

    // Endpoint for rollback: restore a previous version as the active version.
    @CrossOrigin(origins = "http://localhost:5173")
    @PutMapping("/rollback/{fileName}/{version}")
    public ResponseEntity<FileData> rollbackFile(
            @PathVariable String fileName,
            @PathVariable int version) throws IOException {
        FileData rolledBackFile = storageService.rollbackFile(fileName, version);
        return new ResponseEntity<>(rolledBackFile, HttpStatus.OK);
    }

    // Endpoint to create a folder
    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/folder/create")
    public ResponseEntity<String> createFolder(@RequestParam("folderName") String folderName) {
        boolean isCreated = storageService.createFolder(folderName);
        if (isCreated) {
            return new ResponseEntity<>("Folder created successfully", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Failed to create folder", HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint to view files in a folder
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/folder/{folderName}")
    public ResponseEntity<List<FileData>> viewFolder(@PathVariable String folderName) {
        List<FileData> filesInFolder = storageService.getAllFilesInFolder(folderName);
        return new ResponseEntity<>(filesInFolder, HttpStatus.OK);
    }

    // Endpoint to delete a folder
    @CrossOrigin(origins = "http://localhost:5173")
    @DeleteMapping("/folder/delete/{folderName}")
    public ResponseEntity<String> deleteFolder(@PathVariable String folderName) {
        boolean isDeleted = storageService.deleteFolder(folderName);
        if (isDeleted) {
            return new ResponseEntity<>("Folder deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Failed to delete folder", HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint to get previous versions of a specific file (version < 0)
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/previous/{fileName}")
    public ResponseEntity<List<FileData>> getPreviousVersions(@PathVariable String fileName) {
        List<FileData> previousVersions = storageService.getPreviousVersions(fileName);
        return new ResponseEntity<>(previousVersions, HttpStatus.OK);
    }

    // Endpoint to get all previous versions of all files (version < 0)
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/previous")
    public ResponseEntity<List<FileData>> getAllPreviousVersions() {
        List<FileData> previousVersions = storageService.getAllPreviousVersions();
        return new ResponseEntity<>(previousVersions, HttpStatus.OK);
    }
}
