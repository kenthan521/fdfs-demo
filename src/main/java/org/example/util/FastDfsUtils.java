package org.example.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.fdfs.ThumbImageConfig;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 使用FastDFS访问和存储文件
 *
 * @author gl.dong
 * @date 2020/5/26
 */
@Slf4j
@Component("fastDfsUtils")
@ConditionalOnProperty(value = "fdfs.groupName") //判断fastdfs的配置是否存在
public class FastDfsUtils {

    @Autowired
    private FastFileStorageClient storageClient;

    @Autowired
    private ThumbImageConfig thumbImageConfig;

    @Value("${fdfs.groupName}")
    private String groupName;

    @Value("${fdfs.serverUrl}")
    public String serverUrl;

    @Value("${fdfs.fileDownUrl}")
    public String fileDownUrl;

    /**
     * 判断文件或目录是否存在
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public boolean exists(String filePath) throws Exception {
        if (filePath.contains(groupName)) {
            filePath = filePath.replace(groupName + "/", "");
        }
        FileInfo fileInfo;
        try {
            fileInfo = storageClient.queryFileInfo(groupName, filePath);
        } catch (Exception e) {
            return false;
        }
        return fileInfo != null;
    }

    /**
     * 删除文件
     *
     * @param fullRemoteFileName
     * @return
     * @throws Exception
     */
    public boolean delFile(String fullRemoteFileName) throws Exception {
        boolean flag = false;
        try {
            if (fullRemoteFileName.contains(groupName)) {
                fullRemoteFileName = fullRemoteFileName.replace(groupName + "/", "");
            }
            if (exists(fullRemoteFileName)) {
                //删除文件服务器的文件信息
                storageClient.deleteFile(groupName, fullRemoteFileName);
                flag = true;
            }
        } catch (Exception e) {
            throw new Exception("deleted remote exception by fileName(" + fullRemoteFileName + ")", e);
        }
        return flag;
    }

    /**
     * @return boolean
     * @throws
     * @Author mapabc7
     * @Description 删除本地文件
     * @Date 9:26 2020/6/1
     * @Param [fullRemoteFileName]
     */
    public boolean delLocalFile(String fullRemoteFileName) throws Exception {
        try {
            if (!fullRemoteFileName.contains(fileDownUrl)) {
                fullRemoteFileName = fileDownUrl + "/" + fullRemoteFileName;
            }
            File file = new File(fullRemoteFileName);
            // 是文件
            if (file.exists() && file.isFile()) {
                file.delete();
            } else {
                // 是文件夹
                delTempChild(file);
            }
            return true;
        } catch (Exception e) {
            throw new Exception("deleted local file exception by fileName(" + fullRemoteFileName + ")," + e.getMessage(), e);
        }
    }

