package com.funbiscuit.jfx.curvedetect.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MatlabFileHelper {
    public static final int miINT8 = 1;
    public static final int miUINT8 = 2;
    public static final int miINT16 = 3;
    public static final int miUINT16 = 4;
    public static final int miINT32 = 5;
    public static final int miUINT32 = 6;
    public static final int miSINGLE = 7;
    public static final int miDOUBLE = 9;
    public static final int miINT64 = 12;
    public static final int miUINT64 = 13;
    public static final int miMATRIX = 14;
    public static final int mxCHAR_CLASS = 4;
    public static final int mxDOUBLE_CLASS = 6;
    public static final int mxSINGLE_CLASS = 7;
    public static final int mxINT8_CLASS = 8;
    public static final int mxUINT8_CLASS = 9;
    public static final int mxINT16_CLASS = 10;
    public static final int mxUINT16_CLASS = 11;
    public static final int mxINT32_CLASS = 12;
    public static final int mxUINT32_CLASS = 13;


    public MatlabFileHelper() {
    }

    public void writeMatTest(OutputStream outputStream) throws IOException {
        double[] test = new double[]{-0.32D, -1.43D, -2.98D, -3.23D, -4.43387D, -5.9888D};
        writeMatHeader(outputStream);
        writeMatrix(outputStream, "test2x3", test, 2, 3);
        writeMatrix(outputStream, "test3x2", test, 3, 2);
        writeMatrix(outputStream, "test2x3cm", test, 2, 3, false);
        writeMatrix(outputStream, "test3x2cm", test, 3, 2, false);
        writeVector(outputStream, "test6x1", test);
        writeVector(outputStream, "test1x6", test, true);
    }

    public void writeVector(OutputStream outputStream, String name, double[] data) throws IOException {
        writeVector(outputStream, name, data, false);
    }

    public void writeVector(OutputStream outputStream, String name, double[] data, boolean asRow) throws IOException {
        if (asRow) {
            writeMatrix(outputStream, name, data, 1, data.length);
        } else {
            writeMatrix(outputStream, name, data, data.length, 1);
        }
    }

    public void writeMatrix(OutputStream outputStream, String name, double[] data, int rows, int columns) throws IOException {
        writeMatrix(outputStream, name, data, rows, columns, true);
    }

    public void writeMatrix(OutputStream outputStream, String name, double[] data, int rows, int columns, boolean rowMajor) throws IOException {

        if (data.length != rows * columns)
            return;

        int sizeofDouble = 8;

        //write data element
        int dataType = miMATRIX;

        int[] dims = new int[]{rows, columns}; //1xn - row; nx1 - column

        int nameSize = name.length();
        if (nameSize % 8 != 0)
            nameSize += 8 - nameSize % 8;


        int dataSize = rows * columns * sizeofDouble;
        if (dataSize % 8 != 0)
            dataSize += 8 - nameSize % 8;


        int bytesNum = 16 +      //array flags subelement
                16 +             //dimensions subelement
                nameSize + 8 +   //name subelement
                dataSize + 8;    //data subelement

        writeInt(outputStream, dataType);
        writeInt(outputStream, bytesNum);

        //write subelements

        //write array flags block (8 bytes)
        dataType = miUINT32;
        bytesNum = 8;
        writeInt(outputStream, dataType);
        writeInt(outputStream, bytesNum);

        //third byte contains flags (global or complex or logical)
        //fourth byte (lowest) contatins info about data class
        int flags = mxDOUBLE_CLASS;
        writeInt(outputStream, flags);
        //write 4 bytes of undefined data to array flags block
        writeInt(outputStream, 0);


        writeDataElement(outputStream, miINT32, int2byte(dims));
        writeDataElement(outputStream, miINT8, name.getBytes(StandardCharsets.UTF_8));
        writeDataElement(outputStream, miDOUBLE, double2byte(data, rows, rowMajor));
    }

    private byte[] double2byte(double[] src, int rows) {
        return double2byte(src, rows, true);
    }

    private byte[] double2byte(double[] src, int rows, boolean rowMajor) {
        int srcLength = src.length;
        int columns = src.length / rows;
        byte[] dst = new byte[srcLength << 3];//double needs 8 bytes per number

        ByteBuffer buf = ByteBuffer.wrap(dst);

        //matlab reads matrices ar column major so we should make swapping
        if (rowMajor) {
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < rows; row++) {
                    buf.putDouble(src[row * columns + column]);
                }
            }
        } else {
            for (double v : src) {
                buf.putDouble(v);
            }
        }

        return dst;
    }

    private byte[] int2byte(int[] src) {
        int srcLength = src.length;
        byte[] dst = new byte[srcLength << 2]; //int needs 4 bytes per number
        ByteBuffer buf = ByteBuffer.wrap(dst);

        for (int value : src) {
            buf.putInt(value);
        }

        return dst;
    }

    private void writeDataElement(OutputStream outputStream, int type, byte[] data) throws IOException {
        writeDataElementTag(outputStream, type, data);
        writeDataElementBody(outputStream, type, data);
    }

    private void writeDataElementTag(OutputStream outputStream, int type, byte[] data) throws IOException {
        //calculate the number of data bytes:
        int nBytes = data.length;

        if (type == mxCHAR_CLASS) {
            //mxCHAR_CLASS data is 8bit, but is written as 16bit uints
            nBytes *= 2;
            //write the datatype identifier
            writeInt(outputStream, miUINT16);
        } else {
            //write the datatype identifier
            writeInt(outputStream, type);
        }

        //write the number of data bytes to the data element's tag:
        writeInt(outputStream, nBytes);
    }

    private void writeDataElementBody(OutputStream outputStream, int type, byte[] data) throws IOException {
        //calculate the number of data bytes:
        int nBytes = data.length;
        int paddingBytes = 0;
        if (type == mxCHAR_CLASS) {
            //mxCHAR_CLASS data is 8bit, but is written as 16bit uints
            nBytes *= 2;
        }

        // write the data
        if (type == mxCHAR_CLASS) {
            //we have to write one char
            //at a time, each followed by a null byte
            for (byte b : data) {
                outputStream.write(b);
                outputStream.write(0);
            }
        } else {
            outputStream.write(data);
        }

        // padding may be required to ensure 64bit boundaries
        // between data elements
        if (nBytes % 8 > 0) {
            paddingBytes = 8 - nBytes % 8;
        }

        for (int i = 0; i < paddingBytes; i++) {
            outputStream.write(0);
        }
    }

    private void writeInt(OutputStream outputStream, int value) throws IOException {
        byte[] intByteArray = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(intByteArray);
        buffer.clear();
        buffer.putInt(value);
        outputStream.write(buffer.array());
    }

    private void writeShort(OutputStream outputStream, short value) throws IOException {
        byte[] intByteArray = new byte[2];
        ByteBuffer buffer = ByteBuffer.wrap(intByteArray);
        buffer.clear();
        buffer.putShort(value);
        outputStream.write(buffer.array());
    }

    public void writeMatHeader(OutputStream outputStream) throws IOException {
        int headerSize = 116;   //matlab uses 116 bytes for header text
        int subsysSize = 8;     //and 8 bytes for sybsystem data offset

        String head = "File was created by Curve Detect";
        byte[] subsysData = new byte[subsysSize];

        int emptyChar = 0x20;   // ascii for space

        //write header (128 bytes)
        //write text in header (116 bytes)
        for (int k = 0; k < headerSize; ++k) {
            if (k < head.length()) {
                outputStream.write(head.codePointAt(k));
            } else {
                outputStream.write(emptyChar);
            }
        }

        outputStream.write(subsysData);
        //version of mat file is 0x0100
        writeShort(outputStream, (short) 0x0100);
        // endian indicator
        // 0x4D ascii code for M
        // 0x49 ascii code for I
        writeShort(outputStream, (short) 0x4D49);
    }
}
