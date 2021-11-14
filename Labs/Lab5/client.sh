#! /usr/bin/bash

java SSLClient localhost 4445 \
        -Djavax.net.ssl.keyStore="client.keys" -Djavax.net.ssl.keyStorePassword="123456" \
        -Djavax.net.ssl.trustStore="truststore" -Djavax.net.ssl.trustStorePassword="123456" 
     