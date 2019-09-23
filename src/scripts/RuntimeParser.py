#!/usr/bin/env python
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
import json
from os import listdir
from os.path import isfile, join

# vars
dirr = sys.argv[1]
outfile = sys.argv[2]
configs = {}
INF=9999999999.9
#
# listing dir with results (each file must be from a diferent core config)
#results_files = [f for f in listdir(dirr) if f.endswith(".log")] #original
results_files = [f for f in listdir(dirr) if f.startswith("1.") and f.endswith(".log")] # adapted training
#
# extracting the runtime of each method and putting them in a map for each
# configuration
for data_file in results_files:    
    with open(dirr + "/" + data_file) as f:
        config = data_file[:-4]        
        for line in f:          
            sample    = line.rsplit(',', 1)                    
            if (len(sample) >= 2):
                signature = sample[0] 
                runtime   = int(sample[1]) # nanotime
                methods   = configs.get(config, {})
                runtimes  = methods.get(signature, [])

                runtimes.append(runtime)
                methods[signature] = runtimes
                configs[config] = methods
        
#
# calculate the mean runtime for each signature
for config, methods in configs.items():
    for signature, runtimes in methods.items():
        mean = sum(runtimes) / float(len(runtimes))
        methods[signature] = mean
        
#
# getting the best configuration for each signature (method hash + inputs)
# todo: fix handler for equivalent results. Select configuration using less
# resources (big little cores) -- this case did not happen during evaluation

best_configurations = {}
reference_config = '1.8GHz-0xff'
methods = configs[reference_config]  
for signature, avg_runtime in methods.items():
    baseline_sig  = signature 
    best_time     = avg_runtime    
    best_config   = reference_config
    
    for config in results_files:
        config  = config[:-4]
        cfg_methods = configs.get(config, {})
        runtime = cfg_methods.get(signature, INF)
        if runtime < best_time:
            best_time   = runtime
            best_config = config
            
    best_configurations[baseline_sig] = best_config
    print (best_config + " is the best configuration for Input = {" + signature + "}:  " + str(best_time/1000000.0) + " / " + str(avg_runtime/1000000.0))


# Sanitizing map for containing a single entry per configuration
# not actually necessary, just for ploting the flamechart of configs
sanitizer = {}
for sig, config in best_configurations.items():
    tokens = config.split("-")
    freq = tokens[0]
    hwconfig = tokens[1]
    m1 = '1.8GHz-' + hwconfig
    m2 = '1.6GHz-' + hwconfig
    if m1 in best_configurations.itervalues() and m2 in best_configurations.itervalues():
        best_configurations[sig] = m2

# Dumping log to file to run LR script
wf = open(outfile,"w+")
for signature, best in best_configurations.iteritems():
    wf.write(signature + "," + best + "\n")
wf.close()