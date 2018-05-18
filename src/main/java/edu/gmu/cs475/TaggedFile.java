package edu.gmu.cs475;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.concurrent.locks.StampedLock;

import edu.gmu.cs475.struct.ITaggedFile;

public class TaggedFile implements ITaggedFile {
	public HashMap<String, Tag> tags = new HashMap<String, Tag>();
	public StampedLock lock = new StampedLock();
	
	public StampedLock getLock() {
		return lock;
	}
	private Path path;
	public TaggedFile(Path path)
	{
		this.path = path;
	}
	@Override
	public String getName() {
		return path.toString();
	}
	@Override
	public String toString() {
		return getName();
	}

    public boolean addTag(String tagName){
        if(this.tags.containsKey(tagName)){
            return false;
        }
        else{
            this.tags.put(tagName, new Tag(tagName));
            return true;
        }
    }
    
    public Collection<Tag> getTags(){
        return this.tags.values();
    }

    public boolean hasTag(String tagName){
        return this.tags.containsKey(tagName);
    }
    
    public void removeTag(String tagName){
        long stamp = this.lock.writeLock();
        try{
            this.tags.remove(tagName);
        }
        finally { this.lock.unlockWrite(stamp); }

    }
    // returns true of no tags
    public boolean hasNoTags(){
        return this.tags.size()==0;
    }
}
