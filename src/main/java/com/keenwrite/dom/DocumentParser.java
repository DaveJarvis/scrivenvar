package com.keenwrite.dom;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static com.keenwrite.events.StatusEvent.clue;
import static javax.xml.transform.OutputKeys.*;

/**
 * Responsible for initializing an XML parser.
 */
public class DocumentParser {
  private static final DocumentBuilderFactory sDocumentFactory;
  private static DocumentBuilder sDocumentBuilder;
  public static DOMImplementation sDomImplementation;

  private static final TransformerFactory sTransformerFactory =
    TransformerFactory.newInstance();
  private static Transformer sTransformer;

  static {
    sDocumentFactory = DocumentBuilderFactory.newInstance();

    sDocumentFactory.setNamespaceAware( true );
    sDocumentFactory.setIgnoringComments( true );
    sDocumentFactory.setIgnoringElementContentWhitespace( true );

    try {
      sDocumentBuilder = sDocumentFactory.newDocumentBuilder();
      sDomImplementation = sDocumentBuilder.getDOMImplementation();
      sTransformer = sTransformerFactory.newTransformer();

      sTransformer.setOutputProperty( OMIT_XML_DECLARATION, "yes" );
      sTransformer.setOutputProperty( INDENT, "no" );
      sTransformer.setOutputProperty( METHOD, "xml" );
    } catch( final Exception ex ) {
      clue( ex );
    }
  }

  /**
   * Use the {@code static} constants and methods, not an instance, at least
   * until an iterable sub-interface is written.
   */
  private DocumentParser() {}

  public static Document newDocument() {
    return sDocumentBuilder.newDocument();
  }

  /**
   * Creates a new document object model based on the given XML document
   * string. This will return an empty document if the document could not
   * be parsed.
   *
   * @param xml The document text to convert into a DOM.
   * @return The DOM that represents the given XML data.
   */
  public static Document parse( final String xml ) {
    final var input = new InputSource();

    try( final var reader = new StringReader( xml ) ) {
      input.setCharacterStream( reader );
      return sDocumentBuilder.parse( input );
    } catch( final Exception ex ) {
      clue( ex );
      return sDocumentBuilder.newDocument();
    }
  }

  /**
   * Allows an operation to be applied for every node in the document that
   * matches a given tag name pattern.
   *
   * @param document The document to traverse.
   * @param tag      The elements in the document to find (e.g., {@code *}).
   * @param consumer The consumer to call for each matching document node.
   */
  public static void walk(
    final Document document, final String tag, final Consumer<Node> consumer ) {
    assert document != null;
    assert consumer != null;

    final var nodes = document.getElementsByTagName( tag );

    if( nodes != null ) {
      for( int i = 0, len = nodes.getLength(); i < len; i++ ) {
        consumer.accept( nodes.item( i ) );
      }
    }
  }

  public static Node createMeta(
    final Document document, final Map.Entry<String, String> entry ) {
    final var node = document.createElement( "meta" );
    node.setAttribute( "name", entry.getKey() );
    node.setAttribute( "content", entry.getValue() );

    return node;
  }

  public static String toString( final Document document ) {
    try( final var writer = new StringWriter() ) {
      final var domSource = new DOMSource( document );
      final var result = new StreamResult( writer );

      sTransformer.transform( domSource, result );
      return writer.toString();
    } catch( final Exception ex ) {
      clue( ex );
      return "";
    }
  }

  /**
   * Remove whitespace, comments, and XML/DOCTYPE declarations to make
   * processing work with ConTeXt.
   *
   * @param path The SVG file to process.
   * @throws Exception The file could not be processed.
   */
  public static void sanitize( final Path path )
    throws Exception {
    final var file = path.toFile();

    sTransformer.transform(
      new DOMSource( sDocumentBuilder.parse( file ) ), new StreamResult( file )
    );
  }
}