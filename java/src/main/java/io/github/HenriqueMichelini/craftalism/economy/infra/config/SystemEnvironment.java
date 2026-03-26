package io.github.HenriqueMichelini.craftalism.economy.infra.config;

public class SystemEnvironment implements EnvironmentI {

    @Override
    public String getenv(String key) {
        return System.getenv(key);
    }
}
