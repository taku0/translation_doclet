# translation_doclet

Enables easy Javadoc localization. A free (GPLed) alternative to l10ndoclet ( http://sourceforge.net/projects/l10ndoclet/ ).

Features:

- XML friendly: Generates well-formed XML (including markups in comments) file suitable for XSLT or other XML processing tools.
- sed friendly: Removes irreverent spaces from comments suitable for sed or other regexp processing tools.
- OmegaT friendly: Generates a structured XML file as well as a plain HTML file, suitable for OmegaT or other computer assisted translation tools.

This project provides two docklets and two optional XSLT files. ExtractDoclet extracts comment strings from source into structured XML files. InjectDoclet generates translated javadoc using standard doclet. to\_html.xsl and from\_html.xsl converts between structured XML files and plain HTML files.

Batch files extract.sh and inject.sh are sample batch files for JDK 8 java.util.stream translation.

## License

Copyright 2014 taku0 ( https://github.com/taku0 )

Licensed under GPL 2.0 or later. See LICENSE for details.
