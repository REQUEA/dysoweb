package com.requea.dysoweb.panel;

/* (PD) 2001 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * Base32.java
 *
 */

/**
 * Base32 - encodes and decodes 'Canonical' Base32
 *
 * @author  Robert Kaye & Gordon Mohr
 *          Modified and optimized by P. Verdy
 */
public class Base32 {
    private static final String base32Chars = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static final byte[] base32Lookup =
    {         26,27,28,29,30,31,-1,-1,-1,-1,-1,-1,-1,-1, //   23456789:;<=>?
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14, // @ABCDEFGHIJKLMNO
        15,16,17,18,19,20,21,22,23,24,25,-1,-1,-1,-1,-1, // PQRSTUVWXYZ[\]^_
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14, // `abcdefghijklmno
        15,16,17,18,19,20,21,22,23,24,25                 // pqrstuvwxyz
    };
    
    static public String encode(final byte[] bytes)
    {
        StringBuffer base32 = new StringBuffer((bytes.length * 8 + 4) / 5);

        int currByte, digit, i = 0;
        while (i < bytes.length) {
            // INVARIANTS FOR EACH STEP n in [0..5[; digit in [0..31[; 
            // The remaining n bits are already aligned on top positions
            // of the 5 least bits of digit, the other bits are 0.
            ////// STEP n = 0; insert new 5 bits, leave 3 bits
            currByte = bytes[i++] & 255;
            base32.append(base32Chars.charAt(currByte >> 3));
            digit = (currByte & 7) << 2;
            if (i >= bytes.length) { // put the last 3 bits
                base32.append(base32Chars.charAt(digit));
                break;
            }
            ////// STEP n = 3: insert 2 new bits, then 5 bits, leave 1 bit
            currByte = bytes[i++] & 255;
            base32.append(base32Chars.charAt(digit | (currByte >> 6)));
            base32.append(base32Chars.charAt((currByte >> 1) & 31));
            digit = (currByte & 1) << 4;
            if (i >= bytes.length) { // put the last 1 bit
                base32.append(base32Chars.charAt(digit));
                break;
            }
            ////// STEP n = 1: insert 4 new bits, leave 4 bit
            currByte = bytes[i++] & 255;
            base32.append(base32Chars.charAt(digit | (currByte >> 4)));
            digit = (currByte & 15) << 1;
            if (i >= bytes.length) { // put the last 4 bits
                base32.append(base32Chars.charAt(digit));
                break;
            }
            ////// STEP n = 4: insert 1 new bit, then 5 bits, leave 2 bits
            currByte = bytes[i++] & 255;
            base32.append(base32Chars.charAt(digit | (currByte >> 7)));
            base32.append(base32Chars.charAt((currByte >> 2) & 31));
            digit = (currByte & 3) << 3;
            if (i >= bytes.length) { // put the last 2 bits
                base32.append(base32Chars.charAt(digit));
                break;
            }
            ///// STEP n = 2: insert 3 new bits, then 5 bits, leave 0 bit
            currByte = bytes[i++] & 255;
            base32.append(base32Chars.charAt(digit | (currByte >> 5)));
            base32.append(base32Chars.charAt(currByte & 31));
            //// This point is reached for bytes.length multiple of 5
        }
        return base32.toString();
    }

    static public byte[] decode(final String base32)
    {
        byte[] bytes = new byte[base32.length() * 5 / 8];
        int n = 0, offset = 0, i = 0;
        byte nextByte = 0; // init required by Java syntax only

        while (i < base32.length()) {
            /* Skip chars outside the lookup table */
            int lookup = base32.charAt(i++) - '2';
            if (lookup < 0 || lookup >= base32Lookup.length)
                continue;
            byte digit = base32Lookup[lookup];
            if (digit == -1)
                continue;
            switch (n) {
            case 0: // leave 5 bits
                nextByte = (byte)(digit << 3);
                n = 5; break;
            case 5: // insert 3 bits, leave 2 bits
                bytes[offset++] = (byte)(nextByte | (digit >> 2));
                nextByte = (byte)((digit & 3) << 6);
                n = 2; break;
            case 2: // leave 7 bits
                nextByte |= (byte)(digit << 1);
                n = 7; break;
            case 7: // insert 1 bit, leave 4 bits
                bytes[offset++] = (byte)(nextByte | (digit >> 4));
                nextByte = (byte)((digit & 15) << 4);
                n = 4; break;
            case 4: // insert 4 bits, leave 1 bit
                bytes[offset++] = (byte)(nextByte | (digit >> 1));
                nextByte = (byte)((digit & 1) << 7);
                n = 1; break;
            case 1: // leave 6 bits
                nextByte |= (byte)(digit << 2);
                n = 6; break;
            case 6: // insert 2 bits, leave 3 bits
                bytes[offset++] = (byte)(nextByte | (digit >> 3));
                nextByte = (byte)((digit & 7) << 5);
                n = 3; break;
            case 3: // insert 5 bits, leave 0 bit
                bytes[offset++] = (byte)(nextByte | digit);
                n = 0; break;
            }
        }
        // On loop exit, discard remaining n bits.

        // However the output bytes table could be truncated here to "offset" 
        // positions, ignoring the estimated "bytes.length" (this occurs if
        // invalid Base32 characters were present in input string).
        // The side-effect is that the following invalid Base32 string
        // "00000012300000000456" is decoded exactly the same way as
        // "123456AAAAAAAAAAAAAA" where invalid characters are discarded but
        // replaced by trailing null Base32 digits (A), i.e. the binary
        // output will have the expected length with trailing null bits,
        // Wouldn't it be better to throw a known exception instead of
        // continuing the loop above when aninvalid character is detected?

        return bytes;
    }
    
    /** For testing, take a command-line argument in Base32, decode, print in hex, 
     * encode, print
     */
    static public void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Supply a Base32-encoded argument...");
            return;
        }
        System.out.println(" Original Base32: "+args[0]);
        byte[] decoded = Base32.decode(args[0]);
        System.out.print  ("     Decoded Hex: ");
        for(int i = 0; i < decoded.length ; i++) {
            int b = decoded[i] & 255;
            System.out.print((Integer.toHexString(b + 256)).substring(1));
        }
        System.out.println();
        final String reencoded = Base32.encode(decoded);
        System.out.println("Reencoded Base32: "+reencoded);
        System.out.println("       Input was: "+
            (reencoded.equals(args[0]) ? "" : "NOT ")+
            "canonically encoded");
        System.out.print  ("   Redecoded Hex: ");
        byte[] redecoded = Base32.decode(reencoded);
        for(int i = 0; i < decoded.length ; i++) {
            int b = redecoded[i] & 255;
            System.out.print((Integer.toHexString(b + 256)).substring(1));
        }
        System.out.println();
    }
}
