<p float="left">
  <img src="figures/deepmib-demo.gif" />
  <img src="figures/inference-demo.gif" /> 
  <img src="figures/qupath-demo.gif" />
</p>

# NoCodeSeg: Deep segmentation made easy!

This is the official repository for the manuscript *"Code-free development and deployment of deep segmentation models for digital pathology"*, submitted to Frontiers in Medicine. 

The repository contains trained deep models for epithelium segmentation of HE and CD3 immunostained WSIs, as well as source code relevant for importing/exporting annotations/predictions in [QuPath](https://qupath.github.io/), from [DeepMIB](http://mib.helsinki.fi/downloads.html), and [FastPathology](https://github.com/AICAN-Research/FAST-Pathology).

## Getting started

[![Watch the video](figures/youtube-thumbnail.jpg)](https://youtu.be/9dTfUwnL6zY)

A video tutorial of the proposed pipeline was published on [YouTube](https://www.youtube.com/watch?v=9dTfUwnL6zY&ab_channel=HenrikSahlinPettersen).
It includes the steps for: 
* Installing the softwares
* QuPath
  * Loading a project and exporting annotations as tiles/patches
  * Importing predictions for MIB and FastPathology as annotations
* MIB
  * Loading tiles into MIB
  * Preprocessing tiles for training
  * Configuring and training deep segmentation models
  * Exporting the trained model into the ONNX format
* FastPathology
  * Creating configuration file for ONNX model
  * Importing ONNX model
  * Create project, and load WSIs into project
  * Select which WSI to render
  * Deploying model and render predictions on top of the WSI in real time

## Data
The 251 annotated WSIs are being processed before publishing on [DataverseNO](https://dataverse.no/), where it can be downloaded and used for free by anyone.

## Citation
Please, consider citing our paper, if you find the work useful:
<pre>
  @MISC{pettersen2021NoCodeSeg,
  title={Code-free development and deployment of deep segmentation models for digital pathology},
  author={Henrik S. Pettersen, Ilya Belevich, Elin S. Røyset, Erik Smistad, Eija Jokitalo, Ingerid Reinertsen, Ingunn Bakke, and André Pedersen},
  year={2021},
  eprint={some.numbers},
  archivePrefix={arXiv},
  primaryClass={eess.IV}}
</pre>
