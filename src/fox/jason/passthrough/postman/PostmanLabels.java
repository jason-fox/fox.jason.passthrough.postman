package fox.jason.passthrough.postman;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

// Resolves generated heading labels, falling back region -> base language -> English.
final class PostmanLabels {

  private static final String DEFAULT_LANG = "en";
  private static final String RESOURCE_PATH =
      "/fox/jason/passthrough/postman/labels/labels_%s.properties";

  private final Properties properties;

  private PostmanLabels(Properties properties) {
    this.properties = properties;
  }

  static PostmanLabels forLanguage(String lang) {
    String normalized =
        lang == null || lang.trim().isEmpty() ? DEFAULT_LANG : lang.trim().toLowerCase(Locale.ROOT);

    Properties resolved = load(normalized);
    if (resolved == null && normalized.contains("-")) {
      resolved = load(normalized.substring(0, normalized.indexOf('-')));
    }
    if (resolved == null) {
      resolved = load(DEFAULT_LANG);
    }
    return new PostmanLabels(resolved);
  }

  private static Properties load(String lang) {
    String resource = String.format(RESOURCE_PATH, lang);
    try (InputStream in = PostmanLabels.class.getResourceAsStream(resource)) {
      if (in == null) {
        return null;
      }
      Properties loaded = new Properties();
      loaded.load(new InputStreamReader(in, StandardCharsets.UTF_8));
      return loaded;
    } catch (IOException e) {
      return null;
    }
  }

  String get(String key) {
    return properties.getProperty(key, key);
  }
}
