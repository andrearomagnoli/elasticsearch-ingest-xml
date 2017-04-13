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

import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalList;

import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public class XmlProcessor extends AbstractProcessor {

    public static final String TYPE = "xml";

    private final String field;
    private final List<String> exclude;

    public XmlProcessor(String tag, String field, List<String> exclude) throws IOException {
        super(tag);
        this.field = field;
        this.exclude = exclude;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {

        String xml = ingestDocument.getFieldValue(field, String.class);

        if( xml != null && !xml.isEmpty() ) {

            List<Field> fields = new ArrayList<Field>();

            // Parsing
            InputStream stream = new ByteArrayInputStream( xml.getBytes( "utf-8" ) );
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( stream );

            // Processing
            Node root = doc.getDocumentElement();
            visitTree( root, true, false, "", ingestDocument, fields ); 
        }
    }

    // DFS search of the tree
    private void visitTree( Node node, boolean isRoot, boolean isChild, 
            String fieldKey, IngestDocument ingestDocument, List<Field> fields ) {

        if( isRoot ) {
            fieldKey = node.getNodeName();
            fields = trackField( fields, fieldKey );
            fieldKey = updateField( fields, fieldKey );
        }
        else {
            if( isChild ) 
                fieldKey = fieldKey+"-"+node.getNodeName();
            fields = trackField( fields, fieldKey );
            fieldKey = updateField( fields, fieldKey );
        }

        if( node.getNodeType() == ELEMENT_NODE ) {

            readNode( node, fieldKey, ingestDocument );

            // Visit child first
            if( node.hasChildNodes() ){
                visitTree( node.getFirstChild(), false, true, fieldKey, ingestDocument, fields );
            }

            // Visit siblings next
            Node sibling = node.getNextSibling();
            if( sibling != null ) {
                visitTree( sibling, isRoot, false, fieldKey, ingestDocument, fields );
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
                String name = fieldKey+"@"+attribute.getNodeName();
                boolean addField = true;
                if( exclude != null && exclude.size()>0 ) {
                    for( int j=0; j<exclude.size(); j++ ) {
                        if( name.matches( exclude.get(j) ) ) {
                            addField = false;
                            break;
                        }
                    }
                }
                if( addField )
                    ingestDocument.setFieldValue( name, attribute.getNodeValue() );
            }
        }
    }

    // Save content of the given node
    private void saveContent( Node node, String fieldKey, IngestDocument ingestDocument ) {
        boolean addField = true;
        if( exclude != null && exclude.size()>0 ) {
            for( int i=0; i<exclude.size(); i++ ) {
                if( fieldKey.matches( exclude.get(i) ) ) {
                    addField = false;
                    break;
                }
            }
        }
        if( addField )
            ingestDocument.setFieldValue( fieldKey, node.getNodeValue() );
    }

    // Increase by 1 the count if the field is present, or create it if it's not there
    private List<Field> trackField( List<Field> fields, String fieldKey ) {
        if( fields.size() == 0 ) {
            fields.add( new Field( fieldKey ) );
            return fields;
        }
        for( int i=0; i<fields.size(); i++ ){
            if( fields.get(i).getName().equals( fieldKey ) ) {
                fields.get(i).increase();
                return fields;
            }
        }
        fields.add( new Field( fieldKey ) );
        return fields;
    }
    
    // If two tags have the same name, concatenate an incremental integer to the duplicated tag
    private String updateField( List<Field> fields, String fieldKey ) {
        int index = 0;
        for( int i=0; i<fields.size(); i++ ) {
            if( fields.get(i).getName().equals( fieldKey ) ) {
                index = fields.get(i).getCount();
                break;
            }
        }
        if( index > 1 )
            fieldKey = fieldKey+index;
        return fieldKey;
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
            List<String> exclude = readOptionalList(TYPE, tag, config, "exclude");

            return new XmlProcessor(tag, field, exclude);
        }
    }
}

