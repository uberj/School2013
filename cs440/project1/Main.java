import java.io.*;
import java.nio.ByteBuffer;
import java.lang.Long;
import java.util.ArrayList;

import com.sleepycat.db.Cursor;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DatabaseEntry;
import com.sleepycat.db.LockMode;
import com.sleepycat.db.OperationStatus;
import com.sleepycat.db.SecondaryCursor;


public class Main {
    public static String dbName = "imdb";
    public static XMLFileBinding binding = new XMLFileBinding();
    public static Dbs dbs = new Dbs();
	public static OperationStatus ret = null; 
    public static XMLFile xml = null;

    public static byte[] getSizeByteArray(long sizeKey) {
        byte b[] = new byte[8];
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putLong(sizeKey);
        return b;
    }


    public static void populateDB() {
        File rootPath = new File("/scratch/cs440/imdb");
        ArrayList<File> paths = new ArrayList<File>();
        paths = FileData.walkPath(rootPath);
        try {
            dbs.setup(dbName);
        } catch (DatabaseException e){
            System.err.println("Caught Exception creating datatbase :");
            e.printStackTrace();
        }
        DatabaseEntry key = null;
        DatabaseEntry data = null;
        XMLFile found = null;
        try {
            for(File path:paths) {
                xml = new XMLFile(path);
                key = new DatabaseEntry(xml.getName().getBytes());
                data = new DatabaseEntry();
                binding.objectToEntry(xml, data);
                dbs.getDb().put(null, key, data);
            }
        } catch (DatabaseException e) {
            System.err.println("Caught DatabaseException during creation: ");
            e.printStackTrace();
        } catch (NullPointerException npe) {
            System.err.println("Null pointer exception during insertion");
            npe.printStackTrace();
        } finally {
		    dbs.close();
        }
    }


