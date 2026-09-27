package fox.jason.passthrough.postman;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** Entry point: parses a Postman collection and renders it straight to DITA, no Markdown/Pandoc step. */
public final class PostmanConverter {

  private static final String URL_ENCODED = "urlencoded";
  private static final String FORM_DATA = "formdata";

  private final Slugs slugs = new Slugs();
  private final TopicNester nester = new TopicNester(slugs);

  private PostmanConverter() {}

  public static String convertToDita(File inputFile, String fallbackTitle, String sourceFileName)
      throws IOException {
    return new PostmanConverter().convert(inputFile, fallbackTitle, sourceFileName);
  }

  private String convert(File inputFile, String fallbackTitle, String sourceFileName) throws IOException {
    String content = new String(Files.readAllBytes(inputFile.toPath()), StandardCharsets.UTF_8);
    JSONObject root;
    try {
      root = (JSONObject) new JSONParser().parse(content);
    } catch (ParseException e) {
      throw new IOException("Unable to parse Postman collection", e);
    }

    JSONObject info = (JSONObject) root.get("info");
    String title = info != null && str(info, "name") != null ? str(info, "name").trim() : fallbackTitle;

    if (info != null && str(info, "description") != null) {
      addMarkdownAsTopics(str(info, "description"));
    }

    JSONArray items = (JSONArray) root.get("item");
    if (items != null) {
      for (Object o : items) {
        addItem((JSONObject) o, 1);
      }
    }

    StringBuilder out = new StringBuilder();
    out.append("<topic class=\"- topic/topic \" id=\"").append(slugs.slugify(title)).append("\">\n");
    out.append("<title class=\"- topic/title \">").append(MarkdownDita.esc(title)).append("</title>\n");
    if (sourceFileName == null) {
      out.append("<body class=\"- topic/body \"></body>\n");
    } else {
      out.append("<body class=\"- topic/body \">\n");
      out.append(specObject(sourceFileName));
      out.append("</body>\n");
    }
    out.append(nester.render());
    out.append("</topic>\n");
    return out.toString();
  }

  // Same outputclass fox.jason.passthrough.swagger uses so dita-bootstrap.ast's existing
  // ScalarApiReference template picks this up with no changes there - but @data currently
  // references the raw Postman collection, not an OpenAPI document, so Scalar can't actually
  // render it yet. This is plumbing ahead of a Postman-collection-to-OpenAPI mapper, not a
  // finished feature.
  private String specObject(String sourceFileName) {
    return "<object class=\"- topic/object \" data=\""
        + MarkdownDita.esc(sourceFileName)
        + "\" type=\"application/json\" outputclass=\"swagger-spec\">\n"
        + "<fallback class=\"- topic/fallback \"/>\n"
        + "</object>\n";
  }

  private void addMarkdownAsTopics(String markdown) {
    MarkdownDita.walk(
        MarkdownDita.parse(markdown),
        new MarkdownDita.BlockSink() {
          @Override
          public void heading(int level, String plainText, String titleDita) {
            nester.openHeading(level, plainText, titleDita, null);
          }

          @Override
          public void content(String fragment) {
            nester.appendBody(fragment);
          }
        });
  }

  private void addItem(JSONObject item, int level) {
    String name = str(item, "name");
    JSONObject request = (JSONObject) item.get("request");
    if (request != null) {
      String method = str(request, "method");
      nester.openHeading(level, name, MarkdownDita.esc(name), "swagger-" + method.toLowerCase());
      nester.appendBody(renderRequest(request, (JSONArray) item.get("response"), method));
      return;
    }

    nester.openHeading(level, name, MarkdownDita.esc(name), null);
    String description = str(item, "description");
    if (description != null) {
      nester.appendBody(renderDescriptionAsSections(description));
    }
    JSONArray innerItems = (JSONArray) item.get("item");
    if (innerItems != null) {
      for (Object o : innerItems) {
        addItem((JSONObject) o, level + 1);
      }
    }
  }

