package com.baracuda.piepet.nexusrelease.maven;

import hudson.model.AbstractBuild;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by FPPE12 on 2015-11-01.
 */
public class PomInterceptor {
    private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public String getNexusStagingProfileId(AbstractBuild build) throws IOException, InterruptedException {
        if(build.getExecutor().getCurrentWorkspace().child("pom.xml").exists()) {

            String pomXMLasString = build.getExecutor().getCurrentWorkspace().child("pom.xml").readToString();
            NodeList nl = xpathPOM(pomXMLasString, "/project/build/plugins/*/configuration/stagingProfileId");
            if (nl != null && nl.getLength() > 0) {
                return nl.item(0).getTextContent();
            } else {
                return "";
            }
        }
        return "";

    }

    private List<POMElement> getPOMElements(String pom, String xPath) {
        NodeList nl = xpathPOM(pom, xPath);
        List<POMElement> elements = new ArrayList<POMElement>();
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {

                elements.add(new POMElement(nl.item(i).getNodeName(), nl.item(i).getTextContent()));
            }
        }
        return elements;

    }

    private NodeList xpathPOM(String pom, String xPath) {
        NodeList nl=null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(pom));
            Document doc = builder.parse(is);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(xPath);
            nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            return nl;

        } catch (Exception ex) {
            //TODO Add logging
        }
        return nl;
    }

}
