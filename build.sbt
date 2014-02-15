name := "translation_doclet"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
    "nu.validator.htmlparser" % "htmlparser" % "1.4"
)

unmanagedJars in Compile += file(System.getProperty("java.home") + "/../lib/tools.jar")
