package utils;

public class CRC16ModbusNoTable {

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	public static int ModbusRTU(byte[] buf, int offset, int sz) {
		int Sum = 0xffff;

		for (int i = 0; i < sz; i++) {
			int k = i + offset;

			Sum = (Sum ^ (buf[k] & 0xff));

			for (int j = 8; j != 0; j--) {
				if ((Sum & 0x1) == 1) {
					Sum >>>= 1;
					Sum = (Sum ^ 0xA001);
				} else {
					Sum >>>= 1;
				}
			}

		}

		return Sum;

	}

	public static String bytesArrayToHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}