    public static ArrayList<XMLFile> imdbPointQuery(long fileSize) {
        ArrayList<XMLFile> foundEntries = new ArrayList<XMLFile>();
        byte sizeKeyb[] = getSizeByteArray(fileSize);
        try {
            dbs.setup(dbName);
       } catch (DatabaseException e){
           System.err.println("Caught Exception creating datatbase :");
           e.printStackTrace();
       }
        XMLFileBinding binding = new XMLFileBinding();
        SecondaryCursor secCursor = null;
        try {
            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundData = new DatabaseEntry();
            DatabaseEntry sizeKey = new DatabaseEntry(sizeKeyb);
            secCursor = dbs.getSecDb().openSecondaryCursor(null, null);
            while (secCursor.getNext(sizeKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				xml = (XMLFile) binding.entryToObject(foundData);
				if (xml.getSize() != fileSize) {
					break;
				}
                foundEntries.add(xml);
            }
        } catch (DatabaseException e) {
            System.err.println("Database Error: " + e.toString());
            e.printStackTrace();
        } catch (NullPointerException npe) {
			System.err.println("Record not found.");
			System.exit(1);
		} finally {
    		dbs.close();
        }
        return foundEntries;
    }


    public static XMLFile imdbPointQuery(String fileName) {
        XMLFile foundEntry = null;
        try {
            dbs.setup(dbName);
       } catch (DatabaseException e) {
           System.err.println("Caught Exception creating datatbase :");
           e.printStackTrace();
       }
        XMLFileBinding binding = new XMLFileBinding();
        Cursor cursor = null;
        try {
            DatabaseEntry foundData = new DatabaseEntry();
            DatabaseEntry nameKey = new DatabaseEntry(fileName.getBytes());
            dbs.getDb().get(null, nameKey, foundData, null);
			foundEntry =  (XMLFile) binding.entryToObject(foundData);
        } catch (DatabaseException e) {
            System.err.println("Database Error: " + e.toString());
            e.printStackTrace();
        } catch (NullPointerException npe) {
			System.err.println("Record not found.");
			System.exit(1);
		} finally {
		    dbs.close();
        }
        return foundEntry;
    }

    public static ArrayList<XMLFile> imdbRangeQuery(String fileNameMin, String fileNameMax) {
		ArrayList<XMLFile> foundEntries = new ArrayList<XMLFile>();
        long max = Long.valueOf(fileNameMax.replaceAll(".xml", ""));
        long current = 0;
        DatabaseEntry foundData = new DatabaseEntry();
        DatabaseEntry nameKey = new DatabaseEntry(fileNameMin.getBytes());
        try {
			dbs.setup(dbName);
		} catch (DatabaseException e) {
			System.err.println("Caught Exception creating database :");
			e.printStackTrace();
	    }
		XMLFileBinding binding = new XMLFileBinding();
		Cursor cursor = null;
        try {
            cursor = dbs.getDb().openCursor(null, null);
	    	ret = cursor.getSearchKeyRange(nameKey, foundData, LockMode.DEFAULT);
	    	xml = (XMLFile) binding.entryToObject(foundData);
            current = Long.valueOf(xml.getName().replaceAll(".xml", ""));
    		while (ret == OperationStatus.SUCCESS && current <= max) {
                foundEntries.add(xml);
                ret = cursor.getNext(nameKey, foundData, LockMode.DEFAULT);
                xml = (XMLFile) binding.entryToObject(foundData);
                current = Long.valueOf(xml.getName().replaceAll(".xml", ""));
            }
	    } catch (DatabaseException e) {
            System.err.println("Caught Database Exception:");
        } catch (NullPointerException npe) {
            System.err.println("Caught Null Pointer Exception:");
        } finally {
            dbs.close();
        }
        return foundEntries;
    }


    public static ArrayList<XMLFile> imdbPointQuery(long fileSizeMin, long fileSizeMax) {
        ArrayList<XMLFile> foundEntries = new ArrayList<XMLFile>();
        byte sizeKeyb[] = getSizeByteArray(fileSizeMin);

        DatabaseEntry foundKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();
        DatabaseEntry sizeKey = new DatabaseEntry(sizeKeyb);
        try {
            dbs.setup(dbName);
       } catch (DatabaseException e){
           System.err.println("Caught Exception creating datatbase :");
           e.printStackTrace();
       }
        XMLFileBinding binding = new XMLFileBinding();
        SecondaryCursor secCursor = null;
        try {
            secCursor = dbs.getSecDb().openSecondaryCursor(null, null);
            ret = secCursor.getSearchKeyRange(sizeKey, foundKey, foundData, LockMode.DEFAULT);
            xml = (XMLFile) binding.entryToObject(foundData);
            while (ret == OperationStatus.SUCCESS && xml.getSize() <= fileSizeMax) {
                foundEntries.add(xml);
                ret = secCursor.getNext(sizeKey, foundKey, foundData, LockMode.DEFAULT);
                xml = (XMLFile) binding.entryToObject(foundData);
            }
        } catch (DatabaseException e) {
            System.err.println("Database Error: " + e.toString());
            e.printStackTrace();
        } catch (NullPointerException npe) {
            System.err.println("Record not found.");
            System.exit(1);
        } finally {
            dbs.close();
        }
        return foundEntries;
    }


    public static void main(String[] args) {

        if(args.length < 1) {
			System.out.println("Insufficient args. . ERROR!");
			System.exit(1);
		}

        switch(Integer.parseInt(args[0])) {

            case 1:
                System.out.println("Populating database with XML files. . .");
                populateDB();
                break;
            case 2:
                System.out.println("Performing point query on file " + args[1]);
                imdbPointQuery(args[1]);
                break;
            case 3:
                System.out.println("Performing range query over files fom " + args[1] + " - " + args[2]);
                // TODO: Add range query function
                break;
            case 4:
                System.out.println("Performing point query on file size " + args[1]);	
                imdbPointQuery(args[1]);
                break;
            case 5:
                System.out.println("Performing range query over file size from " + args[1] + " - " + args[2]);
                // TODO: Add range query function
                break;
            case 6:
                System.out.println("Performing range query over file name and size from files " + args[1] + " - " + args[2] + " and size " + args[3] + " - "  + args[4]);

            default:
                System.out.println("Invalid query ty.");
        }
    }
}
