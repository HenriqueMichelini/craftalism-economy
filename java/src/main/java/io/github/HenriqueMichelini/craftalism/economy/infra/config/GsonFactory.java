package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;

public class GsonFactory {

    private static Gson instance;

    public static Gson getInstance() {
        if (instance == null) {
            instance = createGson();
        }
        return instance;
    }

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }
}