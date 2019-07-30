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

function convert(request) {

    var snippet = [];
    if (request.method === 'HEAD') {
        snippet.push("curl -I ")
        snippet.push(request.url.raw);
    } else if (request.method === 'GET') {
       

        snippet.push("curl -X " +   
          ( request.url.raw.indexOf("?") ? " -G " : "" ) +request.method)
        snippet.push("\'" + 
          request.url.raw.slice(0, request.url.raw.indexOf("?")) +  "\'");
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
    return snippet.join("  \\\n  ");
  };


function postmanToMarkdown(data) {
  var lines = [];

  if (data.info.name){
    lines.push( "#  " + data.info.name.trim() + "\n");
  }

  if (data.info.description){
    lines.push(data.info.description + "\n");

  }

  for each (var item in data.item) {
    lines.push( "#  " + item.name.trim() + "\n");

    if (item.request.description){
      lines.push(item.request.description + "\n");
    }
   
    lines.push("#### Endpoint: {.section}\n");

    lines.push("");
    lines.push("- Method: **" +  item.request.method.toUpperCase() + "**");
    if(item.request.body){
      lines.push("- Type: **" +  item.request.body.mode.toUpperCase() + "**");
    }
    lines.push("- URL: `" +  item.request.url.raw.slice(0, item.request.url.raw.indexOf("?"))+ "`");
    lines.push("");


    if(item.request.header.length > 0){
      lines.push("#### Headers: {.section}\n");
      lines.push("| Key | Value | Description |");
      lines.push("| --- | ------|-------------|");
      for each (var header in item.request.header) {
        lines.push("| " + header.key + " | " + 
            (header.value || "") + " | " + description(header.description) + " |");
      }
      lines.push("");
    }


    if(item.request.url.query){
      lines.push("#### Query Parameters: {.section}\n");
      lines.push("| Key | Value | Description |");
      lines.push("| --- | ------|-------------|");
      for each (var param in item.request.url.query) {
        lines.push("| `" + param.key + "` | `" + 
            (param.value || "") + "` | " + description(param.description) + " |");
      }
      lines.push("");
    }

    if(item.request.body) {
      if(item.request.body.mode === 'raw'){
        lines.push("#### Body: {.section}\n");
        lines.push("```js");        
        lines.push(item.request.body.raw );
        lines.push("```");

      } else if(item.request.body.mode === 'formdata'){
        lines.push("#### Body: {.section}\n");
        for each (var param in item.request.body.formdata) {
          lines.push("| `" + param.key + "` | `" + 
              (param.value || "") + "` | " + description(param.description) + " |");
        }

      } else if(item.request.body.mode === 'urlencoded'){
        lines.push("#### Body: {.section}\n");
        for each (var param in item.request.body.urlencoded) {
          lines.push("| `" + param.key + "` | `" + 
              (param.value || "") + "` | " + description(param.description) + " |");
        }

      }
    }

    lines.push("#### Example: {.section}\n");
    lines.push("```bash");        
    lines.push(convert(item.request));
    lines.push("```\n");

  }

  return lines.join('\n');
}


var src = attributes.get("src");
var dest = attributes.get("dest");


var data = org.apache.tools.ant.util.FileUtils.readFully(
  new java.io.FileReader(src)
);
var markdown = postmanToMarkdown(JSON.parse(data));

var task = project.createTask("echo");
task.setFile(new java.io.File(dest));
task.setMessage(markdown);
task.perform();

