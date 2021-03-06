/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.almightshell.pf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.io.Writable;

/**
 *
 * @author Shell
 */
public class PerfectFilesUtil {

    private static final LZ4Factory FACTORY = LZ4Factory.fastestInstance();
    private static final LZ4Compressor COMPRESSOR = FACTORY.fastCompressor();
    private static final LZ4FastDecompressor DECOMPRESSOR = FACTORY.fastDecompressor();

    public static byte[] compress(byte[] data) {
        final int decompressedLength = data.length;
        int maxCompressedLength = COMPRESSOR.maxCompressedLength(decompressedLength);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = COMPRESSOR.compress(data, 0, decompressedLength, compressed, 0, maxCompressedLength);

        if (compressed.length == compressedLength) {
            return compressed;
        }

        return Arrays.copyOfRange(compressed, 0, compressedLength);
    }
    
    public static byte[] decompress (byte[] compressed,int decompressedLength) {
        byte[] restored = new byte[decompressedLength];
        int compressedLength2 = DECOMPRESSOR.decompress(compressed, 0, restored, 0, decompressedLength);
        return restored;
    }

    public static long getHash(String name) {
        return name.hashCode();
    }

    public static byte[] serialize(Writable writable) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dataOut = null;
        try {
            dataOut = new DataOutputStream(out);
            writable.write(dataOut);
            return out.toByteArray();
        } finally {
            IOUtils.closeQuietly(dataOut);
        }
    }

    public static <T extends Writable> T asWritable(byte[] bytes, Class<T> clazz) throws IOException {
        T result = null;
        DataInputStream dataIn = null;
        try {
            result = clazz.newInstance();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            dataIn = new DataInputStream(in);
            result.readFields(dataIn);
        } catch (InstantiationException e) {
            // should not happen
            assert false;
        } catch (IllegalAccessException e) {
            // should not happen
            assert false;
        } finally {
            IOUtils.closeQuietly(dataIn);
        }
        return result;
    }

    /**
     * Convert an Object object into stream of bytes.
     *
     * @param s java object.
     * @return stream of bytes
     */
    public static byte[] toObjectStream(Serializable s) {
        // Reference for stream of bytes
        byte[] stream = null;
        // ObjectOutputStream is used to convert a Java object into OutputStream
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);) {
            oos.writeObject(s);
            stream = baos.toByteArray();
        } catch (IOException e) {
            // Error in serialization
            e.printStackTrace();
        }
        return stream;
    }

    /**
     * Convert stream of bytes to Object.
     *
     * @param <T>
     * @param stream byte array
     * @param clazz
     * @return Student object
     */
    public static <T extends Serializable> T toObject(byte[] stream, Class<T> clazz) {
        T s = null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(stream);
                ObjectInputStream ois = new ObjectInputStream(bais);) {
            s = (T) ois.readObject();
        } catch (IOException e) {
            // Error in de-serialization
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // You are converting an invalid stream to Student
            e.printStackTrace();
        }
        return s;
    }

    public static long checkPositionInDirectory(long key, long globalDepth) {
        if (globalDepth <= 0) {
            return 0;
        }
        return key << -globalDepth >>> -globalDepth;
//        String s = Long.toBinaryString(key);
//        s = s.substring((int) (s.length() - globalDepth), s.length());
//        return s.isEmpty() ? 0 : new BigInteger(s, 2).intValue();
    }

    public static int[] checkSplitPositionsInDirectory(long key, long globalDepth) {
        String s = Long.toBinaryString(key);
        s = s.substring((int) (s.length() - globalDepth), s.length());

        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, '0');

        int x = new BigInteger(sb.toString(), 2).intValue();
        sb.setCharAt(0, '1');
        int y = new BigInteger(sb.toString(), 2).intValue();

        return new int[]{Math.min(x, y), Math.max(x, y)};
    }

}
