package com.southwind.controller;

import com.southwind.entity.Attachment;
import com.southwind.service.AttachmentService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attachment")
public class AttachmentController {

  @Autowired
  private AttachmentService attachmentService;

  @Value("${file.upload.path:./uploads}")
  private String uploadPath;

  @PostMapping("/upload")
  public ResultVO upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "businessId", required = false) Integer businessId,
                         @RequestParam(value = "businessType", required = false, defaultValue = "approval") String businessType) {
    if (file.isEmpty()) {
      return ResultVOUtil.fail("请选择文件");
    }

    String fileName = file.getOriginalFilename();
    String extension = "";
    if (fileName != null && fileName.contains(".")) {
      extension = fileName.substring(fileName.lastIndexOf("."));
    }

    String uuid = UUID.randomUUID().toString().replace("-", "");
    String newFileName = uuid + extension;
    String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    String filePath = datePath + "/" + newFileName;

    try {
      Path path = Paths.get(uploadPath, datePath);
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }

      File dest = new File(uploadPath + "/" + filePath);
      file.transferTo(dest);

      Attachment attachment = new Attachment();
      attachment.setBusinessId(businessId);
      attachment.setBusinessType(businessType);
      attachment.setFileName(fileName);
      attachment.setFilePath(filePath);
      attachment.setFileSize(file.getSize());
      attachment.setFileType(file.getContentType());
      attachment.setCreateTime(LocalDateTime.now());

      attachmentService.save(attachment);

      return ResultVOUtil.success(attachment);
    } catch (IOException e) {
      e.printStackTrace();
      return ResultVOUtil.fail("上传失败");
    }
  }

  @GetMapping("/download/{id}")
  public ResponseEntity<Resource> download(@PathVariable("id") Integer id) {
    Attachment attachment = attachmentService.getById(id);
    if (attachment == null) {
      return ResponseEntity.notFound().build();
    }

    File file = new File(uploadPath + "/" + attachment.getFilePath());
    if (!file.exists()) {
      return ResponseEntity.notFound().build();
    }

    Resource resource = new FileSystemResource(file);
    String fileName = attachment.getFileName();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .body(resource);
  }

  @GetMapping("/list")
  public ResultVO list(@RequestParam("businessId") Integer businessId,
                       @RequestParam(value = "businessType", defaultValue = "approval") String businessType) {
    List<Attachment> list = attachmentService.findByBusinessId(businessId, businessType);
    return ResultVOUtil.success(list);
  }

  @DeleteMapping("/delete/{id}")
  public ResultVO delete(@PathVariable("id") Integer id) {
    Attachment attachment = attachmentService.getById(id);
    if (attachment == null) {
      return ResultVOUtil.fail("附件不存在");
    }

    File file = new File(uploadPath + "/" + attachment.getFilePath());
    if (file.exists()) {
      file.delete();
    }

    attachmentService.removeById(id);
    return ResultVOUtil.success(null);
  }
}
