<h1>Postman Plugin for DITA-OT</h1>

This is a DITA-OT Plug-in used to auto-create valid DITA-based REST API documentation. The documentation can be generated directly from a [Postman Collection](https://www.getpostman.com/) file and processed as if it had been written in DITA.

[<img src="https://assets.getpostman.com/common-share/postman-logo-horizontal-orange.svg" align="right" width="181" height="55">](https://www.getpostman.com/)

[Postman](https://www.getpostman.com/) is a software development tool which a developer can use to build, publish, document, design, monitor, test and debug their REST APIs.

This plugin processes a Postman collection to Pandoc markdown, and the converts the markdown to DITA using the [Pandoc DITA-OT Plugin](https://github.com/jason-fox/fox.jason.passthrough.pandoc) allowing the generation of PDF API documentation.

<h4>Sample Postman Request</h4>

```json
{
  "name": "Obtain Entity Data by id",
  "request": {
    "method": "GET", "header": [],
    "url": {
      "raw": "http://{{orion}}/v2/entities/urn:ngsi-ld:Store:001?options=keyValues",
      "protocol": "http",
      "host": ["{{orion}}"],"path": ["v2","entities","urn:ngsi-ld:Store:001"],
      "query": [
        {
          "key": "options", "value": "keyValues",
          "description": "* `keyValues` option in order to get a more compact ...",
        },
        {
          "key": "type", "value": "Store", "disabled": true,
          "description": "Entity type, to avoid ambiguity in case there are ..."
        },
        {
          "key": "attrs", "value": "name", "disabled": true,
          "description": "Ordered list of attribute names to display"
        }
      ]
    },
    "description": "This example returns the context data of `store1`..."
  },
  "response": []
}
```

<h4>Sample DITA Output</h4>

<img src="https://jason-fox.github.io/fox.jason.passthrough.postman/request-formatted.png" align="center" style="border-style:solid; border-width:1px; border-color:#ddd"/>
