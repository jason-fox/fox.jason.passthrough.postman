<?xml version="1.0" encoding="utf-8"?>
<!--
	This file is part of the DITA-OT postman Plug-in project.
	See the accompanying LICENSE file for applicable licenses.
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="*[contains(@class,' topic/object ')][@outputclass = 'swagger-spec']"/>
</xsl:stylesheet>
