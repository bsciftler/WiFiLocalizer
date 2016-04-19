#!/bin/bash
logger 'STARTING PROBE REQUEST CAPTURE'

# Start WiFi monitoring interface
ifconfig wlan0 up;
airmon-ng start wlan0;

# Start tcpdump monitoring and save to file
DATE=$(date +"%Y%m%d%H%M")
LOGFILE="/sd/log/$DATE.log"

touch "$LOGFILE"
tcpdump -s0 -i wlan0mon -l 'type mgt subtype probe-req' > "$LOGFILE" &
/sd/codes/server.py "$LOGFILE" &

logger '=================================='
logger 'PROBE REQUEST CAPTURE STARTUP DONE'
logger '=================================='
