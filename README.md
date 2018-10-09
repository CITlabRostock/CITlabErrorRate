# CITlabErrorRate
A tool for computing error rates for different algorithms:

[![Build Status](http://dbis-halvar.uibk.ac.at/jenkins/buildStatus/icon?job=CITlabErrorRate)](http://dbis-halvar.uibk.ac.at/jenkins/job/CITlabErrorRate)

## Running:

### HTR:
It can calculate Character Error Rate (CER), Word Error Rate (WER),
Bag of Tokens (BOG)
and some more metrics. Type
```
java -cp <this-jar> de.uros.citlab.errorrate.HtrError --help
```
for more information concerning evaluating an HTR result if the files are
PAGE-XML-files. For raw UTF-8 encoded textfiles use
```
java -cp <this-jar> de.uros.citlab.errorrate.HtrErrorTxt
```
or
```
java -cp <this-jar> de.uros.citlab.errorrate.HtrErrorTxtLeip
```

### KWS:

To calculate measures for KWS
```
java -cp <this-jar> de.uros.citlab.errorrate.KwsError
```
can be used. Use --help to see the configuration opportunities

### Text2Image

To calculate measures for image alignment
```
java -cp <this-jar> de.uros.citlab.errorrate.Text2ImageError
```
can be used. Use --help to see the configuration opportunities


## Requirements
- Java >= version 8
- Maven
- All further dependencies are gathered via Maven

## Build
```
git clone https://github.com/CITlabRostock/CITlabErrorRate
cd CITlabErrorRate
mvn install
```