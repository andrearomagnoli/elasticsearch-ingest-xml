# Elasticsearch xml Ingest Processor

This is a domain-specific data ingestion plugin.
It extracts defined field from attributes and values of the XML.

## Usage


```
PUT _ingest/pipeline/xml-pipeline
{
  "description": "XML parsing pipeline",
  "processors": [
    {
      "xml" : {
        "field" : "message"
      }
    }
  ]
}

POST _ingest/pipeline/xml-pipeline/_simulate
{
  "docs" : [
    {
      "_source" :
        {
          "source" : "/path/to/the/file",
          "message" : "<event time=\"time_value\" name=\"name_value\" uid=\"uid_value\">event_value</event>"
        }
    }
  ]
}
```

## Configuration

| Parameter | Use | Required |
| --- | --- | --- |
| field   | The string that contains the XML document to parse. | Yes |

## Setup

In order to install this plugin, you need to create a zip distribution first by running

```bash
gradle clean check
```

This will produce a zip file in `build/distributions`.

After building the zip file, you can install it like this

```bash
/usr/share/elasticsearch/bin/elasticsearch-plugin install file:///path/to/ingest-xml/build/distribution/ingest-xml-5.2.2.zip
```

If you need to work on a different version, you can try to change the setting in `gradle.properties` before compiling. Use at your own risk!

## Bugs & TODO

* Create a more generic version, that accepts XSD and XPATH to define which fields are contained and what save
* Add more tests

