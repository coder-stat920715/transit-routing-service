package com.transit.routing.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Abstraction over where GTFS text files come from (classpath or directory). */
public interface GtfsSource {
    boolean exists(String fileName);

    Reader open(String fileName) throws IOException;

    static GtfsSource classpath(String basePath) {
        String base = basePath.endsWith("/") ? basePath : basePath + "/";
        return new GtfsSource() {
            @Override
            public boolean exists(String fileName) {
                return GtfsSource.class.getClassLoader().getResource(base + fileName) != null;
            }

            @Override
            public Reader open(String fileName) throws IOException {
                InputStream in = GtfsSource.class.getClassLoader().getResourceAsStream(base + fileName);
                if (in == null) {
                    throw new IOException("GTFS file not found on classpath: " + base + fileName);
                }
                return new InputStreamReader(in, StandardCharsets.UTF_8);
            }
        };
    }

    static GtfsSource directory(Path dir) {
        return new GtfsSource() {
            @Override
            public boolean exists(String fileName) {
                return Files.isRegularFile(dir.resolve(fileName));
            }

            @Override
            public Reader open(String fileName) throws IOException {
                return Files.newBufferedReader(dir.resolve(fileName), StandardCharsets.UTF_8);
            }
        };
    }

    /** "classpath:gtfs" -> classpath source, anything else -> directory. */
    static GtfsSource fromLocation(String location) {
        if (location.startsWith("classpath:")) {
            return classpath(location.substring("classpath:".length()));
        }
        return directory(Path.of(location));
    }
}
