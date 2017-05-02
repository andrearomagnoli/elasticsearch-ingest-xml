# Elasticsearch xml Ingest Processor

This is a generic XML data ingestion plugin.
It automatically extracts fields and values from attributes and contents of the XML, without using an XML schema or XPATH.
This plugin uses a DFS algorithm inside the XML structure.
The most important constraint is that the XML must be well formed, with a root tag that contains all other tags.

If you have more tags with the same name, the plugin will name them with an incremental number.

```
<root>
    <event>event1</event>
    <event attr="attr_value">event2</event>
    <event>event3</event>
</root>
```

In the previous example, the algorithm will create four key-values pairs:

```
root-event#text : event1
root-event2#text : event2
root-event2@attr : attr_value
root-event3#text : event3
```

It also includes and `exclude` optional parameter: all fields that match the REGEX will be discarded. For example, `root-event2(.*)` will exclude the second and the third record in the previous example.

## Usage


```
PUT _ingest/pipeline/xml-pipeline
{
  "description": "XML parsing pipeline",
  "processors": [
    {
      "xml" : {
        "field" : "message",
        "exclude" : ["root-event2(.*)"]
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
          "message" : "<root><event time=\"time_value\" name=\"name_value\" uid=\"uid_value\">event_value1</event><event>event_value2</event></root>"
        }
    }
  ]
}
```

## Configuration

| Parameter | Use | Required |
| --- | --- | --- |
| field   | The string that contains the XML document to parse. | Yes |
| exclude | Pattern that match this REGEX will be discarded (check `java.lang.String.matches` for the correct syntax). | No |

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

* Only basic DOM tags are implemented. Should be added the support for more advanced tags (all possible tags are in the code, commented out, to give a guideline)
* The visit of attributes can be improved, according to the DOM logic checking the ATTRIBUTE_NODE type
* Define a custom char to separate each field of the path. Now it is `-` for fields (please note that `.` returns error), `@` for attributes and '#' for DOM-defined keywords

