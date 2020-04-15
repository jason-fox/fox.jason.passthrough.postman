/*
 *  This file is part of the DITA-OT Postman DITA Plug-in project.
 *  See the accompanying LICENSE file for applicable licenses.
 */

package fox.jason.passthrough.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//
//    Entry point to the tidier function which
//    creates markdown text from a Postman collection

public class PostmanToMarkdownTask extends Task {
  private List<String> lines;
  private String src;
  private String dest;
  private JSONParser parser;

  /**
   * Creates a new <code>PostmanToMarkdownTask</code> instance.
   */
  public PostmanToMarkdownTask() {
    super();
    this.src = null;
    this.dest = null;
    this.lines = new ArrayList<>();
    this.parser = new JSONParser();
  }

  /**
   * Method setSrc.
   *
   * @param src String
   */
  public void setSrc(String src) {
    this.src = src;
  }

  /**
   * Method setDest.
   *
   * @param dest String
   */
  public void setDest(String dest) {
    this.dest = dest;
  }

  private String description(String str) {
    if (str == null) {
      return "";
    }
    return str.replaceAll("\n", "<br><br>");
  }

  /**
   * Retrieves the root URL of the request without any query parameters
   */
  private String getURL(String raw) {
    int index = raw.indexOf('?');
    return index != -1 ? raw.substring(0, raw.indexOf('?')) : raw;
  }

  /**
   * If responses are present, adds a series of response blocks to the
   * output
   */
  private void addSampleResponses(JSONArray responses) {
    if (responses.size() == 1) {
      lines.add("#### Example HTTP response {.example}\n");
    } else if (responses.size() > 1) {
      lines.add("#### Example HTTP responses {.example}\n");
    }

    for (Object o : responses) {
      JSONObject response = (JSONObject) o;
      lines.add(
        "#### Response " +
        response.get("code") +
        " - " +
        response.get("status") +
        " {.example}\n"
      );
      lines.add("```" + response.get("_postman_previewlanguage"));
      lines.add(((String) response.get("body")));
      lines.add("```");
    }
  }

  private String getRaw(JSONObject request) {
    return (String) ((JSONObject) request.get("url")).get("raw");
  }

  private String getMethod(JSONObject request) {
    return getAsString(request, "method");
  }

  private String getKey(JSONObject data) {
    return getAsString(data, "key");
  }

  private String getValue(JSONObject data) {
    return getAsString(data, "value");
  }

  private JSONArray getAsArray(JSONObject data, String key) {
    return (JSONArray) data.get(key);
  }

  private JSONObject getAsObject(JSONObject data, String key) {
    return (JSONObject) data.get(key);
  }

  private String getAsString(JSONObject data, String key) {
    return (String) data.get(key);
  }

