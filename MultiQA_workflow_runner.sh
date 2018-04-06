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
dataset="SQuAD"
num_paragraphs=$SLURM_ARRAY_TASK_ID
model_dir="saved_model/${dataset}"
today=$(date +%Y%m%d)
model_name="${num_paragraphs}_${dataset}_${today}"

cd /scratch/ry708/CSCI/dsga_1012/project/MultiQA
[[ ! -d ${model_dir} ]] && mkdir -p ${model_dir}


# Generate dataset
# Result file saved in form

# Train on HPC
python scripts/reader/train.py \
--embedding-file glove.840B.300d.txt \
--data-dir data/custom_datasets \
--model-dir ${model_dir} \
--model-name ${model_name} \
--tune-partial 1000 \
--num-epochs 5
# --train-file
# --dev-file



# Train local
# python scripts/reader/train.py \
# --embedding-file glove.840B.300d.txt \
# --data-dir data/custom_datasets \
# --no-cuda True \
# --tune-partial 1000 \
# --checkpoint True \
# --num-epochs 1


