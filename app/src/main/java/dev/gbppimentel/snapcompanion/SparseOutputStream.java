package dev.gbppimentel.snapcompanion;

import java.io.*;

/** Sparse private snapshots: zero ranges consume logical size, not gigabytes of flash. */
final class SparseOutputStream extends OutputStream {
    private final RandomAccessFile file;
    SparseOutputStream(File target)throws IOException{file=new RandomAccessFile(target,"rw");file.setLength(0);}
    @Override public void write(int value)throws IOException{file.write(value);}
    @Override public void write(byte[] data,int off,int len)throws IOException{
        if(off<0||len<0||off>data.length-len)throw new IndexOutOfBoundsException();
        boolean zero=true;for(int i=off;i<off+len;i++)if(data[i]!=0){zero=false;break;}
        if(zero)file.seek(file.getFilePointer()+len);else file.write(data,off,len);
    }
    @Override public void close()throws IOException{try{file.setLength(file.getFilePointer());}finally{file.close();}}
}
