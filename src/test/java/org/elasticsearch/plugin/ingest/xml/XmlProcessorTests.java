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

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class XmlProcessorTests extends ESTestCase {

    public void testMultipleAttributes() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "<event time=\"time_value\" name=\"name_value\" uid=\"uid_value\">event_value</event>");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        List<String> exclude = new ArrayList<String>();
        XmlProcessor processor = new XmlProcessor(randomAsciiOfLength(10), "source_field", exclude);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("event@time"));
        assertThat(data.get("event@time"), is("time_value"));
        assertThat(data, hasKey("event@name"));
        assertThat(data.get("event@name"), is("name_value"));
        assertThat(data, hasKey("event@uid"));
        assertThat(data.get("event@uid"), is("uid_value"));
        assertThat(data, hasKey("event"));
        assertThat(data.get("event"), is("event_value"));
    }

    public void testMultipleTags() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "<root><event>event_value_a</event><event>event_value_b</event></root>");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        List<String> exclude = new ArrayList<String>();
        XmlProcessor processor = new XmlProcessor(randomAsciiOfLength(10), "source_field", exclude);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("root-event"));
        assertThat(data.get("root-event"), is("event_value_a"));
        assertThat(data, hasKey("root-event2"));
        assertThat(data.get("root-event2"), is("event_value_b"));
    }

    public void testExclude() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "<root><event>event_value_a</event><event test=\"test_value\">event_value_b</event></root>");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        List<String> exclude = new ArrayList<String>();
        exclude.add("root-event2(.*)");
        XmlProcessor processor = new XmlProcessor(randomAsciiOfLength(10), "source_field", exclude);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("root-event"));
        assertThat(data.get("root-event"), is("event_value_a"));
        assertThat(data, not(hasKey("root-event2")));
    }

    public void testComplex() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", 
"<root><node><leaf attr=\"attr_value_a\">leaf_value1</leaf><leaf attr=\"attr_value_b\">leaf_value2</leaf></node></root>");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        List<String> exclude = new ArrayList<String>();
        XmlProcessor processor = new XmlProcessor(randomAsciiOfLength(10), "source_field", exclude);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("root-node-leaf"));
        assertThat(data.get("root-node-leaf"), is("leaf_value1"));
        assertThat(data, hasKey("root-node-leaf@attr"));
        assertThat(data.get("root-node-leaf@attr"), is("attr_value_a"));
        assertThat(data, hasKey("root-node-leaf2"));
        assertThat(data.get("root-node-leaf2"), is("leaf_value2"));
        assertThat(data, hasKey("root-node-leaf2@attr"));
        assertThat(data.get("root-node-leaf2@attr"), is("attr_value_b"));

    }

}

