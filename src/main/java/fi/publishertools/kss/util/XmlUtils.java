package fi.publishertools.kss.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utilities for XML parsing and manipulation.
 */
public final class XmlUtils {

    private XmlUtils() {
    }

    /**
     * Parses XML bytes into a DOM Document.
     */
    public static Document parseXml(byte[] xmlBytes) throws ParserConfigurationException, SAXException, java.io.IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = new ByteArrayInputStream(xmlBytes)) {
            return builder.parse(in);
        }
    }

    /**
     * Escapes a string for safe use in XML attribute or text content.
     */
    public static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Returns the element's local name, or tag name if local name is null (e.g. when no namespace).
     */
    public static String getElementName(Element e) {
        if (e == null) {
            return null;
        }
        String name = e.getLocalName();
        return (name != null && !name.isEmpty()) ? name : e.getTagName();
    }

    /**
     * Finds all descendant elements with the given local name.
     */
    public static List<Element> findElementsByLocalName(Element root, String localName) {
        List<Element> out = new ArrayList<>();
        collectElementsByLocalName(root, localName, out);
        return out;
    }

    private static void collectElementsByLocalName(Element e, String localName, List<Element> out) {
        if (e != null && localName.equals(getElementName(e))) {
            out.add(e);
        }
        if (e == null) {
            return;
        }
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node child = nl.item(i);
            if (child instanceof Element) {
                collectElementsByLocalName((Element) child, localName, out);
            }
        }
    }
}
