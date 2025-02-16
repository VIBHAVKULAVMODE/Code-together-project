package com.codeTogether.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "project_files", indexes = {
    @Index(name = "idx_file_name_version", columnList = "fileName, version")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;
    private String uploadedBy;
    private String role;
    private String projectFolder;

    @Lob
    @Column(name = "file_content", length = 1000)
    private byte[] fileContent;

    private String uploadDate;

    // New field for version control
    private int version;
}
