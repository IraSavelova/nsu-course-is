package nsu.security.demoapplication.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
public class FileController {

    private static final String UPLOAD_DIR = "/app/uploads";
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Operation(summary = "Загрузить файл на сервер")
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(
                            type = "object",
                            requiredProperties = {"file"}
                    ),
                    schemaProperties = {
                        @io.swagger.v3.oas.annotations.media.SchemaProperty(
                                name = "file",
                                schema = @Schema(type = "string", format = "binary")
                        )
                    }
            )
    )
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        if (!isAllowedImageType(file)) {
            return ResponseEntity.badRequest().body("Разрешены только файлы PNG и JPEG");
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);

        Path destination = uploadPath.resolve(file.getOriginalFilename());
        file.transferTo(destination);

        return ResponseEntity.ok("Файл сохранён: " + destination);
    }

    @GetMapping("/files")
    public ResponseEntity<Resource> getFile(@RequestParam String filename) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
        Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();

        log.info("uploadDir: {}", uploadDir);
        log.info("filePath: {}", filePath);

        if (!filePath.startsWith(uploadDir)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping(value = "/files/convert")
    public ResponseEntity<String> convertFile(@RequestParam("filename") String filename) throws IOException, InterruptedException {

        String command = "convert " + UPLOAD_DIR + "/" + filename
                + " " + UPLOAD_DIR + "/converted.jpg";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "sh", "-c", command
        );

        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка конвертации");
        }

        return ResponseEntity.ok("Файл конвертирован");
    }

    @PostMapping(value = "/files/convert_safe")
    public ResponseEntity<String> convertFileSafe(@RequestParam("filename") String filename) throws IOException, InterruptedException {

        Path uploadDir = Paths.get(UPLOAD_DIR)
                .toAbsolutePath()
                .normalize();

        Path inputFile = uploadDir
                .resolve(filename)
                .normalize();

        if (!inputFile.startsWith(uploadDir)) {
            return ResponseEntity.badRequest()
                    .body("Некорректный путь к файлу");
        }

        if (!Files.exists(inputFile)) {
            return ResponseEntity.status(404)
                    .body("Файл не найден: " + filename);
        }

        Path outputFile = uploadDir.resolve("converted_safe.jpg");
        ProcessBuilder processBuilder = new ProcessBuilder(
                "convert",
                inputFile.toString(),
                outputFile.toString()
        );
        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка конвертации");
        }

        return ResponseEntity.ok("Файл конвертирован: " + outputFile);
    }

    private static final Tika tika = new Tika();
    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg");

    private boolean isAllowedImageType(MultipartFile file) throws IOException {
        String detectedType = tika.detect(file.getInputStream());
        return ALLOWED_TYPES.contains(detectedType);
    }
}
