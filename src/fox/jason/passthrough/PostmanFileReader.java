package fox.jason.passthrough;

import fox.jason.passthrough.postman.PostmanConverter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class PostmanFileReader extends AbstractFileReader {

  public PostmanFileReader() {}

  @Override
  protected String runTarget(File inputFile, String title) throws IOException {
    return PostmanConverter.convertToDita(inputFile, title, null);
  }

  @Override
  protected String runTarget(File inputFile, String title, URI sourceUri) throws IOException {
    Path source = Paths.get(sourceUri);
    String specFileName = astSpecFileName(source.getFileName().toString());

    // Same collision as fox.jason.passthrough.swagger's SwaggerFileReader: object/@data must
    // point at a file distinct from this topicref's own href, or DITA-OT's job model reverts to
    // copying the raw source instead of the converted topic.
    Files.copy(
        inputFile.toPath(), source.resolveSibling(specFileName), StandardCopyOption.REPLACE_EXISTING);

    return PostmanConverter.convertToDita(inputFile, title, specFileName);
  }

  private static String astSpecFileName(String originalFileName) {
    int dot = originalFileName.lastIndexOf('.');
    return dot < 0
        ? originalFileName + ".ast"
        : originalFileName.substring(0, dot) + ".ast" + originalFileName.substring(dot);
  }
}
