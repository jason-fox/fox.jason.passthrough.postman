/*
 *  This file is part of the DITA-OT Postman DITA Plug-in project.
 *  See the accompanying LICENSE file for applicable licenses.
 */

//
//    Entry point to the JavaScript tidier function which
//    creates markdown text from a Postman collection
//
//    @param src          - The Postman file to parse
//    @param dest         - The file to write to
//


function description (str){
  str = str || "";
  return str.replace(/\n/g, '<br><br>');
}

function getURL(raw){
  var index = raw.indexOf("?");
  return (index !== -1) ? raw.slice(0, raw.indexOf("?")) : raw;
}

function addSampleResponses(responses) {
  if (responses.length == 1){
    lines.push("#### Example HTTP response {.example}\n");
  } else if (responses.length > 1){
    lines.push("#### Example HTTP responses {.example}\n");
  }

  for each (var response in responses) {
    lines.push("#### Response " + response.code + 
      " - " + response.status + " {.example}\n");
    lines.push("```" + response._postman_previewlanguage);        
    lines.push(response.body);
    lines.push("```");
  }

}

function addSampleCurl(request) {

    lines.push("#### Example Request {.example}\n");
    lines.push("```bash"); 

    var snippet = [];
    if (request.method === 'HEAD') {
        snippet.push("curl -I ")
        snippet.push(request.url.raw);
    } else if (request.method === 'GET') {
        snippet.push("curl -X " +   
          ( request.url.raw.indexOf("?") ? " -G " : "" ) +request.method)
        snippet.push("\'" + 
          getURL(request.url.raw) +  "\'");
    } else {
        snippet.push("curl -X " + request.method)
        snippet.push("\'" + request.url.raw +  "\'");
    }

    for each (var header in request.header) {
      snippet.push("  -H "+ header.key + " :  " + header.value);
    }

    if (request.url.raw.indexOf("?") > 0){
      var params =  request.url.raw.substring( request.url.raw.indexOf("?") + 1).split("&");

      for each (var param in params) {
         snippet.push("  -d '" + param + "'");
      }

    }
    if (request.body) {
        body = request.body;
        switch (body.mode) {
          case 'urlencoded':
           var text = [];
            for each (var data in body.urlencoded) {
              if (!data.disabled) {
                text.push(data.key+ "=" +data.value);
              }
            }
            snippet.push(' -d '+ text.join('&'));
            break;
          case 'raw':
            snippet.push("  -d '" + body.raw +"'");
            break;
          case 'formdata':
            for each (var data in body.formdata) {
              if (!(data.disabled)) {
                if (data.type === 'file') {
                  snippet.push(' -F' + data.key + "=" + data.src);
                }
                else {
                  snippet.push('  -F' + data.key + "=" + data.value);
                }
              }
            }
            break;
          case 'file':
            snippet.push('  --data-binary' + body.key + "=" + body.value);
            break;
          default:
            snippet += snippet.push(" -d '" + body.raw +"'");
        
      }
    }
    lines.push(snippet.join("  \\\n  "));
    lines.push("```\n");
  };

function extractType(header){
  var slash = header.lastIndexOf("/");
  var plus = header.lastIndexOf("+");
  var type;

  if (plus !== -1){
    type = header.substring(plus + 1);
  } else if (slash !== -1) {
    type = header.substring(slash + 1);
  } else {
    type = header;
  }
  return type;
}

function addDescription (desc ) {
  var regex = /^#+ /;
    if (desc ){
      var description = desc.split("\n");
      for each (var descLine in description) {
        if (descLine.startsWith("#")){
          lines.push(descLine.replace( regex,"###### ") + " {.section}");
        } else {
          lines.push(descLine);
        }
      }
    } else {
      lines.push("");
    }
}

function addEndpoint (request ) {
    lines.push("");
    lines.push('```swagger-' + request.method);
    lines.push(request.method.toUpperCase() + " " 
      + getURL(request.url.raw));
    lines.push('```');
    lines.push("");
}

function addRequest (request ) {
    var contentType = "markup";
    lines.push("");
    if(request.header.length > 0){
      lines.push("#### Headers {.section}\n");
      lines.push("| Key | Value | Description |");
      lines.push("| --- | ------|-------------|");
      for each (var header in request.header) {
        if (header.key.toLowerCase() == "content-type"){
          contentType = extractType(header.value);
        } 
        lines.push("| " + header.key + " | " + 
            (header.value || "") + " | " + description(header.description) + " |");
      }
      lines.push("");
    }


    if(request.url.query){
      lines.push("#### Query Parameters {.section}\n");
      lines.push("| Key | Value | Description |");
      lines.push("| --- | ------|-------------|");
      for each (var param in request.url.query) {
        lines.push("| `" + param.key + "` | `" + 
            (param.value || "") + "` | " + description(param.description) + " |");
      }
      lines.push("");
    }

    if(request.body) {
      lines.push("#### Body {.section}\n");
      if(request.body.mode === 'raw'){
        lines.push("```" + contentType);        
        lines.push(request.body.raw );
        lines.push("```");

      } else if(request.body.mode === 'formdata'){
        for each (var param in request.body.formdata) {
          lines.push("| `" + param.key + "` | `" + 
              (param.value || "") + "` | " + description(param.description) + " |");
        }

      } else if(request.body.mode === 'urlencoded'){
        for each (var param in request.body.urlencoded) {
          lines.push("| `" + param.key + "` | `" + 
              (param.value || "") + "` | " + description(param.description) + " |");
        }

      }
    }
}

function postmanToMarkdown(data) {

  if (data.info.name){
    lines.push( "#  " + data.info.name.trim() + "\n");
  }

  if (data.info.description){
    lines.push(data.info.description + "\n");
  }

  for each (var item in data.item) {
      if (item.request){
        lines.push( "#  " + item.name.trim() + " { .swagger-" + item.request.method +"}\n\n");
        addEndpoint (item.request);
        addDescription (item.request.description );
        addRequest (item.request);
        addSampleCurl(item.request);
        addSampleResponses(item.response);
      } else {
       
        lines.push( "#  " + item.name.trim() + "\n\n");
        addDescription (item.description );

        for each (var item in item.item) {
          if (item.request){
            lines.push( "##  " + item.name.trim() + " { .swagger-" + item.request.method +"}\n\n");
            addEndpoint (item.request);
            addDescription (item.request.description);
            addRequest(item.request);
            addSampleCurl(item.request);
            addSampleResponses(item.response);
          }
        }
      }
   }
}


var src = attributes.get("src");
var dest = attributes.get("dest");
var lines = [];


var data = org.apache.tools.ant.util.FileUtils.readFully(
  new java.io.FileReader(src)
);

postmanToMarkdown(JSON.parse(data));
var markdown = lines.join('\n');

var task = project.createTask("echo");
task.setFile(new java.io.File(dest));
task.setMessage(markdown);
task.perform();

