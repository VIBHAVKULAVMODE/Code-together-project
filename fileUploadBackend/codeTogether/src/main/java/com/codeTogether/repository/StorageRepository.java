package com.codeTogether.repository;

import com.codeTogether.entity.FileData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageRepository extends JpaRepository<FileData, Long> {

    Optional<FileData> findByFileName(String fileName);

    Optional<FileData> findByFileNameAndVersionGreaterThan(String fileName, int version);

    List<FileData> findByFileNameAndVersionLessThan(String fileName, int version);

    List<FileData> findByVersionGreaterThan(int version);

    List<FileData> findByVersionLessThan(int version);

    // New method: find a file record by its name and exact version
    Optional<FileData> findByFileNameAndVersion(String fileName, int version);

    List<FileData> findByProjectFolder(String projectFolder);

    void deleteByFileName(String fileName);

    void deleteByProjectFolder(String folderName);
}
