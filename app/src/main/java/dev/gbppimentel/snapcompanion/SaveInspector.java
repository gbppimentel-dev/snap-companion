package dev.gbppimentel.snapcompanion;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Bounded ZIP adapter. Archive paths are never used as filesystem paths. Originals are read only. */
public final class SaveInspector {
    public record Result(List<NewSnapSave.Report> files,int ignored,EdenPhotodex.Snapshot photodex) {
        public Result(List<NewSnapSave.Report> files,int ignored){this(files,ignored,null);}
        public String summary(){int verified=0,empty=0,jpegs=0;for(NewSnapSave.Report r:files){verified+=r.verifiedEntries();empty+=r.emptyEntries();jpegs+=r.jpegEntries();}return files.size()+" validated save container(s) · "+verified+" checksum-verified entries · "+empty+" empty slots · "+jpegs+" embedded JPEG(s)\n"+(photodex==null?"Progress fields unmapped; no registration, score or request completion inferred.":photodex.photos().size()+" verified saved photo categories; request and other fields not decoded.")+"";}
    }
    public static Result inspect(File snapshot,String name,File cache)throws IOException{return inspect(snapshot,name,cache,null);}
    public static Result inspect(File snapshot,String name,File cache,EdenPhotodex decoder)throws IOException {
        try(InputStream in=new FileInputStream(snapshot)){if(in.read()!='P'||in.read()!='K')return new Result(List.of(NewSnapSave.inspect(snapshot,name,null)),0,decoder!=null&&NewSnapSave.NAMES.get(2).equalsIgnoreCase(name)?decoder.read(snapshot):null);}
        List<NewSnapSave.Report> reports=new ArrayList<>();Set<String> found=new HashSet<>();int count=0,ignored=0;long total=0;EdenPhotodex.Snapshot photodex=null;
        try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(new FileInputStream(snapshot)))) {
            ZipEntry e;
            while((e=zip.getNextEntry())!=null){
                if(++count>512)throw new IOException("Archive has too many entries");
                String base=e.getName().replace('\\','/');base=base.substring(base.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
                boolean known=!e.isDirectory()&&NewSnapSave.NAMES.contains(base);
                if(known&&!found.add(base))throw new IOException("Duplicate save identity in archive; select one profile export");
                File copy=known?File.createTempFile("snap-entry-",".bin",cache):null;
                try {
                    OutputStream out=copy==null?null:new SparseOutputStream(copy);
                    try {byte[] buffer=new byte[65536];long size=0;int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>(known?NewSnapSave.MAX_FILE:8L*1024*1024)||total>4L*1024*1024*1024)throw new IOException("Archive exceeds expanded-size limits");if(out!=null)out.write(buffer,0,n);}}
                    finally{if(out!=null)out.close();}
                    if(known){reports.add(NewSnapSave.inspect(copy,base,null));if(decoder!=null&&base.equals(NewSnapSave.NAMES.get(2)))photodex=decoder.read(copy);}else ignored++;
                }finally{if(copy!=null&&!copy.delete())copy.deleteOnExit();}
                zip.closeEntry();
            }
        }
        if(reports.isEmpty())throw new IOException("Archive contains none of the 16 recognized save filenames");
        return new Result(List.copyOf(reports),ignored,photodex);
    }
    public static long copy(InputStream in,OutputStream out,long maximum)throws IOException{byte[] b=new byte[65536];long size=0;int n;while((n=in.read(b))!=-1){size+=n;if(size>maximum)throw new IOException("Document exceeds the snapshot size limit");out.write(b,0,n);}return size;}
}
