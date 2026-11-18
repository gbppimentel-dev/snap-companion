package dev.gbppimentel.snapcompanion.next;
import android.content.*;
import android.net.Uri;
import android.database.Cursor;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;
/** Narrow, read-only document-tree discovery. Ambiguous profile folders require a narrower selection. */
final class SaveDocuments {
    private record Directory(Uri uri,int depth){}
    static Uri metadata(ContentResolver resolver,Uri tree)throws IOException{
        ArrayDeque<Directory> directories=new ArrayDeque<>();Set<String> visited=new HashSet<>();List<Uri> found=new ArrayList<>();directories.add(new Directory(DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)),0));int entries=0;
        while(!directories.isEmpty()){
            Directory directory=directories.removeFirst();String id=DocumentsContract.getDocumentId(directory.uri());if(!visited.add(id))continue;Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
            try(Cursor cursor=resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
                if(cursor==null)throw new IOException("Provider did not expose the selected folder");while(cursor.moveToNext()){if(++entries>512)throw new IOException("Folder is too broad; select one Snap profile's save folder");String child=cursor.getString(0),name=cursor.getString(1),type=cursor.getString(2);Uri uri=DocumentsContract.buildDocumentUriUsingTree(tree,child);if(NewSnapSave.NAMES.get(2).equalsIgnoreCase(name))found.add(uri);else if(DocumentsContract.Document.MIME_TYPE_DIR.equals(type)&&directory.depth()<4&&(name.matches("[A-Fa-f0-9]{16,32}")||name.equalsIgnoreCase("save")||name.equalsIgnoreCase("user")||name.equalsIgnoreCase("nand")))directories.add(new Directory(uri,directory.depth()+1));}
            }
        }
        if(found.size()!=1)throw new IOException(found.isEmpty()?"No Snap Photodex file found. Choose its profile/title folder, or use an exported save ZIP.":"Multiple game profiles found. Select the folder for one profile.");return found.get(0);
    }
}
