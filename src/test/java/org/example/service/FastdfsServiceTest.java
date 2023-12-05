package org.example.service;

import org.example.MainTest;
import org.example.util.FastDfsUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;


class FastdfsServiceTest extends MainTest {

    @Autowired
    private FastDfsUtils fastDfsUtils;

    @Test
    void fastdfsTest() throws Exception {
        String serverUrl = fastDfsUtils.serverUrl;
        System.out.println(serverUrl);

        File file = new File("afile.data");
        String s = fastDfsUtils.writeInputStreamToFile(file);
        System.out.println(s);
    }
}