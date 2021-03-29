<?xml version='1.0' encoding='utf-8'?>

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://www.w3.org/1998/Math/MathML"
    exclude-result-prefixes="xsl"
    version="1.0">

    <xsl:variable name="newline">
<xsl:text>
</xsl:text>
    </xsl:variable>

    <xsl:output method='xml' version="1.0" encoding='utf-8' doctype-public="-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN" doctype-system="http://www.w3.org/TR/MathML2/dtd/xhtml-math11-f.dtd" indent="no"/>

    <xsl:template match="img/@style" />

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="body">
        <xsl:copy>
            <xsl:value-of select="$newline"/>
            <h1 id="title">
            <xsl:value-of select="//head/title"/>
            </h1>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
