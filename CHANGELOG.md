
# Changelog

# 5.0.0
* bugfix: ILineComparison return right index (was always 0)
* new feature: first (untested) version for WER
* simplifying interfaces (have to increase mayor version)

# 4.0.0
* refactor in de.uros.citlab.errorrate. interfaces class IDistance -> IPoint
* refactor in de.uros.citlab.errorrate IErrorModuleWithSegmentation.LineComparison -> ILineComparison
* set logger-levels from INFO to TRACE for time measure
* bugfix: merge lines now handles spaces correctly
* add some javadocs

# 3.5.1
* bugfix: tests

# 3.5.0
* Add detailed result structure for error calculation
* bugfix: fallback-calculation counts correct
* bugfix: recursive subproblem is set up correctly

# 3.4.2
* bugfix: fix usage of remaining line breaks and spaces
* bugfix: Calculation of count ERR
* better implementation of greedy-algorithm if reading order is ignored

# 3.4.1
* bugfix: modify test error rates because of changes in 3.4.0 

# 3.4.0
* right implementation of count for segmentation-mode for end2end
* substitution counts available for end2end
* add additional metrics for error rates: PREC,REC,ACC
* minor fixes

# 3.3.0
* add End2End measure
* change Readme: more parameter description, add End2End as measur
* add command-line tool for End2End measure  

# 3.2.0
* add statistic module to calculate confidence intervals for CER for a given number of lines.

# 3.0.1
* all HtrError classes return a result structure which contains the counts and measures

# 3.0.0
* refactor ErrorRate from eu.transkribs.TranskribsErrorRate to de.uros.citlab.errorrate

# 2.2.6
* increase version of TranskribusInterface to 0.0.2

# 2.2.4
* hotfix #3 Remove commit editor noise

# 2.2.3
* bugfix R-Precision
* make it possible to save PR-Curve with KWSError

# 2.2.2
* bugfix KeywordExtractor: Also put Character Class M,Cs,Co to L
* bugfix KeywordExtractor: works with upper, part and alpha
* update KwsError: delete unused options
## 2.2.1
* bugfix Keyword extractor
* improvements of tests

## 2.2
* bugfix in keyword extractor
* bugfix polygonPart
* make kwsExtractor observable

## 2.1
* add optional time to KWS result
* bugfix: create logical baselin-polygon from json-string on demand

## 2.0.2
*bugfix: switch to TranskribusXMLExtracor-0.3 which is in TranskribusLanguageResources

## 2.0.1
* bugfix: delete dependencies to log4j and undused import

## 2.0
* add result-structure for KWS
* add KWS-measures and PR-Curve
* add Text2Image-measures

## 1.3
* make dependent on TranskribusXMLExtractor-0.1

## 1.2
* tests added for use in transkribus

## 1.1
* use TranskribusTokenizer-0.3 
* Use Tests for both ASV and URO tokenizer

## 1.0
* first stable running version
