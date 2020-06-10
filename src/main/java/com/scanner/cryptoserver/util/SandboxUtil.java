package com.scanner.cryptoserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class SandboxUtil {
    private static final Logger Log = LoggerFactory.getLogger(SandboxUtil.class);

    private String getJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;

        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void writeToFile(String json, String fileName) {
        //arbitrary path - the files will eventually get copied to the Resources folder
        Path path = Paths.get("C:\\dev\\mocks", fileName + ".txt");
        try {
            Files.write(path, json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the JSON from the sandbox file.
     *
     * @param name the name of the sandbox file (without the .txt extension).
     * @return the JSON if the file can be read; return an empty string otherwise.
     */
    public String getJson(String name) {
        ClassLoader cl = SandboxUtil.class.getClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
        Resource resource;
        byte[] bytes = null;
        String fileName = name + ".txt";

        //try to get the data from the sandbox
        resource = resolver.getResource("classpath:sandbox/" + fileName);
        try (InputStream stream = resource.getInputStream()) {
            bytes = new byte[stream.available()];
            stream.read(bytes);
        } catch (IOException e) {
            Log.error("Cannot read from sandbox file: {}", fileName, e);
        }
        return bytes == null ? "" : new String(bytes);
    }

    public void createMock(String name, Object obj) {
        String json = getJson(obj);
        writeToFile(json, name);
    }
}
