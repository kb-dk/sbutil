package dk.statsbiblioteket.util.watch;

import dk.statsbiblioteket.util.Strings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * An extension of {@link FolderWatcher} that will monitor all files in
 * directory, recursively, to a configurable depth.
 */
public class RecursiveFolderWatcher extends FolderWatcher {

    protected int depth;

    /**
     * Create a new recursive folder watcher recursing {@code depth} levels
     * into the directory tree of {@code watchedFolder}.
     *
     * If {@code depth==0} no recursion will happen, if it is {@code <0}
     * recursion will traverse the file hierarchy to any depth.
     * Otherwise if {@code depth >0} directory traversal will only happen
     * to the specified depth.
     *
     * @param watchedFolder the folder to watch for changes
     * @param pollInterval  how often, in seconds, to check for changes
     * @param grace         the grace period for addition notifications
     * @param depth         the depth in the directory structure to recurse to
     * @throws IOException on errors reading the filesystem
     */
    public RecursiveFolderWatcher(File watchedFolder, int depth,
                                  int pollInterval, int grace)
            throws IOException {
        super(watchedFolder, pollInterval, grace);
        this.depth = depth;

        /* We need to update the oldcontent because it will be calculated
         * with depth=0 in the suprt constructor. Damn Java constructors. */
        oldContent = getContent();
    }

    /**
     * Create a new recursive folder watcher recursing {@code depth} levels
     * into the directory tree of {@code watchedFolder}.
     *
     * If {@code depth==0} no recursion will happen, if it is {@code <0}
     * recursion will traverse the file hierarchy to any depth.
     * Otherwise if {@code depth >0} directory traversal will only happen
     * to the specified depth.
     *
     * @param watchedFolder the folder to watch for changes
     * @param pollInterval  how often, in seconds, to check for changes
     * @param depth         the depth in the directory structure to recurse to
     * @throws IOException on errors reading the filesystem
     */
    public RecursiveFolderWatcher(File watchedFolder, int depth,
                                  int pollInterval) throws IOException {
        super(watchedFolder, pollInterval);
        this.depth = depth;

        /* We need to update the oldcontent because it will be calculated
         * with depth=0 in the suprt constructor. Damn Java constructors. */
        oldContent = getContent();
    }

    @Override
    public List<File> getContent() {
        if (!watchedFolder.exists()) {
            log.trace("Watched folder '" + watchedFolder + "' does not exist");
            return null;
        }
        log.trace("Returning content of folder '" + watchedFolder + "'");
        File[] content = watchedFolder.listFiles();

        /* Keep a sorted set of the files */
        Set<File> fileBag = new TreeSet<File>();

        for (File f : content) {
            if (f.isDirectory()) {
                addContent(f, fileBag, depth);
            }

            fileBag.add(f);
        }

        if (log.isTraceEnabled()) {
            log.trace("Contents of " + watchedFolder + " to depth " + depth + ":\n"
                      + Strings.join(fileBag, "\n"));
        }

        return new ArrayList<File>(fileBag);
    }

    private void addContent(File dir, Set<File> fileBag, int depth) {
        if (depth == 0) {
            return;
        }

        if (!dir.isDirectory()) {
            return;
        }

        depth--;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                addContent(f, fileBag, depth);
            } else {
                fileBag.add(f);
            }
        }
    }
}
