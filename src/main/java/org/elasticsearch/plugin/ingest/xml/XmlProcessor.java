/*
 * Copyright [2017] [Andrea Romagnoli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.elasticsearch.plugin.ingest.xml;

import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class XmlProcessor extends AbstractProcessor {

    public static final String TYPE = "xml";

    private final String field;

    public XmlProcessor(String tag, String field) throws IOException {
        super(tag);
        this.field = field;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {

        String xml = ingestDocument.getFieldValue(field, String.class);

        if( xml != null && !xml.isEmpty() ) {

            // Parsing
            InputStream stream = new ByteArrayInputStream( xml.getBytes( "utf-8" ) );
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( stream );

            // Processing
            Node root = doc.getDocumentElement();
            visitTree( root, true, "", ingestDocument ); 
        }
    }

    // DFS search of the tree
    private void visitTree( Node node, boolean isRoot, String fieldKey, IngestDocument ingestDocument ) {

        if( isRoot )
            fieldKey = node.getNodeName();
        else 
            fieldKey = fieldKey+"-"+node.getNodeName();

        if( node.getNodeType() == ELEMENT_NODE ) {

            readNode( node, fieldKey, ingestDocument );

            // Visit child first
            if( node.hasChildNodes() ){
                visitTree( node.getFirstChild(), false, fieldKey, ingestDocument );
            }

            // Visit siblings next
            Node sibling = node.getNextSibling();
            if( sibling != null ) {
                visitTree( sibling, isRoot, fieldKey, ingestDocument );
            }
        }
    }

    // Read node content, save attributes and content (if it hasn't children)
    private void readNode( Node node, String fieldKey, IngestDocument ingestDocument ) {
        if( node.hasAttributes() )
            saveAttributes( node, fieldKey, ingestDocument );
        if( node.hasChildNodes() && node.getChildNodes().getLength()==1 && node.getFirstChild().getNodeType()==TEXT_NODE )
            saveContent( node.getFirstChild(), fieldKey, ingestDocument );
    }

    // Save attributes of the given node
    private void saveAttributes( Node node, String fieldKey, IngestDocument ingestDocument ) {
        NamedNodeMap attributes = node.getAttributes();
        if( attributes != null ) {
            for( int i=0; i<attributes.getLength(); i++ ) {
                Node attribute = attributes.item(i);
                ingestDocument.setFieldValue( fieldKey+"-"+attribute.getNodeName(), attribute.getNodeValue() );
            }
        }
    }

    // Save content of the given node
    private void saveContent( Node node, String fieldKey, IngestDocument ingestDocument ) {
        ingestDocument.setFieldValue( fieldKey+"-content", node.getNodeValue() );
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        @Override
        public XmlProcessor create(Map<String, Processor.Factory> factories, String tag, Map<String, Object> config) 
            throws Exception {
            String field = readStringProperty(TYPE, tag, config, "field");

            return new XmlProcessor(tag, field);
        }
    }
}
