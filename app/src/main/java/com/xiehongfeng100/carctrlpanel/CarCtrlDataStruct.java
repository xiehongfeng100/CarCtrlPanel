package com.xiehongfeng100.carctrlpanel;

/**
 * Created by xiehongfeng100 on 2015-12-06.
 */
public class CarCtrlDataStruct {
    public static int index;
    public static short type;
    public static short key;
    public static int val;
    public static byte buf[] = new byte[28];

    public CarCtrlDataStruct(){
        for(int i = 0; i < 28; i++) {
            buf[i] = 0;
        }
    }

    public static byte serializedStream[] = new byte[40];
    public static void serialization()
    {
        intToBytes(index, serializedStream, 0);
        shortToBytes(type, serializedStream, 4);
        shortToBytes(key, serializedStream, 6);
        intToBytes(val, serializedStream, 8);
        for(int i = 0; i < 28; i++) {
            serializedStream[i + 12] = buf[i];
        }
    }

//    public static void intToBytes(int n, byte[] array, int offset) {
//        array[3 + offset] = (byte) (n & 0xff);
//        array[2 + offset] = (byte) (n >> 8 & 0xff);
//        array[1 + offset] = (byte) (n >> 16 & 0xff);
//        array[offset] = (byte) (n >> 24 & 0xff);
//    }
//
//    public static void shortToBytes(short n, byte[] array, int offset) {
//        array[offset + 1] = (byte) (n & 0xff);
//        array[offset] = (byte) ((n >> 8) & 0xff);
//    }

    public static void intToBytes(int n, byte[] array, int offset) {
        array[3 + offset] = (byte) (n >> 24 & 0xff);
        array[2 + offset] = (byte) (n >> 16 & 0xff);
        array[1 + offset] = (byte) (n >> 8 & 0xff);
        array[offset] = (byte) (n & 0xff);
    }

    public static void shortToBytes(short n, byte[] array, int offset) {
        array[offset + 1] = (byte) ((n >> 8) & 0xff);
        array[offset] = (byte) (n & 0xff);
    }

}


