/**
 *
 */
package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.rms.hibernate.model.RepoItemMetadata;

import java.util.List;
import java.util.Map;

/**
 * This interface declared methods to be implemented by builtin repository searchers
 * @author nnallagatla
 */
public interface IRMSRepositorySearcher {

    /**
     * Adds a new item to the index
     * @param repoItem
     */
    void addRepoItem(StoreItem repoItem) throws RMSRepositorySearchException;

    /**
     * Get an existing item
     * @param repoId
     */
    StoreItem getRepoItem(String repoId, String filePath) throws RMSRepositorySearchException;

    /**
     * Adds item list to the index
     * @param repoItems
     */
    void addRepoItemList(List<RepoItemMetadata> repoItems) throws RMSRepositorySearchException;

    List<StoreItem> listRepoItems(String repoId, String parentPath) throws RMSRepositorySearchException;

    /**
     * Deletes an existing item from index
     * @param repoId
     * @param filePaths
     */
    void deleteRepoItems(String repoId, List<String> filePaths) throws RMSRepositorySearchException;

    void updateRepoItems(String repoId, List<String> filePaths) throws RMSRepositorySearchException;

    void updateOrDeleteRepoItems(String repoId, List<String> filePaths) throws RMSRepositorySearchException;

    /**
     * searches index for searchString and returns matches
     * @param repoId
     * @param searchString
     * @return
     * @throws RMSRepositorySearchException
     */
    List<StoreItem> search(String repoId, String searchString) throws RMSRepositorySearchException;

    /**
     * searches specific path for item and returns single match if found
     * @param repoId
     * @param searchPath
     * @return boolean indicating whether path found in repo
     * @throws RMSRepositorySearchException
     */
    boolean pathExists(String repoId, String searchPath) throws RMSRepositorySearchException;

    /**
     * searches specific path for item and returns pathDisplay
     * @param path
     * @throws RMSRepositorySearchException
     */
    String getDisplayPath(String path, String repoId) throws RMSRepositorySearchException;

    /**
     * searches pathList for items and returns pathDisplayList
     * @param pathList
     * @throws RMSRepositorySearchException
     */
    Map<String, String> getDisplayPathList(List<String> pathList, String repoId) throws RMSRepositorySearchException;

}
