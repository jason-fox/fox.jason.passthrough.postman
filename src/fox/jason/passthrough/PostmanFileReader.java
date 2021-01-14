package fox.jason.passthrough;

import java.io.File;
import java.io.IOException;

public class PostmanFileReader extends PandocFileReader {

  public PostmanFileReader() {}

  private static final String ANT_FILE = "/../process_postman.xml";

  @Override
  protected String runTarget(File inputFile, String title) throws IOException {
    File markdownFile = File.createTempFile("postman", "md");
    markdownFile.deleteOnExit();
    writeToFile(
      executeAntTask(
        calculateJarPath(PostmanFileReader.class) + ANT_FILE,
        inputFile,
        title
      ),
      markdownFile
    );
    return executePandoc(markdownFile, title);
  }
}
