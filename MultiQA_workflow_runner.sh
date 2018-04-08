#!/bin/bash

#SBATCH --verbose
#SBATCH --job-name=MultiQA_testRun
#SBATCH --time=30:00:00
#SBATCH --nodes=1
#SBATCH --mem=64GB
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=8
#SBATCH --gres=gpu:2
#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=ry708@nyu.edu
#SBATCH --output=/scratch/ry708/log/multiQA_%A_%a.out
#SBATCH --error=/scratch/ry708/log/multiQA_%A_%a.err


################## Configs ##################
dataset="QUASAR-T"
data_dir="data/custom_datasets"
length="long"
num_paragraphs=$SLURM_ARRAY_TASK_ID
model_dir="saved_model/${dataset}"
today=$(date +%Y%m%d)
model_name="${num_paragraphs}_${dataset}_${today}"
declare -a data_types=("train" "dev")

cd /scratch/ry708/CSCI/dsga_1012/project/MultiQA
[[ ! -d ${model_dir} ]] && mkdir -p ${model_dir}

# Generate dataset
for dt in "${data_types[@]}"
do
	context_file="${data_dir}/${dataset}_${dt}_${length}_contexts.json"
	question_file="${data_dir}/${dataset}_${dt}_questions.json"
	java -jar scripts/preprocessing/quasar_data_prep.jar \
	${context_file} \
	${question_file} \
	${SLURM_JOBTMP} \
	${num_paragraphs}

	prefix="${num_paragraphs}_${dataset}_${length}_{dt}"
	python scripts/reader/preprocess.py \
	${SLURM_JOBTMP} \
	${SLURM_JOBTMP} \
	--split p${prefix}
done

# Data generated from previous steps
train_data="${num_paragraphs}_${dataset}_${length}_train-processed-corenlp.txt"
dev_data="${num_paragraphs}_${dataset}_${length}_dev-processed-corenlp.txt"
# Train on HPC
python scripts/reader/train.py \
--embedding-file glove.840B.300d.txt \
--data-dir ${SLURM_JOBTMP} \
--model-dir ${model_dir} \
--model-name ${model_name} \
--train-file ${train_data} \
--dev-file ${dev_data} \
--tune-partial 1000 \
--train-file ${train_data} \
--dev-file ${dev_data} \
--num-epochs 1

