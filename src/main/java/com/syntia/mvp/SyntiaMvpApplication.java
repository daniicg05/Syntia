package com.syntia.mvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Clase principal de la aplicación Syntia MVP.
 * <p>
 * Carga las variables del archivo {@code .env} como System properties
 * antes de iniciar Spring, para que los placeholders como
 * {@code ${OPENAI_API_KEY}} se resuelvan correctamente.
 */
@SpringBootApplication
public class SyntiaMvpApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(SyntiaMvpApplication.class, args);
    }

    /**
     * Lee el archivo .env y establece cada variable como System property,
     * siempre que no exista ya como variable de entorno del SO.
     */
    private static void loadDotenv() {
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) return;

        try {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;

                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                // No sobreescribir variables de entorno reales del SO
                if (System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Si no se puede leer .env, se usan los defaults de application.properties
        }
    }
}

