SCALA_HOME=/nix/store/whdn5g2kr5y2vb2y5h78fchskxznalcb-scala-2.10.3

export JAVA_HOME=~/opt/jdk-1.8.0b128

SOURCE_PATH=../jdk8
PACKAGE=java.util.stream

${JAVA_HOME}/bin/javadoc \
    -quiet \
    -doclet org.tatapa.translation_doclet.ExtractDoclet \
    -docletpath target/scala-2.10/translation_doclet_2.10-1.0.jar:${HOME}/.ivy2/cache/nu.validator.htmlparser/htmlparser/bundles/htmlparser-1.4.jar:${SCALA_HOME}/lib/scala-library.jar \
    -sourcepath "${SOURCE_PATH}" \
    ${PACKAGE} \
    > translation.en.xml

xsltproc src/main/xslt/to_html.xsl translation.en.xml > translation.en.html
