package org.agrsw.mavenreleaser;

import java.io.File;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public  class GITClient {
	
	public static String getResource(String path,String resource) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
        
		try {
		    Git git = Git.open(new File(path));
		    ObjectId lastCommitId = git.getRepository().resolve(Constants.HEAD);
		    RevWalk revWalk = new RevWalk(git.getRepository());
			RevCommit commit = revWalk.parseCommit(lastCommitId);
			RevTree tree = commit.getTree();
			
			TreeWalk treeWalk = new TreeWalk(git.getRepository());
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathFilter.create(resource));
			if (!treeWalk.next()) {
				revWalk.close();
		        treeWalk.close();
		        return "";
			}
			 ObjectId objectId = treeWalk.getObjectId(0);
	         ObjectLoader loader = git.getRepository().open(objectId);
	
	         // and then one can the loader to read the file
	         loader.copyTo(stream);
	         revWalk.close();
	         treeWalk.close();
	         return new String(stream.toByteArray());
	         
		}catch (Exception e) {
			return "";
		}
         
	}
	
}
