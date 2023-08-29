echo '{"command":"setdata", "id":"Второй", "data": "v++++  4000%0000."}' > /dev/udp/127.0.0.1/9010
sleep 5
echo '{"command":"setdata", "id":"Второй", "data": "v++++  4000M98D83E00%0000."}' > /dev/udp/127.0.0.1/9010
sleep 1
echo '{"command":"setdata", "id":"Второй", "data": "v++++  4000%0000."}' > /dev/udp/127.0.0.1/9010
sleep 1
echo '{"command":"cardresponse", "phisicalObject":"TruckScale[1]"}' > /dev/udp/127.0.0.1/9010


