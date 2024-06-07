package com.filedownload.filedownloaddemo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid",strategy = "uuid2")
    private String id;
    private String fileName;
    private  String fileType;

    @Lob
    @Column(length = 65555)
    private byte[] data;

    public Attachment(String fileName, String fileType, byte[] data) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
    }

    @Column(nullable = false)
    private long fileSize;

    public void setFileSize(long fileSize){
        this.fileSize=fileSize;
    }

    public long getFileSize(){
        return data.length;
    }


}
