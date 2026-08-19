#doc=ddf
doc=jhv-heliocentric-3d-data-interface
pandoc='pandoc --standalone --wrap=none --syntax-highlighting=idiomatic --number-sections --toc'
tex_options='--top-level-division=chapter -V classoption=oneside --template=./templates/eisvogel.latex --pdf-engine=xelatex'

gpp -x -T ${doc}.md | ${pandoc} ${tex_options} -o ${doc}.pdf
#gpp -x -T ${doc}.md | ${pandoc} -o ${doc}.docx
