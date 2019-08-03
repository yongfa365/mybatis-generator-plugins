package yongfa365.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.DomWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User:zhangweixiao
 * Description:
 * old nodes is your existing xml file's first level nodes,like <insert><resultMap>
 * new nodes is mybatis-generator generate for you to combine
 * This compare the first level node's name and "id" attribute of new nodes and old nodes
 * if the two equal,then new node will not generate
 * so this can make you modification in old nodes not override.
 * if you want to regenrate old node,delete it,it will generate new.
 */
public class CombineXml2Plugin extends PluginAdapter {
    //shellCallback use TargetProject and TargetPackage to get targetFile
    ShellCallback shellCallback = new DefaultShellCallback(false);
    //save new nodes
    org.mybatis.generator.api.dom.xml.Document document;

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    /**
     * assing document variable to get new nodes
     *
     * @param document
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean sqlMapDocumentGenerated(org.mybatis.generator.api.dom.xml.Document document, IntrospectedTable introspectedTable) {
        this.document = document;
        return true;
    }


    //new nodes is generated,but not write on disk,we just need to filter.
    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        try {
            //get old nodes
            File directory = shellCallback.getDirectory(sqlMap.getTargetProject(), sqlMap.getTargetPackage());
            File xmlFile = new File(directory, sqlMap.getFileName());
            if (!directory.exists() || !xmlFile.exists()) {
                return true;
            }



            Document oldDoc = getDocFromFile(xmlFile);
            org.w3c.dom.Element rootElement = oldDoc.getDocumentElement();
            NodeList oldNodeList = rootElement.getChildNodes();

            //get new nodes
            List<Element> newElements = document.getRootElement().getElements();

            //get nodeName and the value of id attribute use regex
            Pattern p = Pattern.compile("<(\\w+)\\s+id=\"(\\w+)\"");

            Set<Node> needDeleteNodes = new HashSet<Node>();
            boolean findSameNode = false;
            // traverse new nodes to compare old nodes to filter
            for (Iterator<Element> newElement = newElements.iterator(); newElement.hasNext(); ) {
                String newNodeName = "";
                String NewIdValue = "";
                Element element = newElement.next();
                Matcher m = p.matcher(element.getFormattedContent(0));
                if (m.find()) {
                    //get nodeName and the value of id attribute
                    newNodeName = m.group(1);
                    NewIdValue = m.group(2);
                }

                for (int i = 0; i < oldNodeList.getLength(); i++) {
                    Node oldNode = oldNodeList.item(i);
                    if (oldNode.getNodeType() == Node.ELEMENT_NODE && newNodeName.equals(oldNode.getNodeName())) {
                        NamedNodeMap attr = oldNode.getAttributes();
                        Node id = attr.getNamedItem("id");
                        if (id != null && id.getNodeValue().equals(NewIdValue)) {
                            Node prev = oldNode.getPreviousSibling();
                            //处理删除元素后，存在空行的问题 https://stackoverflow.com/a/14255174/1879111
                            if (prev != null && prev.getNodeType() == Node.TEXT_NODE && prev.getNodeValue().trim().length() == 0) {
                                needDeleteNodes.add(prev);
                            }
                            needDeleteNodes.add(oldNode);
                        }
                    }
                }
            }

            for (Node node : needDeleteNodes) {
                rootElement.removeChild(node);
            }
            //MBG合并逻辑与我们预期不同，自行先处理下原XML文件
            String oldXmlChanged = new DomWriter().toString(oldDoc);
            Files.write(xmlFile.toPath(), oldXmlChanged.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private Document getDocFromFile(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new NullEntityResolver());

        Document existingDocument = builder.parse(xmlFile);
        return existingDocument;
    }

    private class NullEntityResolver implements EntityResolver {
        /**
         * returns an empty reader. This is done so that the parser doesn't
         * attempt to read a DTD. We don't need that support for the merge and
         * it can cause problems on systems that aren't Internet connected.
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {

            StringReader sr = new StringReader(""); //$NON-NLS-1$

            return new InputSource(sr);
        }
    }
}