  private String renderRequest(JSONObject request, JSONArray responses, String method) {
    StringBuilder out = new StringBuilder();
    JSONObject url = (JSONObject) request.get("url");
    String raw = url != null ? str(url, "raw") : "";
    out.append("<codeblock class=\"+ topic/pre pr-d/codeblock \" outputclass=\"swagger-")
        .append(method.toLowerCase())
        .append("\">")
        .append(MarkdownDita.esc(method.toUpperCase()))
        .append(' ')
        .append(MarkdownDita.esc(withoutQuery(raw)))
        .append("</codeblock>\n");

    String description = str(request, "description");
    if (description != null) {
      out.append(renderDescriptionAsSections(description));
    }

    JSONArray headers = (JSONArray) request.get("header");
    String contentType = "markup";
    if (headers != null && !headers.isEmpty()) {
      for (Object o : headers) {
        JSONObject header = (JSONObject) o;
        if ("content-type".equalsIgnoreCase(str(header, "key"))) {
          contentType = extractType(str(header, "value"));
        }
      }
      out.append(section("Headers", keyValueTable(headers, false)));
    }

    JSONArray query = url != null ? (JSONArray) url.get("query") : null;
    if (query != null && !query.isEmpty()) {
      out.append(section("Query Parameters", keyValueTable(query, true)));
    }

    JSONObject body = (JSONObject) request.get("body");
    if (body != null) {
      out.append(section("Body", renderBody(body, contentType)));
    }

    out.append(example("Example Request", "bash", curlSnippet(request)));

    if (responses != null) {
      for (Object o : responses) {
        JSONObject response = (JSONObject) o;
        String lang = str(response, "_postman_previewlanguage");
        out.append(
            example(
                "Response " + response.get("code") + " - " + response.get("status"),
                lang == null ? "text" : lang,
                str(response, "body")));
      }
    }

    return out.toString();
  }

  private String renderBody(JSONObject body, String contentType) {
    String mode = str(body, "mode");
    if ("raw".equals(mode)) {
      String raw = str(body, "raw");
      return "<codeblock class=\"+ topic/pre pr-d/codeblock \" outputclass=\""
          + MarkdownDita.esc(contentType)
          + "\">"
          + MarkdownDita.esc(raw == null ? "" : raw)
          + "</codeblock>\n";
    }
    if (FORM_DATA.equals(mode) || URL_ENCODED.equals(mode)) {
      JSONArray items = (JSONArray) body.get(mode);
      return items == null ? "" : keyValueTable(items, true);
    }
    return "";
  }

  private String section(String title, String bodyDita) {
    return "<section class=\"- topic/section \" id=\""
        + slugs.slugify(title)
        + "\" outputclass=\"section\">\n<title class=\"- topic/title \">"
        + MarkdownDita.esc(title)
        + "</title>\n"
        + bodyDita
        + "</section>\n";
  }

  private String example(String title, String lang, String content) {
    return "<example class=\"- topic/example \" id=\""
        + slugs.slugify(title)
        + "\" outputclass=\"example\">\n<title class=\"- topic/title \">"
        + MarkdownDita.esc(title)
        + "</title>\n<codeblock class=\"+ topic/pre pr-d/codeblock \" outputclass=\""
        + MarkdownDita.esc(lang)
        + "\">"
        + MarkdownDita.esc(content == null ? "" : content)
        + "</codeblock>\n</example>\n";
  }

  private String renderDescriptionAsSections(String markdown) {
    StringBuilder out = new StringBuilder();
    boolean[] sectionOpen = {false};
    MarkdownDita.walk(
        MarkdownDita.parse(markdown),
        new MarkdownDita.BlockSink() {
          @Override
          public void heading(int level, String plainText, String titleDita) {
            if (sectionOpen[0]) {
              out.append("</section>\n");
            }
            out.append("<section class=\"- topic/section \" id=\"")
                .append(slugs.slugify(plainText))
                .append("\" outputclass=\"section\">\n<title class=\"- topic/title \">")
                .append(titleDita)
                .append("</title>\n");
            sectionOpen[0] = true;
          }

          @Override
          public void content(String fragment) {
            out.append(fragment);
          }
        });
    if (sectionOpen[0]) {
      out.append("</section>\n");
    }
    return out.toString();
  }