    private void delTempChild(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();//获取文件夹下所有子文件夹
            //递归删除目录中的子目录下
            if (children != null) {
                for (String child : children) {
                    delTempChild(new File(file, child));
                }
            }
        } else {
            // 目录空了，进行删除
            file.delete();
        }
    }

    /**
     * 将数据流写入到远程文件系统中
     *
     * @return
     * @throws Exception
     */
    public String writeInputStreamToFile(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            StorePath storePath = storageClient.uploadFile(in, file.length(), FilenameUtils.getExtension(file.getName()), null);
            return storePath.getFullPath();
        } catch (Exception e) {
            throw new Exception("upload remote path exception," + e.getMessage(), e);
        }
    }

    /**
     * 将数据流写入到远程文件系统中
     *
     * @return
     * @throws Exception
     */
    public String writeInputStreamToFile(InputStream inputStream, long fileSize, String remoteFilename) throws Exception {
        try {
            StorePath storePath = storageClient.uploadFile(inputStream, fileSize, FilenameUtils.getExtension(remoteFilename), null);
            return storePath.getFullPath();
        } catch (Exception e) {
            throw new Exception("upload remote path exception," + e.getMessage(), e);
        } finally {
            inputStream.close();
        }
    }

    public boolean downloadToLocal(String localFilePath, String remoteDir) {
        File file = new File(localFilePath);
        return downloadToLocal(file, remoteDir);
    }

    public boolean downloadToLocal(File file, String remoteDir) {
        // 先删 后下载
        FileUtil.delFile(file);
        byte[] bytes = null;
        boolean flag = false;
        OutputStream outputStream = null;
        try {
            if (remoteDir.contains(groupName)) {
                remoteDir = remoteDir.replace(groupName + "/", "");
            }
            if (storageClient.queryFileInfo(groupName, remoteDir) != null) {
                bytes = storageClient.downloadFile(groupName, remoteDir, new DownloadByteArray());
            }
            // 创建目录
            FileUtil.newFolder(file.getParentFile());
            outputStream = new FileOutputStream(file);
            if (bytes == null || bytes.length <= 0) {
                log.error("download remote(" + remoteDir + ") to local exception, 文件系统无此文件");
                return false;
            }
            outputStream.write(bytes);
            flag = true;
        } catch (Exception e) {
            log.error("download remote:{} to local:{} path exception error:{}", remoteDir, file.getPath(), e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("文件流关闭失败:{}", e.getMessage());
                }
            }
        }
        return flag;
    }

    /**
     * 根据当前图片地址返回缩略图图片地址
     *
     * @param url 当前图片地址
     * @return 缩略图图片地址
     * @throws Exception
     * @Author gl.dong
     */
    public String getThumbFromUrl(String url) throws Exception {
        String thumbUrl = "";
        if (url.contains(groupName)) {
            url = url.replace(groupName + "/", "");
        }
        try {
            if (exists(url)) {
                thumbUrl = thumbImageConfig.getThumbImagePath(url);
            }
        } catch (Exception e) {
            throw new Exception(" get thumb url fail", e);
        }
        return thumbUrl;
    }


    /**
     * 将流转为字节
     *
     * @param inStream
     * @return
     * @throws Exception
     */
    public static byte[] inputStreamToBytes(InputStream inStream) throws Exception {
        byte[] bytes = null;
        try (ByteArrayOutputStream swapStream = new ByteArrayOutputStream()) {
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = inStream.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            bytes = swapStream.toByteArray();
        } catch (Exception e) {
            log.error("将流转为字节失败", e);
            throw new Exception("将流转为字节失败", e);
        }
        return bytes;
    }

    /**
     * 传入文件路径 将文件转为字节
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public byte[] readFileToBytes(String filePath) throws Exception {
        byte[] bytes = null;
        String group = "group1";
        try {
            if (storageClient.queryFileInfo(group, filePath) != null) {
                bytes = storageClient.downloadFile(group, filePath, new DownloadByteArray());
            }
        } catch (Exception e) {
            throw new Exception("download remote(" + filePath + ") path exception", e);
        }
        return bytes;
    }

    /**
     * 将字节流数据写入到远程文件系统中
     *
     * @param bytes
     * @return 远程文件路径
     * @throws Exception
     */
    public String writeByteArrayToFile(byte[] bytes, String remoteDir, String remoteName) throws Exception {
        InputStream in = byteToInputStream(bytes);
        return writeInputStreamToFile(in, bytes.length, remoteName);
    }

    /**
     * 字节转换InputStream
     *
     * @param in
     * @return
     */
    public static InputStream byteToInputStream(byte[] in) throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(in);
        return is;
    }

    public static void main(String[] args) {
        String a = "http://219.142.87.76:8080/" + "group1/M00/00/19/wKgFwF7QpByAGClYAAA6xbqsNdg30.docx";
        String b = a.replace("http://219.142.87.76:8080/", "");
        System.out.println(a.lastIndexOf(")"));
        System.out.println(a.substring(a.lastIndexOf(".")));
    }

}
