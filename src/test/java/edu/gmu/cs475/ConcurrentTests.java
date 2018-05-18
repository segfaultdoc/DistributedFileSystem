package edu.gmu.cs475;

import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.gmu.cs475.internal.Command;
import edu.gmu.cs475.struct.ITag;
import edu.gmu.cs475.struct.ITaggedFile;
import edu.gmu.cs475.struct.NoSuchTagException;
import edu.gmu.cs475.internal.DeadlockDetectorAndRerunRule;

public class ConcurrentTests {
	/* Leave at 6 please */
	public static final int N_THREADS = 6;

	@Rule
	public DeadlockDetectorAndRerunRule timeout = new DeadlockDetectorAndRerunRule(10000);

	/**
	 * Use this instance of fileManager in each of your tests - it will be
	 * created fresh for each test.
	 */
	AbstractFileTagManager fileManager;

	/**
	 * Automatically called before each test, initializes the fileManager
	 * instance
	 */
	@Before
	public void setup() throws IOException {
		fileManager = new FileTagManager();
		fileManager.init(Command.listAllFiles());
	}

	/**
	 * Create N_THREADS threads, with half of the threads adding new tags and
	 * half iterating over the list of tags and counting the total number of
	 * tags. Each thread should do its work (additions or iterations) 1,000
	 * times. Assert that the additions all succeed and that the counting
	 * doesn't throw any ConcurrentModificationException. There is no need to
	 * make any assertions on the number of tags in each step.
	 */
	@Test
	public void testP1AddAndListTag() {
        Thread[] threads = new Thread[N_THREADS]; 
        AtomicInteger nExceptions = new AtomicInteger(0);
        AtomicInteger numTags = new AtomicInteger(0);
        AtomicInteger exception = new AtomicInteger(0);

        for(int i = 0; i <N_THREADS; i++) { 
            final int start = i*1000;
            final int end = start+1000;
            final int tNum = i; Runnable r = () -> {
          
            try{
               // System.out.println("First Try Hello from thread "+ tNum + "!");
                for(int j =start; j<end; j++){
                    if(tNum %2 == 0){
                        try{
                        this.fileManager.addTag("1" + j);
                        }
                        catch(Throwable t){
                            t.printStackTrace();
                            exception.incrementAndGet();
                        }                        
                    }
                    else{
                        try{
                            Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
                            while(itr.hasNext()){         
                                numTags.incrementAndGet();
                                itr.next();
                            }
                        }
                        catch (Throwable t){
                            t.printStackTrace();
                            exception.incrementAndGet();
                        }
                    }                
                }
            } catch(ConcurrentModificationException e){
                exception.incrementAndGet();        
            }     
        };
        threads[i] = new Thread(r);
        threads[i].start();
        }
        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }
        assertEquals("Expected no exceptions", 0, exception.get()); 
	}

	/**
	 * Create N_THREADS threads, and have each thread add 1,000 different tags;
	 * assert that each thread creating a different tag succeeds, and that at
	 * the end, the list of tags contains all of tags that should exist
	 */
	@Test
	public void testP1ConcurrentAddTagDifferentTags() {
        Thread[] threads = new Thread[N_THREADS]; 
        AtomicInteger nExceptions = new AtomicInteger(0);
        //AtomicInteger actualTags = new AtomicInteger(0);
        //AtomicInteger expectedTags = new AtomicInteger(6000);
        AtomicInteger exception = new AtomicInteger(0);
        String [] expectedTags = new String[6000];
        for(int i = 0; i <N_THREADS; i++) { 
            final int start = i*1000;
            final int end = start+1000;
            final int tNum = i; Runnable r = () -> {
            
                try{
                    //System.out.println("First Try Hello from thread "+ tNum + "!");
                    for(int j =start; j<end; j++){
                        this.fileManager.addTag("1" + j);                 
                        expectedTags[j] = "1"+j;
                        Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
                        while(itr.hasNext()){         
                            //actualTags.incrementAndGet();
                            itr.next();
                       }   
                    }                
                } catch(ConcurrentModificationException e){
                    exception.incrementAndGet();
                    assertEquals("Concurrent Mod Exceptions", false, true);        
                } catch(Throwable t){
                    t.printStackTrace();
                }
                    
            };
              
        
        threads[i] = new Thread(r);
        threads[i].start();
        }
       

        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }

         Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
        int count =0;
        ArrayList <Tag> actual = new ArrayList<Tag>();    
        while(itr.hasNext()){
            Tag tag = (Tag) itr.next();
            actual.add(tag);               
        }
        boolean contains = false;
        for(int i =0; i <expectedTags.length; i++){
            for(Tag t: actual){
                if(t.getName().equals(expectedTags[i])){
                    contains =true;
                    break;
                }
                
            }
            assertEquals("Tags", true, contains);
            contains = false;
        }
        //String fail = ("Expected " + expectedTags.get() + " tags, but got " + actualTags.get());
        //assertEquals(fail, actualTags.get(), expectedTags.get()); 
	}


	

	/**
	 * Create N_THREADS threads. Each thread should try to add the same 1,000
	 * tags of your choice. Assert that each unique tag is added exactly once
	 * (there will be N_THREADS attempts to add each tag). At the end, assert
	 * that all tags that you created exist by iterating over all tags returned
	 * by listTags()
	 */
	@Test
	public void testP1ConcurrentAddTagSameTags() {
        Thread[] threads = new Thread[N_THREADS]; 
        AtomicInteger nExceptions = new AtomicInteger(0); 
        AtomicInteger exception = new AtomicInteger(0);
        String [] expectedTags = new String[6];
        for(int i = 0; i <N_THREADS; i++) { 
            final int start = i*1000;
            final int end = start+1000;
            final int tNum = i; Runnable r = () -> {
            
                try{
                    //System.out.println("First Try Hello from thread "+ tNum + "!");
                    for(int j =start; j<end; j++){
                        this.fileManager.addTag("1" + tNum);                 
                        expectedTags[tNum] = "1"+ tNum;
                        Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
                        while(itr.hasNext()){         
                            //actualTags.incrementAndGet();
                            itr.next();
                       }   
                    }                
                } catch(ConcurrentModificationException e){
                    exception.incrementAndGet();
                    assertEquals("Concurrent Mod Exceptions", false, true);        
                } catch(Throwable t){
                    t.printStackTrace();
                }
                    
            };
              
        
        threads[i] = new Thread(r);
        threads[i].start();
        }
       

        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }

         Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
        int count =0;
        ArrayList <Tag> actual = new ArrayList<Tag>();    
        while(itr.hasNext()){
            Tag tag = (Tag) itr.next();
            actual.add(tag);               
        }
        boolean contains = false;
        for(int i =0; i <expectedTags.length; i++){
            for(Tag t: actual){
                if(t.getName().equals(expectedTags[i])){
                    contains =true;
                    break;
                }
                
            }
            assertEquals("Tags", true, contains);
            contains = false;
        }
        //String fail = ("Expected " + expectedTags.get() + " tags, but got " + actualTags.get());
        assertEquals("tag number differs", expectedTags.length, actual.size()); 
       /* for(int i=0; i < actual.size(); i++){
            for(int j = 0; j<actual.size(); j++){
                if(j != i && actual.get(i).getName().equals(actual.get(j).getName())){
                    assertEquals("Duplicates", false, true);
                }
            }
        
        }*/
	}
	

	/**
	 * Create 1000 tags. Save the number of files (returned by listFiles()) to a
	 * local variable.
	 * 
	 * Then create N_THREADS threads. Each thread should iterate over all files
	 * (from listFiles()). For each file, it should select a tag and random from
	 * the list returned by listTags(). Then, it should tag that file with that
	 * tag. Then (regardless of the tagging sucedding or not), it should pick
	 * another random tag, and delete it. You do not need to care if the
	 * deletions pass or not either.
	 * 
	 * 
	 * At the end (once all threads are completed) you should check that the
	 * total number of files reported by listFiles matches what it was at the
	 * beginning. Then, you should list all of the tags, and all of the files
	 * that have each tag, making sure that the total number of files reported
	 * this way also matches the starting count. Finally, check that the total
	 * number of tags on all of those files matches the count returned by
	 * listTags.
	 * 
	 */
	@Test
	public void testP2ConcurrentDeleteTagTagFile() throws Exception {
        //AtomicInteger nFilesBefore = new AtomicInteger(0);
        //AtomicInteger nFilesAfter = new AtomicInteger(0);
        int nFilesBefore = 0;
        int nFilesAfter = 0;
        int nFilesTagged = 0;
        int nTags = 0;
        Thread [] threads = new Thread[N_THREADS];
        for(int i =0; i<1000; i++){
            this.fileManager.addTag(""+i);
        }
        Iterator <? extends ITaggedFile> itrBefore = this.fileManager.listAllFiles().iterator();
        while(itrBefore.hasNext()){
            itrBefore.next();
            nFilesBefore++;
        }
        Random rand = new Random();
        for(int i = 0; i <N_THREADS; i++) { 
            Iterator <? extends ITaggedFile> itrThreads = this.fileManager.listAllFiles().iterator();
            final int start = i*1000;
            final int end = start+1000;
            final int tNum = i; Runnable r = () -> {
            
                try{

                    while(itrThreads.hasNext()){
                        ITaggedFile f = (ITaggedFile) itrThreads.next();
                        int k = rand.nextInt(1000);
                        this.fileManager.tagFile(f.getName(), ""+k);
                        k = rand.nextInt(1000); 
                        this.fileManager.deleteTag(""+k);
                    }
                } catch(ConcurrentModificationException e){
                    e.printStackTrace();
                }
                catch (Throwable t){
                    t.printStackTrace();
                }
            };
        
            threads[i] = new Thread(r);
            threads[i].start();
        }
       

        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }

        Iterator <? extends ITaggedFile> itrAfter = this.fileManager.listAllFiles().iterator();
        HashSet <String> taggedFiles = new HashSet<String>();
        while(itrAfter.hasNext()){
            ITaggedFile f = (ITaggedFile) itrAfter.next();
            taggedFiles.add(f.getName());
            nFilesAfter++;
        }
        assertEquals("List out put doesn't match", nFilesAfter, nFilesBefore);
        Iterator <? extends ITag> itrTags = this.fileManager.listTags().iterator();
        HashSet <String> taggedFiles2 = new HashSet<String>();
        while(itrTags.hasNext()){
            ITag t = (ITag) itrTags.next();
            nTags++;
        }
        HashSet<String> tagCheck = new HashSet<String>();
        for(String f: taggedFiles){
            Iterator <? extends ITag> thisFilesTags = this.fileManager.getTags(f).iterator();
            
            while(thisFilesTags.hasNext()){
                ITag t = (ITag)thisFilesTags.next();
               // if(t !=null && !t.toString().equals("untagged")){
                if(t != null){
                    taggedFiles2.add(f.toString());
                    tagCheck.add(t.toString());        
                } 
                    
            }
                
       }
       assertEquals("Total number of files not equal", taggedFiles2.size(), nFilesBefore);
       assertEquals("Total number of Tags not equal", nTags, tagCheck.size());
        
        
	}

	/**
	 * Create a tag. Add each tag to every file. Then, create N_THREADS and have
	 * each thread iterate over all of the files returned by listFiles(),
	 * calling removeTag on each to remove that newly created tag from each.
	 * Assert that each removeTag succeeds exactly once.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testP2RemoveTagWhileRemovingTags() throws Exception {
        this.fileManager.addTag("1");
        AtomicInteger numFiles = new AtomicInteger(0);
        AtomicInteger nSuccess = new AtomicInteger(0);
        Thread [] threads = new Thread[N_THREADS];
        Iterator <? extends ITaggedFile> files = this.fileManager.listAllFiles().iterator();
        while(files.hasNext()){
            ITaggedFile f = (ITaggedFile) files.next();
            this.fileManager.tagFile(f.toString(), "1");
            numFiles.incrementAndGet();
        }

        for(int i = 0; i <N_THREADS; i++) { 
            Iterator <? extends ITaggedFile> itrThreads = this.fileManager.listAllFiles().iterator();
                
            final int tNum = i; Runnable r = () -> {
            
                try{
                    while(itrThreads.hasNext()){
                        ITaggedFile f = (ITaggedFile) itrThreads.next();
                        try{
                            this.fileManager.removeTag(f.toString(), "1");
                            nSuccess.incrementAndGet();
                        } catch(ConcurrentModificationException e){
                               
                                assertEquals("Concurrent Mod Exceptions", false, true);        
                        } catch(Throwable t){
                            t.printStackTrace();
                        }

                    }
                     
                }
                catch(ConcurrentModificationException e){
                        
                        assertEquals("Concurrent Mod Exceptions", false, true);        
                   } catch(Throwable t){
                        t.printStackTrace();
                    }                   
            };
                      
        threads[i] = new Thread(r);
        threads[i].start();
        
        }
     
        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }
        assertEquals("Incorrect remove", nSuccess.get(), numFiles.get());
	}

	/**
	 * Create N_THREADS threads and N_THREADS/2 tags. Half of the threads will
	 * attempt to tag every file with (a different) tag. The other half of the
	 * threads will count the number of files currently having each of those
	 * N_THREADS/2 tags. Assert that there all operations succeed, and that
	 * there are no ConcurrentModificationExceptions. Do not worry about how
	 * many files there are of each tag at each step (no need to assert on
	 * this).
	 */
	@Test
	public void testP2TagFileAndListFiles() throws Exception {
        Thread[] threads = new Thread[N_THREADS];
        ArrayList <String> t = new ArrayList <String> (); 
        AtomicInteger numFiles = new AtomicInteger(0);
        AtomicInteger exception = new AtomicInteger(0);
        for(int i=0; i<(N_THREADS/2); i++){
            this.fileManager.addTag(""+i);
            t.add(""+i);
        }
        
        for(int i = 0; i <N_THREADS; i++) { 
            Iterator <? extends ITaggedFile> itrThreads = this.fileManager.listAllFiles().iterator();
            final int tNum = i; Runnable r = () -> {            
                try{
                    
                    if(tNum%2==0){
                        while(itrThreads.hasNext()){
                            ITaggedFile f = (ITaggedFile) itrThreads.next();
                            for(String tag: t){
                                if(this.fileManager.tagFile(f.toString(), tag) != false){
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        while(itrThreads.hasNext()){
                            ITaggedFile f = (ITaggedFile) itrThreads.next();
                            Iterator <? extends ITag> itr = this.fileManager.getTags(f.toString()).iterator();
                            while(itr.hasNext()){
                               ITag tag1 = itr.next();
                               if(t.contains(tag1.toString())){
                                   numFiles.incrementAndGet();
                                   break;
                               }
                            }
                        }
                    }
                   } catch(ConcurrentModificationException e){
                         assertEquals("Concurrent Mod Exceptions", false, true);        
                   } catch(Throwable tc){
                        exception.incrementAndGet();

                        tc.printStackTrace();
                        assertEquals("Exception", false,true);
                    }                   
            };
                      
        threads[i] = new Thread(r);
        threads[i].start();
        
        }
     
        for (Thread ter: threads){
            try{
                ter.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }



	}

	/**
	 * Create N_THREADS threads, and have each try to echo some text into all of
	 * the files using echoAll. At the end, assert that all files have the same
	 * text.
	 */
	@Test
	public void testP3ConcurrentEchoAll() throws Exception {
        Thread[] threads = new Thread[N_THREADS]; 
        AtomicInteger nExceptions = new AtomicInteger(0); 
        AtomicInteger exception = new AtomicInteger(0);
        this.fileManager.addTag("ech");
        Iterator <? extends ITaggedFile> fileItr = this.fileManager.listAllFiles().iterator();
        while(fileItr.hasNext()){
            ITaggedFile f = (ITaggedFile) fileItr.next();
            this.fileManager.tagFile(f.getName(), "ech");               
        }
        for(int i = 0; i <N_THREADS; i++) { 
            final int tNum = i; Runnable r = () -> {            
                try{
                    this.fileManager.echoToAllFiles("ech", "hello");
                   } catch(ConcurrentModificationException e){
                        exception.incrementAndGet();
                        assertEquals("Concurrent Mod Exceptions", false, true);        
                   } catch(Throwable t){
                        t.printStackTrace();
                    }                   
            };
                      
        threads[i] = new Thread(r);
        threads[i].start();
        
        }
     
        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }

        Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
        int count =0;
        ArrayList <ITaggedFile> actual = new ArrayList<ITaggedFile>();    
        while(itr.hasNext()){
            ITaggedFile file = (ITaggedFile) itr.next();
            actual.add(file);               
        }
        ArrayList <String> content = new ArrayList<String> ();
        boolean contains = false;
        for(ITaggedFile f: actual){
            
            BufferedReader reader = new BufferedReader(new FileReader(f.getName()));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = reader.readLine()) != null){
                sb.append(line);
            }
            content.add(sb.toString());
          
        } 
       String file1 = content.get(0);        
       for(int i =0;  i<content.size(); i++){
           if(!file1.equals(content.get(i))){
                assertEquals("file contents are different", true, false);
           }
       }
       
        //String fail = ("Expected " + expectedTags.get() + " tags, but got " + actualTags.get());
         

	}

	/**
	 * Create N_THREADS threads, and have half of those threads try to echo some
	 * text into all of the files. The other half should try to cat all of the
	 * files, asserting that all of the files should always have the same
	 * content.
	 */
	@Test
	public void testP3EchoAllAndCatAll() throws Exception {
        Thread[] threads = new Thread[N_THREADS]; 
        AtomicInteger nExceptions = new AtomicInteger(0); 
        AtomicInteger exception = new AtomicInteger(0);
        this.fileManager.addTag("echos");
        Iterator <? extends ITaggedFile> fileItr = this.fileManager.listAllFiles().iterator();
        while(fileItr.hasNext()){
            ITaggedFile f = (ITaggedFile) fileItr.next();
            this.fileManager.tagFile("echos", f.getName());               
        }
        for(int i = 0; i <N_THREADS; i++) { 
            final int tNum = i; Runnable r = () -> {            
                try{
                    this.fileManager.echoToAllFiles("echos", "hello");
                   } catch(ConcurrentModificationException e){
                        exception.incrementAndGet();
                        assertEquals("Concurrent Mod Exceptions", false, true);        
                   } catch(Throwable t){
                        t.printStackTrace();
                    }                   
            };
                      
        threads[i] = new Thread(r);
        threads[i].start();
        
        }
     
        for (Thread t: threads){
            try{
                t.join();
            } catch (InterruptedException e){
                e.printStackTrace();
                fail("Thread died");
            }
        }

        Iterator <? extends ITag> itr = this.fileManager.listTags().iterator();
        int count =0;
        ArrayList <ITaggedFile> actual = new ArrayList<ITaggedFile>();    
        while(itr.hasNext()){
            ITaggedFile file = (ITaggedFile) itr.next();
            actual.add(file);               
        }
        ArrayList <String> content = new ArrayList<String> ();
        boolean contains = false;
        for(ITaggedFile f: actual){
            BufferedReader reader = new BufferedReader(new FileReader(f.getName()));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = reader.readLine()) != null){
                sb.append(line);
            }
            content.add(sb.toString());
          
        } 
       String file1 = content.get(0);        
       for(int i =0;  i<content.size(); i++){
           if(!file1.equals(content.get(i))){
                assertEquals("file contents are different", true, false);
           }
       }
	}
}