  /**
   * Generates a sample Curl request from the request data
   */
  private void addSampleCurl(JSONObject request) {
    lines.add("#### Example Request {.example}\n");
    lines.add("```bash");

    List<String> snippet = new ArrayList<>();
    if ("HEAD".equals(getMethod(request))) {
      snippet.add("curl -I ");
      snippet.add(getRaw(request));
    } else if ("GET".equals(getMethod(request))) {
      snippet.add(
        "curl -X " +
        (getRaw(request).indexOf("?") > 0 ? " -G " : "") +
        getMethod(request)
      );
      snippet.add("'" + getURL(getRaw(request)) + "'");
    } else {
      snippet.add("curl -X " + getMethod(request));
      snippet.add("'" + getRaw(request) + "'");
    }

    for (Object o : getAsArray(request, "header")) {
      JSONObject header = (JSONObject) o;
      snippet.add("  -H " + getKey(header) + " :  " + getValue(header));
    }

    if (getRaw(request).indexOf("?") > 0) {
      String[] params = getRaw(request)
        .substring(getRaw(request).indexOf('?') + 1)
        .split("&");

      for (String param : params) {
        snippet.add("  -d '" + param + "'");
      }
    }
    if (request.get("body") != null) {
      JSONObject body = getAsObject(request, "body");
      switch (getAsString(body, "mode")) {
        case "urlencoded":
          List<String> text = new ArrayList<>();
          for (Object o : getAsArray(body, "urlencoded")) {
            JSONObject data = (JSONObject) o;
            if (!(Boolean) data.get("disabled")) {
              text.add(getKey(data) + "=" + getValue(data));
            }
          }
          snippet.add(" -d " + String.join("&", text));
          break;
        case "raw":
          snippet.add("  -d '" + getAsString(body, "raw") + "'");
          break;
        case "formdata":
          for (Object o : getAsArray(body, "formdata")) {
            JSONObject data = (JSONObject) o;
            if (!(Boolean) data.get("disabled")) {
              if ("file".equals(getAsString(data, "type"))) {
                snippet.add(
                  " -F" + getKey(data) + "=" + getAsString(data, "src")
                );
              } else {
                snippet.add("  -F" + getKey(data) + "=" + getValue(data));
              }
            }
          }
          break;
        case "file":
          snippet.add("  --data-binary" + getKey(body) + "=" + getValue(body));
          break;
        default:
          snippet.add(" -d '" + getAsString(body, "raw") + "'");
      }
    }
    lines.add(String.join("  \\\n  ", snippet));
    lines.add("```\n");
  }

  /**
   * Obtains the language from the content type
   */
  private String extractType(String header) {
    int slash = header.lastIndexOf('/');
    int plus = header.lastIndexOf('+');
    String type;

    if (plus != -1) {
      type = header.substring(plus + 1);
    } else if (slash != -1) {
      type = header.substring(slash + 1);
    } else {
      type = header;
    }
    return type;
  }

  /**
   * Amends the description switching all embedded markdown
   * description headers to section headers
   */
  private void addDescription(String desc) {
    if (desc != null) {
      for (String descLine : desc.split("\n")) {
        if (descLine.startsWith("#")) {
          lines.add(descLine.replaceAll("^#+ ", "###### ") + " {.section}");
        } else {
          lines.add(descLine);
        }
      }
    } else {
      lines.add("");
    }
  }

  /**
   *  Creates a swagger style codeblock header for the request
   */
  private void addEndpoint(JSONObject request) {
    lines.add("");
    lines.add("```swagger-" + getMethod(request));
    lines.add(getMethod(request).toUpperCase() + " " + getURL(getRaw(request)));
    lines.add("```");
    lines.add("");
  }

  /**
   * Takes the request data and creates tabular information out of
   * the Headers, Query Parameters and Body
   */
  private void addRequest(JSONObject request) {
    String contentType = "markup";
    lines.add("");
    JSONArray headers = getAsArray(request, "header");
    if (headers != null && headers.size() > 0) {
      lines.add("#### Headers {.section}\n");
      lines.add("| Key | Value | Description |");
      lines.add("| --- | ------|-------------|");
      for (Object o : headers) {
        JSONObject header = (JSONObject) o;
        if ("content-type".equals(getKey(header).toLowerCase())){
          contentType = extractType(getValue(header));
        }
        String value = getValue(header) != null ? getValue(header) : "";
        lines.add(
          "| " +
          getKey(header) +
          " | " +
          value +
          " | " +
          description(getAsString(header, "description")) +
          " |"
        );
      }
      lines.add("");
    }

    JSONArray query = getAsArray(getAsObject(request, "url"), "query");

    if (query != null && query.size() > 0) {
      lines.add("#### Query Parameters {.section}\n");
      lines.add("| Key | Value | Description |");
      lines.add("| --- | ------|-------------|");
      for (Object o : (JSONArray) query) {
        JSONObject param = (JSONObject) o;
        String value = getValue(param) != null ? getValue(param) : "";
        lines.add(
          "| `" +
          param.get("key") +
          "` | `" +
          value +
          "` | " +
          description(getAsString(param, "description")) +
          " |"
        );
      }
      lines.add("");
    }

    JSONObject body = getAsObject(request, "body");
    if (body != null) {
      lines.add("#### Body {.section}\n");
      String mode = getAsString(body, "mode");
      if ("raw".equals(mode)) {
        lines.add("```" + contentType);
        lines.add(getAsString(body, "raw"));
        lines.add("```");
      } else if ("formdata".equals(mode)) {
        for (Object o : getAsArray(body, "formdata")) {
          JSONObject param = (JSONObject) o;
          String value = getValue(param) != null ? getValue(param) : "";
          lines.add(
            "| `" +
            getKey(param) +
            "` | `" +
            value +
            "` | " +
            description(getAsString(param, "description")) +
            " |"
          );
        }
      } else if ("urlencoded".equals(mode)) {
        for (Object o : getAsArray(body, "urlencoded")) {
          JSONObject param = (JSONObject) o;
          String value = getValue(param) != null ? getValue(param) : "";
          lines.add(
            "| `" +
            getKey(param) +
            "` | `" +
            value +
            "` | " +
            description(getAsString(param, "description")) +
            " |"
          );
        }
      }
    }
  }

