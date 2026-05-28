package rf.ebanina.utils.io.formats.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rf.ebanina.utils.io.formats.xml.dto.XmlData;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class Xml {
    public static Map<String, XmlData> parseXmlToXmlData(InputStream inputStream) throws Exception {
        Map<String, XmlData> rootMap = new LinkedHashMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        Element rootElement = doc.getDocumentElement();
        NodeList children = rootElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String nodeName = childElement.hasAttribute("name")
                        ? childElement.getAttribute("name")
                        : childElement.getTagName();

                XmlData xmlData = new XmlData();
                buildTree(childElement, xmlData);
                rootMap.put(nodeName, xmlData);
            }
        }

        return rootMap;
    }

    private static void buildTree(Element element, XmlData currentData) {
        NodeList children = element.getChildNodes();
        boolean hasChildElements = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                hasChildElements = true;
                Element childElement = (Element) node;
                String nodeName = childElement.hasAttribute("name")
                        ? childElement.getAttribute("name")
                        : childElement.getTagName();

                XmlData childData = new XmlData();
                currentData.getChildren().put(nodeName, childData);

                buildTree(childElement, childData);
            }
        }

        if (!hasChildElements) {
            currentData.setData(getElementContent(element));
        }
    }

    private static String getElementContent(Element element) {
        NodeList children = element.getChildNodes();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            }
        }
        return sb.toString().trim();
    }
}
