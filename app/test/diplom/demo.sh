#!/bin/bash
# Demo script for Diplom Project
echo "Демо симуляция данных от оборудования"

echo "Инициализация соединений"
echo '{"command":"stop"}' > /dev/udp/127.0.0.1/9010
echo '{"command":"init"}' > /dev/udp/127.0.0.1/9010
sleep 2
echo "Весы в нуле"
echo '{"command":"setdata", "id":"Первый", "data": "=000000."}' > /dev/udp/127.0.0.1/9010
echo '{"command":"setdata", "id":"Второй", "data": "v++++     0G%0000."}' > /dev/udp/127.0.0.1/9010
echo '{"command":"setdata", "id":"Третий", "data": "v++++     0G%0000."}' > /dev/udp/127.0.0.1/9010
echo '{"command":"setdata", "id":"Четвертый", "data": "v++++     0G%0000."}' > /dev/udp/127.0.0.1/9010




while true
do
sleep 3
echo "Заезжает цистерна на ЖД весы"
echo '{"command":"setdata", "id":"Первый", "data": "=000100."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=000200."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=000400."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=000800."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=001000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=002000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=004000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=008000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=010000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=015000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=020000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=025000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=030000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=035000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=040000."}' > /dev/udp/127.0.0.1/9010

sleep 4
echo "Оператор взвешивает"
sleep 4
echo "Цистерна съезжает с ЖД весов"

echo '{"command":"setdata", "id":"Первый", "data": "=040000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=035000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=030000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=025000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=020000."}' > /dev/udp/127.0.0.1/9010
sleep 0.4
echo '{"command":"setdata", "id":"Первый", "data": "=015000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=010000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=008000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=004000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=003000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=002000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=001000."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=000500."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=000100."}' > /dev/udp/127.0.0.1/9010
sleep 0.2
echo '{"command":"setdata", "id":"Первый", "data": "=000000."}' > /dev/udp/127.0.0.1/9010
sleep 5



echo "Автоцистерна заезжает на весы"
weight=0
while [ $weight -le 40000 ]
do
  w="$weight"
  while [ ${#w} -lt 6 ]
  do
      w="0${w}"
  done

  weight=$[ $weight + 1000 ]

  commlast='R%0000."}'
  # shellcheck disable=SC2089
  comm='{"command":"setdata", "id":"Второй", "data": "v-+++'
  comm="${comm}${w}${commlast}"
  # shellcheck disable=SC2090
  echo $comm > /dev/udp/127.0.0.1/9010
  sleep 0.2
done

echo '{"command":"setdata", "id":"Второй", "data": "v++++040000R%0000."}' > /dev/udp/127.0.0.1/9010

sleep 4

echo "Обработка карты"
echo '{"command":"setdata", "id":"Второй", "data": "v++++040000M98D83E00R%0000."}' > /dev/udp/127.0.0.1/9010
echo '{"command":"setdata", "id":"Второй", "data": "v++++040000R%0000."}' > /dev/udp/127.0.0.1/9010

sleep 4

echo "Ответ на карту"
echo '{"command":"cardresponse", "phisicalObject":"TruckScale[1]"}' > /dev/udp/127.0.0.1/9010
echo '{"command":"setdata", "id":"Второй", "data": "v++++040000G%0000."}' > /dev/udp/127.0.0.1/9010

sleep 4

echo "Автоцистерна съезжает с весов"

while [ $weight -gt 0 ]
do
   w="$weight"
    while [ ${#w} -lt 6 ]
    do
        w="0${w}"
    done

    weight=$[ $weight - 1000 ]

    commlast='G%0000."}'
    # shellcheck disable=SC2089
    comm='{"command":"setdata", "id":"Второй", "data": "v+-++'
    comm="${comm}${w}${commlast}"
    # shellcheck disable=SC2090
    echo $comm > /dev/udp/127.0.0.1/9010
    sleep 0.2
done

echo '{"command":"setdata", "id":"Второй", "data": "v++++     0G%0000."}' > /dev/udp/127.0.0.1/9010





done
echo "Разрыв соединений"
echo '{"command":"stop"}' > /dev/udp/127.0.0.1/9010

