/*
 */
package com.requea.dysoweb.util.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;




/**
 * Utilities to manipulate w3c DOM trees.
 * @author Pierre Dubois
 */
public class XMLUtils {

    private static DocumentBuilderFactory fFactory;
    private static Stack fParsersPool = new Stack();

    static {
        fFactory =
            DocumentBuilderFactory.newInstance();
        fFactory.setNamespaceAware(true);
        fFactory.setIgnoringElementContentWhitespace(true);
        fFactory.setValidating(false);
    }
    
    public static synchronized DocumentBuilder getParser() throws ParserConfigurationException {
        if(fParsersPool.isEmpty()) {
            // create a new parser
            DocumentBuilder builder = fFactory.newDocumentBuilder();
            return builder;
        } else {
            return (DocumentBuilder)fParsersPool.pop();
        }
    }
    public static synchronized void releaseParser(DocumentBuilder parser) {
        if(parser != null)
            fParsersPool.push(parser);
    }
    
    /**
     * Parse an xml document as an imput stream.
     * @param is
     * @return
     * @throws XMLException
     */
    public static Document parse(InputStream is) throws XMLException {
        DocumentBuilder builder = null;
        try {
            builder = getParser();
            // parse the document
            try {
                Document doc = builder.parse(is);
                return doc;
            } catch (SAXParseException e) {
                String msg = e.getLocalizedMessage();
                msg += " line:" + e.getLineNumber();
                throw new XMLException(msg);
            } catch (SAXException e) {
                throw new XMLException(e);
            } catch (IOException e) {
                throw new XMLException(e);
            }
        } catch (ParserConfigurationException e) {
            throw new XMLException(e);
        } finally {
            releaseParser(builder);
        }
    }

    /**
     * Parses an xml document as a string.
     * @param xml
     * @return
     * @throws XMLException
     */
    public static Document parse(String xml) throws XMLException {
        DocumentBuilder builder = null;
        try {
            builder = getParser();
            // parse the document
            try {
                InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                Document doc = builder.parse(is);
                return doc;
            } catch (SAXException e) {    
                throw new XMLException(e);
            } catch (IOException e) {
                throw new XMLException(e); 
            }
        } catch (ParserConfigurationException e) {
            throw new XMLException(e);
        } finally {
            releaseParser(builder);
        }
    }


    /**
     * Creates a new and empty document.
     * @return
     */
    public static Document newDocument() {
        DocumentBuilder builder = null;
        try {
            builder = getParser();
            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } finally {
            releaseParser(builder);
        }
    }

    /**
     * Creates a new document with a document root element.
     * @param name
     * @return
     */
    public static Element newElement(String name) {
        DocumentBuilder builder = null;
        try {
            builder = getParser();
            Document doc = builder.newDocument();
            Element el = doc.createElement(name);
            doc.appendChild(el);
            return el;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } finally {
            releaseParser(builder);
        }
    }

    /**
     * Serialize an element to a string.
     * @param element
     * @return
     */
    public static String ElementToString(Element element) {
        return privateElementToString(element, true, true);
    }

    public static String ElementToString(Element element, boolean pretty) {
        return privateElementToString(element, true, pretty);
    }

    /**
     * Serialize a document to a string
     * @param doc
     * @return
     */
    public static String DocumentToString(Document doc) {
        return privateElementToString(doc.getDocumentElement(), false, true);
    }
    public static String DocumentToString(Document doc, boolean pretty) {
        return privateElementToString(doc.getDocumentElement(), false, pretty);
    }

    /**
     * Get the first child element to match a given name. 
     * Look for a child element in the same namespace as the parent.
     * 
     * @param parent
     * @param name
     * @return
     */
    public static Element getChild(Element parent, String name) {
        Element child = getFirstChild(parent);
        while(child != null) {
            String tagName = child.getTagName(); 
            if(tagName != null && tagName.equals(name)) {
                return child;
            }
            if(child.getPrefix() != null && child.getPrefix().equals(parent.getPrefix()) && child.getLocalName() != null && child.getLocalName().equals(name)) {
                return child;
            }
            child = getNext(child);
        }
        return child;
    }

    /**
     * Get the first child element to match a given name. 
     * Look for a child element in the same namespace as the parent.
     * 
     * @param parent
     * @param name
     * @return
     */
    public static Element getChild(Element parent, String ns, String name) {
        Element child = getFirstChild(parent);
        while(child != null) {
            if(child.getLocalName().equals(name)) {
                if(ns == null && child.getNamespaceURI() == null) {
                    return child;
                } else if(ns != null && ns.equals(child.getNamespaceURI())) {
                    return child;
                }
            }
            child = getNext(child);
        }
        return child;
    }

    /**
     * Get the first child element of an element.
     * @param el
     * @return
     */
    public static Element getFirstChild(Element el) {
        if(el == null) {
            return null;
        }
        NodeList lst = el.getChildNodes();
        int len = lst.getLength();
        for (int i = 0; i < len; i++) {
            Node n = lst.item(i);
            if (n instanceof Element)
                return (Element) n;
        }
        return null;
    }
    
    /**
     * Get the next sibling element of a given element.
     * @param el
     * @return
     */
    public static Element getNext(Element el) {
        Node n = el.getNextSibling();
        while (n != null && !(n instanceof Element)) {
            // get the next one
            n = n.getNextSibling();
        }

        if (n instanceof Element) {
            return (Element) n;
        }
        // else, nothing to return
        return null;
    }

