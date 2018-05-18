package edu.gmu.cs475;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import edu.gmu.cs475.struct.ITag;
import edu.gmu.cs475.struct.ITaggedFile;
import edu.gmu.cs475.struct.NoSuchTagException;
import edu.gmu.cs475.struct.TagExistsException;
import java.util.concurrent.locks.*;
import java.util.*;
import java.lang.StringBuilder;
import java.io.*;


public class FileTagManager extends AbstractFileTagManager {

    
    // mapping string "tagnames" to tags for easier access
    private HashMap <String, Tag> tagMap = new HashMap<String, Tag>(); 
    // set of unique files
    private HashSet <TaggedFile> fileSet = new HashSet<TaggedFile>();
    // private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

	@Override
	public Iterable<? extends ITag> listTags() {
		// TODO Auto-generated method stub
        this.lock.readLock().lock();
        Collection <Tag> tags = null;
        try{ 
		    tags =  new ArrayList<Tag> (this.tagMap.values());
           // sleep(1);
            return tags;
        } finally{
            lock.readLock().unlock();
            
        }
            //return tags;
	}

	@Override
	public ITag addTag(String name) throws TagExistsException {
		// TODO Auto-generated method stub
        
        this.lock.writeLock().lock();
        try{
            // if tag does not exist throw exception 
           if(!(this.tagDNE(name))){
                throw new TagExistsException();
            }
            else{
                Tag newTag = new Tag (name);                        
                this.tagMap.put(name, newTag);       
                return newTag;              
            }
        }
        finally{
            lock.writeLock().unlock();
        }
      

        
	}

	@Override
	public synchronized ITag editTag(String oldTagName, String newTagName) throws TagExistsException, NoSuchTagException {
		// TODO Auto-generated method stub
        // if tag does not exist throw exception
        if(this.tagDNE(oldTagName)){
            throw new NoSuchTagException();
        }
        else if(!(this.tagDNE(newTagName))){
            throw new TagExistsException();
        }
        else{
            Tag oldTag = this.getTag(oldTagName); 
            oldTag.setName(newTagName);
            return oldTag;
        }
        
	}

    /**
     *
     * Returns the value mapped to the
     * specified string key
     *
     */
    public Tag getTag(String tagName){
        return this.tagMap.get(tagName);
    }
    
    /**
     * Calls javas hashmap function to
     * to check if the given is contained
     * True if tag does not exist
     */
    public synchronized boolean tagDNE(String tagName){
        return !(this.tagMap.containsKey(tagName));       
    }

    /**
     * Calls javas hashset contains() method
     * to check if the given string exists
     * True if file does not exitst
     */
    public synchronized boolean fileDNE(String fileName){;
        for(TaggedFile f: this.fileSet){
            if(f.toString().equals(fileName)){  return false; }
        }

        return true;
                  
    }


    /**
     * checks to see if the given tag
     * is assigned to any files getting a collection
     * view of the file values.
     * 
     * 
     */
    public boolean isTagAssigned(String tagName){
        for(TaggedFile file: this.fileSet){
            if(file.hasTag(tagName)){
                return true;   
            }
        }
        return false;
    }

    /**
     *  returns file specified by filename
     *
     */
     public TaggedFile getFile(String fileName){
        for(TaggedFile f: this.fileSet){
            if(f.getName().equals(fileName)){
                return f;
            }           
        }
        return null;

     }

	@Override
	public ITag deleteTag(String tagName) throws NoSuchTagException, DirectoryNotEmptyException {
		// TODO Auto-generated method stub
        //synchronized(this.tagMap){
        this.lock.writeLock().lock();    
        try{
            if(this.tagDNE(tagName)){
                throw new NoSuchTagException();
            }
            else if(this.isTagAssigned(tagName)){
                throw new DirectoryNotEmptyException("This tag is assigned to a file");
            }
            else{
                Tag tag = this.getTag(tagName);
                this.tagMap.remove(tagName);
                return tag;
            }
        }
        finally{
            this.lock.writeLock().unlock();
        }
	
	}

	@Override
    /**
    * Initialize your FileTagManager with the starting set of files.
    * You do not need to persist tags on files from run to run.
    * Each file should start with the default tag "untagged"
    * @param files Starting list of files to consider
    */
	public void init(List<Path> files) {
		// TODO Auto-generated method stub
         
        TaggedFile taggedFile = null;        
        for(Path p: files){ 
            taggedFile = new TaggedFile(p);
            taggedFile.addTag("untagged");
            this.fileSet.add(taggedFile);    
        }
		
	}

	@Override
	public Iterable<? extends ITaggedFile> listAllFiles() {
		// TODO Auto-generated method stub
        synchronized(this.fileSet){ 
        return this.fileSet;
        }
	}

