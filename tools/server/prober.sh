#!/bin/bash
logger '****** Start boot script ****'

DATE=$(date +"%Y%m%d%H%M")

# Clean up older files from log directory and archive them
mv -n /sd/log/*.pcap* /sd/log/archive/ >/dev/null
sleep 5

# Running tcpdump

logger '***** Start tcp dump *****'
ifconfig wlan0 up;
airmon-ng start wlan0;
tcpdump -U -s0 -i wlan0mon -w /sd/log/device-$DATE.pcap >/dev/null 2>&1 'type mgt subtype probe-req' &
logger '***** Finished boot script *****'
