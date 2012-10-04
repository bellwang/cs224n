# setup the symbol link
cd
mkdir -p CS224Ngit/cs224n/pa1-mt/training
wc -l /afs/ir/class/cs224n/pa1/data/mt/training/corpus.{e,f}
cd CS224Ngit/cs224n/pa1-mt/training
ln -s /afs/ir/class/cs224n/pa1/data/mt/training/corpus.e
ln -s /afs/ir/class/cs224n/pa1/data/mt/training/corpus.f
ln -s /afs/ir/class/cs224n/pa1/data/mt/training/FilePairs.training