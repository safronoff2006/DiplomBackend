echo '{"command":"setdata", "id":"Второй", "data": "v++++  4000%0000."}' > /dev/udp/127.0.0.1/9010
sleep 5
echo '{"command":"setdata", "id":"Второй", "data": "v++++  4000MD234A678%0000."}' > /dev/udp/127.0.0.1/9010
sleep 2
echo '{"command":"setdata", "id":"Второй", "data": "v++++  4000%0000."}' > /dev/udp/127.0.0.1/9010

