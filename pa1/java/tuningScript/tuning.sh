#parameters
#1. Phrase length—the maximum string length (in tokens) of either side of a translation rule. =>$1
#2. Linear distortion limit—maximum re-ordering cost for picking foreign words to translate out of order.3 =>$2
#3. Tuning algorithm (MERT vs. PRO)—learning algorithm for optimizing the log-linear model parameters. =>$3

#setup training models
HOME=~/cs224n/pa1-mt
MOSES=/afs/ir/class/cs224n/bin/mosesdecoder
GIZA=/afs/ir/class/cs224n/bin/giza-pp-read-only/external-bin-dir
mkdir -p $HOME/train/model
$MOSES/scripts/training/train-model.perl --max-phrase-length 6 \
--external-bin-dir $GIZA --first-step 4 --last-step 9 \
-root-dir $HOME/train -corpus $HOME/training/corpus -f f -e e \
-alignment-file $HOME/training/corpus -alignment align \
-lm 0:3:"$HOME"/lm.bin:8

#tune with MERT
mkdir -p $HOME/tune
$MOSES/scripts/training/mert-moses.pl \
--working-dir $HOME/tune\
--decoder-flags="-distortion-limit 6" $HOME/mt-dev.fr $HOME/mt-dev.en \
$MOSES/bin/moses $HOME/train/model/moses.ini --mertdir $MOSES/bin/

#You can re-tune with PRO by adding the --pairwise-ranked argument to the tuning command (you don’t need to re-run phrase table extraction).

#Now you can decode the development-test set with your model:
cat $HOME/mt-dev-test.fr | $MOSES/bin/moses -du \
-f $HOME/tune/moses.ini > mt-dev-test.out

#evaluation
$MOSES/scripts/generic/multi-bleu.perl mt-dev-test.en < mt-dev-test.out