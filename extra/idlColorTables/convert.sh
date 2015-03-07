# Converts the gradient files from the ouput to proper wants by deleting the whitespace

for i in aiaTables/*.rggr
do
  cat $i | sed -e 's/^ *//g' -e 's/ *$//g' -e 's/  */ /g' > `echo $i | sed 's/rggr/ggr/'`
done