package com.project.stockwork.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.CommonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "文件上传")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final List<String> ALLOW_EXT = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @ApiOperation("上传生产现场照片")
    @PostMapping("/upload")
    public CommonResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = FileUtil.extName(original).toLowerCase();
        if (!ALLOW_EXT.contains(ext)) {
            throw new BizException("仅支持 jpg/jpeg/png/webp/gif 图片");
        }
        String fileName = IdUtil.fastSimpleUUID() + "." + ext;
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("上传目录创建失败");
        }
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", "/uploads/" + fileName);
        result.put("name", original);
        return CommonResult.ok(result);
    }
}