    /**
     * Get the next sibling element of a given element.
     * @param el
     * @return
     */
    public static Element getNextSibling(Element el) {
        String tagName = el.getTagName();
        if(tagName == null) {
            return null;
        }
        Node n = el.getNextSibling();
        while (n != null && (
                !(n instanceof Element) || 
                !tagName.equals(((Element)n).getTagName()))) {
            // get the next one
            n = n.getNextSibling();
        }

        if (n instanceof Element) {
            return (Element) n;
        } else {
            // else, nothing to return
            return null;
        }
    }

    /**
     * Get the previous sibling element of a given element.
     * @param el
     * @return
     */
    public static Element getPrevious(Element el) {
        Node n = el.getPreviousSibling();
        while (n != null && !(n instanceof Element)) {
            // get the next one
            n = n.getPreviousSibling();
        }

        if (n instanceof Element) {
            return (Element) n;
        } else {
            // else, nothing to return
            return null;
        }
    }

    /**
     * Get the previous sibling element of a given element.
     * @param el
     * @return
     */
    public static Element getPreviousSibling(Element el) {
        Node n = el.getPreviousSibling();
        while (n != null && ( 
                !(n instanceof Element) || 
                !el.getTagName().equals(((Element)n).getTagName()))) {
            // get the next one
            n = n.getPreviousSibling();
        }

        if (n instanceof Element) {
            return (Element) n;
        } else {
            // else, nothing to return
            return null;
        }
    }

    /**
     * Returns the text value of an element.
     * @param el
     * @return
     */
    public static String getTextValue(Element el) {
        StringBuffer b = new StringBuffer();
        // retrieve the text node child
        NodeList nl = el.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node n = nl.item(i);
            if (n instanceof Text) {
                Text t = (Text) n;
                b.append(t.getData());
            }
        }
        // trim the result, ignoring the first spaces and cariage return
        int iFirst =0;
        for(; iFirst<b.length(); iFirst++) {
            char c = b.charAt(iFirst);
            if(c != ' ' && c != '\r' && c != '\n' && c != '\t') {
                break;
            }
        }
        // start by the end as well
        int iLast = b.length()-1; 
        for(; iLast>=0; iLast--) {
            char c = b.charAt(iLast);
            if(c != ' ' && c != '\r' && c != '\n' && c != '\t') {
                break;
            }
        }
        if(iLast < iFirst) {
            return "";
        } else {
            return b.substring(iFirst, iLast+1);
        }
    }

    /**
     * Get the text value of a child element with a given name. 
     * @param parent
     * @param name
     * @return
     */
    public static String getChildText(Element parent, String name) {
        Element child = getChild(parent, name);
        if (child != null) {
            return getTextValue(child);
        }
        return null;
    }

    /**
     * Get the text value of a child element with a given name. 
     * @param parent
     * @param name
     * @return
     */
    public static String getChildText(Element parent, String ns, String name) {
        Element child = getChild(parent, ns, name);
        if (child != null) {
            return getTextValue(child);
        }
        return null;
    }

    /**
     * Adds an element as a child of a given element. 
     * The child is created with the same namespace as the parent. 
     * @param parent
     * @param name
     * @return
     */
    public static Element addElement(Element parent, String name) {
        Document doc = parent.getOwnerDocument();
        String qname;
        if(parent.getPrefix() != null) {
            qname = parent.getPrefix() + ":" + name;
        } else {
            qname = name;
        }
        Element child = doc.createElementNS(parent.getNamespaceURI(), qname);
        parent.appendChild(child);
        return child;
    }

    /**
     * Adds an element as a child of a given element and sets the text value.
     * The child is created with the same namespace as the parent. 
     * 
     * @param parent
     * @param name
     * @param textValue
     * @return
     */
    public static Element addElement(
        Element parent,
        String name,
        String textValue) {
        
        Element child = addElement(parent, name);
        // create a text node
        if(textValue == null) {
            textValue = "";
        }
        Text txt = child.getOwnerDocument().createTextNode(textValue);
        child.appendChild(txt);
        return child;
    }

    /**
     * Sets the text value for a given element.
     * @param el
     * @param value
     */
    public static void setText(Element el, String value) {
        // remove the children if already exist
        while (el.getFirstChild() != null) {
            el.removeChild(el.getFirstChild());
        }
        if(value == null) {
            value = "";
        }
        Text txt = el.getOwnerDocument().createTextNode(value);
        el.appendChild(txt);
    }

    /**
     * Sets the text value for a given element as a CDATA section
     * @param el
     * @param value
     */
    public static void setCDATA(Element el, String value) {
        // remove the children if already exist
        while (el.getFirstChild() != null) {
            el.removeChild(el.getFirstChild());
        }
        if(value == null) {
            value = "";
        }
        CDATASection txt = el.getOwnerDocument().createCDATASection(value);
        el.appendChild(txt);
    }

    /**
     * Retrieve the namespace for a given prefix. 
     * Does a lookup into the parent hierarchy.
     * @param el
     * @param prefix
     * @return
     */
    public static String getNamespace(Element el, String prefix) {
        Element parent = el;
        while (parent != null) {
            String ns = parent.getAttribute("xmlns:" + prefix);
            if (ns != null && ns.length() > 0) {
                return ns;
            }
            // get the parent
            Node n = parent.getParentNode();
            if (n instanceof Element) {
                parent = (Element) n;
            } else {
                parent = null;
            }
        }
        // nothing found
        return null;
    }

    /*
     * serialize an element to a string.
     */
    private static String privateElementToString(
        Element element,
        boolean omitXMLDecl,
        boolean pretty) {
        
        return DOM2Writer.nodeToString(element, omitXMLDecl, pretty);
    }
    
    public static String getAttribute(Element el, String att) {
        String str = el.getAttribute(att);
        if(str == null || str.length() == 0) {
            return null;
        } else {
            return str;
        }
    }
}
