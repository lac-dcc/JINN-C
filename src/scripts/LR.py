#!/usr/bin/env python
# coding: utf-8
# coding: utf-8
#
# Copyright (C) 2019 juniocezar.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
# MA 02110-1301  USA
#

import sys
import pandas as pd
import numpy as np
from sklearn import linear_model
from sklearn import metrics
from sklearn.model_selection import train_test_split
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import StandardScaler

import warnings
warnings.filterwarnings('ignore')

# Dataset Path
DATASET_PATH = sys.argv[1]
CSV = sys.argv[2]

configs_data = pd.read_csv(CSV, header=None)
configs_data = configs_data.sample(frac=1)
samples = configs_data.iloc[:,1:-1] # gets all columns but last (runtime) and first (signature)
targets = configs_data.iloc[:,-1] # get last column (runtime)
signature = str(configs_data.iloc[0][0])

print( "Number of observations :: " + str(len(configs_data.index)))
print( "Number of columns :: " + str(len(configs_data.columns)))
print( "Headers :: " + str(configs_data.columns.values))

if len(targets.unique()) == 1:
    # Benchmark with static configuration
    print("Static Configuration: \"" + targets.unique()[0] + "\";")
    w = open(DATASET_PATH + "/" + signature + ".static.txt","w+")
    w.write("\"" + targets.unique()[0] + "\";")
    w.close()
    exit()

# defining the models
multinomial_model  = linear_model.LogisticRegression(multi_class='multinomial', solver='lbfgs') 
traditional_model  = linear_model.LogisticRegression()

# using random split
# train_x, test_x, train_y, test_y = train_test_split(samples, targets, train_size=0.7)
    
# Train the two models
train_x = StandardScaler().fit_transform(samples)
#train_x = samples
train_y = targets
mul_lr = multinomial_model.fit(train_x, train_y)
tra_lr = traditional_model.fit(train_x, train_y)
a=StandardScaler().fit(samples)


print ("Classes: ")
print (tra_lr.classes_)
print ("Matrix - Theta")
print (tra_lr.coef_.T.round(5))
print ("Intercept - Theta_0")
print (tra_lr.intercept_)

#
# Dumping coeficients to a file in JAVA format
classes = ""
for c in tra_lr.classes_:
	classes = classes + "\"" + c + "\","
classes = "{" + classes[:-1] + "};"
print ("Classes: " + classes)

interception = "" # theta 0
for i in tra_lr.intercept_:
	interception = interception + str(i) + ","
interception = "{" + interception[:-1] + "};"
print ("Interception with plane: " + interception)

coef = ""
for row in tra_lr.coef_.T:
	coef = coef + "{"
	for col in row:
		coef = coef + str(col) + ","
	coef = coef[:-1] + "},\n"
coef = "{" + coef[:-2] + "};"

print(coef)

normMean = "" # normalizationMean
for i in a.mean_:
	normMean = normMean + str(i) + ","
normMean = "{" + normMean[:-1] + "};"
print ("Normalization mean: " + normMean)


normScale = "" # normalizationScale
for i in a.scale_:
	normScale = normScale + str(i) + ","
normScale = "{" + normScale[:-1] + "};"
print ("Normalization scale: " + normScale)

normElements = len(a.scale_) - 1

wfa = open(DATASET_PATH + "/" + signature + ".coef.txt","w+")
wfb = open(DATASET_PATH + "/" + signature + ".class.txt","w+")
wfc = open(DATASET_PATH + "/" + signature + ".inter.txt","w+")
wfd = open(DATASET_PATH + "/" + signature + ".n-mean.txt","w+")
wfe = open(DATASET_PATH + "/" + signature + ".n-scale.txt","w+")
wff = open(DATASET_PATH + "/" + signature + ".n.txt","w+")

wfa.write(coef + "\n")
wfb.write(classes + "\n")
wfc.write(interception + "\n")
wfd.write(normMean + "\n")
wfe.write(normScale + "\n")
wff.write(str(normElements) + "\n")

wfa.close()
wfb.close()
wfc.close()
wfd.close()
wfe.close()
wff.close()
