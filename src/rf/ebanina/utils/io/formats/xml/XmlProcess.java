package rf.ebanina.utils.io.formats.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class XmlProcess {
    public static TreeMap<String, String> parseXmlToTreeMap(InputStream inputStream) throws Exception {
        TreeMap<String, String> resultMap = new TreeMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        Element rootElement = doc.getDocumentElement();
        parseElement(rootElement, "", resultMap);

        return resultMap;
    }

    private static void parseElement(Element element, String currentPath, TreeMap<String, String> resultMap) {
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String nodeName = childElement.hasAttribute("name")
                        ? childElement.getAttribute("name")
                        : childElement.getTagName();

                String nextPath = currentPath.isEmpty() ? nodeName : currentPath + "." + nodeName;

                boolean hasChildElements = false;
                NodeList subChildren = childElement.getChildNodes();
                for (int j = 0; j < subChildren.getLength(); j++) {
                    if (subChildren.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        hasChildElements = true;
                        break;
                    }
                }

                if (hasChildElements) {
                    parseElement(childElement, nextPath, resultMap);
                } else {
                    String content = getElementContent(childElement);
                    resultMap.put(nextPath, content);
                }
            }
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