  private String keyValueTable(JSONArray items, boolean codeValues) {
    StringBuilder out = new StringBuilder();
    out.append("<table class=\"- topic/table \">\n<tgroup class=\"- topic/tgroup \" cols=\"3\">\n");
    for (int i = 1; i <= 3; i++) {
      out.append("<colspec class=\"- topic/colspec \" colname=\"c")
          .append(i)
          .append("\" colnum=\"")
          .append(i)
          .append("\"/>\n");
    }
    out.append("<thead class=\"- topic/thead \"><row class=\"- topic/row \">");
    out.append("<entry class=\"- topic/entry \" colname=\"c1\">Key</entry>");
    out.append("<entry class=\"- topic/entry \" colname=\"c2\">Value</entry>");
    out.append("<entry class=\"- topic/entry \" colname=\"c3\">Description</entry>");
    out.append("</row></thead>\n<tbody class=\"- topic/tbody \">\n");
    for (Object o : items) {
      JSONObject item = (JSONObject) o;
      if (Boolean.TRUE.equals(item.get("disabled"))) {
        continue;
      }
      String key = str(item, "key");
      String value = str(item, "value");
      String description = str(item, "description");
      out.append("<row class=\"- topic/row \">");
      out.append("<entry class=\"- topic/entry \" colname=\"c1\">")
          .append(cell(key, codeValues))
          .append("</entry>");
      out.append("<entry class=\"- topic/entry \" colname=\"c2\">")
          .append(cell(value, codeValues))
          .append("</entry>");
      out.append("<entry class=\"- topic/entry \" colname=\"c3\">")
          .append(description == null ? "" : MarkdownDita.esc(description))
          .append("</entry>");
      out.append("</row>\n");
    }
    out.append("</tbody>\n</tgroup>\n</table>\n");
    return out.toString();
  }

  private String cell(String value, boolean code) {
    String text = MarkdownDita.esc(value == null ? "" : value);
    return code ? "<codeph class=\"+ topic/ph pr-d/codeph \">" + text + "</codeph>" : text;
  }

  private String curlSnippet(JSONObject request) {
    String method = str(request, "method");
    JSONObject url = (JSONObject) request.get("url");
    String raw = url != null ? str(url, "raw") : "";
    List<String> snippet = new ArrayList<>();

    if ("HEAD".equals(method)) {
      snippet.add("curl -I ");
      snippet.add(raw);
    } else if ("GET".equals(method)) {
      snippet.add("curl -X " + (raw.indexOf('?') > 0 ? " -G " : "") + method);
      snippet.add("'" + withoutQuery(raw) + "'");
    } else {
      snippet.add("curl -X " + method);
      snippet.add("'" + raw + "'");
    }

    JSONArray headers = (JSONArray) request.get("header");
    if (headers != null) {
      for (Object o : headers) {
        JSONObject header = (JSONObject) o;
        snippet.add("  -H " + str(header, "key") + " :  " + str(header, "value"));
      }
    }

    if (raw.indexOf('?') > 0) {
      for (String param : raw.substring(raw.indexOf('?') + 1).split("&")) {
        snippet.add("  -d '" + param + "'");
      }
    }

    JSONObject body = (JSONObject) request.get("body");
    if (body != null) {
      String mode = str(body, "mode");
      switch (mode == null ? "" : mode) {
        case URL_ENCODED:
          List<String> text = new ArrayList<>();
          JSONArray encoded = (JSONArray) body.get(URL_ENCODED);
          if (encoded != null) {
            for (Object o : encoded) {
              JSONObject data = (JSONObject) o;
              if (!Boolean.TRUE.equals(data.get("disabled"))) {
                text.add(str(data, "key") + "=" + str(data, "value"));
              }
            }
          }
          snippet.add(" -d " + String.join("&", text));
          break;
        case "raw":
          snippet.add("  -d '" + str(body, "raw") + "'");
          break;
        case FORM_DATA:
          JSONArray formData = (JSONArray) body.get(FORM_DATA);
          if (formData != null) {
            for (Object o : formData) {
              JSONObject data = (JSONObject) o;
              if (!Boolean.TRUE.equals(data.get("disabled"))) {
                if ("file".equals(str(data, "type"))) {
                  snippet.add(" -F" + str(data, "key") + "=" + str(data, "src"));
                } else {
                  snippet.add("  -F" + str(data, "key") + "=" + str(data, "value"));
                }
              }
            }
          }
          break;
        case "file":
          snippet.add("  --data-binary" + str(body, "key") + "=" + str(body, "value"));
          break;
        default:
          snippet.add(" -d '" + str(body, "raw") + "'");
      }
    }

    return String.join("  \\\n  ", snippet);
  }

  private static String withoutQuery(String raw) {
    int index = raw.indexOf('?');
    return index != -1 ? raw.substring(0, index) : raw;
  }

  private static String extractType(String header) {
    if (header == null) {
      return "markup";
    }
    int slash = header.lastIndexOf('/');
    int plus = header.lastIndexOf('+');
    if (plus != -1) {
      return header.substring(plus + 1);
    }
    if (slash != -1) {
      return header.substring(slash + 1);
    }
    return header;
  }

  private static String str(JSONObject data, String key) {
    return (String) data.get(key);
  }
}
