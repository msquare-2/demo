package com.example.demo.utils;

import org.springframework.util.ResourceUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TemplateUtils {

    public static String loadTemplate(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(ResourceUtils.getFile(filePath).toURI())));
    }
}