  /**
   * Generates the full output from the postman file
   */
  private String postmanToMarkdown(JSONObject data) {
    JSONObject info = getAsObject(data, "info");
    JSONObject item;
    JSONObject request;
    JSONArray response;
    JSONObject innerItem;
    JSONObject innerRequest;
    JSONArray innerResponse;

    if (info.get("name") != null) {
      lines.add("#  " + (getAsString(info, "name")).trim() + "\n");
    }

    if (info.get("description") != null) {
      lines.add(getAsString(info, "description") + "\n");
    }

    for (Object o : getAsArray(data, "item")) {
      item = (JSONObject) o;
      request = getAsObject(item, "request");
      response = getAsArray(item, "response");
      if (request != null) {
        lines.add(
          "#  " +
          getAsString(item, "name").trim() +
          " { .swagger-" +
          getMethod(request) +
          "}\n\n"
        );

        addEndpoint(request);
        addDescription(getAsString(request, "description"));
        addRequest(request);
        addSampleCurl(request);
        addSampleResponses(response);
      } else {
        lines.add("\n\n#  " + getAsString(item, "name").trim() + "\n\n");
        addDescription(getAsString(item, "description"));

        for (Object oo : getAsArray(item, "item")) {
          innerItem = (JSONObject) oo;
          innerRequest = getAsObject(innerItem, "request");
          innerResponse = getAsArray(innerItem, "response");
          if (innerRequest != null) {
            lines.add(
              "##  " +
              getAsString(innerItem, "name").trim() +
              " { .swagger-" +
              getMethod(innerRequest) +
              "}\n\n"
            );
            addEndpoint(innerRequest);
            addDescription((String) innerRequest.get("description"));
            addRequest(innerRequest);
            addSampleCurl(innerRequest);
            addSampleResponses(innerResponse);
          }
        }
      }
    }

    return String.join("\n", lines);
  }

  /**
   * Method execute.
   *
   * @throws BuildException if something goes wrong
   */
  @Override
  public void execute() {
    //
    //    @param src          - The Postman file to parse
    //    @param dest         - The file to write to
    //
    if (this.src == null) {
      throw new BuildException("You must supply a Postman file to parse");
    }
    if (this.dest == null) {
      throw new BuildException("You must supply a file to write to");
    }

    try {
      String data = FileUtils.readFully(new java.io.FileReader(this.src));

      JSONObject json = (JSONObject) this.parser.parse(data);
      String markdown = postmanToMarkdown(json);

      Echo task = (Echo) getProject().createTask("echo");
      task.setFile(new java.io.File(dest));
      task.setMessage(markdown);
      task.perform();
    } catch (IOException e) {
      throw new BuildException("Unable to read file", e);
    } catch (ParseException e) {
      throw new BuildException("Unable to translate JSON", e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException(e);
    }
  }
}
