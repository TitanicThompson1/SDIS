#! /usr/bin/bash

java SSLServer 4445 \
        -Djavax.net.ssl.keyStore="server.keys" -Djavax.net.ssl.keyStorePassword="123456" \
        -Djavax.net.ssl.trustStore="truststore" -Djavax.net.ssl.trustStorePassword="123456" 
     