	@Override
	public Iterable<? extends ITaggedFile> listFilesByTag(String tag) throws NoSuchTagException {
		// TODO Auto-generated method stub
        synchronized(this.fileSet){
        if(this.tagDNE(tag) && !(tag.equals("untagged"))){
            throw new NoSuchTagException();
        }
        else{
            Set <TaggedFile> taggedSet = new HashSet <TaggedFile>();       
            TaggedFile tmp = null;
            for(TaggedFile file: this.fileSet){
                if(file.hasTag(tag)){
                    taggedSet.add(file);
                }

            }
		    return taggedSet;
        }
        }
	}

	@Override
	public boolean tagFile(String file, String tag) throws NoSuchFileException, NoSuchTagException {
		// TODO Auto-generated method stub
         this.lock.writeLock().lock();
         try{
            if(tag.equals("untagged")){ return false; }
            else if(this.tagDNE(tag)){
                throw new NoSuchTagException();
            }
            else if(this.fileDNE(file)){
                throw new NoSuchFileException("This file does not exist");
            }
            else{
                TaggedFile taggedFile = this.getFile(file);
                if(taggedFile.hasTag(tag)){  return false;  }
                else {  
                    if(taggedFile.hasTag("untagged")){
                        taggedFile.removeTag("untagged");
                    }
                    taggedFile.addTag(tag);
                    return true; 
                }
            }
        }
        finally{
            this.lock.writeLock().unlock();
        }
    
    }

	@Override
	public boolean removeTag(String file, String tag) throws NoSuchFileException, NoSuchTagException {
		// TODO Auto-generated method stub
        synchronized(this.tagMap){
        
            if(tag.equals("untagged")){ return false; }
            else if(this.tagDNE(tag)){ throw new NoSuchTagException(); }
       
            else if(this.fileDNE(file)){ throw new NoSuchFileException("This file does not exist"); }
           
            TaggedFile f = this.getFile(file);          
            if(f != null && f.hasTag(tag)){
                f.removeTag(tag);
                if(f.hasNoTags()){
                    f.addTag("untagged");
                }
                return true;
            }
            else{ return false; }            	   
        }       
    }
	@Override
	public Iterable<? extends ITag> getTags(String file) throws NoSuchFileException {
		// TODO Auto-generated method stub
		synchronized(this.fileSet){
            if(this.fileDNE(file)){
                throw new NoSuchFileException("This file does not exist");
            }
            else{
                TaggedFile taggedFile = this.getFile(file);
                return taggedFile.getTags();
            }
        }
	}

	@Override
	public String catAllFiles(String tag) throws NoSuchTagException, IOException {
		
        // TODO Auto-generated method stub
        StringBuilder sb = new StringBuilder();
        long stamp;

        if(this.tagDNE(tag) && !tag.equals("untagged")){ throw new NoSuchTagException(); }
        else{
          
            for(TaggedFile f: this.fileSet){
                if(f.hasTag(tag)){
                    stamp = this.lockFile(f.getName(), false);
                    try{ 
                    BufferedReader reader = new BufferedReader(new FileReader(f.getName()));
                    String line;
                    
                        while((line = reader.readLine()) != null){ 
                            sb.append(line);
                        }
                        reader.close();
                    } catch(IOException e){
                        e.printStackTrace();
                    }catch(Throwable t){
                        t.printStackTrace();
                    }
                    finally{
                            this.unLockFile(f.getName(),stamp, false);
                    }

                }
            }

        }
		return sb.toString();
	}

	@Override
	public void echoToAllFiles(String tag, String content) throws NoSuchTagException, IOException {
		// TODO Auto-generated method stub
        StringBuilder sb = new StringBuilder();
        long stamp;
        if(this.tagDNE(tag) && !tag.equals("untagged")){ throw new NoSuchTagException(); }
        else{
           
            for(TaggedFile f: this.fileSet){
                if(f.hasTag(tag)){
                    stamp = this.lockFile(f.getName(), true);
                    try{
                        BufferedWriter writer = new BufferedWriter(new FileWriter(f.getName()));
                        writer.write(content);
                        writer.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                    finally{
                        this.unLockFile(f.getName(),stamp, true);
                    }
                    
                }
            }
        }
	}

	@Override
	public long lockFile(String name, boolean forWrite) throws NoSuchFileException {
		// TODO Auto-generated method stub
        TaggedFile f =null;
        synchronized(this.fileSet){
            f = this.getFile(name);
        }
        long stamp;
        if(forWrite == true){
            stamp = f.getLock().writeLock();
        }
        else{
            stamp = f.getLock().readLock();
        }
		return stamp;
	}

	@Override
	public void unLockFile(String name, long stamp, boolean forWrite) throws NoSuchFileException {
		// TODO Auto-generated method stub
		TaggedFile f =null;
        synchronized(this.fileSet){
            f = this.getFile(name);
        }
        
        if(forWrite == true){
            f.getLock().unlockWrite(stamp);
        }
        else{
            f.getLock().unlockRead(stamp);
        }
		
	}

	


}
