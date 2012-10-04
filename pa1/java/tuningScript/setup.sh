# setup the symbol link
cd
mkdir -p cs224n/pa1-mt/training
wc -l /afs/ir/class/cs224n/pa1/data/mt/training/corpus.{e,f}
cd cs224n/pa1-mt/training
ln -s /afs/ir/class/cs224n/pa1/data/mt/training/corpus.e
ln -s /afs/ir/class/cs224n/pa1/data/mt/training/corpus.f
ln -s /afs/ir/class/cs224n/pa1/data/mt/training/FilePairs.training