<xsl:transform
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns="http://www.w3.org/1999/xhtml"
    version="1.0">
  <xsl:strip-space
      elements="documents package class constructor field method" />
  <xsl:output
      method="xml"
      doctype-system="about:legacy-compat"
      omit-xml-declaration="yes"
      indent="yes"
      encoding="UTF-8"/>
  <xsl:template match="/">
    <html>
      <head>
        <meta charset="UTF-8"/>
        <title>Documents</title>
      </head>
      <body>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="documents | package | class | constructor | field | method">
    <xsl:if test="string(.)">
      <div data-type="{local-name()}" data-name="{@name}">
        <xsl:apply-templates/>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="text">
    <xsl:if test="string(.)">
      <div data-type="text"><xsl:apply-templates/></div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tag">
    <xsl:if test="string(.)">
      <div data-type="tag">
        <xsl:if test="@exceptionName">
          <xsl:attribute name="data-exception-name"><xsl:value-of select="@exceptionName"/></xsl:attribute>
        </xsl:if>

        <xsl:if test="@parameterName">
          <xsl:attribute name="data-parameter-name"><xsl:value-of select="@parameterName"/></xsl:attribute>
        </xsl:if>

        <xsl:if test="@isTypeParameter">
          <xsl:attribute name="data-is-type-parameter">data-is-type-parameter</xsl:attribute>
        </xsl:if>
        <xsl:apply-templates/>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:transform>
