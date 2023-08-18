import utils.CRC16Modbus

import scala.util.matching.Regex
Int.MaxValue
val pSvetofor:Regex = "[RG?]".r
pSvetofor.matches("W")
val s = "12345678R"
s.takeRight(1)

val modbus = new CRC16Modbus()
modbus.reset()
val str = "v-+++ 24000R%"
modbus.update(str.getBytes(),0,str.length)
modbus.getValue
modbus.getValue.toHexString

modbus.getCrcBytes
val hv = CRC16Modbus.bytesArrayToHexString( modbus.getCrcBytes )
java.lang.Long.parseLong(hv,16)

