xsltproc src/main/xslt/from_html.xsl translation.ja.html > translation.ja.xml 

SCALA_HOME=/nix/store/whdn5g2kr5y2vb2y5h78fchskxznalcb-scala-2.10.3
export JAVA_HOME=~/opt/jdk-1.8.0b128

SOURCE_PATH=../jdk8
PACKAGE=java.util.stream

${JAVA_HOME}/bin/javadoc \
    -locale ja_JP \
    -noqualifier all \
    -tag implNote:a:"実装注記" \
    -tag apiNote:a:"API注記" \
    -tag implSpec:a:"実装仕様" \
    -quiet \
    -doclet org.tatapa.translation_doclet.InjectDoclet  \
    -translation translation.ja.xml \
    -docletpath target/scala-2.10/translation_doclet_2.10-1.0.jar:${HOME}/.ivy2/cache/nu.validator.htmlparser/htmlparser/bundles/htmlparser-1.4.jar:${SCALA_HOME}/lib/scala-library.jar  \
    -breakiterator \
    -encoding UTF-8 \
    -d javadoc \
    -use -charset UTF-8 \
    -docencoding UTF-8 \
    -windowtitle 'java.util.stream API仕様 非公式翻訳' \
    -sourcepath "${SOURCE_PATH}" \
    ${PACKAGE}
