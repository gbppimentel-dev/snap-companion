package dev.gbppimentel.snapcompanion.next;

import android.content.Context;
import android.graphics.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Private personal photos, separate from game saves. Sources and associations are explicit. */
final class PersonalGallery {
    record Photo(String id,int species,int stars,String course,String form,int score,String medal,List<Integer> components,String tags,boolean favorite,long created,String source,String linkedHash){}
    private static final java.util.concurrent.ConcurrentHashMap<String,Object> LOCKS=new java.util.concurrent.ConcurrentHashMap<>();
    private final Object lock;
    private final File directory,index;
    PersonalGallery(Context context,String profile){if(!profile.matches("default|[A-Za-z0-9-]{1,80}"))throw new IllegalArgumentException("Invalid profile");directory=new File(context.getFilesDir(),"gallery/"+profile);directory.mkdirs();index=new File(directory,"index.json");lock=LOCKS.computeIfAbsent(directory.getAbsolutePath(),key->new Object());}
    List<Photo> all()throws IOException{synchronized(lock){
        if(!index.exists())return List.of();try{JSONArray rows=new JSONArray(new String(new AtomicFile(index).readFully(),StandardCharsets.UTF_8));List<Photo> out=new ArrayList<>();for(int i=0;i<rows.length();i++)out.add(read(rows.getJSONObject(i)));out.sort(Comparator.comparingLong(Photo::created).reversed());return List.copyOf(out);}catch(JSONException e){throw new IOException("Gallery index could not be read; existing files preserved",e);}
    }}
    File file(Photo photo){if(!photo.id().matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Invalid photo ID");return new File(directory,photo.id()+".jpg");}
    Photo add(byte[] jpeg,int species,int stars,String course,int score,String medal,List<Integer> components,String source,String hash)throws IOException{return addGuarded(jpeg,species,stars,course,score,medal,components,source,hash,()->true);}
    Photo addAlbum(byte[] jpeg,byte[] thumbnail,byte[] metadata,int slot,String identity,java.util.function.BooleanSupplier allowed)throws IOException{synchronized(lock){
        if(metadata.length!=328||slot<0||slot>=2376)throw new IOException("Unsupported album association");
        Photo p=addGuarded(jpeg,0,0,"",0,"Unspecified",List.of(),"Eden personal album · saved slot "+slot+" · species/score not decoded",identity,allowed);if(p==null)return null;
        try{writeAttachment(p,".album.bin",metadata);if(thumbnail!=null)writeAttachment(p,".thumb.jpg",thumbnail);return p;}catch(IOException e){try{delete(p);}catch(IOException cleanup){e.addSuppressed(cleanup);}throw e;}
    }}
    private File attachment(Photo p,String suffix){file(p);return new File(directory,p.id()+suffix);}
    private void writeAttachment(Photo p,String suffix,byte[] data)throws IOException{AtomicFile target=new AtomicFile(attachment(p,suffix));FileOutputStream out=null;try{out=target.startWrite();out.write(data);target.finishWrite(out);}catch(IOException e){if(out!=null)target.failWrite(out);throw e;}}
    byte[] albumMetadata(Photo p)throws IOException{synchronized(lock){File f=attachment(p,".album.bin");return f.exists()?new AtomicFile(f).readFully():null;}}
    Photo addGuarded(byte[] jpeg,int species,int stars,String course,int score,String medal,List<Integer> components,String source,String hash,java.util.function.BooleanSupplier allowed)throws IOException{synchronized(lock){if(!allowed.getAsBoolean())return null;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,options);if(options.outWidth<1||options.outHeight<1||(long)options.outWidth*options.outHeight>100_000_000L)throw new IOException("Unsupported photo dimensions");
        if(!"image/jpeg".equals(options.outMimeType)||Math.max(options.outWidth,options.outHeight)>4096){options.inJustDecodeBounds=false;options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>2048)options.inSampleSize*=2;Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,options);if(bitmap==null)throw new IOException("Photo decoding failed");try{ByteArrayOutputStream encoded=new ByteArrayOutputStream();if(!bitmap.compress(Bitmap.CompressFormat.JPEG,95,encoded))throw new IOException("Photo conversion failed");jpeg=encoded.toByteArray();}finally{bitmap.recycle();}}
        String id=UUID.randomUUID().toString();Photo p=new Photo(id,species,stars,course,"",score,medal,List.copyOf(components),"",false,System.currentTimeMillis(),source,hash);
        AtomicFile out=new AtomicFile(file(p));FileOutputStream stream=null;try{stream=out.startWrite();stream.write(jpeg);out.finishWrite(stream);}catch(IOException e){if(stream!=null)out.failWrite(stream);throw e;}
        try{List<Photo> rows=new ArrayList<>(all());rows.add(p);write(rows);return p;}catch(IOException e){file(p).delete();throw e;}
    }}
    void update(Photo photo)throws IOException{synchronized(lock){List<Photo> rows=new ArrayList<>(all());boolean found=rows.removeIf(p->p.id().equals(photo.id()));if(!found)throw new IOException("Photo was removed");rows.add(photo);write(rows);}}
    private void removeFiles(Photo p)throws IOException{for(File f:List.of(file(p),attachment(p,".album.bin"),attachment(p,".thumb.jpg")))if(!f.delete()&&f.exists())throw new IOException("Could not delete a personal photo or its saved metadata");}
    void delete(Photo photo)throws IOException{synchronized(lock){List<Photo> rows=new ArrayList<>(all());rows.removeIf(p->p.id().equals(photo.id()));write(rows);removeFiles(photo);}}
    void deleteAll()throws IOException{synchronized(lock){for(Photo photo:all())removeFiles(photo);write(List.of());}}
    void export(OutputStream output)throws IOException{
        List<Photo> snapshot;List<File> files=new ArrayList<>();JSONArray rows=new JSONArray();
        synchronized(lock){snapshot=all();try{for(Photo p:snapshot){JSONObject row=json(p);files.add(file(p));File metadata=attachment(p,".album.bin"),thumbnail=attachment(p,".thumb.jpg");if(metadata.exists()){files.add(metadata);row.put("opaqueAlbumMetadataFile",metadata.getName()).put("albumMetadataArchive",6);}if(thumbnail.exists()){files.add(thumbnail);row.put("gameThumbnailFile",thumbnail.getName()).put("thumbnailArchive",10);}rows.put(row);}}catch(JSONException e){throw new IOException(e);}}
        try(ZipOutputStream zip=new ZipOutputStream(output)){zip.putNextEntry(new ZipEntry("index.json"));zip.write(rows.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();byte[] buffer=new byte[65536];for(File f:files){zip.putNextEntry(new ZipEntry(f.getName()));try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1)zip.write(buffer,0,n);}zip.closeEntry();}}
    }
    private void write(List<Photo> rows)throws IOException{AtomicFile target=new AtomicFile(index);FileOutputStream out=null;try{JSONArray data=new JSONArray();for(Photo p:rows)data.put(json(p));out=target.startWrite();out.write(data.toString().getBytes(StandardCharsets.UTF_8));target.finishWrite(out);}catch(Exception e){if(out!=null)target.failWrite(out);throw new IOException("Gallery update failed; previous index preserved",e);}}
    private static JSONObject json(Photo p)throws JSONException{return new JSONObject().put("id",p.id()).put("species",p.species()).put("stars",p.stars()).put("course",p.course()).put("form",p.form()).put("score",p.score()).put("medal",p.medal()).put("components",new JSONArray(p.components())).put("tags",p.tags()).put("favorite",p.favorite()).put("created",p.created()).put("source",p.source()).put("linkedHash",p.linkedHash());}
    private static Photo read(JSONObject r)throws JSONException{List<Integer> parts=new ArrayList<>();JSONArray a=r.getJSONArray("components");for(int i=0;i<a.length();i++)parts.add(a.getInt(i));return new Photo(r.getString("id"),r.getInt("species"),r.getInt("stars"),r.getString("course"),r.optString("form"),r.getInt("score"),r.getString("medal"),List.copyOf(parts),r.getString("tags"),r.getBoolean("favorite"),r.getLong("created"),r.getString("source"),r.optString("linkedHash"));}
}
