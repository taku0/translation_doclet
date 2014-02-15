<!--
translation_doclet, helper doclets for translating Javadoc.
Copyright (C) 2014  taku0 ( https://github.com/taku0 )

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Please contact taku0 ( https://github.com/taku0 ) if you need additional
information or have any questions.
 -->
<xsl:transform
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    version="1.0">
  <xsl:strip-space elements="xhtml:div" />
  <xsl:output
      method="xml"
      omit-xml-declaration="yes"
      indent="yes"
      encoding="UTF-8"/>
  <xsl:template match="/">
    <documents>
      <xsl:apply-templates select="//xhtml:div[@data-type='package']"/>
    </documents>
  </xsl:template>

  <xsl:template match="xhtml:div[@data-type]">
    <xsl:choose>
      <xsl:when test="@data-type='package' or @data-type='class' or @data-type='constructor' or @data-type='field' or @data-type='method'">
        <xsl:element name="{@data-type}">
          <xsl:attribute name="name"><xsl:value-of select="@data-name"/></xsl:attribute>
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:when>
      <xsl:when test="@data-type='text'">
        <text><xsl:apply-templates/></text>
      </xsl:when>
      <xsl:when test="@data-type='tag'">
        <tag>
          <xsl:if test="@data-exception-name">
            <xsl:attribute name="exceptionName"><xsl:value-of select="@data-exception-name"/></xsl:attribute>
          </xsl:if>

          <xsl:if test="@data-parameter-name">
            <xsl:attribute name="parameterName"><xsl:value-of select="@data-parameter-name"/></xsl:attribute>
          </xsl:if>

          <xsl:if test="@data-is-type-parameter">
            <xsl:attribute name="isTypeParameter">isTypeParameter</xsl:attribute>
          </xsl:if>
          <xsl:apply-templates/>
        </tag>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="xhtml:*">
    <xsl:element name="{local-name()}">
      <xsl:apply-templates select="@*|node()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:transform>
