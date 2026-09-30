package fox.jason.passthrough;

import fox.jason.passthrough.postman.PostmanConverter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PostmanFileReader extends AbstractFileReader {

  public PostmanFileReader() {}

  @Override
  protected String runTarget(File inputFile, String title) throws IOException {
    return PostmanConverter.convertToDita(inputFile, title, null, getDefaultLanguage());
  }

  @Override
  protected String runTarget(File inputFile, String title, URI sourceUri) throws IOException {
    Path source = Paths.get(sourceUri);
    String specFileName = "postman/" + source.getFileName();

    return PostmanConverter.convertToDita(inputFile, title, specFileName, getDefaultLanguage());
  }
}
