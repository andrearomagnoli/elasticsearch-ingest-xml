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
import static org.w3c.dom.Node.ATTRIBUTE_NODE;
import static org.w3c.dom.Node.TEXT_NODE;
import static org.w3c.dom.Node.CDATA_SECTION_NODE;
import static org.w3c.dom.Node.ENTITY_REFERENCE_NODE;
import static org.w3c.dom.Node.ENTITY_NODE;
import static org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE;
import static org.w3c.dom.Node.COMMENT_NODE;
import static org.w3c.dom.Node.DOCUMENT_NODE;
import static org.w3c.dom.Node.DOCUMENT_TYPE_NODE;
import static org.w3c.dom.Node.DOCUMENT_FRAGMENT_NODE;
import static org.w3c.dom.Node.NOTATION_NODE;

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
            visitTree( root, "", false, ingestDocument, fields ); 
        }
    }

    // DFS search of the tree
    private void visitTree( Node node, 
                            String parent,
                            boolean isChild,
                            IngestDocument ingestDocument,
                            List<Field> fields ) {

        String fieldKey = "";

        switch( node.getNodeType() ){

            case ELEMENT_NODE :
                if( !parent.equals("") )
                    fieldKey = parent+"-"+node.getNodeName();
                else
                    fieldKey = node.getNodeName();
                fields = updateFieldsCount( fields, fieldKey );
                fieldKey = concatCount( fields, fieldKey );
                if( node.hasAttributes() )
                    saveAttributes( node, fieldKey, ingestDocument );
                break;
//            case ENTITY_REFERENCE_NODE :
//                fieldKey = fieldKey+node.getNodeName();
//                fields = updateCount( fields, fieldKey );
//                fieldKey = updateField( fields, fieldKey );
//                break;
            case TEXT_NODE :
            case COMMENT_NODE :
            case CDATA_SECTION_NODE :
            case DOCUMENT_NODE :
                if( parent.equals("") ){
                    if( checkExclude(node.getParentNode().getNodeName()+node.getNodeName()) ){
                        ingestDocument.setFieldValue( node.getParentNode().getNodeName()+node.getNodeName(), node.getNodeValue() );
                    }
                }
                else{
                    if( checkExclude(parent+node.getNodeName()) ){
                        ingestDocument.setFieldValue( parent+node.getNodeName(), node.getNodeValue() );
                    }
                }
                break;
//            case ATTRIBUTE_NODE :
//                break;
//            case ENTITY_NODE :
//                break;
//            case PROCESSING_INSTRUCTION_NODE :
//                break;
//            case DOCUMENT_TYPE_NODE :
//                break;
//            case DOCUMENT_FRAGMENT_NODE :
//                break;
//            case NOTATION_NODE :
//                break;
            default :
                break;
        }

        // Visit the Child
        if( node.hasChildNodes() ){
            visitTree( node.getFirstChild(), fieldKey, true, ingestDocument, fields );
        }

        // Visit the Sibling
        Node sibling = node.getNextSibling();
        if( sibling != null ) {
            visitTree( sibling, parent, false, ingestDocument, fields );
        }
    }

    // Check if you can add a value according to the exclude List
    private boolean checkExclude( String value ){
        boolean addField = true;
        if( exclude != null && exclude.size()>0 ) {
            for( int j=0; j<exclude.size(); j++ ) {
                if( value.matches( exclude.get(j) ) ) {
                    addField = false;
                    break;
                }
            }
        }
        return addField;
    }

    // Save attributes of the given node
    private void saveAttributes( Node node, String fieldKey, IngestDocument ingestDocument ) {
        NamedNodeMap attributes = node.getAttributes();
        if( attributes != null ) {
            for( int i=0; i<attributes.getLength(); i++ ) {
                Node attribute = attributes.item(i);
                String name = fieldKey+"@"+attribute.getNodeName();
                if( checkExclude( name ) ) {
                    ingestDocument.setFieldValue( name, attribute.getNodeValue() );
                }
            }
        }
    }

    // Increase by 1 the count if the field is present, or create it if it's not there
    private List<Field> updateFieldsCount( List<Field> fields, String fieldKey ) {
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
    private String concatCount( List<Field> fields, String fieldKey ) {
        
        int index = 0;
        for( int i=0; i<fields.size(); i++ ) {
            if( fields.get(i).getName().equals( fieldKey ) ) {
                index = fields.get(i).getCount();
                break;
            }
        }
        
        return ( index>1 ? fieldKey+index : fieldKey );
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

