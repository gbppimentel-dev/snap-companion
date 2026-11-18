package dev.gbppimentel.snapcompanion.next;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Rotating private snapshots are independent of the preferences file and undo log. */
final class BackupStore {
    private final File directory;
    BackupStore(Context context,String profile){if(!profile.matches("default|[a-f0-9-]{36}"))throw new IllegalArgumentException("Invalid profile");directory=new File(context.getFilesDir(),"backups/"+profile);directory.mkdirs();}
    synchronized void save(String json,long now)throws IOException{List<File> existing=list();if(!existing.isEmpty()&&now-existing.get(0).lastModified()<60_000)return;AtomicFile file=new AtomicFile(new File(directory,"progress-"+now+".json"));FileOutputStream out=null;try{out=file.startWrite();out.write(json.getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}List<File> rows=list();for(int i=12;i<rows.size();i++)if(!rows.get(i).delete())throw new IOException("Old backup could not be removed");}
    synchronized List<File> list(){File[] files=directory.listFiles(f->f.getName().matches("progress-[0-9]+\\.json"));if(files==null)return List.of();List<File> out=new ArrayList<>(Arrays.asList(files));out.sort(Comparator.comparing(File::getName).reversed());return List.copyOf(out);}
    synchronized String read(File file)throws IOException{if(!list().contains(file))throw new IOException("Unknown backup");byte[] bytes=new AtomicFile(file).readFully();if(bytes.length>2_000_000)throw new IOException("Backup exceeds 2 MB");return new String(bytes,StandardCharsets.UTF_8);}
    synchronized void deleteAll()throws IOException{File[] files=directory.listFiles();if(files!=null)for(File file:files)if(!file.delete())throw new IOException("Could not delete a local backup");}
}
