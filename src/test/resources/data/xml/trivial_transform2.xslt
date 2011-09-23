<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <Index:document Index:resolver="test1">
            <xsl:attribute name="Index:id">
                <xsl:value-of select="record/id"/>
            </xsl:attribute>

            <xsl:for-each select="record">
                <Index:fields>
                    <Index:field Index:type="keyword" Index:name="two">
                        <xsl:value-of select="elementTwo"/>
                    </Index:field>
                </Index:fields>
            </xsl:for-each>
        </Index:document>
    </xsl:template>
</xsl:stylesheet>
