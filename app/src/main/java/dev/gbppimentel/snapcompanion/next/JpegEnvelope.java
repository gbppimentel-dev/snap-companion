package dev.gbppimentel.snapcompanion.next;

import java.util.Arrays;
/** Find the first complete JPEG, respecting segment lengths, byte stuffing and restart markers. */
final class JpegEnvelope {
    static byte[] extract(byte[] bytes,int start){
        if(start<0||start+4>bytes.length||(bytes[start]&255)!=255||(bytes[start+1]&255)!=216)return new byte[0];int p=start+2;boolean entropy=false;
        while(p<bytes.length){
            if((bytes[p]&255)!=255){if(entropy){p++;continue;}return new byte[0];}
            while(p<bytes.length&&(bytes[p]&255)==255)p++;if(p>=bytes.length)return new byte[0];int marker=bytes[p++]&255;
            if(entropy&&(marker==0||marker>=208&&marker<=215))continue;
            if(marker==217)return Arrays.copyOfRange(bytes,start,p);
            if(marker==216||marker==0)return new byte[0];if(marker==1)continue;
            if(p+2>bytes.length)return new byte[0];int length=((bytes[p]&255)<<8)|(bytes[p+1]&255);if(length<2||p+length>bytes.length)return new byte[0];p+=length;entropy=marker==218;
        }return new byte[0];
    }
}
