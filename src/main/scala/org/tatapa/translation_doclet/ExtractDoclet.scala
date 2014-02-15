package org.tatapa.translation_doclet

import com.sun.javadoc._

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import org.w3c.dom.Node
import org.w3c.dom.Document

import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import nu.validator.htmlparser.common.XmlViolationPolicy

object ExtractDoclet {
  def languageVersion() = 
    com.sun.tools.doclets.internal.toolkit.AbstractDoclet.languageVersion()

  def start(root: RootDoc): Boolean = {
    val classes = root.classes

    val classesByPackage = classes.groupBy(_.containingPackage)

    val document =
      DocumentBuilderFactory.newInstance.newDocumentBuilder.newDocument

    val documentsElement = document.createElement("documents")

    document.appendChild(documentsElement)

    val packageElements = formatElements(classesByPackage.keys.toSeq, "package", document)(_.name) { packageDoc =>
      formatElements(classesByPackage(packageDoc), "class", document)(_.qualifiedName) { classDoc =>
        Seq(
          formatElements(classDoc.constructors, "constructor", document)(constructor => constructor.name + constructor.signature)(),
          formatElements(classDoc.enumConstants, "enumConstant", document)(_.name)(),
          formatElements(classDoc.fields, "field", document)(_.name)(),
          formatElements(classDoc.methods, "method", document)(method => method.name ++ method.signature)()
        ).flatten
      }
    }

    packageElements.foreach(documentsElement.appendChild _)

    val domSource = new DOMSource(document)
    val result = new StreamResult(System.out)
    val transformer = TransformerFactory.newInstance.newTransformer

    transformer.transform(domSource, result)

    true
  }

  def formatElements[T <: Doc](
    docs: Seq[T], elementName: String, document: Document)(
    getName: T => String)(
    formatChild: T => Seq[Node] = (x: T) => Seq.empty) = {

    for (doc <- docs) yield {
      val element = document.createElement(elementName)

      element.setAttribute("name", getName(doc))

      val textElement = document.createElement("text")

      for (node <- formatInlineTags(doc.inlineTags)) {
        textElement.appendChild(document.adoptNode(node))
      }

      element.appendChild(textElement)

      formatBlockTags(doc.tags, document).foreach(element.appendChild _)

      formatChild(doc).foreach(element.appendChild _)

      element
    }
  }

  def formatInlineTags(tags: Array[Tag]) = {
    val text = tags.map { tag =>
      if (tag.name == "Text") {
        tag.text
      } else {
        " {%s %s}".format(tag.name, tag.text)
      }
    }.mkString

    parseHTML(cleanupText(text))
  }

  def cleanupText(text: String) = {
    var preCount = 0
    val builder = new StringBuilder

    for (line <- text.replaceAll("\r\n", "\n").replaceAll("\r", "\n").lines) {
      if (preCount > 0) {
        builder.append("\n")
        builder.append(line.replaceAll("^ ", ""))
      } else {
        builder.append(line
                         .replaceAll("^[\\s]+", " ")
                         .replaceAll("[\\s]+$", ""))
      }

      preCount += "<pre[\\s]*>".r.findAllIn(line).length
      preCount -= "</pre[\\s]*>".r.findAllIn(line).length
    }

    builder.toString
  }

  def formatBlockTags(tags: Array[Tag], document: Document) = {
    tags.map { tag =>
      val element = document.createElement("tag")

      element.setAttribute("name", tag.name)

      tag match {
        case throwsTag: ThrowsTag => {
          element.setAttribute("exceptionName", throwsTag.exceptionName())
        }
        case paramTag: ParamTag => {
          element.setAttribute("parameterName", paramTag.parameterName())

          if (paramTag.isTypeParameter) {
            element.setAttribute("isTypeParameter", "isTypeParameter")
          }
        }
        case _ => {
          // なにもしない
        }
      }

      for (node <- formatInlineTags(tag.inlineTags)) {
        element.appendChild(document.adoptNode(node))
      }

      element
    }
  }

  def parseHTML(text: String) = {
    val tagNames = Set("a", "abbr", "address", "article", "aside", "audio", "b", "bdi", "bdo", "blockquote", "br", "button", "canvas", "cite", "code", "data", "datalist", "del", "details", "dfn", "dialog", "div", "dl", "em", "embed", "fieldset", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "i", "iframe", "img", "input", "ins", "kbd", "keygen", "label", "main", "map", "mark", "math", "meter", "nav", "noscript", "object", "ol", "output", "p", "pre", "progress", "q", "ruby", "s", "samp", "script", "section", "select", "small", "span", "strong", "sub", "sup", "svg", "table", "template", "textarea", "time", "u", "ul", "var", "video", "wbr", "li", "thead", "tbody", "tr", "th", "td", "caption", "col", "colgroup")

    val escaped = text
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")

    val replaced = tagNames.foldLeft(escaped) { (escaped, name) =>
      escaped
        .replaceAll(s"&lt;(${name}(?:(?!&gt;)[^<>])*)&gt;", "<$1>")
        .replaceAll(s"&lt;/(${name}[\\s]*)&gt;", "</$1>")
    }

    val parser = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET)
    val source = new org.xml.sax.InputSource(new StringReader(replaced))

    val nodeList = parser.parseFragment(source, "body").getChildNodes

    0.until(nodeList.getLength).map(nodeList.item _)
  }
}
