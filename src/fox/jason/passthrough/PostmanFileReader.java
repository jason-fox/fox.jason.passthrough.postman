package fox.jason.passthrough;

import fox.jason.passthrough.postman.PostmanConverter;
import java.io.File;
import java.io.IOException;

public class PostmanFileReader extends AbstractFileReader {

  public PostmanFileReader() {}

  @Override
  protected String runTarget(File inputFile, String title) throws IOException {
    return PostmanConverter.convertToDita(inputFile, title);
  }
}
