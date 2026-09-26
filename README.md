# Postman Plugin for DITA-OT [<img src="https://jason-fox.github.io/fox.jason.passthrough.postman/postman.png" align="right" width="300">](http://postmandita-ot.rtfd.io/)

[![license](https://img.shields.io/github/license/jason-fox/fox.jason.passthrough.postman.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![DITA-OT 4.2](https://img.shields.io/badge/DITA--OT-4.2-green.svg)](http://www.dita-ot.org/4.2)
[![CI](https://github.com/jason-fox/fox.jason.passthrough.postman/workflows/CI/badge.svg)](https://github.com/jason-fox/fox.jason.passthrough.postman/actions?query=workflow%3ACI)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fox.jason.passthrough.postman&metric=alert_status)](https://sonarcloud.io/dashboard?id=fox.jason.passthrough.postman)

This is a [DITA-OT Plug-in](https://www.dita-ot.org/plugins) used to auto-create valid DITA-based REST API
documentation. The documentation can be generated directly from a [Postman Collection](https://www.getpostman.com/) file
and processed as if it had been written in DITA.

:arrow_forward: [Video from DITA-OT Day 2019](https://youtu.be/cd7XThpkivw)

[![](https://jason-fox.github.io/fox.jason.passthrough.postman/nothing-video.png)](https://youtu.be/cd7XThpkivw)

<details>
<summary><strong>Table of Contents</strong></summary>

-   [Background](#background)
-   [Install](#install)
    -   [Installing DITA-OT](#installing-dita-ot)
    -   [Installing the Plug-in](#installing-the-plug-in)
-   [Usage](#usage)
-   [License](#license)

</details>

## Background

[<img src="https://assets.getpostman.com/common-share/postman-logo-horizontal-orange.svg" align="right" height="55">](https://www.getpostman.com/)

[Postman](https://www.getpostman.com/) is a software development tool which a developer can use to build, publish,
document, design, monitor, test and debug their REST APIs.

This plugin parses a Postman collection and generates DITA directly - the structured parts (endpoints, headers, query
parameters, body, curl examples, sample responses) are built straight from the JSON, and any Markdown in collection,
folder, or request `description` fields is parsed with [flexmark](https://github.com/vsch/flexmark-java) and rendered
to DITA directly. There is no intermediate Markdown file and no dependency on Pandoc.

#### Sample Postman Request

```json
{
    "name": "Obtain Entity Data by id",
    "request": {
        "method": "GET",
        "header": [],
        "url": {
            "raw": "http://{{orion}}/v2/entities/urn:ngsi-ld:Store:001?options=keyValues",
            "protocol": "http",
            "host": ["{{orion}}"],
            "path": ["v2", "entities", "urn:ngsi-ld:Store:001"],
            "query": [
                {
                    "key": "options",
                    "value": "keyValues",
                    "description": "* `keyValues` option in order to get a more compact ..."
                },
                {
                    "key": "type",
                    "value": "Store",
                    "disabled": true,
                    "description": "Entity type, to avoid ambiguity in case there are ..."
                },
                {
                    "key": "attrs",
                    "value": "name",
                    "disabled": true,
                    "description": "Ordered list of attribute names to display"
                }
            ]
        },
        "description": "This example returns the context data of `store1`..."
    },
    "response": []
}
```

#### Sample DITA Output

> ![](https://jason-fox.github.io/fox.jason.passthrough.postman/request-formatted.png)

## Install

The DITA-OT postman plug-in has been tested against [DITA-OT 3.x](http://www.dita-ot.org/download). It is recommended
that you upgrade to the latest version.

### Installing DITA-OT

<a href="https://www.dita-ot.org"><img src="https://www.dita-ot.org/images/dita-ot-logo.svg" align="right" height="55"></a>

The DITA-OT postman plug-in is a file reader for the DITA Open Toolkit.

-   Full installation instructions for downloading DITA-OT can be found
    [here](https://www.dita-ot.org/4.0/topics/installing-client.html).

    1.  Download the `dita-ot-4.2.zip` package from the project website at
        [dita-ot.org/download](https://www.dita-ot.org/download)
    2.  Extract the contents of the package to the directory where you want to install DITA-OT.
    3.  **Optional**: Add the absolute path for the `bin` directory to the _PATH_ system variable.

    This defines the necessary environment variable to run the `dita` command from the command line.

```console
curl -LO https://github.com/dita-ot/dita-ot/releases/download/4.2/dita-ot-4.2.zip
unzip -q dita-ot-4.2.zip
rm dita-ot-4.2.zip
```

### Installing the Plug-in

-   Run the plug-in installation commands:

```console
dita install https://github.com/jason-fox/fox.jason.extend.css/archive/master.zip
dita install https://github.com/jason-fox/fox.jason.passthrough.postman/archive/master.zip
```

The `dita` command line tool requires no additional configuration.

This plugin ships its own copy of the `swagger-get`/`swagger-post` request styling, so it no longer requires the
[Swagger plugin](https://github.com/jason-fox/fox.jason.passthrough.swagger) to be installed. If that plugin
_is_ also installed, its copy of the CSS is used instead (avoiding duplicate rules), and its additional XSL adds a
coloured verb badge to the endpoint code block that this plugin's CSS alone does not provide.

## Usage

To mark a file to be passed through for **Postman** processing, label it with `format="postman"` within the `*.ditamap`
as shown:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE bookmap PUBLIC "-//OASIS//DTD DITA BookMap//EN" "bookmap.dtd">
<bookmap>
    ...etc
    <appendices toc="yes" print="yes">
      <topicmeta>
        <navtitle>Appendices</navtitle>
      </topicmeta>
      <appendix format="postman" href="postman_collection.json"/>
   </appendices>
</bookmap>
```

The additional file will be converted to a `*.dita` file and will be added to the build job without further processing.
Unless overridden, the `navtitle` of the included topic will be the same as root name of the file. Any underscores in
the filename will be replaced by spaces in title.

## License

[Apache 2.0](LICENSE) © 2019 - 2024 Jason Fox

The Program includes the following additional software components which were obtained under license. See
[NOTICES.txt](NOTICES.txt) for the full text of each license.

-   json-simple-1.1.1.jar - https://github.com/fangyidong/json-simple - **Apache 2.0 license**
-   JetBrains Annotations - https://github.com/JetBrains/java-annotations - **Apache 2.0 license**
-   flexmark and its flexmark-util-\* submodules and flexmark-ext-tables - https://github.com/vsch/flexmark-java -
    **BSD 2-Clause license**
