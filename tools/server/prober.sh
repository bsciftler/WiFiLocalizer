#!/bin/bash
logger 'STARTING PROBE REQUEST CAPTURE'

# Start WiFi monitoring interface
ifconfig wlan0 up;
airmon-ng start wlan0;

# Start tcpdump monitoring and save to file
DATE=$(date +"%Y%m%d%H%M")
tcpdump -s0 -i wlan0mon -l 'type mgt subtype probe-req' > "$DATE.log" &
./server.py "$DATE.log" > /dev/null

logger '=================================='
logger 'PROBE REQUEST CAPTURE STARTUP DONE'
logger '=================================='
