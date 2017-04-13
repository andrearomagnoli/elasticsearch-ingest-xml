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

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public class XmlProcessorTests extends ESTestCase {

    public void testMultipleAttributes() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "<event time=\"time_value\" name=\"name_value\" uid=\"uid_value\">event_value</event>");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        XmlProcessor processor = new XmlProcessor(randomAsciiOfLength(10), "source_field");
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("event-time"));
        assertThat(data.get("event-time"), is("time_value"));
        assertThat(data, hasKey("event-name"));
        assertThat(data.get("event-name"), is("name_value"));
        assertThat(data, hasKey("event-uid"));
        assertThat(data.get("event-uid"), is("uid_value"));
        assertThat(data, hasKey("event-content"));
        assertThat(data.get("event-content"), is("event_value"));
    }

    public void testMultipleTags() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", "<root><event>event_value_a</event><event>event_value_b</event></root>");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        XmlProcessor processor = new XmlProcessor(randomAsciiOfLength(10), "source_field");
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("root-event-content"));
        assertThat(data.get("root-event-content"), is("event_value_a"));
        assertThat(data, hasKey("root-event2-content"));
        assertThat(data.get("root-event2-content"), is("event_value_b"));
    }
}

