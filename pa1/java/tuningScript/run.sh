#execute the run.sh with parameters:
#$1 = phrase string length
#$2 = distortion limit-maximum
#$3 = MERT(0) or PRO(1)

################################################################
#parameters
#1. Phrase length—the maximum string length (in tokens) of either side of a translation rule. =>$1
#2. Linear distortion limit—maximum re-ordering cost for picking foreign words to translate out of order.3 =>$2
#3. Tuning algorithm (MERT vs. PRO)—learning algorithm for optimizing the log-linear model parameters. =>$3

################################################################
#run phrase-table extraction
function PhraseTableExtraction {
$MOSES/scripts/training/train-model.perl --max-phrase-length $1 \
--external-bin-dir $GIZA --first-step 4 --last-step 9 \
-root-dir $HOME/train -corpus $HOME/training/corpus -f f -e e \
-alignment-file $HOME/training/corpus -alignment align \
-lm 0:3:"$HOME"/lm.bin:8
}

################################################################
#tuning
function Tuning{
if [ $2 eq 0]
then
#tune with MERT
echo "run MERT Tuning."
mkdir -p $HOME/tune
$MOSES/scripts/training/mert-moses.pl \
--working-dir $HOME/tune\
--decoder-flags="-distortion-limit \$1" $HOME/mt-dev.fr $HOME/mt-dev.en \
$MOSES/bin/moses $HOME/train/model/moses.ini --mertdir $MOSES/bin/
else
#tune with PRO
echo "run PRO Tuning."
mkdir -p $HOME/tune
$MOSES/scripts/training/mert-moses.pl \
--pairwise-ranked\
--working-dir $HOME/tune\
--decoder-flags="-distortion-limit \$1" $HOME/mt-dev.fr $HOME/mt-dev.en \
$MOSES/bin/moses $HOME/train/model/moses.ini --mertdir $MOSES/bin/
fi
#You can re-tune with PRO by adding the --pairwise-ranked argument to the tuning command (you don’t need to re-run phrase table extraction).

#Now you can decode the development-test set with your model:
cat $HOME/mt-dev-test.fr | $MOSES/bin/moses -du \
-f $HOME/tune/moses.ini > mt-dev-test.out

}

################################################################
#tuning
function Evaluation{
#evaluation
$MOSES/scripts/generic/multi-bleu.perl mt-dev-test.en < mt-dev-test.out >> output
}

################################################################
#####################RUNNING STAGE############################
################################################################
cd
cd cs224n/pa1-mt

set HOME=~/cs224n/pa1-mt
set MOSES=/afs/ir/class/cs224n/bin/mosesdecoder
set GIZA=/afs/ir/class/cs224n/bin/giza-pp-read-only/external-bin-dir
mkdir -p $HOME/train/model

#iteration 1
#for fixed max-phrase-length
PhraseTableExtraction 6

#for different tuning parameters
Tuning 14 0

#record the BLEU value
Evaluation