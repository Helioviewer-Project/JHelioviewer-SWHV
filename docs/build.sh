doc=ddf
pandoc='pandoc --wrap=none --listings --number-sections --toc --toc-depth=6'
tex_options='--top-level-division=chapter -V classoption=oneside --template=eisvogel.latex --pdf-engine=pdflatex'

gpp -x -T ${doc}.md | ${pandoc} ${tex_options} -o ${doc}.pdf
