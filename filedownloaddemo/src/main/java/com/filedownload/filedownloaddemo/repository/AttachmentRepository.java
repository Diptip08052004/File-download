package com.filedownload.filedownloaddemo.repository;

import com.filedownload.filedownloaddemo.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment,String> {

}
