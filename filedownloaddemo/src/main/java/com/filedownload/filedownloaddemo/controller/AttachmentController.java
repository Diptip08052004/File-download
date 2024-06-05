package com.filedownload.filedownloaddemo.controller;

import com.filedownload.filedownloaddemo.entity.Attachment;
import com.filedownload.filedownloaddemo.service.AttachmentService;
import com.filedownload.filedownloaddemo.service.ResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    public ResponseEntity<Resource>downloadFile(@PathVariable("fileId") String fileId) throws Exception {

        Attachment attachment=attachmentService.getAttachment(fileId);
        if(attachment == null){
            return ResponseEntity.notFound().build();
        }
        long fileSize=attachment.getData().length;
        if (fileSize > FILE_SIZE_THRESHOLD){
            downloadStatus.put(fileId,"In_PROGRESS");
            startAsyncDownload(fileId,attachment);
            return ResponseEntity.accepted().body(new ByteArrayResource("Download initiated. check status with /status/{fileId}".getBytes()));
        }else {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+attachment.getFileName()+"\"")
                    .header("File-Size",String.valueOf(fileSize))
                    .body(new ByteArrayResource(attachment.getData()));
        }
    }

    @GetMapping("/status/{fileId}")
    public ResponseEntity<String> getstatus(@PathVariable("fileId") String fileId){
        String status=downloadStatus.get(fileId);
        if (status==null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @Async
    private CompletableFuture<Void> startAsyncDownload(String fileId, Attachment attachment) {
        try {
            Thread.sleep(10000);
            downloadStatus.put(fileId,"COMPLETED");
        }catch (InterruptedException e){
            downloadStatus.put(fileId,"FAILED");
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture(null);
    }

}
