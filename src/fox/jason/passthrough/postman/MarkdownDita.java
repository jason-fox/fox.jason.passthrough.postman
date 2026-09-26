package fox.jason.passthrough.postman;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.Collections;

/** Parses Markdown with flexmark and renders it directly to DITA fragments. */
final class MarkdownDita {

  private static final Parser PARSER =
      Parser.builder(
              new MutableDataSet()
                  .set(Parser.EXTENSIONS, Collections.singletonList(TablesExtension.create())))
          .build();

  private MarkdownDita() {}

  interface BlockSink {
    void heading(int level, String plainText, String titleDita);

    void content(String ditaFragment);
  }

  static Node parse(String markdown) {
    return PARSER.parse(markdown == null ? "" : markdown);
  }

  /** Walks the top-level blocks of a parsed document, reporting headings separately from content. */
  static void walk(Node document, BlockSink sink) {
    for (Node block : document.getChildren()) {
      if (block instanceof Heading) {
        Heading heading = (Heading) block;
        sink.heading(heading.getLevel(), plainText(heading), renderChildren(heading));
      } else {
        String fragment = renderBlock(block);
        if (!fragment.isEmpty()) {
          sink.content(fragment);
        }
      }
    }
  }

  static String renderBlock(Node block) {
    if (block instanceof Paragraph) {
      Node onlyChild = block.getFirstChild();
      if (onlyChild instanceof Image && onlyChild.getNext() == null) {
        return renderFigure((Image) onlyChild);
      }
      return "<p class=\"- topic/p \">" + renderChildren(block) + "</p>\n";
    }
    if (block instanceof BulletList) {
      return renderList(block, "ul");
    }
    if (block instanceof OrderedList) {
      return renderList(block, "ol");
    }
    if (block instanceof BlockQuote) {
      StringBuilder out = new StringBuilder();
      for (Node child : block.getChildren()) {
        out.append(renderBlock(child));
      }
      return out.toString();
    }
    if (block instanceof FencedCodeBlock) {
      FencedCodeBlock code = (FencedCodeBlock) block;
      String lang = code.getInfo().toString().trim();
      return "<codeblock class=\"+ topic/pre pr-d/codeblock \""
          + (lang.isEmpty() ? "" : " outputclass=\"" + esc(lang) + "\"")
          + ">"
          + esc(trimTrailingNewline(code.getContentChars().toString()))
          + "</codeblock>\n";
    }
    if (block instanceof IndentedCodeBlock) {
      return "<codeblock class=\"+ topic/pre pr-d/codeblock \">"
          + esc(trimTrailingNewline(((IndentedCodeBlock) block).getContentChars().toString()))
          + "</codeblock>\n";
    }
    if (block instanceof TableBlock) {
      return renderTable((TableBlock) block);
    }
    if (block instanceof ThematicBreak) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (Node child : block.getChildren()) {
      out.append(renderBlock(child));
    }
    return out.toString();
  }

  private static String renderList(Node list, String tag) {
    StringBuilder out = new StringBuilder();
    String itemClass = "- topic/li ";
    out.append("<").append(tag).append(" class=\"- topic/").append(tag).append(" \">\n");
    for (Node item : list.getChildren()) {
      if (!(item instanceof ListItem)) {
        continue;
      }
      out.append("<li class=\"").append(itemClass).append("\">");
      boolean first = true;
      for (Node itemBlock : item.getChildren()) {
        if (first && itemBlock instanceof Paragraph) {
          out.append(renderChildren(itemBlock));
        } else {
          out.append(renderBlock(itemBlock));
        }
        first = false;
      }
      out.append("</li>\n");
    }
    out.append("</").append(tag).append(">\n");
    return out.toString();
  }

  private static String renderFigure(Image image) {
    String alt = renderChildren(image);
    StringBuilder out = new StringBuilder();
    out.append("<fig class=\"- topic/fig \">\n");
    if (!alt.isEmpty()) {
      out.append("<title class=\"- topic/title \">").append(alt).append("</title>\n");
    }
    out.append("<image class=\"- topic/image \" href=\"")
        .append(esc(image.getUrl().toString()))
        .append("\" scalefit=\"yes\" scope=\"external\"/>\n");
    out.append("</fig>\n");
    return out.toString();
  }

