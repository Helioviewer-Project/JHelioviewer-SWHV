doc=ddf
pandoc='pandoc --standalone --wrap=none --listings --number-sections --toc --toc-depth=6'
tex_options='--top-level-division=chapter -V classoption=oneside --template=eisvogel.latex --highlight-style=kate --pdf-engine=xelatex'

gpp -x -T ${doc}.md | ${pandoc} ${tex_options} -o ${doc}.pdf
#gpp -x -T ${doc}.md | ${pandoc} -o ${doc}.docx
