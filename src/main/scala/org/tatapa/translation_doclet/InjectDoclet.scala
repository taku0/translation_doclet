package org.tatapa.translation_doclet

import com.sun.javadoc._
import com.sun.tools.doclets.formats.html.HtmlDoclet

import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.sax.SAXResult

import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Element
import org.w3c.dom.Document

import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.sax.HtmlSerializer
import nu.validator.htmlparser.sax.HtmlParser

object external {
  case class Package(name: String,
                     text: Seq[Node],
                     tags: Iterable[Tag],
                     classes: Iterable[Class]) {
    def rawCommentText = makeRawCommentText(text, tags)
  }

  case class Class(name: String,
                   text: Seq[Node],
                   tags: Iterable[Tag],
                   constructors: Iterable[Member],
                   enumConstants: Iterable[Member],
                   fields: Iterable[Member],
                   methods: Iterable[Member]) {
    def rawCommentText = makeRawCommentText(text, tags)
  }

  case class Member(name: String, text: Seq[Node], tags: Iterable[Tag]) {
    def rawCommentText = makeRawCommentText(text, tags)
  }

  abstract class Tag(name: String, text: Seq[Node]) {
    def rawCommentText = "{@%s %s}".format(name, external.toString(text))
  }

  case class GenericTag(name: String, text: Seq[Node]) extends Tag(name, text)

  case class ThrowsTag(name: String,
                       exceptionName: String,
                       text: Seq[Node]) extends Tag(name, text) {
    override def rawCommentText = {
      "{@%s %s %s}".format(name, exceptionName, external.toString(text))
    }
  }

  case class ParamTag(name: String,
                      isTypeParameter: Boolean,
                      parameterName: String,
                      text: Seq[Node]) extends Tag(name, text) {
    override def rawCommentText = {
      val parameterNameString = if (isTypeParameter) {
        "<%s>".format(parameterName)
      } else {
        parameterName
      }

      "{@%s %s %s}".format(name, parameterNameString, external.toString(text))
    }
  }

  def makeRawCommentText(text: Seq[Node], tags: Iterable[Tag]) = {
    external.toString(text) + "\n" + tags.map(_.rawCommentText).mkString("\n")
  }

  def toString(nodes: Seq[Node]) = {
    // 本来はpreやcodeの中だけするべき。
    val template = """<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" version="1.0"><xsl:output method="xml" omit-xml-declaration="yes" indent="no" encoding="UTF-8"/><xsl:template match="@*|node()"><xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy></xsl:template><xsl:template match="text()"><xsl:value-of select="." disable-output-escaping="yes"/></xsl:template></xsl:stylesheet>"""

    val writer = new StringWriter
    val serializer = TransformerFactory
      .newInstance
      .newTransformer(new StreamSource(new StringReader(template)))

    for (node <- nodes) {
      serializer.transform(new DOMSource(node), new StreamResult(writer))
    }

    writer.toString
  }
}

object InjectDoclet {
  def optionLength(option: String) = {
    if (option == "-translation") {
      2
    } else {
      HtmlDoclet.optionLength(option)
    }
  }

  def validOptions(options: Array[Array[String]],
                   reporter: DocErrorReporter) = {
    val isFile =
      options.find(args => args(0) == "-translation").map { args =>
        new File(args(1)).canRead()
      }.getOrElse(false)

    if (!isFile) {
      reporter.printError("required: -translation xml_file")
    }

    HtmlDoclet.validOptions(options, reporter) && isFile
  }

  def languageVersion() = 
    com.sun.tools.doclets.internal.toolkit.AbstractDoclet.languageVersion()