  private static String renderTable(TableBlock table) {
    StringBuilder out = new StringBuilder();
    java.util.List<java.util.List<String>> headerRows = new java.util.ArrayList<>();
    java.util.List<java.util.List<String>> bodyRows = new java.util.ArrayList<>();
    int cols = 0;
    for (Node section : table.getChildren()) {
      boolean isHead = section instanceof com.vladsch.flexmark.ext.tables.TableHead;
      boolean isBody = section instanceof com.vladsch.flexmark.ext.tables.TableBody;
      if (!isHead && !isBody) {
        continue;
      }
      for (Node rowNode : section.getChildren()) {
        if (!(rowNode instanceof TableRow)) {
          continue;
        }
        java.util.List<String> cells = new java.util.ArrayList<>();
        for (Node cellNode : rowNode.getChildren()) {
          if (cellNode instanceof TableCell) {
            cells.add(renderChildren(cellNode));
          }
        }
        cols = Math.max(cols, cells.size());
        (isHead ? headerRows : bodyRows).add(cells);
      }
    }
    if (cols == 0) {
      return "";
    }
    out.append("<table class=\"- topic/table \">\n<tgroup class=\"- topic/tgroup \" cols=\"")
        .append(cols)
        .append("\">\n");
    for (int i = 1; i <= cols; i++) {
      out.append("<colspec class=\"- topic/colspec \" colname=\"c")
          .append(i)
          .append("\" colnum=\"")
          .append(i)
          .append("\"/>\n");
    }
    if (!headerRows.isEmpty()) {
      out.append("<thead class=\"- topic/thead \">\n");
      for (java.util.List<String> row : headerRows) {
        out.append(renderTableRow(row, cols));
      }
      out.append("</thead>\n");
    }
    out.append("<tbody class=\"- topic/tbody \">\n");
    for (java.util.List<String> row : bodyRows) {
      out.append(renderTableRow(row, cols));
    }
    out.append("</tbody>\n</tgroup>\n</table>\n");
    return out.toString();
  }

  private static String renderTableRow(java.util.List<String> cells, int cols) {
    StringBuilder out = new StringBuilder("<row class=\"- topic/row \">");
    for (int i = 0; i < cols; i++) {
      out.append("<entry class=\"- topic/entry \" colname=\"c")
          .append(i + 1)
          .append("\">")
          .append(i < cells.size() ? cells.get(i) : "")
          .append("</entry>");
    }
    out.append("</row>\n");
    return out.toString();
  }

  private static String renderChildren(Node node) {
    StringBuilder out = new StringBuilder();
    for (Node child : node.getChildren()) {
      out.append(renderInline(child));
    }
    return out.toString();
  }

  private static String renderInline(Node node) {
    if (node instanceof Text) {
      return esc(((Text) node).getChars().unescape());
    }
    if (node instanceof Emphasis) {
      return "<i class=\"+ topic/ph hi-d/i \">" + renderChildren(node) + "</i>";
    }
    if (node instanceof StrongEmphasis) {
      return "<b class=\"+ topic/ph hi-d/b \">" + renderChildren(node) + "</b>";
    }
    if (node instanceof Code) {
      return "<codeph class=\"+ topic/ph pr-d/codeph \">"
          + esc(((Code) node).getText().unescape())
          + "</codeph>";
    }
    if (node instanceof Image) {
      Image image = (Image) node;
      String alt = renderChildren(image);
      return "<image class=\"- topic/image \" href=\""
          + esc(image.getUrl().toString())
          + "\" scalefit=\"yes\" scope=\"external\">"
          + (alt.isEmpty() ? "" : "<alt class=\"- topic/alt \">" + alt + "</alt>")
          + "</image>";
    }
    if (node instanceof Link) {
      Link link = (Link) node;
      String url = link.getUrl().toString();
      boolean internal = url.startsWith("#");
      return "<xref class=\"- topic/xref \" href=\""
          + esc(url)
          + "\""
          + (internal ? " format=\"dita\"" : " format=\"html\" scope=\"external\"")
          + ">"
          + renderChildren(link)
          + "</xref>";
    }
    if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
      return "\n";
    }
    return renderChildren(node);
  }

  private static String plainText(Node node) {
    StringBuilder out = new StringBuilder();
    collectPlainText(node, out);
    return out.toString().trim();
  }

  private static void collectPlainText(Node node, StringBuilder out) {
    if (node instanceof Text) {
      out.append(((Text) node).getChars().unescape());
      return;
    }
    for (Node child : node.getChildren()) {
      collectPlainText(child, out);
    }
  }

  private static String trimTrailingNewline(String text) {
    return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
  }

  static String esc(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&':
          sb.append("&amp;");
          break;
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }
}
