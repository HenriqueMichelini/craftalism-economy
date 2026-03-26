package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class ConfigLoaderTest {

    private FileConfiguration config;
    private Logger logger;
    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        CraftalismEconomy plugin = mock(CraftalismEconomy.class);
        config = mock(FileConfiguration.class);
        logger = mock(Logger.class);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);

        // For tests that don't need environment mocking, use the real environment
        loader = new ConfigLoader(plugin);
    }

    @Test
    void locale_validLocale_returnsCorrectLocale() {
        when(config.getString("locale", "en-US")).thenReturn("pt-BR");

        Locale result = loader.locale();

        assertEquals(Locale.forLanguageTag("pt-BR"), result);
    }

    @Test
    void locale_invalidLocale_fallsBackToUS() {
        when(config.getString("locale", "en-US")).thenReturn("INVALID_LOCALE");

        Locale result = loader.locale();

        assertEquals(Locale.US, result);
        verify(logger).warning(
            "Invalid locale 'INVALID_LOCALE', defaulting to en-US"
        );
    }

    @Test
    void currencySymbol_readsValueOrDefault() {
        when(config.getString("currency-symbol", "$")).thenReturn("€");

        assertEquals("€", loader.currencySymbol());
    }

    @Test
    void nullRepresentation_readsValueOrDefault() {
        when(config.getString("null-representation", "—")).thenReturn("N/A");

        assertEquals("N/A", loader.nullRepresentation());
    }

    @Test
    void defaultBalance_withPositiveValue_returnsConfigured() {
        when(config.getLong("default-balance", 100_000_000L)).thenReturn(
            500_000L
        );

        assertEquals(500_000L, loader.defaultBalance());
    }

    @Test
    void defaultBalance_withNegativeValue_returnsFallback() {
        when(config.getLong("default-balance", 100_000_000L)).thenReturn(-50L);

        long result = loader.defaultBalance();

        assertEquals(100_000_000L, result);
        verify(logger).warning(
            "Invalid negative default balance, using 100000000"
        );
    }

    @Test
    void baseUrl_readsValueFromConnectionConfig() {
        // Create a fresh plugin mock
        CraftalismEconomy plugin = mock(CraftalismEconomy.class);
        // Mock the environment to return null for the env var
        EnvironmentI environment = mock(EnvironmentI.class);
        when(environment.getenv("CRAFTALISM_API_URL")).thenReturn(null);

        try (
            MockedConstruction<ConnectionConfig> mocked = mockConstruction(
                ConnectionConfig.class,
                (connectionConfig, context) ->
                    when(connectionConfig.getUrl()).thenReturn(
                        "https://external-api.test"
                    )
            )
        ) {
            // Use the test constructor with mocked environment
            ConfigLoader loader = new ConfigLoader(plugin, environment);

            assertEquals("https://external-api.test", loader.baseUrl());
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void oauthFields_readFromConnectionConfig() {
        CraftalismEconomy plugin = mock(CraftalismEconomy.class);
        // Mock all environment variables to be null so we fall back to connection config
        EnvironmentI environment = mock(EnvironmentI.class);
        when(environment.getenv("AUTH_ISSUER_URI")).thenReturn(null);
        when(environment.getenv("AUTH_TOKEN_PATH")).thenReturn(null);
        when(environment.getenv("MINECRAFT_CLIENT_ID")).thenReturn(null);
        when(environment.getenv("MINECRAFT_CLIENT_SECRET")).thenReturn(null);
        when(environment.getenv("CRAFTALISM_API_KEY")).thenReturn(null);
        when(environment.getenv("MINECRAFT_CLIENT_SCOPES")).thenReturn(null);

        try (
            MockedConstruction<ConnectionConfig> mocked = mockConstruction(
                ConnectionConfig.class,
                (connectionConfig, context) -> {
                    when(connectionConfig.getAuthServerUrl()).thenReturn(
                        "https://auth.example.test"
                    );
                    when(connectionConfig.getTokenPath()).thenReturn(
                        "/realms/token"
                    );
                    when(connectionConfig.getClientId()).thenReturn(
                        "minecraft-server"
                    );
                    when(connectionConfig.getClientSecret()).thenReturn(
                        "super-secret"
                    );
                    when(connectionConfig.getScopes()).thenReturn(
                        "api:read api:write"
                    );
                }
            )
        ) {
            ConfigLoader loader = new ConfigLoader(plugin, environment);

            assertEquals("https://auth.example.test", loader.authServerUrl());
            assertEquals("/realms/token", loader.tokenPath());
            assertEquals("minecraft-server", loader.clientId());
            assertEquals("super-secret", loader.clientSecret());
            assertEquals("api:read api:write", loader.oauthScopes());
            assertEquals(1, mocked.constructed().size());
        }
    }
}
