list=bla.files
lib=`find lib | tr '\n' :`
find src -name *.java > $list
mkdir out
cov-build --dir cov-int javac -cp $lib -d out @$list
rm -fr out $list
