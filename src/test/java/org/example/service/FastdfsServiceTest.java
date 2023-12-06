package org.example.service;

import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.io.FilenameUtils;
import org.example.MainTest;
import org.example.util.FastDfsUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class FastdfsServiceTest extends MainTest {

    @Autowired
    protected FastFileStorageClient storageClient;

    @Test
    void fastdfsTest() throws Exception {

        File file = new File("afile.data");
        Set<MetaData> metaDataSet = createMetaData();

        StorePath path = storageClient.uploadFile(Files.newInputStream(file.toPath()), file.length(),
                FilenameUtils.getExtension(file.getName()), metaDataSet);

        System.out.println(path);
    }

    private Set<MetaData> createMetaData() {
        Set<MetaData> metaDataSet = new HashSet<>();
        metaDataSet.add(new MetaData("Author", "tobato"));
        metaDataSet.add(new MetaData("CreateDate", "2016-01-05"));
        return metaDataSet;
    }
}