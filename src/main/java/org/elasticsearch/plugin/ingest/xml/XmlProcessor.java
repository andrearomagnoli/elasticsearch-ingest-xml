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

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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

        String eventTime = null;
        String eventName = null;
        String eventUid = null;
        String eventValue = null;

        String buffer = null;
        String content = ingestDocument.getFieldValue(field, String.class);

        if( content != null && !content.isEmpty() && content.startsWith("<event" ) ) {

            InputStream stream = new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ) );
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader( stream );

            while( reader.hasNext() ) { 
                int event = reader.next();

                switch( event ) {

                    case XMLStreamConstants.START_ELEMENT: 
                        if( "event".equals( reader.getLocalName() ) ) {
                            for( int i=0; i<reader.getAttributeCount(); i++ ) {
                                String attribute = reader.getAttributeLocalName(i);

                                switch( attribute ) {
                                    case "time" :
                                        eventTime = reader.getAttributeValue(i);
                                        break;
                                    case "name" :
                                        eventName = reader.getAttributeValue(i);
                                        break;
                                    case "uid" :
                                        eventUid = reader.getAttributeValue(i);
                                        break;
                                    default :
                                        break;
                                }
                            }
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS: 
                        buffer = reader.getText().trim();
                        break;

                    case XMLStreamConstants.END_ELEMENT: 
                        eventValue = buffer;
                        break;

                    default : 
                        break;            
                }
            }

            ingestDocument.setFieldValue( "event-time", eventTime );
            ingestDocument.setFieldValue( "event-name", eventName );
            ingestDocument.setFieldValue( "event-uid", eventUid );
            ingestDocument.setFieldValue( "event-value", eventValue );
        }
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
