package com.filedownload.filedownloaddemo.controller;

import com.filedownload.filedownloaddemo.entity.Attachment;
import com.filedownload.filedownloaddemo.service.AttachmentService;
import com.filedownload.filedownloaddemo.service.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@EnableAsync
public class AttachmentController {

    private AttachmentService attachmentService;
    private ConcurrentHashMap<String,String> downloadStatus = new ConcurrentHashMap<>();
    private static final long FILE_SIZE_THRESHOLD = 1000000L;

    @Autowired
    public AttachmentController(AttachmentService attachmentService){
        this.attachmentService=attachmentService;
    }

    @PostMapping("/upload")
    public ResponseData uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        Attachment attachment=null;
        String downloadURL="";
        attachment=attachmentService.saveAttachment(file);
        downloadURL= ServletUriComponentsBuilder.fromCurrentContextPath().path("/download/").path(attachment.getId()).toUriString();

        return new ResponseData(attachment.getFileName(),downloadURL,file.getContentType(),file.getSize());
    }

    @GetMapping("download/{fileId}")
    public ResponseEntity<Resource>downloadFile(@PathVariable("fileId") String fileId, HttpServletRequest request) throws Exception {
        Attachment attachment=attachmentService.getAttachment(fileId);
        if(attachment == null){
            return ResponseEntity.notFound().build();
        }
        long fileSize=attachment.getData().length;
        if (fileSize > FILE_SIZE_THRESHOLD){
            downloadStatus.putIfAbsent(fileId,"In_PROGRESS");
            startAsyncDownload(fileId,attachment,request);
            return ResponseEntity.accepted().body(new ByteArrayResource("Download initiated. check status with /status/{fileId}".getBytes()));
        }else {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+attachment.getFileName()+"\"")
                    .header("File-Size",String.valueOf(fileSize))
                    .body(new ByteArrayResource(attachment.getData()));
        }
    }

//    @GetMapping("/status/{fileId}")
//    public ResponseEntity<String> getstatus(@PathVariable("fileId") String fileId){
//        String status=downloadStatus.get(fileId);
//        if (status==null){
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(status);
//    }

    @Async
    private CompletableFuture<Void> startAsyncDownload(String fileId, Attachment attachment, HttpServletRequest request) {
        try {
            long start=0;
            long end=attachment.getData().length-1;
            String range=request.getHeader("Range");
            if (range!=null){
                String[] rangeValues=range.substring("bytes=".length()).split("-");
                start = Long.parseLong(rangeValues[0]);
                if (rangeValues.length>1){
                    end=Long.parseLong(rangeValues[1]);
                }
            }
            File outputFile=new File("downloaded_file.txt");
            if (outputFile.exists()){
                start=outputFile.length();
            }
            HttpURLConnection httpFileConnection=(HttpURLConnection) new URL("http://localhost:8090/download/"+fileId).openConnection();
            httpFileConnection.setRequestProperty("Range","bytes="+start+"-"+end);
            httpFileConnection.connect();
            int responseCode=httpFileConnection.getResponseCode();
            if (responseCode == 206){
                try (BufferedInputStream in = new BufferedInputStream(httpFileConnection.getInputStream());
                     FileOutputStream fileOutputStream=new FileOutputStream(outputFile,true)){
                    byte dataBuffer[]=new byte[1024];
                    int bytesRead;
                    while ((bytesRead=in.read(dataBuffer,0,1024))!=-1){
                        fileOutputStream.write(dataBuffer,0,bytesRead);
                    }
                }
            }else {
                downloadStatus.put(fileId,"FAILD");
            }

        } catch (Exception e) {
            downloadStatus.put(fileId, "FAILED");
        } finally {
            downloadStatus.remove(fileId);
        }
        return CompletableFuture.completedFuture(null);
    }

}
