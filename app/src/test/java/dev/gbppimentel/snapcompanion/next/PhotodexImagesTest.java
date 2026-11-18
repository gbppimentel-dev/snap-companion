package dev.gbppimentel.snapcompanion.next;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
public class PhotodexImagesTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 private PersonalGallery.Photo photo(String hash)throws Exception{String id=UUID.randomUUID().toString();try(FileOutputStream out=new FileOutputStream(new File(tmp.getRoot(),id))){out.write(1);}return new PersonalGallery.Photo(id,810,1,"","",4200,"Diamond",List.of(),"",false,0,"fixture",hash);}
 private File file(PersonalGallery.Photo p){return new File(tmp.getRoot(),p.id());}
 @Test public void indexIsBoundToCurrentSlotsAndOwnerWithoutRemovingHistory()throws Exception{var slot=new EdenPhotodex.Photo(810,1,4200,"Diamond",5);String identity=AutoGallery.identity("owner",slot);var older=photo(AutoGallery.identity("owner",new EdenPhotodex.Photo(810,1,4100,"Diamond",4)));var foreign=photo(AutoGallery.identity("other",slot));var current=photo(identity);var duplicate=photo(identity);AtomicInteger checks=new AtomicInteger();var result=PhotodexImages.current(List.of(slot),"owner",List.of(older,foreign,current,duplicate),p->{checks.incrementAndGet();return file(p);});assertEquals(1,result.images().size());assertEquals(1,result.files().size());assertEquals(current.id(),result.images().get(identity).id());assertEquals(1,checks.get());for(var p:List.of(older,foreign,current,duplicate))assertTrue(file(p).isFile());}
 @Test public void missingImageRemainsPendingAndAnExactReadableDuplicateCanRecover()throws Exception{var slot=new EdenPhotodex.Photo(810,1,4200,"Diamond",5);String hash=AutoGallery.identity("owner",slot);var absent=photo(hash);assertTrue(file(absent).delete());assertTrue(PhotodexImages.current(List.of(slot),"owner",List.of(absent),this::file).images().isEmpty());var intact=photo(hash);assertEquals(intact,PhotodexImages.current(List.of(slot),"owner",List.of(absent,intact),this::file).images().get(hash));}
 @Test public void emptyCollectionOrUnknownOwnerDoesNotProbeHistoricalFiles()throws Exception{var slot=new EdenPhotodex.Photo(810,1,4200,"Diamond",5);var old=photo(AutoGallery.identity("owner",slot));for(var result:List.of(PhotodexImages.current(List.of(),"owner",List.of(old),p->{fail();return null;}),PhotodexImages.current(List.of(slot),"",List.of(old),p->{fail();return null;})))assertTrue(result.files().isEmpty());}
}
