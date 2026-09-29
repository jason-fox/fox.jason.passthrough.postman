package fox.jason.passthrough.postman;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Builds a nested DITA {@code <topic>} tree from a flat stream of heading events, the way a
 * document outline nests: a heading becomes a child of the nearest earlier heading at a shallower
 * level. Content added between headings goes into the body of whichever heading is currently open
 * (or the root, if none yet).
 */
final class TopicNester {

  private static final class Frame {
    final int level;
    final String plainTitle;
    final String titleDita;
    final String outputclass;
    final StringBuilder body = new StringBuilder();
    final StringBuilder children = new StringBuilder();

    Frame(int level, String plainTitle, String titleDita, String outputclass) {
      this.level = level;
      this.plainTitle = plainTitle;
      this.titleDita = titleDita;
      this.outputclass = outputclass;
    }
  }

  private final Slugs slugs;
  private final Deque<Frame> stack = new ArrayDeque<>();

  TopicNester(Slugs slugs) {
    this.slugs = slugs;
    stack.push(new Frame(0, null, null, null));
  }

  /**
   * @param plainTitle unformatted heading text, used only to derive the topic id
   * @param titleDita rendered inline DITA for the {@code <title>} element (may contain markup)
   */
  void openHeading(int level, String plainTitle, String titleDita, String outputclass) {
    while (stack.peek().level >= level) {
      closeFrame(stack.pop());
    }
    stack.push(new Frame(level, plainTitle, titleDita, outputclass));
  }

  void appendBody(String ditaFragment) {
    stack.peek().body.append(ditaFragment);
  }

  private void closeFrame(Frame frame) {
    StringBuilder xml = new StringBuilder();
    xml.append("<topic class=\"- topic/topic \" id=\"").append(slugs.slugify(frame.plainTitle));
    if (frame.outputclass != null) {
      xml.append("\" outputclass=\"").append(frame.outputclass);
    }
    xml.append("\">\n<title class=\"- topic/title \">").append(frame.titleDita).append("</title>\n");
    xml.append("<body class=\"- topic/body \">").append(frame.body).append("</body>\n");
    xml.append(frame.children);
    xml.append("</topic>\n");
    stack.peek().children.append(xml);
  }

  /** Closes every remaining open heading and returns the whole document's body content. */
  String render() {
    while (stack.size() > 1) {
      closeFrame(stack.pop());
    }
    Frame root = stack.pop();
    return root.body.toString() + root.children;
  }
}
