package com.m5c.safesockets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple service class, computes the MD5 Hash (String) for a given String.
 *
 * @author m5c
 */
public class Md5Hasher
{

    public static String getMessageHash(String message)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(message.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Unable to create message hash");
        }
    }

}
