#!/bin/sh

//java -classpath lib/*:bin/. edu.upenn.cis.cis455.webserver.HttpServer 8080 /home/cis555/workspace/555-hw1/www &
java -classpath lib/*:bin/. edu.upenn.cis.cis455.webserver.HttpServer
sleep 2
curl http://localhost:8080/control
curl http://localhost:8080/shutdown
