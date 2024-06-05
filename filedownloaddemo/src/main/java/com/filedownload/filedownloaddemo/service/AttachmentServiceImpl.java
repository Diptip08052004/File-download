package com.filedownload.filedownloaddemo.service;

import com.filedownload.filedownloaddemo.entity.Attachment;
import com.filedownload.filedownloaddemo.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class AttachmentServiceImpl implements AttachmentService{

    private AttachmentRepository attachmentRepository;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public Attachment saveAttachment(MultipartFile file) throws Exception {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            // Validate file name
            if (fileName.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence: " + fileName);
            }

            Attachment attachment = new Attachment(fileName, file.getContentType(), file.getBytes());
            return attachmentRepository.save(attachment);

        } catch (IOException ex) {
            throw new Exception("Failed to read file bytes: " + fileName, ex);
        } catch (IllegalArgumentException ex) {
            throw new Exception("Invalid file path: " + fileName, ex);
        } catch (Exception ex) {
            throw new Exception("Could not save file: " + fileName, ex);
        }
    }

    @Override
    public Attachment getAttachment(String fileId) throws Exception {
        return attachmentRepository.findById(fileId).orElseThrow(
                ()->new Exception("File not Found with id "+fileId));
    }
}
