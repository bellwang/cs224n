#!/bin/bash

################################################################
#parameters:
#$1 = phrase string length
#run phrase-table extraction
function PhraseTableExtraction {
HOME=~/cs224n/pa1-mt
MOSES=/afs/ir/class/cs224n/bin/mosesdecoder
GIZA=/afs/ir/class/cs224n/bin/giza-pp-read-only/external-bin-dir

$MOSES/scripts/training/train-model.perl --max-phrase-length $1 \
--external-bin-dir $GIZA --first-step 4 --last-step 9 \
-root-dir $HOME/train -corpus $HOME/training/corpus -f f -e e \
-alignment-file $HOME/training/corpus -alignment align \
-lm 0:3:"$HOME"/lm.bin:8
}

################################################################
#parameters:
#$1 = distortion limit-maximum
#$2 = MERT(0) or PRO(1)
#tuning
function Tuning {
HOME=~/cs224n/pa1-mt
MOSES=/afs/ir/class/cs224n/bin/mosesdecoder
GIZA=/afs/ir/class/cs224n/bin/giza-pp-read-only/external-bin-dir


if  [  "$2"  == "0" ]
then
#tune with MERT
echo "##############################run MERT Tuning."
mkdir -p $HOME/tune
$MOSES/scripts/training/mert-moses.pl \
--working-dir $HOME/tune \
--decoder-flags="-distortion-limit $1" $HOME/mt-dev.fr $HOME/mt-dev.en \
$MOSES/bin/moses $HOME/train/model/moses.ini --mertdir $MOSES/bin/
else
#tune with PRO
echo "##############################run PRO Tuning."
mkdir -p $HOME/tune
$MOSES/scripts/training/mert-moses.pl \
--pairwise-ranked \
--working-dir $HOME/tune \
--decoder-flags="-distortion-limit $1" $HOME/mt-dev.fr $HOME/mt-dev.en \
$MOSES/bin/moses $HOME/train/model/moses.ini --mertdir $MOSES/bin/
fi
#You can re-tune with PRO by adding the --pairwise-ranked argument to the tuning command (you don’t need to re-run phrase table extraction).

#Now you can decode the development-test set with your model:
cat $HOME/mt-dev-test.fr | $MOSES/bin/moses -du \
-f $HOME/tune/moses.ini > mt-dev-test.out
}

################################################################
##record the BLEU value for evaluation
function Evaluation {
MOSES=/afs/ir/class/cs224n/bin/mosesdecoder
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

#####i = fixed max-phrase-length
for i in 7 8
do
	echo "1##############################Phrase Extraction $i"
	#PhraseTableExtraction $i

	##### j = distortion limit
	for j in 12 13 14
	do
		echo "2##############################Tuning MERT $j"
		#Tuning $j 0

		echo "3##############################Start to Evaluation"
		echo "$i $j 0" >> output
		#Evaluation

		echo "2##############################Tuning PRO $j"
		#Tuning $j 1

		echo "3##############################Start to Evaluation"
		echo "$i $j 1" >> output
		#Evaluation
	done
done

#for different tuning parameters
Tuning 12 1