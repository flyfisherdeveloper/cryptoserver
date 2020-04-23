package com.scanner.cryptoserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MockUtil {

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
        Path path = Paths.get("C:\\dev\\mocks", fileName + ".txt");
        try {
            Files.write(path, json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createMock(String name, Object obj) {
        String json = getJson(obj);
        writeToFile(json, name);
    }
}
