package dev.gbppimentel.snapcompanion;
import java.io.File;
import java.util.*;
import java.util.function.Function;

/** Index only current official slots. Historical/private records remain untouched on disk. */
final class PhotodexImages {
    record Index(Map<String,PersonalGallery.Photo> images,Map<String,File> files){}
    static Index current(Collection<EdenPhotodex.Photo> slots,String user,List<PersonalGallery.Photo> cached,Function<PersonalGallery.Photo,File> resolve){
        Set<String> needed=new HashSet<>();if(!user.isEmpty())for(var slot:slots)needed.add(AutoGallery.identity(user,slot));
        if(needed.isEmpty())return new Index(Map.of(),Map.of());
        Map<String,PersonalGallery.Photo> images=new HashMap<>();Map<String,File> files=new HashMap<>();
        for(var photo:cached){
            if(!needed.contains(photo.linkedHash())||images.containsKey(photo.linkedHash()))continue;
            File file=resolve.apply(photo);if(!file.isFile()||file.length()==0)continue;
            images.put(photo.linkedHash(),photo);files.put(photo.id(),file);
            if(images.size()==needed.size())break;
        }
        return new Index(Map.copyOf(images),Map.copyOf(files));
    }
}
