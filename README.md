# TechServices_v0

There are two maven projects in this folder.  

# Masker
The first project is masker.  It is a Java main program with system out to stdout.  It is just an example of how one can might mask/obfuscate sensitive data.

# Portfolio
The second project is portfolio. It requires JRE 1.8.

It is a backend Jetty REST web services project for a financial advisor.

It is built using "mvn clean install"

To run type "mvn exec:java".  It will run on port 8080 by default.

It consists of two services:

# 1) get invest portfolio
      will provide a json object of portfolio allocation percentages based on an increasing risk level from 1 to 10

Example Usage:
http://localhost:8080/invest/portfolios?riskLevel=1

Example Response:
{"level":1,"bonds_pct":80,"large_cap_pct":20,"mid_cap_pct":0,"foreign_pct":0,"small_cap_pct":0}

# 2) get invest adjustments 
  will provide a json array value of portfolio debits and credits to balance dollar amounts according to risk level portfolio and current investment asset allocations

Example Usage:
http://localhost:8080/invest/adjustments?riskLevel=7&bondAmt=8&largeCapAmt=33&midCapAmt=14&foreignAmt=36&smallCapAmt=9

Example Response:
[{2=11.0}, {3=-11.0}, {1=-8.00}, {0=8.00}, {0=4.00}, {4=-4.00}]



