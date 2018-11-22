# CITlabErrorRate
A tool for computing error rates for different algorithms:

[![Build Status](http://dbis-halvar.uibk.ac.at/jenkins/buildStatus/icon?job=CITlabErrorRate)](http://dbis-halvar.uibk.ac.at/jenkins/job/CITlabErrorRate)

## Requirements
- Java >= version 8
- Maven
- All further dependencies are gathered via Maven

## Build
```
git clone https://github.com/CITlabRostock/CITlabErrorRate
cd CITlabErrorRate
mvn package [-DskipTests=true]
```
## Running:

### End-2-End Character Error Rate (CER)
This tool makes it possible to measure the CER of an End2End system.
In general it calculate the number of manipulations
(insertion, deletion, substitution) that have to be done to come from the
hypothesis/recognition to the ground truth/reference. The CER is equal to
``#manipulation/#GT``, whereals ``#GT`` is the number of ground truth characters.

The tool has several options to be configured.
A first overview over all parameters can be gathered by  
```
java -jar target/CITlabErrorRate.jar \
<list_pageXml_groundtruth> \
<list_pageXml_hypothesis> \
[-d] [-D] [-g] [-h] [-l] [-N] [-n] [-r] [-s] [-t <arg>] [-u]
```
Parameters that manipulate/normalize both, the ground truth and the hypothesis:
* __-l__ The CER is only calculated on letters, numbers and spaces.
All other characters like punktuations and symbols are ignored.
Examples: ``this, 1 word!`` leads to ``this 1 word`` ; ``31.Nov.2019`` leads to ``31Nov2019``; ``12.000 $ budget``->``12000 budget``
* __-N__ the text will be normalized according the unicode standard NFKC
(see [http://unicode.org/reports/tr15/](http://unicode.org/reports/tr15/#Compatibility_Equivalence_Figure) for details).
Example: ``ſ`` leads to ``s``
* __-n__ the text will be normalized according the unicode standard NFC
(see [http://unicode.org/reports/tr15/](http://unicode.org/reports/tr15/#Canonical_Equivalence_Figure) for details).
Example: ``a^`` leads to ``â``, whereas ``^`` is the accent circumfelx``\u+005e``
* __-u__ make text to upper (so it is case insensitive).
Example: ``Straße`` leads to ``STRASSE``

Parameter that determin how the error is calculated:
* __-r__ the reading order is ignored.
So ``["first line", "second line"]`` vs. ``["second line", "first line"]`` would be correct.
* __-s__ the right segmentation plays a role.
That means a space ``\+u0020`` can be interpretet as space or as split of lines.
So ``["split and", "merge lines"]`` vs. ``["split", "and merge", "lines"]`` would be correct.
* __-g__ the geometric postion of the line plays a role. The couverage between two lines have above a threshold (see parameter -t).
* __-t \<FLOAT\>__ the minimal couverage [0.0,1.0) between two line so that they were assumed to be adjacent.

Parameter for analizing errors, but __not implemented yet__:
* __-d__ the algorithm will return all manipulations which had to be done to come from the hypothesis to the ground truth (insertions, deletions, substitutions)
* __-D__ the algorithm will return all operations which had to be done to come from the hypothesis to the ground truth (corrects, insertions, deletions, substitutions)

### HTR:
It can calculate Character Error Rate (CER), Word Error Rate (WER),
Bag of Tokens (BOG)
and some more metrics. Type
```
java -cp target/CITlabErrorRate.jar de.uros.citlab.errorrate.HtrError --help
```
for more information concerning evaluating an HTR result if the files are
PAGE-XML-files. For raw UTF-8 encoded textfiles use
```
java -cp target/CITlabErrorRate.jar de.uros.citlab.errorrate.HtrErrorTxt --help
```
or
```
java -cp target/CITlabErrorRate.jar de.uros.citlab.errorrate.HtrErrorTxtLeip
```

### KWS:

To calculate measures for KWS
```
java -cp target/CITlabErrorRate.jar de.uros.citlab.errorrate.KwsError
```
can be used. Use --help to see the configuration opportunities

### Text2Image

To calculate measures for image alignment
```
java -cp target/CITlabErrorRate.jar de.uros.citlab.errorrate.Text2ImageError
```
can be used. Use --help to see the configuration opportunities