  def start(root: RootDoc): Boolean = {
    val fileOption = root
      .options
      .find(args => args(0) == "-translation")
      .map(_(1))
      .map(new File(_))

    val file =
      fileOption.getOrElse(
        throw new IllegalArgumentException("XML file required"))

    val document =
      DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(file)

    val translatedPackages = toPackages(document.getDocumentElement)
    val translatedClasses = translatedPackages.flatMap(_.classes)

    val translatedPackageMap = translatedPackages.map(p => p.name -> p).toMap
    val translatedClassMap = translatedClasses.map(p => p.name -> p).toMap

    val classDocs = root.classes
    val packageDocs = classDocs.map(_.containingPackage).toSet

    for {
      packageDoc <- packageDocs
      translatedPackage <- translatedPackageMap.get(packageDoc.name)
    } {
      packageDoc.setRawCommentText(translatedPackage.rawCommentText)
    }

    for {
      classDoc <- classDocs
      translatedClass <- translatedClassMap.get(classDoc.qualifiedName)
    } {
      classDoc.setRawCommentText(translatedClass.rawCommentText)



      setRawCommentText(classDoc.constructors, translatedClass.constructors)(constructor => constructor.name + constructor.signature)

      setRawCommentText(classDoc.enumConstants, translatedClass.enumConstants)(_.name)
      setRawCommentText(classDoc.fields, translatedClass.fields)(_.name)
      setRawCommentText(classDoc.methods, translatedClass.methods)(method => method.name + method.signature)

      def setRawCommentText[A <: Doc](
        docs: Seq[A], translatedMembers: Iterable[external.Member])(
        getName: A => String) {

        val map = translatedMembers.map(m => m.name -> m).toMap

        for {
          doc <- docs
          translated <- map.get(getName(doc))
        } {
          doc.setRawCommentText(translated.rawCommentText)
        }
      }
    }

    HtmlDoclet.start(root)
  }

  case class RichNodeList[A <: Node](nodes: Seq[A]) {
    def select(name: String): RichNodeList[Element] = RichNodeList(
      toSeq.collect {
        case element: Element if element.getNodeName == name => element
      }
    )

    def toSeq: Seq[A] = nodes
  }

  object RichNodeList {
    implicit def toSeq[A <: Node](nodeList: RichNodeList[A]): Seq[A] = {
      nodeList.toSeq
    }
  }

  implicit def toSeq(nodeList: NodeList): Seq[Node] =
    0.until(nodeList.getLength).map(nodeList.item _)


  implicit def enrichNodeList(nodeList: NodeList): RichNodeList[Node] =
    RichNodeList(nodeList)

  def toPackages(node: Node) =
    node.getChildNodes.select("package").map(toPackage _)

  def toPackage(element: Element) = external.Package(
    element.getAttribute("name"),
    element.getChildNodes.select("text").flatMap(_.getChildNodes),
    element.getChildNodes.select("tag").map(toTag _),
    element.getChildNodes.select("class").map(toClass _)
  )

  def toTag(element: Element) = {
    if (element.hasAttribute("exceptionName")) {
      external.ThrowsTag(element.getAttribute("name"),
                         element.getAttribute("exceptionName"),
                         element.getChildNodes)
    } else if (element.hasAttribute("parameterName")) {
      external.ParamTag(element.getAttribute("name"),
                        element.hasAttribute("isTypeParameter"),
                        element.getAttribute("parameterName"),
                        element.getChildNodes)
    } else {
      external.GenericTag(element.getAttribute("name"), element.getChildNodes)
    }
  }

  def toClass(element: Element) = external.Class(
    element.getAttribute("name"),
    element.getChildNodes.select("text").flatMap(_.getChildNodes),
    element.getChildNodes.select("tag").map(toTag _),
    element.getChildNodes.select("constructor").map(toMember _),
    element.getChildNodes.select("enumConstant").map(toMember _),
    element.getChildNodes.select("field").map(toMember _),
    element.getChildNodes.select("method").map(toMember _)
  )

  def toMember(element: Element) = external.Member(
    element.getAttribute("name"),
    element.getChildNodes.select("text").flatMap(_.getChildNodes),
    element.getChildNodes.select("tag").map(toTag _)
  )
}
