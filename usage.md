<h1>Usage</h1>

To mark a file to be passed through for **Postman** processing, label it with `format="postman"` within the `*.ditamap` as
shown:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE bookmap PUBLIC "-//OASIS//DTD DITA BookMap//EN" "bookmap.dtd">
<bookmap>
    ...etc
    <appendices toc="yes" print="printonly">
      <topicmeta>
        <navtitle>Appendices</navtitle>
      </topicmeta>
      <appendix format="postman" href="postman_collection.json"/>
   </appendices>
</bookmap>
```

The additional file will be converted to a `*.dita` file and will be added to the build job without further processing. Unless overriden, the `navtitle` of the included topic will be the same as root name of the file. Any underscores in the filename will be replaced by spaces in title.
