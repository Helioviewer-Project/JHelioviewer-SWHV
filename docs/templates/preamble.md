---
author: [SWHV Team]
tags: [SWHV, JHV]
book: true
header-includes:
- |
  \usepackage{pdfpages}
  \AtBeginDocument{\addtokomafont{disposition}{\sffamily}}

  \setkomafont{chapterentry}{\sffamily}
  \setkomafont{chapterentrypagenumber}{\sffamily}

  \DeclareTOCStyleEntry[
    entryformat=\sffamily,
    pagenumberformat=\sffamily
  ]{tocline}{section}

  \DeclareTOCStyleEntry[
    entryformat=\sffamily,
    pagenumberformat=\sffamily
  ]{tocline}{subsection}

mainfont: SourceSerif4
mainfontoptions:
- Path=./fonts/source/
- Extension=.otf
- UprightFont=*-Regular
- BoldFont=*-Bold
- ItalicFont=*-It
- BoldItalicFont=*-BoldIt
mathfont: SourceSerif4
mathfontoptions:
- Path=./fonts/source/
- Extension=.otf
- UprightFont=*-Regular
- BoldFont=*-Bold
- ItalicFont=*-It
- BoldItalicFont=*-BoldIt
sansfont: SourceSans3
sansfontoptions:
- Path=./fonts/source/
- Extension=.otf
- UprightFont=*-Regular
- BoldFont=*-Bold
- ItalicFont=*-It
- BoldItalicFont=*-BoldIt
monofont: SourceCodePro
monofontoptions:
- Path=./fonts/source/
- Extension=.otf
- UprightFont=*-Regular
- BoldFont=*-Bold
- ItalicFont=*-It
- BoldItalicFont=*-BoldIt
fontsize: 10pt
papersize: A4
lot: true
lof: true
toc-depth: 6
secnumdepth: 6
colorlinks: true
code-block-font-size: \footnotesize
footnotes-pretty: true
mathspec: true
table-use-row-colors: true
titlepage: true
titlepage-color: "0088cc"
titlepage-text-color: "ffffff"
titlepage-rule-color: "ffffff"
titlepage-rule-height: 5
listings-disable-line-numbers: true
titlepage-logo: ./templates/hvLogo.png
logo-width: 64pt
---
\frenchspacing
