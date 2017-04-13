# Elasticsearch xml Ingest Processor

This is a generic XML data ingestion plugin.
It automatically extracts fields and values from attributes and contents of the XML, without using an XML schema or XPATH.
This plugin uses a DFS algorithm inside the XML structure.

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

* Define a variable `exclude` or `include` to filter certains fields
* Define a variable to specify the name of the fields with the `content` of a tag. Now it is `content`: What happens if there is an attribute called `content` as well?
* Define a custom char to separate each field of the path. Now it is `-` (please note that `.` returns error)
* Test with more XML, now the testing section is quite basic

